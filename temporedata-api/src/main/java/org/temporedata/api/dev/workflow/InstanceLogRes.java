package org.temporedata.api.dev.workflow;

import lombok.Data;

/**
 * Workflow instance log entry (P0-2).
 */
@Data
public class InstanceLogRes {

    private String id;
    private String nodeInstanceId;
    private String level;
    private String message;
    private String createTime;
}