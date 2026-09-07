package org.temporedata.api.datasource.sql;

import org.temporedata.api.datasource.DatasourceType;

/**
 * SQL dialect discriminator used to route SQL parsing to the best-fit parser.
 * Derived from the datasource plug-in type; unknown/null degrades to {@link #OTHER}.
 */
public enum Dialect {

    MYSQL,
    POSTGRESQL,
    ORACLE,
    CLICKHOUSE,
    DORIS,
    STARROCKS,
    HIVE,
    OCEANBASE,
    OTHER;

    /**
     * Map a datasource type to its parsing dialect. Unknown types map to {@link #OTHER}.
     */
    public static Dialect from(DatasourceType type) {
        if (type == null) {
            return OTHER;
        }
        switch (type) {
            case MYSQL: return MYSQL;
            case POSTGRESQL: return POSTGRESQL;
            case ORACLE: return ORACLE;
            case CLICKHOUSE: return CLICKHOUSE;
            case DORIS: return DORIS;
            case STARROCKS: return STARROCKS;
            case HIVE: return HIVE;
            case OCEANBASE: return OCEANBASE;
            default: return OTHER;
        }
    }
}