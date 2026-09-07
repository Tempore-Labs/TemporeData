package org.temporedata.api.gov.quality;

import lombok.Data;

/**
 * Quality rule response.
 */
@Data
public class QualityRuleRes {

    private String id;

    private String name;

    private String datasourceId;

    private String datasourceName;

    private String tableName;

    private String columnName;

    private String ruleType;

    private String ruleConfig;

    private String description;

    private String lastStatus;

    private String createTime;
}