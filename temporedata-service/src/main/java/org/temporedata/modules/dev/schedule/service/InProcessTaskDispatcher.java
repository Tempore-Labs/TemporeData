package org.temporedata.modules.dev.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.dev.workflow.WorkflowRunRes;
import org.temporedata.modules.dev.schedule.entity.TaskDefineEntity;
import org.temporedata.modules.dev.schedule.entity.TaskInstanceEntity;
import org.temporedata.modules.dev.schedule.execute.SchedulerTaskExecutor;
import org.temporedata.modules.dev.workflow.runner.WorkflowRunner;
import org.springframework.stereotype.Component;

/**
 * In-process dispatcher: runs the target directly on the executing node.
 * WORKFLOW targets are dispatched to the real DAG runtime; SQL / SCRIPT / SYNC
 * are dispatched to real executors (JDBC / sandboxed shell / JDBC table copy).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InProcessTaskDispatcher implements TaskDispatcher {

    private final WorkflowRunner workflowRunner;
    private final SchedulerTaskExecutor executor;

    @Override
    public TaskDispatchResult dispatch(TaskDefineEntity task, TaskInstanceEntity instance) {
        String type = task.getTaskType() == null ? "WORKFLOW" : task.getTaskType();
        String target = task.getTargetRef();
        String params = task.getParamsJson();
        switch (type.toUpperCase()) {
            case "SQL":
                return executor.sql(target, params, instance.getBizDate());
            case "SCRIPT":
                return executor.script(target, instance.getBizDate());
            case "SYNC":
                return executor.sync(target, params);
            case "WORKFLOW":
                if (target == null || target.isBlank()) {
                    return TaskDispatchResult.fail("WORKFLOW task has empty target_ref");
                }
                try {
                    WorkflowRunRes run = workflowRunner.run(
                            target, "SCHEDULE", instance.getId(), 5, "default", instance.getBizDate());
                    if ("SUCCESS".equals(run.getStatus())) {
                        return TaskDispatchResult.ok("Workflow run success (instance " + run.getInstanceId() + ")");
                    }
                    return TaskDispatchResult.fail("Workflow run " + run.getStatus()
                            + (run.getNodeResults() == null ? "" : " (" + run.getNodeResults().size() + " nodes)"));
                } catch (Exception e) {
                    log.error("Workflow dispatch failed for target {}", target, e);
                    return TaskDispatchResult.fail("Workflow dispatch failed: " + e.getMessage());
                }
            default:
                return TaskDispatchResult.fail("Task type not implemented yet: " + type);
        }
    }
}