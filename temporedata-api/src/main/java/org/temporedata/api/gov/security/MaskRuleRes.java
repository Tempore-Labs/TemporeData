package org.temporedata.api.gov.security;

import lombok.Data;

/**
 * Data masking rule response.
 */
@Data
public class MaskRuleRes {

    private String id;

    private String name;

    private String ruleType;

    private String maskPattern;

    private String description;

    private String datasourceId;

    private String tableName;

    private String columnName;

    private Integer status;

    private String createTime;

    private String updateTime;
}