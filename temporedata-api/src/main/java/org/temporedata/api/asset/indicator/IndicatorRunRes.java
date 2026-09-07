package org.temporedata.api.asset.indicator;

import lombok.Data;

/**
 * Indicator execution history record DTO.
 */
@Data
public class IndicatorRunRes {

    private String id;

    private String indicatorId;

    private String resultValue;

    private String unit;

    private Long durationMs;

    private String status;

    private String errorMsg;

    private String executeTime;
}