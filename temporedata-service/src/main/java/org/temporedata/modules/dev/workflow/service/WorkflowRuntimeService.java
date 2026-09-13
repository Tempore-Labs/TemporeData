package org.temporedata.modules.dev.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.workflow.RuntimeCommandRes;
import org.temporedata.api.dev.workflow.WorkflowRunRes;
import org.temporedata.modules.dev.workflow.entity.WorkflowInstanceEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeInstanceEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowRunCommandEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowInstanceRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowNodeInstanceRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowRunCommandRepository;
import org.temporedata.modules.dev.workflow.runner.RuntimeControlRegistry;
import org.temporedata.modules.dev.workflow.runner.WorkflowRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Runtime management (P0-3): pause / resume / stop / rerun / priority / pool.
 *
 * Control commands set an in-memory signal that the running workflow thread
 * observes (no unsafe writes to the instance row from the command thread),
 * and are recorded to zy_wf_run_command for audit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowRuntimeService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowNodeInstanceRepository nodeInstanceRepository;
    private final WorkflowRunCommandRepository commandRepository;
    private final RuntimeControlRegistry controlRegistry;
    private final WorkflowRunner workflowRunner;

    // ---- Control commands ----

    @Transactional
    public void pause(String instanceId, String operator) {
        WorkflowInstanceEntity inst = findInstance(instanceId);
        requireActive(inst, "pause");
        controlRegistry.action(instanceId).requestPause();
        recordCommand(instanceId, "Pause", "ALL", null, operator, "paused by " + operator);
        log.info("Pause requested for instance {}", instanceId);
    }

    @Transactional
    public void resume(String instanceId, String operator) {
        WorkflowInstanceEntity inst = findInstance(instanceId);
        if (!"PAUSED".equals(inst.getStatus()) && !"RUNNING".equals(inst.getStatus())) {
            throw new BusinessException("Instance is not pausable/running, cannot resume: " + instanceId);
        }
        controlRegistry.action(instanceId).requestResume();
        recordCommand(instanceId, "Resume", "ALL", null, operator, "resumed by " + operator);
        log.info("Resume requested for instance {}", instanceId);
    }

    @Transactional
    public void stop(String instanceId, String operator) {
        WorkflowInstanceEntity inst = findInstance(instanceId);
        requireActive(inst, "stop");
        controlRegistry.action(instanceId).requestStop();
        recordCommand(instanceId, "Stop", "ALL", null, operator, "stopped by " + operator);
        log.info("Stop requested for instance {}", instanceId);
    }

    /**
     * Rerun: create a fresh instance for the workflow with the source's
     * priority/pool. scope=NODE (from a failed node) currently reruns the whole
     * workflow; per-node sub-DAG rerun is reserved for a follow-up.
     */
    @Transactional
    public WorkflowRunRes rerun(String instanceId, String scope, String fromNodeId, String operator) {
        WorkflowInstanceEntity inst = findInstance(instanceId);
        String s = scope == null ? "ALL" : scope;
        recordCommand(instanceId, "Rerun", s, fromNodeId, operator, "rerun by " + operator);
        int priority = inst.getPriority() == null ? 5 : inst.getPriority();
        String pool = inst.getPool() == null ? "default" : inst.getPool();
        log.info("Rerun instance {} (scope {})", instanceId, s);
        return workflowRunner.run(inst.getWorkflowId(), "RERUN", null, priority, pool);
    }

    /**
     * Force-mark a failed node instance as SUCCESS so the rest of the DAG can
     * proceed. Records an audit command; only applies to non-active instances.
     */
    @Transactional
    public void forceSuccess(String instanceId, String nodeInstanceId, String operator) {
        WorkflowInstanceEntity inst = findInstance(instanceId);
        if ("RUNNING".equals(inst.getStatus()) || "PAUSED".equals(inst.getStatus())) {
            throw new BusinessException("Instance is active, cannot force success while running: " + instanceId);
        }
        WorkflowNodeInstanceEntity ni = nodeInstanceRepository.findById(nodeInstanceId)
                .orElseThrow(() -> new BusinessException("Node instance not found: " + nodeInstanceId));
        ni.setStatus("SUCCESS");
        ni.setErrorMsg(null);
        ni.setResult("[force success] by " + operator);
        ni.setFinishTime(LocalDateTime.now());
        nodeInstanceRepository.save(ni);
        recordCommand(instanceId, "ForceSuccess", "NODE", ni.getNodeId(), operator,
                "force success " + ni.getNodeName() + " by " + operator);
        log.info("Forced node instance {} to success", nodeInstanceId);
    }

    /** Re-run failed nodes (and their downstream) from the last run. */
    @Transactional
    public WorkflowRunRes recoverFailed(String instanceId, String operator) {
        WorkflowInstanceEntity inst = findInstance(instanceId);
        recordCommand(instanceId, "RecoverFailed", "FAILED", null, operator, "recover failed by " + operator);
        int priority = inst.getPriority() == null ? 5 : inst.getPriority();
        String pool = inst.getPool() == null ? "default" : inst.getPool();
        return workflowRunner.run(inst.getWorkflowId(), "RECOVER", null, priority, pool);
    }

    /**
     * Backfill: generate one run per historical date between start and end.
     * DAG node dependencies are preserved per-run by the runner; instances are
     * either serialised (default) or issued concurrently.
     */
    @Transactional
    public Map<String, Object> backfill(String workflowId, String start, String end,
                                        String interval, String concurrency) {
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        if (s.isAfter(e)) {
            throw new BusinessException("backfill start must be <= end");
        }
        List<String> dates = new ArrayList<>();
        for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
            dates.add(d.toString());
        }
        boolean serialize = !"PARALLEL".equalsIgnoreCase(concurrency);
        if (serialize) {
            for (String date : dates) {
                workflowRunner.run(workflowId, "BACKFILL", null, 5, "default", date);
            }
        } else {
            CompletableFuture<?>[] futures = dates.stream()
                    .map(date -> CompletableFuture.runAsync(
                            () -> workflowRunner.run(workflowId, "BACKFILL", null, 5, "default", date)))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
        }
        Map<String, Object> out = new HashMap<>();
        out.put("workflowId", workflowId);
        out.put("generated", dates.size());
        out.put("interval", interval == null ? "DAILY" : interval);
        out.put("concurrency", serialize ? "SERIAL" : "PARALLEL");
        out.put("dates", dates);
        return out;
    }

    // ---- Priority / pool ----

    @Transactional
    public void setPriority(String instanceId, int priority) {
        WorkflowInstanceEntity inst = findInstance(instanceId);
        inst.setPriority(priority);
        instanceRepository.save(inst);
    }

    @Transactional
    public void setPool(String instanceId, String pool) {
        WorkflowInstanceEntity inst = findInstance(instanceId);
        inst.setPool(pool);
        instanceRepository.save(inst);
    }

    // ---- Query ----

    @Transactional(readOnly = true)
    public List<RuntimeCommandRes> commands(String instanceId) {
        return commandRepository.findByInstanceIdOrderByCreateTimeAsc(instanceId).stream()
                .map(this::toCommandRes)
                .collect(Collectors.toList());
    }

    // ---- Helpers ----

    private WorkflowInstanceEntity findInstance(String id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Workflow instance not found: " + id));
    }

    private void requireActive(WorkflowInstanceEntity inst, String op) {
        String status = inst.getStatus();
        if (!"RUNNING".equals(status) && !"PAUSED".equals(status)) {
            throw new BusinessException("Instance is " + status + ", cannot " + op);
        }
    }

    private void recordCommand(String instanceId, String type, String scope,
                               String targetNodeId, String operator, String reason) {
        try {
            commandRepository.save(WorkflowRunCommandEntity.builder()
                    .instanceId(instanceId)
                    .type(type)
                    .scope(scope)
                    .targetNodeId(targetNodeId)
                    .state("DONE")
                    .operator(operator)
                    .reason(reason)
                    .createTime(LocalDateTime.now())
                    .doneTime(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record run command for instance {}", instanceId, e);
        }
    }

    private RuntimeCommandRes toCommandRes(WorkflowRunCommandEntity e) {
        RuntimeCommandRes r = new RuntimeCommandRes();
        r.setId(e.getId());
        r.setInstanceId(e.getInstanceId());
        r.setType(e.getType());
        r.setScope(e.getScope());
        r.setTargetNodeId(e.getTargetNodeId());
        r.setState(e.getState());
        r.setOperator(e.getOperator());
        r.setReason(e.getReason());
        r.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().format(DTF) : null);
        r.setDoneTime(e.getDoneTime() != null ? e.getDoneTime().format(DTF) : null);
        return r;
    }
}