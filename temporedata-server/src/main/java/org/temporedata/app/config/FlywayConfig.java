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
                .load();
        flyway.migrate();
        return flyway;
    }
}