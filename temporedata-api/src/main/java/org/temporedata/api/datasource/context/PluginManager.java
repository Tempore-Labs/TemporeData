package org.temporedata.api.datasource.context;

import org.temporedata.api.datasource.DatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads uploaded driver plugin jars (a single local jar or a set resolved from
 * a Maven GAV) in isolation and registers them into {@link DatasourcePluginContext}.
 *
 * <p>Contract: the jar set must contain a class implementing
 * {@link DatasourceProcessor} and declare it via the standard SPI descriptor
 * {@code META-INF/services/org.temporedata.api.datasource.DatasourceProcessor}.
 * Each set is loaded by its own {@link URLClassLoader} so drivers and plugin
 * classes do not collide with other modules.</p>
 */
@Component
public class PluginManager {

    private final DatasourcePluginContext context;

    /** primary key -> classloader (keeps loaded drivers alive). */
    private final Map<String, URLClassLoader> loaders = new ConcurrentHashMap<>();

    /** primary key -> registered type. */
    private final Map<String, DatasourceType> loadedTypes = new ConcurrentHashMap<>();

    public PluginManager(DatasourcePluginContext context) {
        this.context = context;
    }

    /** Load a single local driver jar (legacy path-based upload). */
    public PluginLoadResult load(String jarPath) {
        return loadFiles(List.of(new File(jarPath)), jarPath);
    }

    /** Load a set of jars (driver + transitive deps) resolved from a Maven GAV. */
    public PluginLoadResult loadFiles(List<File> jars) {
        return loadFiles(jars, jars.isEmpty() ? "" : jars.get(0).getAbsolutePath());
    }

    private PluginLoadResult loadFiles(List<File> jars, String key) {
        if (jars == null || jars.isEmpty()) {
            throw new IllegalArgumentException("No plugin jar provided");
        }
        URL[] urls = jars.stream().map(this::toUrl).toArray(URL[]::new);
        URLClassLoader loader = new URLClassLoader(urls, DatasourceProcessor.class.getClassLoader());
        try {
            ServiceLoader<DatasourceProcessor> serviceLoader = ServiceLoader.load(DatasourceProcessor.class, loader);
            DatasourceProcessor processor = serviceLoader.iterator().next();
            context.register(processor.type(), processor);
            loaders.put(key, loader);
            loadedTypes.put(key, processor.type());
            return new PluginLoadResult(processor.type().name(), processor.dialectName());
        } catch (RuntimeException e) {
            closeQuietly(loader);
            throw e;
        }
    }

    /** Unload a plugin by its load key, releasing its classloader. */
    public void unload(String key) {
        DatasourceType type = loadedTypes.remove(key);
        if (type != null) {
            context.unregister(type);
        }
        URLClassLoader loader = loaders.remove(key);
        closeQuietly(loader);
    }

    /**
     * Unload by datasource type: unregisters the external processor and restores
     * any built-in baseline. Used when deleting a GAV-resolved plugin.
     */
    public void unloadByType(String dbType) {
        DatasourceType type = DatasourceType.of(dbType);
        if (type != null) {
            context.unregister(type);
        }
    }

    private URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid jar path: " + file, e);
        }
    }

    private void closeQuietly(URLClassLoader loader) {
        if (loader != null) {
            try {
                loader.close();
            } catch (Exception ignore) {
                // best-effort close
            }
        }
    }

    /** Result of loading a plugin. */
    public static class PluginLoadResult {
        private final String type;
        private final String dialectName;

        public PluginLoadResult(String type, String dialectName) {
            this.type = type;
            this.dialectName = dialectName;
        }

        public String getType() {
            return type;
        }

        public String getDialectName() {
            return dialectName;
        }
    }
}