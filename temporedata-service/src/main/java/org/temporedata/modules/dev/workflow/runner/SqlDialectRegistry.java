package org.temporedata.modules.dev.workflow.runner;

import com.alibaba.druid.DbType;
import org.temporedata.api.datasource.sql.Dialect;
import org.temporedata.api.datasource.sql.SqlDialectContribution;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Single source of truth for the {@link Dialect} → Druid {@link DbType} mapping.
 * Parser and firewall read the DbType from here instead of hand-written switches,
 * so a new datasource only needs a registration (or a SPI contribution) rather
 * than rewriting the parser (see design doc §4.4).
 */
@Component
public class SqlDialectRegistry {

    private final Map<Dialect, DbType> mapping = new LinkedHashMap<>();

    public SqlDialectRegistry() {
        mapping.put(Dialect.MYSQL, DbType.mysql);
        mapping.put(Dialect.POSTGRESQL, DbType.postgresql);
        mapping.put(Dialect.ORACLE, DbType.oracle);
        mapping.put(Dialect.CLICKHOUSE, DbType.clickhouse);
        // Doris / StarRocks speak the MySQL protocol
        mapping.put(Dialect.DORIS, DbType.mysql);
        mapping.put(Dialect.STARROCKS, DbType.mysql);
        // OceanBase speaks the MySQL protocol; Hive SQL is approximated via mysql parsing
        mapping.put(Dialect.OCEANBASE, DbType.mysql);
        mapping.put(Dialect.HIVE, DbType.mysql);
        // unknown dialect degrades to mysql parsing
        mapping.put(Dialect.OTHER, DbType.mysql);
    }

    /** Auto-load dialect contributions from datasource plug-ins (SPI, §4.4 L2). */
    @PostConstruct
    public void loadContributions() {
        for (SqlDialectContribution contribution : ServiceLoader.load(SqlDialectContribution.class)) {
            try {
                register(contribution.dialect(), DbType.valueOf(contribution.dbTypeName().toUpperCase()));
            } catch (IllegalArgumentException ignore) {
                // unknown DbType name: skip contribution
            }
        }
    }

    /** Register or override a dialect-to-DbType mapping (extension point for new datasources). */
    public synchronized void register(Dialect dialect, DbType dbType) {
        if (dialect != null && dbType != null) {
            mapping.put(dialect, dbType);
        }
    }

    public boolean exists(Dialect dialect) {
        return dialect != null && mapping.containsKey(dialect);
    }

    public DbType dbType(Dialect dialect) {
        Dialect key = dialect == null ? Dialect.OTHER : dialect;
        DbType dbType = mapping.get(key);
        return dbType != null ? dbType : DbType.mysql;
    }
}