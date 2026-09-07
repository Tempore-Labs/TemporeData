package org.temporedata.modules.dev.workflow.entity;

import lombok.Data;

/**
 * Workflow edge — stored as JSON inside WorkflowEntity.edgesJson.
 */
@Data
public class WorkflowEdgeEntity {

    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private String edgeType; // SUCCESS, FAILURE, ALWAYS
    private String conditionExpr;
}