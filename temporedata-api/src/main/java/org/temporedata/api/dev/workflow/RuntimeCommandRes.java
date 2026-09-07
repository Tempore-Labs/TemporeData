package org.temporedata.api.dev.workflow;

import lombok.Data;

/**
 * Auditable runtime command record (pause / resume / stop / rerun).
 */
@Data
public class RuntimeCommandRes {

    private String id;
    private String instanceId;
    private String type;
    private String scope;
    private String targetNodeId;
    private String state;
    private String operator;
    private String reason;
    private String createTime;
    private String doneTime;
}