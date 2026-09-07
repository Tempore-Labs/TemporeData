package org.temporedata.api.gov.security;

import lombok.Data;

/**
 * Data masking rule request.
 */
@Data
public class MaskRuleReq {

    private String name;

    private String ruleType; // PHONE, EMAIL, ID_CARD, NAME, BANK_CARD, CUSTOM

    private String maskPattern; // e.g. "***", "####", or regex

    private String description;

    private String datasourceId; // bind to datasource

    private String tableName; // bind to table

    private String columnName; // bind to column

    private Integer status; // 1=enabled, 0=disabled
}