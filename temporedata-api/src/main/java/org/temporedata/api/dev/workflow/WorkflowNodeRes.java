package org.temporedata.api.dev.workflow;

import lombok.Data;

/**
 * Workflow node in response.
 */
@Data
public class WorkflowNodeRes {

    private String id;

    private String name;

    private String type;

    private String datasourceId;

    private String datasourceName;

    private String sql;

    private Integer positionX;

    private Integer positionY;

    private Integer retryCount;

    private Integer retryInterval;

    private Integer timeoutSeconds;

    private String priority;

    private String failStrategy;

    private String params;

    // ---- New node type fields ----
    private String sparkConf;
    private String httpUrl;
    private String httpMethod;
    private String httpHeaders;
    private String dependencyType;
    private Integer dependencyTimeout;
    private String emailTo;
    private String emailSubject;
    private String qualityType;
    private Integer qualityThreshold;
}