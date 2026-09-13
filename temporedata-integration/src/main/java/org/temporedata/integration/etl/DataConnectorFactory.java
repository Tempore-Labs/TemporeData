package org.temporedata.integration.etl;

/**
 * [ENT-P0] Connector factory SPI.
 *
 * <p>Each {@code DataConnectorFactory} knows how to instantiate connectors of one or more
 * {@code type()}s, optionally loading the driver classes in an isolated {@link PluginClassLoader}
 * to avoid JDBC version conflicts between datasources.
 */
public interface DataConnectorFactory {

    /** Connector type(s) this factory supports, e.g. {"mysql"} or {"mysql","mariadb"}. */
    String[] supportedTypes();

    /** Whether a given type is supported. */
    default boolean supports(String type) {
        for (String t : supportedTypes()) {
            if (t.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    /** Create and {@code init} a connector of {@code type} with {@code config}. */
    Connector create(String type, ConnectorConfig config);

    /**
     * Isolation strategy hint: when true the registry builds a {@link PluginClassLoader} and
     * delegates driver resolution there.
     */
    default boolean isolatedLoading() {
        return false;
    }
}