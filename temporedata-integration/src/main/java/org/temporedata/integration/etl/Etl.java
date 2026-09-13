package org.temporedata.integration.etl;

/**
 * [ENT-P0] ETL / CDC engine namespace root.
 *
 * <p>Hosts the Connector SPI ({@link Connector} and its Reader / Writer / CdcReader /
 * SchemaReader specializations), value types (DataRow / ColumnSchema / ChangeEvent) and the
 * isolation loader (PluginClassLoader) with a registry (ConnectorRegistry).
 *
 * <p>Boundary contract: atomic data movement &amp; change-data capture live here; upper layers
 * (development / metadata / quality / asset) consume this SPI, never the reverse.
 */
public final class Etl {

    private Etl() {
        // namespace marker only
    }
}