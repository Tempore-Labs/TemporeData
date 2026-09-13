package org.temporedata.integration.etl;

/**
 * [ENT-P0] Base Connector SPI: identity + lifecycle.
 *
 * <p>A {@code Connector} is a pluggable data-movement adapter. Implementations must expose a
 * unique {@code type()} and be constructible through {@link DataConnectorFactory} for that
 * type. Lifecycle is {@code init(config)} -&gt; use -&gt; {@code close()}.
 */
public interface Connector {

    /** Connector identifier, e.g. "mysql", "postgresql", "clickhouse". */
    String type();

    /** Friendly descriptor for UIs / logs. */
    default String displayName() {
        return type();
    }

    /** Validate + apply the runtime config. Throws on unsupported/invalid options. */
    void init(ConnectorConfig config);

    /** Release resources (connections, threads, decoder handles). Idempotent. */
    void close();
}