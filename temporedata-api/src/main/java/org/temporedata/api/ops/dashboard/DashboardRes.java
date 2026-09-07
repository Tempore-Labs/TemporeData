package org.temporedata.api.ops.dashboard;

import lombok.Data;

import java.util.List;

/**
 * Dashboard overview response with aggregated stats.
 */
@Data
public class DashboardRes {

    // Overall stats
    private int datasourceCount;
    private int tableCount;
    private long totalRows;
    private int syncTaskCount;
    private int syncSuccessCount;
    private int syncFailCount;
    private int qualityRuleCount;
    private int qualityPassCount;
    private int qualityFailCount;
    private int workflowCount;
    private int apiCount;
    private long apiCallTotal;

    // Sync status distribution
    private List<StatItem> syncStatus;

    // Quality status distribution
    private List<StatItem> qualityStatus;

    // Top tables by row count
    private List<TableStat> topTables;

    // Recent sync tasks
    private List<SyncStat> recentSyncs;

    // Hourly execution trend (last 24h)
    private List<TrendItem> hourlyTrend;

    // Datasource type distribution
    private List<StatItem> datasourceTypeDist;

    // Failed sync tasks
    private List<FailedTask> failedSyncs;

    // Failed quality rules
    private List<FailRule> failedRules;

    // Datasource list (name/type)
    private List<DataSourceStat> datasources;

    @Data
    public static class StatItem {
        private String name;
        private int value;
    }

    @Data
    public static class TableStat {
        private String tableName;
        private long rowCount;
    }

    @Data
    public static class SyncStat {
        private String taskName;
        private String status;
        private String lastRunTime;
        private Integer rowCount;
    }

    @Data
    public static class TrendItem {
        private String hour;
        private int success;
        private int failed;
    }

    @Data
    public static class FailedTask {
        private String taskName;
        private String targetTable;
        private String lastRunTime;
        private String errorMsg;
    }

    @Data
    public static class FailRule {
        private String ruleName;
        private String tableName;
        private String columnName;
        private String ruleType;
        private String description;
    }

    @Data
    public static class DataSourceStat {
        private String name;
        private String type;
    }
}