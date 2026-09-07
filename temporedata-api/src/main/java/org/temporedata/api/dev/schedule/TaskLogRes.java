package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * Per-instance log entry.
 */
@Data
public class TaskLogRes {

    private String id;
    private String instanceId;
    private String level;
    private String message;
    private String createTime;
}