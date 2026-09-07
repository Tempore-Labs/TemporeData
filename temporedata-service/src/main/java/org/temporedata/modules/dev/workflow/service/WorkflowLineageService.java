package org.temporedata.modules.dev.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.sql.ColumnLineage;
import org.temporedata.api.datasource.sql.Dialect;
import org.temporedata.api.dev.workflow.SqlParseRes;
import org.temporedata.modules.dev.workflow.entity.WorkflowColumnLineageEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowLineageEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowColumnLineageRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowLineageRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowRepository;
import org.temporedata.modules.dev.workflow.runner.SqlFirewall;
import org.temporedata.modules.dev.workflow.runner.SqlLineageParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Workflow task-level lineage (P1-5). Parses each SQL node of a workflow into
 * table reads/writes and exposes a combined task + table graph.
 */
@Slf4j
@Service
public class WorkflowLineageService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowLineageRepository lineageRepository;
    private final WorkflowColumnLineageRepository columnLineageRepository;
    private final SqlLineageParser sqlLineageParser;
    private final SqlFirewall sqlFirewall;
    private final ObjectMapper objectMapper;

    public WorkflowLineageService(WorkflowRepository workflowRepository,
                                  WorkflowLineageRepository lineageRepository,
                                  WorkflowColumnLineageRepository columnLineageRepository,
                                  SqlLineageParser sqlLineageParser,
                                  SqlFirewall sqlFirewall,
                                  ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.lineageRepository = lineageRepository;
        this.columnLineageRepository = columnLineageRepository;
        this.sqlLineageParser = sqlLineageParser;
        this.sqlFirewall = sqlFirewall;
        this.objectMapper = objectMapper;
    }

    /** Parse a SQL statement (IDE live analysis). */
    public SqlParseRes parseSql(String sql) {
        return sqlLineageParser.parse(sql);
    }

    /** Parse a SQL statement for a specific data source (IDE live analysis, dialect-aware). */
    public SqlParseRes parseSql(String sql, String datasourceType) {
        DatasourceType type = DatasourceType.of(datasourceType);
        return sqlLineageParser.parse(sql, type);
    }

    /** Firewall check (SQL-injection). Returns violations; empty list = safe. */
    public java.util.List<String> validateSql(String sql, String datasourceType) {
        return sqlFirewall.validate(sql, Dialect.from(DatasourceType.of(datasourceType)));
    }

    /** Re-extract per-node table dependencies for a workflow. */
    @Transactional
    public int sync(String workflowId) {
        WorkflowEntity wf = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new BusinessException("Workflow not found: " + workflowId));
        List<WorkflowNodeEntity> nodes = parseNodes(wf.getNodesJson());

        lineageRepository.deleteByWorkflowId(workflowId);
        columnLineageRepository.deleteByWorkflowId(workflowId);
        List<WorkflowLineageEntity> rows = new ArrayList<>();
        List<WorkflowColumnLineageEntity> colRows = new ArrayList<>();
        for (WorkflowNodeEntity node : nodes) {
            String sql = node.getSql();
            if (sql == null || sql.isBlank()) continue;
            DatasourceType type = DatasourceType.of(node.getDatasourceType());
            Dialect dialect = Dialect.from(type);
            SqlParseRes parsed = sqlLineageParser.parse(sql, type);
            if (!parsed.isSuccess()) continue;
            String typeName2 = parsed.getSqlType();
            String dialectName = dialect.name();
            for (String source : parsed.getSources()) {
                rows.add(WorkflowLineageEntity.builder()
                        .workflowId(workflowId).nodeId(node.getId()).nodeName(node.getName())
                        .sourceTable(source).targetTable(null).sqlType(typeName2).dialect(dialectName).build());
            }
            if (parsed.getTargetTable() != null) {
                rows.add(WorkflowLineageEntity.builder()
                        .workflowId(workflowId).nodeId(node.getId()).nodeName(node.getName())
                        .sourceTable(null).targetTable(parsed.getTargetTable()).sqlType(typeName2).dialect(dialectName).build());
            }
            if (parsed.getColumnLineage() != null) {
                for (ColumnLineage cl : parsed.getColumnLineage()) {
                    colRows.add(WorkflowColumnLineageEntity.builder()
                            .workflowId(workflowId).nodeId(node.getId()).nodeName(node.getName())
                            .targetTable(cl.getTargetTable()).targetColumn(cl.getTargetColumn())
                            .sourceTable(cl.getSourceTable()).sourceColumn(cl.getSourceColumn())
                            .dialect(dialectName).build());
                }
            }
        }
        lineageRepository.saveAll(rows);
        columnLineageRepository.saveAll(colRows);
        log.info("Synced task-level lineage for workflow {}, rows={}, colRows={}", workflowId, rows.size(), colRows.size());
        return rows.size();
    }

    /** Combined task + table graph for a workflow (nodes/edges in LineageGraph shape). */
    @Transactional(readOnly = true)
    public Map<String, Object> graph(String workflowId) {
        List<WorkflowLineageEntity> rows = lineageRepository.findByWorkflowId(workflowId);

        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // producer: table -> task node id that writes it
        Map<String, String> producer = new HashMap<>();
        for (WorkflowLineageEntity r : rows) {
            if (r.getTargetTable() != null) producer.put(r.getTargetTable(), taskId(r.getNodeId()));
        }

        for (WorkflowLineageEntity r : rows) {
            String task = taskId(r.getNodeId());
            ensureNode(nodes, task, r.getNodeName(), "TASK");

            if (r.getSourceTable() != null) {
                String tbl = tableId(r.getSourceTable());
                ensureNode(nodes, tbl, r.getSourceTable(), "TABLE");
                addEdge(edges, edgeKeys, tbl, task, "READ");
                String p = producer.get(r.getSourceTable());
                if (p != null && !p.equals(task)) {
                    addEdge(edges, edgeKeys, p, task, "DEP");
                }
            }
            if (r.getTargetTable() != null) {
                String tbl = tableId(r.getTargetTable());
                ensureNode(nodes, tbl, r.getTargetTable(), "TABLE");
                addEdge(edges, edgeKeys, task, tbl, "WRITE");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workflowId", workflowId);
        result.put("nodes", new ArrayList<>(nodes.values()));
        result.put("edges", edges);
        result.put("columnEdges", columnEdges(workflowId));
        return result;
    }

    /** Column-level lineage edges (target col <- source col), best-effort (P3). */
    private List<Map<String, Object>> columnEdges(String workflowId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (WorkflowColumnLineageEntity r : columnLineageRepository.findByWorkflowId(workflowId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("targetTable", r.getTargetTable());
            m.put("targetColumn", r.getTargetColumn());
            m.put("sourceTable", r.getSourceTable());
            m.put("sourceColumn", r.getSourceColumn());
            out.add(m);
        }
        return out;
    }

    private List<WorkflowNodeEntity> parseNodes(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<WorkflowNodeEntity>>() {});
        } catch (Exception e) {
            throw new BusinessException("Failed to parse workflow nodes: " + e.getMessage());
        }
    }

    private void ensureNode(Map<String, Map<String, Object>> nodes, String id, String name, String nodeType) {
        if (nodes.containsKey(id)) return;
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("id", id);
        n.put("name", name);
        n.put("nodeType", nodeType);
        nodes.put(id, n);
    }

    private void addEdge(List<Map<String, Object>> edges, Set<String> keys,
                         String source, String target, String label) {
        String key = source + "|" + target;
        if (!keys.add(key)) return;
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("source", source);
        e.put("target", target);
        e.put("label", label);
        edges.add(e);
    }

    private String taskId(String nodeId) {
        return "task:" + nodeId;
    }

    private String tableId(String table) {
        return "tbl:" + table;
    }
}