package org.temporedata.api.dev.workflow;

import lombok.Data;

/**
 * Workflow edge in response.
 */
@Data
public class WorkflowEdgeRes {

    private String id;

    private String sourceNodeId;

    private String targetNodeId;

    private String edgeType;

    private String conditionExpr;
}