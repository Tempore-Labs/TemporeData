package org.temporedata.modules.dev.workflow.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.workflow.WorkflowEdgeReq;
import org.temporedata.api.dev.workflow.WorkflowNodeReq;
import org.temporedata.api.dev.workflow.WorkflowReq;
import org.temporedata.api.dev.workflow.WorkflowRes;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P1-7 modeling by generation/import: JSON (Python SDK output) and Excel
 * imports both produce a {@link WorkflowReq} and delegate to WorkflowService.
 */
@Service
public class WorkflowImportService {

    private final WorkflowService workflowService;

    public WorkflowImportService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    // ---- JSON (Python SDK / programmatic) ----

    public WorkflowRes importJson(WorkflowReq req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BusinessException("Workflow name is required");
        }
        if (req.getNodes() == null || req.getNodes().isEmpty()) {
            throw new BusinessException("Workflow has no nodes");
        }
        return workflowService.create(req);
    }

    public Map<String, Object> exportJson(String workflowId) {
        WorkflowRes wf = workflowService.get(workflowId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", wf.getName());
        out.put("description", wf.getDescription());
        out.put("nodes", wf.getNodes());
        out.put("edges", wf.getEdges());
        return out;
    }

    // ---- Excel ----

    /**
     * Import a workflow from an .xlsx workbook (模板见 {exportExcel})，返回导入摘要。
     */
    public Map<String, Object> importExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            List<WorkflowNodeReq> nodes = readNodes(wb, errors);
            List<WorkflowEdgeReq> edges = readEdges(wb, errors);
            Map<String, Object> meta = readSchedule(wb);

            if (nodes.isEmpty()) {
                throw new BusinessException("节点 Sheet 无有效数据");
            }
            String name = meta.containsKey("name") && meta.get("name") != null
                    ? String.valueOf(meta.get("name")) : "导入工作流_" + System.currentTimeMillis();

            WorkflowReq req = new WorkflowReq();
            req.setName(name);
            req.setDescription(meta.containsKey("description") ? String.valueOf(meta.get("description")) : null);
            req.setNodes(nodes);
            req.setEdges(edges);

            WorkflowRes created = workflowService.create(req);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("workflowId", created.getId());
            result.put("name", created.getName());
            result.put("nodeCount", nodes.size());
            result.put("edgeCount", edges.size());
            result.put("errors", errors);
            return result;
        } catch (IOException e) {
            throw new BusinessException("读取 Excel 失败: " + e.getMessage());
        }
    }

    /**
     * Export a workflow as an .xlsx template workbook (节点 / 边 / 调度 sheets).
     */
    public byte[] exportExcel(String workflowId) {
        WorkflowRes wf = workflowService.get(workflowId);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet nodeSheet = wb.createSheet("节点");
            writeRow(nodeSheet, 0, "id", "name", "type", "datasource_id", "sql", "retry_count", "priority", "fail_strategy", "params");
            List<WorkflowNodeReq> nodes = toNodes(wf.getNodes());
            for (int i = 0; i < nodes.size(); i++) {
                WorkflowNodeReq n = nodes.get(i);
                writeRow(nodeSheet, i + 1, n.getId(), n.getName(), n.getType(),
                        n.getDatasourceId(), n.getSql(), n.getRetryCount(), n.getPriority(), n.getFailStrategy(), n.getParams());
            }
            wb.forEach(sheet -> {
                if (sheet.getSheetName().equals("节点")) sheet.createFreezePane(0, 1);
            });

            Sheet edgeSheet = wb.createSheet("边");
            writeRow(edgeSheet, 0, "id", "source_node_id", "target_node_id", "edge_type", "condition_expr");
            List<WorkflowEdgeReq> edges = toEdges(wf.getEdges());
            for (int i = 0; i < edges.size(); i++) {
                WorkflowEdgeReq e = edges.get(i);
                writeRow(edgeSheet, i + 1, e.getId(), e.getSourceNodeId(), e.getTargetNodeId(), e.getEdgeType(), e.getConditionExpr());
            }

            Sheet schedSheet = wb.createSheet("调度");
            writeRow(schedSheet, 0, "workflow_name", "description", "cron", "biz_date_mode", "calendar_id", "enabled", "timezone");
            writeRow(schedSheet, 1, wf.getName(), wf.getDescription(), wf.getCronExpression(), "", "", "", "");

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出 Excel 失败: " + e.getMessage());
        }
    }

    // ---- Excel readers ----

    private List<WorkflowNodeReq> readNodes(Workbook wb, List<String> errors) {
        List<WorkflowNodeReq> nodes = new ArrayList<>();
        Sheet sheet = wb.getSheet("节点");
        if (sheet == null) return nodes;
        int header = findHeader(sheet, "id");
        for (int r = header + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String id = cell(row, "id");
            if (id == null || id.isBlank()) { errors.add("行" + (r + 1) + ": 缺少 id"); continue; }
            WorkflowNodeReq n = new WorkflowNodeReq();
            n.setId(id);
            n.setName(orDefault(cell(row, "name"), id));
            n.setType(orDefault(cell(row, "type"), "SQL"));
            n.setDatasourceId(cell(row, "datasource_id"));
            n.setSql(cell(row, "sql"));
            n.setRetryCount(intOf(cell(row, "retry_count")));
            n.setPriority(cell(row, "priority"));
            n.setFailStrategy(cell(row, "fail_strategy"));
            n.setParams(cell(row, "params"));
            nodes.add(n);
        }
        return nodes;
    }

    private List<WorkflowEdgeReq> readEdges(Workbook wb, List<String> errors) {
        List<WorkflowEdgeReq> edges = new ArrayList<>();
        Sheet sheet = wb.getSheet("边");
        if (sheet == null) return edges;
        int header = findHeader(sheet, "id");
        for (int r = header + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String id = cell(row, "id");
            String src = cell(row, "source_node_id");
            String tgt = cell(row, "target_node_id");
            if (src == null || tgt == null || src.isBlank() || tgt.isBlank()) {
                errors.add("行" + (r + 1) + ": 边缺少 source/target"); continue;
            }
            WorkflowEdgeReq e = new WorkflowEdgeReq();
            e.setId(id == null || id.isBlank() ? "e" + r : id);
            e.setSourceNodeId(src);
            e.setTargetNodeId(tgt);
            e.setEdgeType(orDefault(cell(row, "edge_type"), "SUCCESS"));
            e.setConditionExpr(cell(row, "condition_expr"));
            edges.add(e);
        }
        return edges;
    }

    private Map<String, Object> readSchedule(Workbook wb) {
        Map<String, Object> meta = new LinkedHashMap<>();
        Sheet sheet = wb.getSheet("调度");
        if (sheet == null) return meta;
        int header = findHeader(sheet, "workflow_name");
        Row row = sheet.getRow(header + 1);
        if (row == null) return meta;
        meta.put("name", cell(row, "workflow_name"));
        meta.put("description", cell(row, "description"));
        return meta;
    }

    private int findHeader(Sheet sheet, String name) {
        Row r = sheet.getRow(0);
        if (r == null) return 0;
        for (int i = 0; i < r.getLastCellNum(); i++) {
            String v = r.getCell(i) == null ? "" : r.getCell(i).toString();
            if (v.equalsIgnoreCase(name)) return 0;
        }
        return 0;
    }

    private void writeRow(Sheet sheet, int r, Object... values) {
        Row row = sheet.createRow(r);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object v = values[i];
            if (v instanceof Number) cell.setCellValue(((Number) v).doubleValue());
            else cell.setCellValue(v == null ? "" : String.valueOf(v));
        }
    }

    private String cell(Row row, String name) {
        int idx = headerIndex(row, name);
        if (idx < 0) return null;
        Cell c = row.getCell(idx);
        return c == null ? null : c.toString();
    }

    private int headerIndex(Row row, String name) {
        if (row == null) return -1;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c != null && name.equalsIgnoreCase(c.toString())) return i;
        }
        return -1;
    }

    private String orDefault(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }

    private Integer intOf(String v) {
        if (v == null || v.isBlank()) return null;
        try { return (int) Double.parseDouble(v.trim()); } catch (Exception e) { return null; }
    }

    private List<WorkflowNodeReq> toNodes(List<org.temporedata.api.dev.workflow.WorkflowNodeRes> res) {
        List<WorkflowNodeReq> list = new ArrayList<>();
        if (res == null) return list;
        for (org.temporedata.api.dev.workflow.WorkflowNodeRes n : res) {
            WorkflowNodeReq r = new WorkflowNodeReq();
            r.setId(n.getId());
            r.setName(n.getName());
            r.setType(n.getType());
            r.setDatasourceId(n.getDatasourceId());
            r.setSql(n.getSql());
            r.setRetryCount(n.getRetryCount());
            r.setPriority(n.getPriority());
            r.setFailStrategy(n.getFailStrategy());
            r.setParams(n.getParams());
            list.add(r);
        }
        return list;
    }

    private List<WorkflowEdgeReq> toEdges(List<org.temporedata.api.dev.workflow.WorkflowEdgeRes> res) {
        List<WorkflowEdgeReq> list = new ArrayList<>();
        if (res == null) return list;
        for (org.temporedata.api.dev.workflow.WorkflowEdgeRes e : res) {
            WorkflowEdgeReq r = new WorkflowEdgeReq();
            r.setId(e.getId());
            r.setSourceNodeId(e.getSourceNodeId());
            r.setTargetNodeId(e.getTargetNodeId());
            r.setEdgeType(e.getEdgeType());
            r.setConditionExpr(e.getConditionExpr());
            list.add(r);
        }
        return list;
    }
}