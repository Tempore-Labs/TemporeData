package org.temporedata.api.asset.indicator;

import lombok.Data;

/**
 * Indicator list / detail response.
 */
@Data
public class IndicatorRes {

    private String id;

    private String name;

    private String code;

    private String type;

    private String description;

    private String querySql;

    private String datasourceId;

    private String unit;

    private String tenantId;

    private String version;

    private String theme;

    private String owner;

    private String status;

    private String scheduleCron;

    private Boolean scheduleEnabled;

    private String createTime;

    private String updateTime;

    private String latestStatus;

    private String latestValue;

    private String latestExecuteTime;

    private Long latestDurationMs;

    private String latestErrorMsg;
}