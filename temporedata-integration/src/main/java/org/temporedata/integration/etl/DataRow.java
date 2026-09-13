package org.temporedata.integration.etl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [ENT-P0] A single logical data row: column-name -&gt; value, insertion-ordered.
 * Values are raw JDBC-ish objects; type mapping is the connector's responsibility.
 */
public final class DataRow {

    private final Map<String, Object> cells;

    private DataRow(Map<String, Object> cells) {
        this.cells = Collections.unmodifiableMap(new LinkedHashMap<>(cells));
    }

    public static DataRow of(Map<String, Object> cells) {
        return new DataRow(cells == null ? Map.of() : cells);
    }

    public Object get(String column) {
        return cells.get(column);
    }

    /** @return unmodifiable ordered column -&gt; value view */
    public Map<String, Object> cells() {
        return cells;
    }

    @Override
    public String toString() {
        return cells.toString();
    }
}