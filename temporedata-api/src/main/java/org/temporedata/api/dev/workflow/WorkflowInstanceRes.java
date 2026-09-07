package org.temporedata.api.dev.workflow;

import lombok.Data;

import java.util.List;

/**
 * Workflow instance detail including per-node statuses (P0-2).
 */
@Data
public class WorkflowInstanceRes {

    private String id;
    private String workflowId;
    private String taskInstanceId;
    private String status;
    private String triggerType;
    private String startTime;
    private String finishTime;
    private String resultMsg;
    private List<WorkflowNodeInstanceRes> nodes;
}