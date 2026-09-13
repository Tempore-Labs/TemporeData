package org.temporedata.modules.dev.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.workflow.WorkflowEdgeReq;
import org.temporedata.api.dev.workflow.WorkflowEdgeRes;
import org.temporedata.api.dev.workflow.WorkflowNodeReq;
import org.temporedata.api.dev.workflow.WorkflowNodeRes;
import org.temporedata.api.dev.workflow.WorkflowReq;
import org.temporedata.api.dev.workflow.WorkflowRes;
import org.temporedata.modules.dev.workflow.entity.WorkflowEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowVersionEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowVersionRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the DAG definition persistence contract that the visualization editor
 * relies on: nodes/edges (including layout positionX/Y) round-trip through
 * {@code nodesJson}/{@code edgesJson} on create / get / update.
 */
class WorkflowServiceTest {

    private WorkflowRepository workflowRepository;
    private WorkflowVersionRepository versionRepository;
    private WorkflowService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        versionRepository = mock(WorkflowVersionRepository.class);
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workflowRepository.findById(anyString())).thenReturn(Optional.empty());
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.findFirstByWorkflowIdOrderByVersionNoDesc(anyString()))
                .thenReturn(Optional.empty());
        service = new WorkflowService(workflowRepository, versionRepository, objectMapper);
    }

    @AfterEach
    void tearDown() {
        org.temporedata.security.context.TenantContext.clear();
    }

    private static WorkflowReq req() {
        WorkflowReq r = new WorkflowReq();
        r.setName("daily_etl");

        WorkflowNodeReq n1 = new WorkflowNodeReq();
        n1.setId("n1"); n1.setName("取数"); n1.setType("SQL");
        n1.setDatasourceId("ds-1"); n1.setSql("select 1"); n1.setPositionX(40); n1.setPositionY(20);

        WorkflowEdgeReq e1 = new WorkflowEdgeReq();
        e1.setId("e1"); e1.setSourceNodeId("n1"); e1.setTargetNodeId("n2");
        e1.setEdgeType("CONDITION"); e1.setConditionExpr("${ok}");
        r.setNodes(List.of(n1));
        r.setEdges(List.of(e1));
        return r;
    }

    @Test
    void createRoundTripsNodesAndEdgesIncludingLayout() {
        WorkflowRes res = service.create(req());

        assertEquals("DRAFT", res.getStatus());
        assertEquals(1, res.getNodes().size());
        WorkflowNodeRes n = res.getNodes().get(0);
        assertEquals("n1", n.getId());
        assertEquals("SQL", n.getType());
        assertEquals("ds-1", n.getDatasourceId());
        assertEquals(Integer.valueOf(40), n.getPositionX());
        assertEquals(Integer.valueOf(20), n.getPositionY());

        WorkflowEdgeRes e = res.getEdges().get(0);
        assertEquals("CONDITION", e.getEdgeType());
        assertEquals("${ok}", e.getConditionExpr());
    }

    @Test
    void createPersistsDagAsNodesAndEdgesJson() {
        service.create(req());

        ArgumentCaptor<WorkflowEntity> captor = ArgumentCaptor.forClass(WorkflowEntity.class);
        verify(workflowRepository).save(captor.capture());
        WorkflowEntity saved = captor.getValue();

        assertTrue(saved.getNodesJson().contains("\"positionX\":40"));
        assertTrue(saved.getNodesJson().contains("\"sql\":\"select 1\""));
        assertTrue(saved.getNodesJson().contains("\"datasourceId\":\"ds-1\""));
        assertTrue(saved.getEdgesJson().contains("\"edgeType\":\"CONDITION\""));
        assertTrue(saved.getEdgesJson().contains("\"conditionExpr\":\"${ok}\""));
    }

    @Test
    void getRehydratesDagFromStoredJson() {
        WorkflowEntity entity = WorkflowEntity.builder()
                .id("wf-1").name("daily_etl")
                .nodesJson("[{\"id\":\"n1\",\"name\":\"取数\",\"type\":\"SQL\",\"datasourceId\":\"ds-1\"," +
                        "\"sql\":\"select 1\",\"positionX\":40,\"positionY\":20}]")
                .edgesJson("[{\"id\":\"e1\",\"sourceNodeId\":\"n1\",\"targetNodeId\":\"n2\"," +
                        "\"edgeType\":\"SUCCESS\"}]")
                .build();
        when(workflowRepository.findById(anyString())).thenReturn(Optional.of(entity));

        WorkflowRes res = service.get("wf-1");

        assertEquals("daily_etl", res.getName());
        assertEquals(1, res.getNodes().size());
        assertEquals(Integer.valueOf(40), res.getNodes().get(0).getPositionX());
        assertEquals("SUCCESS", res.getEdges().get(0).getEdgeType());
    }

    @Test
    void updateReplacesDagJson() {
        WorkflowEntity entity = WorkflowEntity.builder()
                .id("wf-1").name("old")
                .nodesJson("[]").edgesJson("[]").build();
        when(workflowRepository.findById(anyString())).thenReturn(Optional.of(entity));

        WorkflowReq r = req(); // carries n1
        service.update("wf-1", r);

        ArgumentCaptor<WorkflowEntity> captor = ArgumentCaptor.forClass(WorkflowEntity.class);
        verify(workflowRepository).save(captor.capture());
        WorkflowEntity saved = captor.getValue();
        assertEquals("daily_etl", saved.getName());
        assertTrue(saved.getNodesJson().contains("\"id\":\"n1\""));
        assertTrue(saved.getEdgesJson().contains("\"conditionExpr\":\"${ok}\""));
    }

    @Test
    void getThrowsBusinessExceptionWhenWorkflowMissing() {
        when(workflowRepository.findById(anyString())).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.get("missing"));
        assertTrue(ex.getMessage().contains("not found"), ex.getMessage());
    }

    // ---- P2: versioning + online/offline ----

    @Test
    void onlineMarksStatusAndUpdateIsBlockedWhileOnline() {
        WorkflowEntity entity = WorkflowEntity.builder().id("wf-1").name("w").status("DRAFT").build();
        when(workflowRepository.findById(anyString())).thenReturn(Optional.of(entity));

        WorkflowRes on = service.online("wf-1");
        assertEquals("ONLINE", on.getStatus());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("wf-1", req()));
        assertTrue(ex.getMessage().contains("ONLINE"), ex.getMessage());
    }

    @Test
    void updateSnapshotsVersionOnlyWhenDefinitionChanges() {
        WorkflowEntity entity = WorkflowEntity.builder()
                .id("wf-1").name("old").status("DRAFT")
                .nodesJson("[]").edgesJson("[]").build();
        when(workflowRepository.findById(anyString())).thenReturn(Optional.of(entity));

        service.update("wf-1", req()); // definition changes from [] to n1
        verify(versionRepository).save(any(WorkflowVersionEntity.class));
    }

    @Test
    void rollbackRestoresVersionSnapshot() {
        WorkflowEntity entity = WorkflowEntity.builder()
                .id("wf-1").name("w").status("DRAFT")
                .nodesJson("[{\"id\":\"x\"}]").edgesJson("[]").build();
        WorkflowVersionEntity ver = WorkflowVersionEntity.builder()
                .id("v-9").workflowId("wf-1").versionNo(1).name("w")
                .nodesJson("[{\"id\":\"n1\",\"name\":\"取数\",\"type\":\"SQL\"}]")
                .edgesJson("[]").build();
        when(workflowRepository.findById(anyString())).thenReturn(Optional.of(entity));
        when(versionRepository.findById("v-9")).thenReturn(Optional.of(ver));

        WorkflowRes res = service.rollback("wf-1", "v-9");
        assertEquals(1, res.getNodes().size());
        assertEquals("n1", res.getNodes().get(0).getId());
    }
}