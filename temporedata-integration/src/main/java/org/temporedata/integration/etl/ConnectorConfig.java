package org.temporedata.integration.etl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [ENT-P0] Typed, immutable view over raw connector properties (JDBC URL, credentials,
 * dialect, offsets, checkpoint, ...). Backed by an ordered map so provider options keep order.
 */
public final class ConnectorConfig {

    private final Map<String, String> props;

    private ConnectorConfig(Map<String, String> props) {
        this.props = new LinkedHashMap<>(props);
    }

    public static ConnectorConfig of(Map<String, String> props) {
        return new ConnectorConfig(props == null ? Map.of() : props);
    }

    public String get(String key) {
        return props.get(key);
    }

    public String get(String key, String defaultValue) {
        return props.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String v = props.get(key);
        return v == null ? defaultValue : Integer.parseInt(v);
    }

    public long getLong(String key, long defaultValue) {
        String v = props.get(key);
        return v == null ? defaultValue : Long.parseLong(v);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = props.get(key);
        return v == null ? defaultValue : Boolean.parseBoolean(v);
    }

    public Map<String, String> asMap() {
        return Map.copyOf(props);
    }
}