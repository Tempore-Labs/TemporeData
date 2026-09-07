package org.temporedata.api.dev.workflow;

import lombok.Data;

/**
 * Workflow edge (dependency) definition.
 */
@Data
public class WorkflowEdgeReq {

    private String id;

    private String sourceNodeId;

    private String targetNodeId;

    private String edgeType; // SUCCESS, FAILURE, ALWAYS

    private String conditionExpr;
}