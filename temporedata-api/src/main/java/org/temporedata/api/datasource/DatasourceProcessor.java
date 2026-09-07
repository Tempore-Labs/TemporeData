package org.temporedata.api.datasource;

import org.temporedata.api.datasource.conn.DatasourceConnection;
import org.temporedata.api.datasource.param.DatasourceParamDTO;

import java.sql.Connection;
import java.util.Collections;
import java.util.Set;

/**
 * Datasource dialect SPI (mirrors DolphinScheduler {@code DatasourceProcessor}).
 *
 * <p>One implementation per database type, provided by a dedicated module
 * ({@code temporedata-datasource-{type}}). A processor owns everything
 * dialect-specific: URL building, driver loading, connection and metadata
 * queries. The manager is fully decoupled from any single database.</p>
 */
public interface DatasourceProcessor {

    /** Unique type key. */
    DatasourceType type();

    /** Human readable display name, e.g. MySQL. */
    String dialectName();

    /** Default port used when the datasource does not specify one. */
    int defaultPort();

    /** Concrete {@link DatasourceParamDTO} class used by this dialect. */
    Class<? extends DatasourceParamDTO> paramClass();

    /**
     * Build the JDBC connection configuration (driver + url + credentials)
     * without opening a connection.
     */
    DatasourceConnection buildConnection(DatasourceParamDTO param);

    /**
     * Open a raw {@link Connection} to the datasource.
     *
     * @throws java.sql.SQLException on connection failure
     */
    Connection getConnection(DatasourceParamDTO param) throws Exception;

    /**
     * Test connectivity by opening and closing a connection.
     *
     * @return true when a connection can be established
     * @throws Exception on connection failure (checked path)
     */
    boolean check(DatasourceParamDTO param) throws Exception;

    /** Optional: list database names. Empty by default. */
    default Set<String> queryDatabases(DatasourceParamDTO param) {
        return Collections.emptySet();
    }

    /** Optional: list table names within a database. Empty by default. */
    default Set<String> queryTables(DatasourceParamDTO param, String database) {
        return Collections.emptySet();
    }
}