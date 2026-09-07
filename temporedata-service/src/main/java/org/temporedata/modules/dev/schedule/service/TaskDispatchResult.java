package org.temporedata.modules.dev.schedule.service;

import lombok.Data;

/**
 * Result of dispatching a task instance.
 */
@Data
public class TaskDispatchResult {

    private final boolean success;
    private final String message;

    public static TaskDispatchResult ok(String message) {
        return new TaskDispatchResult(true, message);
    }

    public static TaskDispatchResult fail(String message) {
        return new TaskDispatchResult(false, message);
    }
}