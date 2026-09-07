package org.temporedata.api.dev.workflow;

import lombok.Data;

/**
 * Per-node status within a workflow run (P0-2).
 */
@Data
public class WorkflowNodeInstanceRes {

    private String id;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private String status;
    private String startTime;
    private String finishTime;
    private Long durationMs;
    private Integer retryTimes;
    private String result;
    private String errorMsg;
}