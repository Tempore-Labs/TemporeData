package org.temporedata.integration.etl;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * [ENT-P0] Child-first ClassLoader for isolating connector/driver classes.
 *
 * <p>Delegates to the parent only after failing to resolve locally, so that two connectors
 * can ship conflicting JDBC driver versions without colliding on the app classpath.
 */
public class PluginClassLoader extends URLClassLoader {

    private static final String[] PARENT_FIRST_PREFIXES = {
            "java.", "javax.", "jakarta.", "org.slf4j.", "org.springframework."
    };

    private final List<URL> pluginUrls;

    public PluginClassLoader(String name, URL[] urls, java.lang.ClassLoader parentLoader) {
        super(name, urls, parentLoader);
        this.pluginUrls = new ArrayList<>(List.of(urls));
    }

    public void addUrls(URL[] urls) {
        for (URL u : urls) {
            addURL(u);
            pluginUrls.add(u);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null && !isParentFirst(name)) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ignore) {
                    // fall through to parent
                }
            }
            if (c == null) {
                c = super.loadClass(name, false);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private boolean isParentFirst(String name) {
        for (String prefix : PARENT_FIRST_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}