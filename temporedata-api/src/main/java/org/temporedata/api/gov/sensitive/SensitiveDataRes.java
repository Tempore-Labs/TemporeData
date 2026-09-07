package org.temporedata.api.gov.sensitive;

import lombok.Data;

/**
 * Sensitive data response DTO.
 */
@Data
public class SensitiveDataRes {

    private String id;

    private String datasourceId;

    private String tableName;

    private String columnName;

    private String sensitiveType;

    private String maskRule;

    private String level;

    private String description;

    private String status;

    private String tenantId;

    private String createTime;

    private String updateTime;
}