package org.temporedata.modules.dev.workflow.runner;

import com.alibaba.druid.DbType;
import com.alibaba.druid.wall.Violation;
import com.alibaba.druid.wall.WallCheckResult;
import com.alibaba.druid.wall.WallProvider;
import com.alibaba.druid.wall.spi.ClickhouseWallProvider;
import com.alibaba.druid.wall.spi.MySqlWallProvider;
import com.alibaba.druid.wall.spi.OracleWallProvider;
import com.alibaba.druid.wall.spi.PGWallProvider;
import org.temporedata.api.datasource.sql.Dialect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL injection firewall (P3) backed by Druid's WallFilter rules. Cheap,
 * dialect-aware check that a statement contains no obvious malicious SQL;
 * returns the list of violations (empty = safe). Resolves the DbType from
 * {@link SqlDialectRegistry} so new datasources are covered without code change.
 */
@Component
public class SqlFirewall {

    private final SqlDialectRegistry registry;

    public SqlFirewall(SqlDialectRegistry registry) {
        this.registry = registry;
    }

    public List<String> validate(String sql, Dialect dialect) {
        try {
            WallProvider provider = providerFor(registry.dbType(dialect));
            WallCheckResult result = provider.check(sql);
            List<String> violations = new ArrayList<>();
            for (Violation v : result.getViolations()) {
                violations.add(v.getMessage());
            }
            return violations;
        } catch (Exception e) {
            return List.of("SQL check error: " + e.getMessage());
        }
    }

    private WallProvider providerFor(DbType dbType) {
        if (dbType == DbType.postgresql) {
            return new PGWallProvider();
        }
        if (dbType == DbType.oracle) {
            return new OracleWallProvider();
        }
        if (dbType == DbType.clickhouse) {
            return new ClickhouseWallProvider();
        }
        // MySQL + Doris/StarRocks (MySQL protocol) + unknown fallback
        return new MySqlWallProvider();
    }
}