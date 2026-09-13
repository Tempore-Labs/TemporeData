package org.temporedata.modules.dev.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.temporedata.modules.dev.workflow.entity.WorkflowColumnLineageEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowLineageEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowColumnLineageRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowLineageRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowRepository;
import org.temporedata.modules.dev.workflow.runner.SqlFirewall;
import org.temporedata.modules.dev.workflow.runner.SqlLineageParser;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the task + table DAG graph assembly that feeds the visualization.
 * {@code graph(workflowId)} folds per-node lineage rows into READ / WRITE /
 * task-level DEP edges, mirroring what the frontend DAG canvas consumes.
 */
class WorkflowLineageServiceTest {

    private WorkflowLineageRepository lineageRepository;
    private WorkflowColumnLineageRepository columnLineageRepository;
    private WorkflowLineageService service;

    @BeforeEach
    void setUp() {
        lineageRepository = mock(WorkflowLineageRepository.class);
        columnLineageRepository = mock(WorkflowColumnLineageRepository.class);
        service = new WorkflowLineageService(
                mock(WorkflowRepository.class),
                lineageRepository,
                columnLineageRepository,
                mock(SqlLineageParser.class),
                mock(SqlFirewall.class),
                new ObjectMapper());
    }

    private static WorkflowLineageEntity row(String nodeId, String nodeName,
                                             String sourceTable, String targetTable) {
        return WorkflowLineageEntity.builder()
                .workflowId("wf-x").nodeId(nodeId).nodeName(nodeName)
                .sourceTable(sourceTable).targetTable(targetTable)
                .sqlType("SELECT").dialect("MYSQL").build();
    }

    @Test
    void graphAssemblesTaskAndTableNodesWithReadWriteEdges() {
        // task1 writes cust; task2 reads cust and writes agg
        when(lineageRepository.findByWorkflowId(anyString())).thenReturn(List.of(
                row("task1", "T1", null, "cust"),
                row("task2", "T2", "cust", "agg")));
        when(columnLineageRepository.findByWorkflowId(anyString())).thenReturn(List.of());

        Map<String, Object> g = service.graph("wf-x");

        assertEquals("wf-x", g.get("workflowId"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) g.get("nodes");
        assertEquals(4, nodes.size(), "task1, task2, tbl:cust, tbl:agg");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) g.get("edges");
        assertEquals(4, edges.size());
        // task1 -> tbl:cust WRITE, tbl:cust -> task2 READ, task1 -> task2 DEP, task2 -> tbl:agg WRITE
        assertTrue(hasEdge(edges, "task:task1", "tbl:cust", "WRITE"));
        assertTrue(hasEdge(edges, "tbl:cust", "task:task2", "READ"));
        assertTrue(hasEdge(edges, "task:task1", "task:task2", "DEP"));
        assertTrue(hasEdge(edges, "task:task2", "tbl:agg", "WRITE"));
    }

    @Test
    void graphEmitColumnEdgesFromColumnLineage() {
        when(lineageRepository.findByWorkflowId(anyString())).thenReturn(List.of(
                row("task1", "T1", null, "cust")));
        when(columnLineageRepository.findByWorkflowId(anyString())).thenReturn(List.of(
                WorkflowColumnLineageEntity.builder()
                        .workflowId("wf-x").nodeId("task1").nodeName("T1")
                        .targetTable("agg").targetColumn("id")
                        .sourceTable("cust").sourceColumn("uid")
                        .dialect("MYSQL").build()));

        Map<String, Object> g = service.graph("wf-x");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> colEdges = (List<Map<String, Object>>) g.get("columnEdges");
        assertEquals(1, colEdges.size());
        assertEquals("agg", colEdges.get(0).get("targetTable"));
        assertEquals("uid", colEdges.get(0).get("sourceColumn"));
    }

    @Test
    void graphDeduplicatesRepeatEdgesBetweenSameNodes() {
        // a single task reading the same table twice must not duplicate the READ edge
        when(lineageRepository.findByWorkflowId(anyString())).thenReturn(List.of(
                row("task1", "T1", "cust", "agg"),
                row("task1", "T1", "cust", "agg")));
        when(columnLineageRepository.findByWorkflowId(anyString())).thenReturn(List.of());

        Map<String, Object> g = service.graph("wf-x");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) g.get("edges");
        assertEquals(2, edges.size(), "only READ and WRITE, the duplicate READ is de-duplicated");
    }

    @Test
    void graphWithNoRowsIsEmptyGraph() {
        when(lineageRepository.findByWorkflowId(anyString())).thenReturn(List.of());
        when(columnLineageRepository.findByWorkflowId(anyString())).thenReturn(List.of());

        Map<String, Object> g = service.graph("wf-x");

        assertEquals(0, ((List<?>) g.get("nodes")).size());
        assertEquals(0, ((List<?>) g.get("edges")).size());
    }

    @SuppressWarnings("unchecked")
    private static boolean hasEdge(List<Map<String, Object>> edges, String source, String target, String label) {
        return edges.stream().anyMatch(e ->
                source.equals(e.get("source")) && target.equals(e.get("target")) && label.equals(e.get("label")));
    }
}