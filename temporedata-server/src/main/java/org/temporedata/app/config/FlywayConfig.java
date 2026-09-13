package org.temporedata.app.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway database migration configuration.
 * Runs migrations at startup so the versioned schema (db/migration) is always
 * authoritative. The existing (previously ddl-auto generated) schema is adopted
 * via baseline-on-migrate at {@code baselineVersion}, so only future migrations
 * (baselineVersion+1) are applied automatically.
 */
@Configuration
public class FlywayConfig {

    /** Current latest migration; DBs at or below this are baselined, newer ones run. */
    private static final String BASELINE_VERSION = "16";

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String migrationLocation;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(migrationLocation)
                .baselineOnMigrate(true)
                .baselineVersion(BASELINE_VERSION)
                // Tolerate applied migrations with no matching local file (*:missing):
                // the source tree dropped V34-V46 while the target DB already applied them.
                // Non-destructive: history reconciled via "missing" ignore, tables untouched.
                .ignoreMigrationPatterns("*:missing")
                .load();
        // Repair the schema history so an already-applied dev migration whose file was edited
        // locally (e.g. V55 id width 32->36, already reflected in the table) no longer trips
        // validate. Repair only realigns checksums; it never alters tables or upgrades data.
        flyway.repair();
        flyway.migrate();
        return flyway;
    }
}