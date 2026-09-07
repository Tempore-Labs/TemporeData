package org.temporedata.api.gov.quality;

import lombok.Data;

/**
 * Quality rule create / update request.
 */
@Data
public class QualityRuleReq {

    private String name;

    private String datasourceId;

    private String tableName;

    private String columnName;

    private String ruleType; // NOT_NULL, UNIQUE, RANGE, REGEX, CUSTOM_SQL

    private String ruleConfig; // JSON: {"min":0,"max":100} or {"pattern":"^[a-z]+$"} or raw SQL

    private String description;
}