package org.temporedata.api.asset.indicator;

import lombok.Data;

/**
 * Indicator execution statistics for the dashboard cards.
 */
@Data
public class IndicatorStatsRes {

    private int totalRuns;

    private int successRuns;

    private int failedRuns;
}