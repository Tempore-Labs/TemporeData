package org.temporedata.integration.etl;

import java.util.List;

/**
 * [ENT-P0] Schema discovery SPI.
 *
 * <p>Reads physical schema (tables / columns / types / indexes / comments) from the remote
 * database. Feeds the Metadata Center's Schema Crawler and DDL-building pipelines.
 */
public interface SchemaReader extends Connector {

    /** Qualified table names visible to the configured account/tenant. */
    List<String> listTables(String schema);

    /** Logical columns of one table. */
    List<ColumnSchema> readSchema(String table);

    /** DDL statement that materializes {@code table} (normalized dialect DDL). */
    default String toDdl(String table) {
        return "";
    }
}