package org.temporedata.modules.dev.workflow.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.workflow.WorkflowRunRes;
import org.temporedata.modules.dev.workflow.entity.WorkflowEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowInstanceLogRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowInstanceRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowNodeInstanceRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Workflow DAG runtime. Focus on the DAG-execution guarantees
 * surfaced to the visualization frontend: cycle rejection, dangling-edge
 * rejection, topological order, failure → downstream skip, and condition edges.
 */
class WorkflowRunnerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WorkflowRepository workflowRepository;
    private WorkflowRunner runner;
    private NodeExecutor executor;

    /** Build a runner with all collaborators mocked except the real ObjectMapper. */
    private void setUp(WorkflowEntity wf) {
        workflowRepository = mock(WorkflowRepository.class);
        WorkflowInstanceRepository instanceRepository = mock(WorkflowInstanceRepository.class);
        WorkflowNodeInstanceRepository nodeInstanceRepository = mock(WorkflowNodeInstanceRepository.class);
        WorkflowInstanceLogRepository logRepository = mock(WorkflowInstanceLogRepository.class);
        NodeExecutorRegistry executorRegistry = mock(NodeExecutorRegistry.class);
        ConditionEvaluator conditionEvaluator = mock(ConditionEvaluator.class);
        RuntimeControlRegistry controlRegistry = mock(RuntimeControlRegistry.class);

        executor = mock(NodeExecutor.class);
        when(executorRegistry.get(anyString())).thenReturn(executor);

        RuntimeControlRegistry.RuntimeAction action = mock(RuntimeControlRegistry.RuntimeAction.class);
        when(action.isStopRequested()).thenReturn(false);
        when(controlRegistry.action(any())).thenReturn(action);

        when(workflowRepository.findById(anyString())).thenReturn(java.util.Optional.of(wf));

        runner = new WorkflowRunner(workflowRepository, instanceRepository, nodeInstanceRepository,
                logRepository, executorRegistry, conditionEvaluator, controlRegistry, objectMapper);
        // @PostConstruct init() only runs under Spring; call it directly so the
        // node execution thread pool is available in a plain unit test.
        runner.init();
    }

    /** Make the single mocked executor return a scripted result per node id. */
    private void stubNodeResults(Map<String, NodeRunResult> byNodeId) {
        try {
            when(executor.execute(any(), any())).thenAnswer(inv -> {
                WorkflowNodeEntity n = inv.getArgument(0);
                NodeRunResult r = byNodeId.get(n.getId());
                return r != null ? r : NodeRunResult.failed("no script for " + n.getId());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String nodesJson(String... ids) {
        return "[" + String.join(",", java.util.Arrays.stream(ids)
                .map(id -> "{\"id\":\"" + id + "\",\"name\":\"" + id.toUpperCase() + "\",\"type\":\"SQL\"}")
                .collect(java.util.stream.Collectors.toList())) + "]";
    }

    private static String edgeJson(String source, String target, String edgeType, String cond) {
        StringBuilder sb = new StringBuilder("{\"id\":\"" + source + "_" + target + "\",")
                .append("\"sourceNodeId\":\"").append(source).append("\",")
                .append("\"targetNodeId\":\"").append(target).append("\"");
        if (edgeType != null) sb.append(",\"edgeType\":\"").append(edgeType).append("\"");
        if (cond != null) sb.append(",\"conditionExpr\":\"").append(cond).append("\"");
        return sb.append("}").toString();
    }

    private static WorkflowEntity wf(String nodes, String... edges) {
        String edgesArr = "[" + String.join(",", edges) + "]";
        return WorkflowEntity.builder().name("wf").nodesJson(nodes).edgesJson(edgesArr).build();
    }

    @Test
    void runRejectsAcyclicViolatingCycleWithBusinessException() {
        WorkflowEntity wf = wf(nodesJson("a", "b"),
                edgeJson("a", "b", "SUCCESS", null),
                edgeJson("b", "a", "SUCCESS", null));
        setUp(wf);

        Exception ex = assertThrows(BusinessException.class,
                () -> runner.run("wf-1", "MANUAL", null));
        assertTrue(ex.getMessage().contains("cycle"), ex.getMessage());
    }

    @Test
    void runRejectsEdgeReferencingMissingNode() {
        WorkflowEntity wf = wf(nodesJson("a"), edgeJson("a", "ghost", "SUCCESS", null));
        setUp(wf);

        Exception ex = assertThrows(BusinessException.class,
                () -> runner.run("wf-2", "MANUAL", null));
        assertTrue(ex.getMessage().contains("missing node"), ex.getMessage());
    }

    @Test
    void linearDagExecutesToSuccessInTopologicalOrder() throws Exception {
        WorkflowEntity wf = wf(nodesJson("a", "b"), edgeJson("a", "b", "SUCCESS", null));
        setUp(wf);
        stubNodeResults(Map.of("a", NodeRunResult.ok("ok", 1), "b", NodeRunResult.ok("ok", 2)));

        WorkflowRunRes res = runner.run("wf-3", "MANUAL", null);

        assertEquals("SUCCESS", res.getStatus(), "workflow should succeed");
        List<WorkflowRunRes.NodeRunItem> results = res.getNodeResults();
        assertEquals(2, results.size());
        // capacity A runs, then B (topological order)
        assertEquals("a", results.get(0).getNodeId());
        assertEquals("b", results.get(1).getNodeId());
        assertTrue(results.stream().allMatch(r -> "SUCCESS".equals(r.getStatus())));
    }

    @Test
    void failingNodeCausesDownstreamToBeSkipped() throws Exception {
        WorkflowEntity wf = wf(nodesJson("a", "b"), edgeJson("a", "b", "SUCCESS", null));
        setUp(wf);
        stubNodeResults(Map.of("a", NodeRunResult.failed("boom")));

        WorkflowRunRes res = runner.run("wf-4", "MANUAL", null);

        assertEquals("FAILED", res.getStatus());
        Map<String, WorkflowRunRes.NodeRunItem> byId = res.getNodeResults().stream()
                .collect(java.util.stream.Collectors.toMap(WorkflowRunRes.NodeRunItem::getNodeId, r -> r));
        assertEquals("FAILED", byId.get("a").getStatus());
        assertEquals("SKIPPED", byId.get("b").getStatus());
    }

    @Test
    void conditionEdgeNotMatchingSkipsDownstreamEvenWhenSourceSucceeds() throws Exception {
        WorkflowEntity wf = wf(nodesJson("a", "b"),
                edgeJson("a", "b", "CONDITION", "${code} == 'ok'"));
        setUp(wf);
        stubNodeResults(Map.of("a", NodeRunResult.ok("ok", 1)));

        WorkflowRunRes res = runner.run("wf-5", "MANUAL", null);

        assertEquals("SUCCESS", res.getStatus());
        Map<String, WorkflowRunRes.NodeRunItem> byId = res.getNodeResults().stream()
                .collect(java.util.stream.Collectors.toMap(WorkflowRunRes.NodeRunItem::getNodeId, r -> r));
        // condition not satisfied for B → it is not run but the DAG still succeeds
        assertEquals("SKIPPED", byId.get("b").getStatus());
        Mockito.verify(workflowRepository).save(any(WorkflowEntity.class));
    }
}