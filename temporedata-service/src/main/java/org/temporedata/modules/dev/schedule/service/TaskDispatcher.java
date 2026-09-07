package org.temporedata.modules.dev.schedule.service;

import org.temporedata.modules.dev.schedule.entity.TaskDefineEntity;
import org.temporedata.modules.dev.schedule.entity.TaskInstanceEntity;

/**
 * Strategy that actually runs a task instance's target (workflow, sql, http...).
 * Scheduler (trigger) and dispatcher (execution) are decoupled. New backends
 * (e.g. remote workers via MQ) plug in here without touching the coordinator.
 */
public interface TaskDispatcher {

    /**
     * Dispatch the target of <code>task</code> for <code>instance</code>.
     *
     * @return ok/fail result with a human-readable message
     */
    TaskDispatchResult dispatch(TaskDefineEntity task, TaskInstanceEntity instance);
}