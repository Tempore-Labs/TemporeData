package org.temporedata.integration.etl;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [ENT-P0] Connector registry &amp; factory lookup.
 *
 * <p>Holds {@link DataConnectorFactory} instances (registered via Spring or a manual init) and
 * creates connectors by type. For factories requesting isolated loading, a per-group
 * {@link PluginClassLoader} is maintained so differing JDBC driver versions stay apart.
 */
public class ConnectorRegistry {

    private final Map<String, DataConnectorFactory> factoriesByType = new ConcurrentHashMap<>();
    private final Map<String, PluginClassLoader> loadersByGroup = new ConcurrentHashMap<>();

    public void register(DataConnectorFactory factory) {
        for (String type : factory.supportedTypes()) {
            factoriesByType.put(type.toLowerCase(), factory);
        }
    }

    public boolean supports(String type) {
        return factoriesByType.containsKey(type.toLowerCase());
    }

    public void unregister(String type) {
        factoriesByType.remove(type.toLowerCase());
    }

    /**
     * Create a connector of {@code type}. An isolated factory gets the union of the given
     * {@code pluginUrls} (driver jars) on a child-first classloader for the target type.
     */
    public Connector create(String type, ConnectorConfig config, URL[] pluginUrls) {
        DataConnectorFactory factory = factoriesByType.get(type.toLowerCase());
        if (factory == null) {
            throw new IllegalArgumentException("no connector factory for type: " + type);
        }
        if (!factory.isolatedLoading() || pluginUrls == null || pluginUrls.length == 0) {
            return factory.create(type, config);
        }
        PluginClassLoader loader = loadersByGroup.computeIfAbsent(
                type.toLowerCase(),
                k -> new PluginClassLoader("connector-" + k, pluginUrls, getClass().getClassLoader()));
        return makeInLoader(loader, factory, type, config);
    }

    private Connector makeInLoader(PluginClassLoader loader, DataConnectorFactory factory,
                                   String type, ConnectorConfig config) {
        // Load the concrete connector class via the isolated loader and instantiate reflectively.
        // Keeping this path minimal: the factory is trusted, driver deps are isolated.
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            return factory.create(type, config);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}