package org.temporedata.api.asset.indicator;

import lombok.Data;

/**
 * Create / update indicator request.
 */
@Data
public class IndicatorReq {

    private String name;

    private String code;

    private String type; // COUNT, SUM, AVG, RATIO, CUSTOM

    private String description;

    private String querySql;

    private String datasourceId;

    private String unit;

    private String version;

    private String theme;

    private String owner;

    private String status; // ENABLED / DISABLED

    private String scheduleCron;

    private Boolean scheduleEnabled;
}