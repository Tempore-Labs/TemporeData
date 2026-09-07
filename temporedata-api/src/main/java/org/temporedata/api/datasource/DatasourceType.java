package org.temporedata.api.datasource;

/**
 * Supported datasource type keys. Both built-in and external plugins resolve
 * against this enumeration (case-insensitive lookup by uppercase name).
 */
public enum DatasourceType {

    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    CLICKHOUSE("ClickHouse"),
    ORACLE("Oracle"),
    DORIS("Doris"),
    STARROCKS("StarRocks"),
    HIVE("Hive"),
    OCEANBASE("OceanBase");

    private final String displayName;

    DatasourceType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Case-insensitive lookup; returns null when the type is unknown. */
    public static DatasourceType of(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}