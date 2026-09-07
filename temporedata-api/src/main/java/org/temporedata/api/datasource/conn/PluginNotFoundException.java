package org.temporedata.api.datasource.conn;

/**
 * Thrown by {@code DatasourcePluginContext} when no processor is registered for
 * a given datasource type.
 */
public class PluginNotFoundException extends RuntimeException {

    public PluginNotFoundException(String message) {
        super(message);
    }

    public PluginNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}