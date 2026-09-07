package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * Task instance response (one fire of a task).
 */
@Data
public class TaskInstanceRes {

    private String id;
    private String taskId;
    private Long instanceNo;
    private String triggerTime;
    private String startTime;
    private String finishTime;
    private String bizDate;
    /** PENDING | RUNNING | SUCCESS | FAILED | TIMEOUT | SKIPPED | CANCELED. */
    private String status;
    private String triggerNode;
    private String resultMsg;
}