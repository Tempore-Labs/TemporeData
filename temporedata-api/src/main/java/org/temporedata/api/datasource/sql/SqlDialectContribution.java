package org.temporedata.api.datasource.sql;

/**
 * SPI: a datasource plug-in declares how its {@link Dialect} maps to a Druid
 * {@code DbType} name, so a new datasource is automatically covered by the single
 * parser / firewall without modifying them (§4.4 L2). Loaded via
 * {@code META-INF/services/org.temporedata.api.datasource.sql.SqlDialectContribution}.
 */
public interface SqlDialectContribution {

    /** Dialect this contribution provides. */
    Dialect dialect();

    /** Druid {@code com.alibaba.druid.DbType} enum name, e.g. "mysql" / "clickhouse". */
    String dbTypeName();
}