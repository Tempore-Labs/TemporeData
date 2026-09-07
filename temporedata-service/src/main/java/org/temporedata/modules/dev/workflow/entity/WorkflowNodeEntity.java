package org.temporedata.modules.dev.workflow.entity;

import lombok.Data;

/**
 * Workflow node — stored as JSON inside WorkflowEntity.nodesJson.
 */
@Data
public class WorkflowNodeEntity {

    private String id;
    private String name;
    private String type; // SQL, SHELL, PYTHON
    private String datasourceId;
    private String datasourceName;
    /** Optional SQL data source type (e.g. MYSQL / CLICKHOUSE) for dialect-aware lineage parsing (P2). */
    private String datasourceType;
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