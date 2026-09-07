package org.temporedata.api.dev.workflow;

import lombok.Data;

import java.util.List;

/**
 * Create / update workflow request.
 */
@Data
public class WorkflowReq {

    private String name;

    private String description;

    private List<WorkflowNodeReq> nodes;

    private List<WorkflowEdgeReq> edges;
}