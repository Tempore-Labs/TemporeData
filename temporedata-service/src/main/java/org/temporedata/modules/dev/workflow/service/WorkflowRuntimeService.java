package org.temporedata.modules.dev.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.workflow.RuntimeCommandRes;
import org.temporedata.api.dev.workflow.WorkflowRunRes;
import org.temporedata.modules.dev.workflow.entity.WorkflowInstanceEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowRunCommandEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowInstanceRepository;
import org.temporedata.modules.dev.workflow.repository.WorkflowRunCommandRepository;
import org.temporedata.modules.dev.workflow.runner.RuntimeControlRegistry;
import org.temporedata.modules.dev.workflow.runner.WorkflowRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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