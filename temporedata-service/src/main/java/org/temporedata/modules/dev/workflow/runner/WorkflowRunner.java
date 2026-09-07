package org.temporedata.modules.dev.workflow.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.workflow.InstanceLogRes;
import org.temporedata.api.dev.workflow.WorkflowInstanceRes;
import org.temporedata.api.dev.workflow.WorkflowNodeInstanceRes;
import org.temporedata.api.dev.workflow.WorkflowRunRes;
import org.temporedata.modules.dev.workflow.entity.WorkflowEdgeEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowInstanceEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowInstanceLogEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeInstanceEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowInstanceLogRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowInstanceRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowNodeInstanceRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Workflow DAG runtime (P0-2).
 *
 * Executes a workflow's DAG in topological order with:
 *  - cycle detection (Kahn) before starting
 *  - per-node readiness on incoming-edge resolution
 *  - conditional branches (CONDITION edges), evaluated against upstream results
 *  - retry + timeout per node
 *  - an instance persisted to zy_wf_instance with per-node statuses and logs
 */
@Slf4j
@Component
public class WorkflowRunner {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WorkflowRepository workflowRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowNodeInstanceRepository nodeInstanceRepository;
    private final WorkflowInstanceLogRepository logRepository;
    private final NodeExecutorRegistry executorRegistry;
    private final ConditionEvaluator conditionEvaluator;
    private final RuntimeControlRegistry controlRegistry;
    private final ObjectMapper objectMapper;

    private ExecutorService pool;

    @org.springframework.beans.factory.annotation.Value("${temporedata.runtime.pause-max-seconds:300}")
    private long pauseMaxSeconds;

    public WorkflowRunner(WorkflowRepository workflowRepository,
                          WorkflowInstanceRepository instanceRepository,
                          WorkflowNodeInstanceRepository nodeInstanceRepository,
                          WorkflowInstanceLogRepository logRepository,
                          NodeExecutorRegistry executorRegistry,
                          ConditionEvaluator conditionEvaluator,
                          RuntimeControlRegistry controlRegistry,
                          ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.instanceRepository = instanceRepository;
        this.nodeInstanceRepository = nodeInstanceRepository;
        this.logRepository = logRepository;
        this.executorRegistry = executorRegistry;
        this.conditionEvaluator = conditionEvaluator;
        this.controlRegistry = controlRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "wf-node-worker");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }

    // ================= Run =================

    /**
     * Run a workflow synchronously (default priority/pool, no biz date).
     */
    @Transactional
    public WorkflowRunRes run(String workflowId, String triggerType, String taskInstanceId) {
        return run(workflowId, triggerType, taskInstanceId, 5, "default", null);
    }

    /**
     * Run a workflow synchronously with a priority/pool (no biz date).
     */
    @Transactional
    public WorkflowRunRes run(String workflowId, String triggerType, String taskInstanceId,
                              int priority, String pool) {
        return run(workflowId, triggerType, taskInstanceId, priority, pool, null);
    }

    /**
     * Run a workflow synchronously. The run honours pause / resume / stop
     * requests raised via {@link RuntimeControlRegistry} and waits (bounded)
     * when paused. <code>bizDate</code> is injected into the instance and each
     * node context (P1-4) so SQL nodes can reference ${biz_date}.
     */
    @Transactional
    public WorkflowRunRes run(String workflowId, String triggerType, String taskInstanceId,
                              int priority, String pool, String bizDate) {
        WorkflowEntity wf = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new BusinessException("Workflow not found: " + workflowId));

        List<WorkflowNodeEntity> nodes = parseNodes(wf.getNodesJson());
        List<WorkflowEdgeEntity> edges = parseEdges(wf.getEdgesJson());
        if (nodes.isEmpty()) {
            throw new BusinessException("Workflow has no nodes, cannot run: " + workflowId);
        }

        // Build graph structures.
        Map<String, WorkflowNodeEntity> nodeById = new LinkedHashMap<>();
        Map<String, Integer> inCount = new HashMap<>();
        Map<String, List<WorkflowEdgeEntity>> outEdges = new HashMap<>();
        for (WorkflowNodeEntity n : nodes) {
            nodeById.put(n.getId(), n);
            inCount.put(n.getId(), 0);
            outEdges.put(n.getId(), new ArrayList<>());
        }
        for (WorkflowEdgeEntity e : edges) {
            if (!nodeById.containsKey(e.getSourceNodeId()) || !nodeById.containsKey(e.getTargetNodeId())) {
                throw new BusinessException("Edge references missing node: " + e.getId());
            }
            outEdges.get(e.getSourceNodeId()).add(e);
            inCount.merge(e.getTargetNodeId(), 1, Integer::sum);
        }

        // Cycle detection (Kahn).
        Map<String, Integer> indegree = new HashMap<>(inCount);
        Deque<String> topo = new ArrayDeque<>();
        nodeById.keySet().stream().filter(id -> indegree.get(id) == 0).forEach(topo::add);
        int visited = 0;
        while (!topo.isEmpty()) {
            String id = topo.poll();
            visited++;
            for (WorkflowEdgeEntity e : outEdges.get(id)) {
                int left = indegree.get(e.getTargetNodeId()) - 1;
                indegree.put(e.getTargetNodeId(), left);
                if (left == 0) topo.add(e.getTargetNodeId());
            }
        }
        if (visited != nodeById.size()) {
            throw new BusinessException("Workflow contains a cycle, cannot run: " + workflowId);
        }

        // Create instance.
        WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
                .workflowId(workflowId)
                .taskInstanceId(taskInstanceId)
                .bizDate(bizDate)
                .status("RUNNING")
                .triggerType(triggerType == null ? "MANUAL" : triggerType)
                .startTime(LocalDateTime.now())
                .priority(priority)
                .pool(pool)
                .build();
        instanceRepository.save(instance);
        logInstance(instance.getId(), null, "INFO", "Workflow run started, nodes=" + nodes.size()
                + ", pool=" + pool + ", priority=" + priority
                + (bizDate == null ? "" : ", bizDate=" + bizDate));

        Map<String, Integer> pendingDeps = new HashMap<>(inCount);
        Map<String, Integer> satisfiedDeps = new HashMap<>();
        Map<String, NodeRunResult> upstream = new HashMap<>();
        Map<String, WorkflowNodeInstanceEntity> nodeInstances = new LinkedHashMap<>();

        // Ready starters: nodes with no incoming edges.
        Deque<Work> work = new ArrayDeque<>();
        nodeById.keySet().stream().filter(id -> inCount.get(id) == 0)
                .forEach(id -> work.add(new Work(id, true)));

        RuntimeControlRegistry.RuntimeAction action = controlRegistry.action(instance.getId());
        boolean failed = false;
        boolean stopped = false;
        try {
            while (!work.isEmpty()) {
                if (checkControl(instance, action)) {
                    stopped = true;
                    break;
                }
                Work item = work.poll();
                WorkflowNodeEntity node = nodeById.get(item.nodeId);
                if (node == null) continue;

                if (!item.run) {
                    markNode(nodeInstances, instance, node, "SKIPPED", null, null, null, null);
                    logInstance(instance.getId(), nodeInstances.get(node.getId()).getId(), "INFO",
                            "Node skipped: " + node.getName());
                    propagate(node.getId(), "SKIPPED", outEdges, pendingDeps, satisfiedDeps, upstream, work);
                    continue;
                }

                WorkflowNodeInstanceEntity ni = ensureNodeInstance(nodeInstances, instance, node);
                ni.setStatus("RUNNING");
                ni.setStartTime(LocalDateTime.now());
                nodeInstanceRepository.save(ni);
                logInstance(instance.getId(), ni.getId(), "INFO", "Node started: " + node.getName());

                NodeExecutionContext ctx = NodeExecutionContext.builder()
                        .instanceId(instance.getId())
                        .nodeInstanceId(ni.getId())
                        .bizDate(bizDate)
                        .upstream(upstream)
                        .build();

                NodeRunResult result = executeWithTimeout(node, ctx, ni);

                if (result.isSuccess()) {
                    ni.setStatus("SUCCESS");
                    ni.setResult(result.getMessage());
                    ni.setErrorMsg(null);
                    ni.setFinishTime(LocalDateTime.now());
                    ni.setDurationMs(duration(ni));
                    nodeInstanceRepository.save(ni);
                    upstream.put(node.getId(), result);
                    logInstance(instance.getId(), ni.getId(), "INFO", "Node success: " + node.getName());
                    propagate(node.getId(), "SUCCESS", outEdges, pendingDeps, satisfiedDeps, upstream, work);
                } else {
                    ni.setStatus("FAILED");
                    ni.setResult(result.getMessage());
                    ni.setErrorMsg(result.getMessage());
                    ni.setFinishTime(LocalDateTime.now());
                    ni.setDurationMs(duration(ni));
                    nodeInstanceRepository.save(ni);
                    logInstance(instance.getId(), ni.getId(), "ERROR", "Node failed: " + node.getName() + " - " + result.getMessage());
                    failed = true;
                    break;
                }
            }
        } finally {
            controlRegistry.remove(instance.getId());
        }

        // Mark nodes that never started (stop/failed run).
        String unstartedStatus = stopped ? "STOPPED" : (failed ? "SKIPPED" : null);
        if (unstartedStatus != null) {
            for (WorkflowNodeEntity n : nodeById.values()) {
                if (!nodeInstances.containsKey(n.getId())) {
                    markNode(nodeInstances, instance, n, unstartedStatus, null, null, null, null);
                }
            }
        }

        String finalStatus = stopped ? "STOPPED" : (failed ? "FAILED" : "SUCCESS");
        instance.setStatus(finalStatus);
        instance.setFinishTime(LocalDateTime.now());
        instance.setResultMsg(buildSummary(finalStatus, nodeInstances.values()));
        instanceRepository.save(instance);
        logInstance(instance.getId(), null, "FAILED".equals(finalStatus) || "STOPPED".equals(finalStatus) ? "ERROR" : "INFO",
                "Workflow " + finalStatus);

        wf.setLastExecuteTime(LocalDateTime.now());
        workflowRepository.save(wf);

        return toRunRes(instance, nodeInstances);
    }

    /**
     * Check pause / stop signals raised while the workflow is running.
     *
     * @return true if the run should stop, false to continue
     */
    private boolean checkControl(WorkflowInstanceEntity instance,
                                 RuntimeControlRegistry.RuntimeAction action) {
        if (action.isStopRequested()) {
            instance.setStatus("STOPPED");
            instance.setStoppedAt(LocalDateTime.now());
            instanceRepository.save(instance);
            logInstance(instance.getId(), null, "ERROR", "Workflow stopped by user");
            return true;
        }
        if (action.pauseRequested) {
            instance.setStatus("PAUSED");
            instance.setPausedAt(LocalDateTime.now());
            instanceRepository.save(instance);
            logInstance(instance.getId(), null, "INFO", "Workflow paused");

            long deadline = System.currentTimeMillis() + pauseMaxSeconds * 1000;
            synchronized (action.monitor) {
                while (action.pauseRequested && !action.stopRequested) {
                    long remain = deadline - System.currentTimeMillis();
                    if (remain <= 0) break;
                    try {
                        action.monitor.wait(remain);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            if (action.isStopRequested()) {
                instance.setStatus("STOPPED");
                instance.setStoppedAt(LocalDateTime.now());
                instanceRepository.save(instance);
                logInstance(instance.getId(), null, "ERROR", "Workflow stopped during pause");
                return true;
            }
            instance.setStatus("RUNNING");
            instance.setPausedAt(null);
            instanceRepository.save(instance);
            logInstance(instance.getId(), null, "INFO", "Workflow resumed");
        }
        return false;
    }

    // ================= Node execution =================

    private NodeRunResult executeWithTimeout(WorkflowNodeEntity node, NodeExecutionContext ctx,
                                             WorkflowNodeInstanceEntity ni) {
        int retry = node.getRetryCount() == null ? 0 : node.getRetryCount();
        int intervalMs = node.getRetryInterval() == null ? 0 : node.getRetryInterval() * 1000;
        int timeout = node.getTimeoutSeconds() == null ? 600 : node.getTimeoutSeconds();
        NodeExecutor executor = executorRegistry.get(node.getType());

        NodeRunResult last = NodeRunResult.failed("not executed");
        for (int attempt = 0; attempt <= retry; attempt++) {
            Future<NodeRunResult> f = pool.submit(() -> executor.execute(node, ctx));
            try {
                last = f.get(timeout, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                f.cancel(true);
                last = NodeRunResult.failed("node timed out after " + timeout + "s");
                ni.setRetryTimes(attempt);
                return last;
            } catch (Exception e) {
                last = NodeRunResult.failed("execution error: " + e.getMessage());
            }
            if (last.isSuccess()) {
                ni.setRetryTimes(attempt);
                return last;
            }
            if (attempt < retry) {
                try { Thread.sleep(intervalMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        ni.setRetryTimes(retry);
        return last;
    }

    /**
     * Resolve outgoing edges of a finished source node and enqueue ready targets.
     */
    private void propagate(String sourceId, String sourceStatus,
                           Map<String, List<WorkflowEdgeEntity>> outEdges,
                           Map<String, Integer> pendingDeps,
                           Map<String, Integer> satisfiedDeps,
                           Map<String, NodeRunResult> upstream,
                           Deque<Work> work) {
        List<WorkflowEdgeEntity> edges = outEdges.getOrDefault(sourceId, Collections.emptyList());
        for (WorkflowEdgeEntity e : edges) {
            String target = e.getTargetNodeId();
            boolean satisfied = false;
            if ("SUCCESS".equals(sourceStatus)) {
                String edgeType = e.getEdgeType() == null ? "SUCCESS" : e.getEdgeType();
                if ("CONDITION".equalsIgnoreCase(edgeType)) {
                    satisfied = evalCondition(e.getConditionExpr(), upstream);
                } else {
                    satisfied = true;
                }
            }
            if (satisfied) satisfiedDeps.merge(target, 1, Integer::sum);
            int left = pendingDeps.get(target) - 1;
            pendingDeps.put(target, left);
            if (left == 0) {
                boolean runTarget = satisfiedDeps.getOrDefault(target, 0) > 0;
                work.add(new Work(target, runTarget));
            }
        }
    }

    private boolean evalCondition(String expr, Map<String, NodeRunResult> upstream) {
        try {
            return conditionEvaluator.evaluate(expr, upstream);
        } catch (Exception e) {
            log.warn("Condition evaluation failed (treated as false): {}", expr);
            return false;
        }
    }

    // ================= Persistence helpers =================

    private WorkflowNodeInstanceEntity ensureNodeInstance(Map<String, WorkflowNodeInstanceEntity> map,
                                                          WorkflowInstanceEntity instance,
                                                          WorkflowNodeEntity node) {
        WorkflowNodeInstanceEntity ni = map.get(node.getId());
        if (ni == null) {
            ni = WorkflowNodeInstanceEntity.builder()
                    .instanceId(instance.getId())
                    .nodeId(node.getId())
                    .nodeName(node.getName())
                    .nodeType(node.getType())
                    .status("PENDING")
                    .retryTimes(0)
                    .build();
            nodeInstanceRepository.save(ni);
            map.put(node.getId(), ni);
        }
        return ni;
    }

    private void markNode(Map<String, WorkflowNodeInstanceEntity> map,
                          WorkflowInstanceEntity instance,
                          WorkflowNodeEntity node,
                          String status,
                          String result,
                          String errorMsg,
                          LocalDateTime start,
                          Long duration) {
        WorkflowNodeInstanceEntity ni = ensureNodeInstance(map, instance, node);
        ni.setStatus(status);
        if (result != null) ni.setResult(result);
        if (errorMsg != null) ni.setErrorMsg(errorMsg);
        if (start == null) start = LocalDateTime.now();
        ni.setStartTime(start);
        ni.setFinishTime(LocalDateTime.now());
        ni.setDurationMs(duration != null ? duration : durationBetween(start));
        nodeInstanceRepository.save(ni);
    }

    private void logInstance(String instanceId, String nodeInstanceId, String level, String message) {
        try {
            logRepository.save(WorkflowInstanceLogEntity.builder()
                    .instanceId(instanceId)
                    .nodeInstanceId(nodeInstanceId)
                    .level(level)
                    .message(message)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist workflow instance log", e);
        }
    }

    private Long duration(WorkflowNodeInstanceEntity ni) {
        if (ni.getStartTime() == null) return 0L;
        return java.time.Duration.between(ni.getStartTime(), LocalDateTime.now()).toMillis();
    }

    private Long durationBetween(LocalDateTime start) {
        return java.time.Duration.between(start, LocalDateTime.now()).toMillis();
    }

    private String buildSummary(String finalStatus, java.util.Collection<WorkflowNodeInstanceEntity> nodeInstances) {
        long success = nodeInstances.stream().filter(n -> "SUCCESS".equals(n.getStatus())).count();
        long skipped = nodeInstances.stream().filter(n -> "SKIPPED".equals(n.getStatus())).count();
        long stopped = nodeInstances.stream().filter(n -> "STOPPED".equals(n.getStatus())).count();
        long failedNodes = nodeInstances.stream().filter(n -> "FAILED".equals(n.getStatus())).count();
        return finalStatus + ": nodes=" + nodeInstances.size()
                + ", success=" + success + ", skipped=" + skipped + ", failed=" + failedNodes
                + (stopped > 0 ? ", stopped=" + stopped : "");
    }

    // ================= Queries =================

    @Transactional(readOnly = true)
    public List<WorkflowInstanceRes> instances(String workflowId) {
        List<WorkflowInstanceRes> result = new ArrayList<>();
        for (WorkflowInstanceEntity inst : instanceRepository.findByWorkflowIdOrderByStartTimeDesc(workflowId)) {
            result.add(toInstanceRes(inst));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceRes instanceDetail(String instanceId) {
        WorkflowInstanceEntity inst = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BusinessException("Workflow instance not found: " + instanceId));
        return toInstanceRes(inst);
    }

    @Transactional(readOnly = true)
    public List<InstanceLogRes> logs(String instanceId) {
        List<InstanceLogRes> result = new ArrayList<>();
        for (WorkflowInstanceLogEntity l : logRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId)) {
            InstanceLogRes r = new InstanceLogRes();
            r.setId(l.getId());
            r.setNodeInstanceId(l.getNodeInstanceId());
            r.setLevel(l.getLevel());
            r.setMessage(l.getMessage());
            r.setCreateTime(l.getCreatedAt() != null ? l.getCreatedAt().format(DTF) : null);
            result.add(r);
        }
        return result;
    }

    // ================= Mapping =================

    private WorkflowRunRes toRunRes(WorkflowInstanceEntity inst,
                                    Map<String, WorkflowNodeInstanceEntity> nodeInstances) {
        WorkflowRunRes res = new WorkflowRunRes();
        res.setInstanceId(inst.getId());
        res.setStatus(inst.getStatus());
        res.setStartTime(inst.getStartTime() != null ? inst.getStartTime().format(DTF) : null);
        res.setEndTime(inst.getFinishTime() != null ? inst.getFinishTime().format(DTF) : null);
        List<WorkflowRunRes.NodeRunItem> items = new ArrayList<>();
        nodeInstances.values().forEach(ni -> {
            WorkflowRunRes.NodeRunItem it = new WorkflowRunRes.NodeRunItem();
            it.setNodeId(ni.getNodeId());
            it.setNodeName(ni.getNodeName());
            it.setStatus(ni.getStatus());
            it.setErrorMsg(ni.getErrorMsg());
            it.setDurationMs(ni.getDurationMs() == null ? 0 : ni.getDurationMs());
            items.add(it);
        });
        res.setNodeResults(items);
        return res;
    }

    private WorkflowInstanceRes toInstanceRes(WorkflowInstanceEntity inst) {
        WorkflowInstanceRes res = new WorkflowInstanceRes();
        res.setId(inst.getId());
        res.setWorkflowId(inst.getWorkflowId());
        res.setTaskInstanceId(inst.getTaskInstanceId());
        res.setStatus(inst.getStatus());
        res.setTriggerType(inst.getTriggerType());
        res.setStartTime(inst.getStartTime() != null ? inst.getStartTime().format(DTF) : null);
        res.setFinishTime(inst.getFinishTime() != null ? inst.getFinishTime().format(DTF) : null);
        res.setResultMsg(inst.getResultMsg());
        List<WorkflowNodeInstanceEntity> nodes = nodeInstanceRepository.findByInstanceIdOrderByCreatedAtAsc(inst.getId());
        List<WorkflowNodeInstanceRes> nodeRes = new ArrayList<>();
        nodes.forEach(n -> {
            WorkflowNodeInstanceRes nr = new WorkflowNodeInstanceRes();
            nr.setId(n.getId());
            nr.setNodeId(n.getNodeId());
            nr.setNodeName(n.getNodeName());
            nr.setNodeType(n.getNodeType());
            nr.setStatus(n.getStatus());
            nr.setStartTime(n.getStartTime() != null ? n.getStartTime().format(DTF) : null);
            nr.setFinishTime(n.getFinishTime() != null ? n.getFinishTime().format(DTF) : null);
            nr.setDurationMs(n.getDurationMs());
            nr.setRetryTimes(n.getRetryTimes());
            nr.setResult(n.getResult());
            nr.setErrorMsg(n.getErrorMsg());
            nodeRes.add(nr);
        });
        res.setNodes(nodeRes);
        return res;
    }

    private List<WorkflowNodeEntity> parseNodes(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<WorkflowNodeEntity>>() {});
        } catch (Exception e) {
            throw new BusinessException("Failed to parse workflow nodes: " + e.getMessage());
        }
    }

    private List<WorkflowEdgeEntity> parseEdges(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<WorkflowEdgeEntity>>() {});
        } catch (Exception e) {
            throw new BusinessException("Failed to parse workflow edges: " + e.getMessage());
        }
    }

    /** Work item: node id + whether it should run (false = skip due to unsatisfied deps). */
    private static final class Work {
        final String nodeId;
        final boolean run;
        Work(String nodeId, boolean run) { this.nodeId = nodeId; this.run = run; }
    }
}