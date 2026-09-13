package org.temporedata.api.dev.workflow;

import lombok.Data;

import java.util.List;

/**
 * Workflow detail response (includes nodes, edges, schedules).
 */
@Data
public class WorkflowRes {

    private String id;

    private String name;

    private String description;

    private String status;

    private String cronExpression;

    private String schedulePolicy;

    private String scheduleMissfire;

    private List<WorkflowNodeRes> nodes;

    private List<WorkflowEdgeRes> edges;

    private String createTime;
}