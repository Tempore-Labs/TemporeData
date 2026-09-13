package org.temporedata.metadata.drift;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * [ENT-P0] Schema Drift detector.
 *
 * <p>Compares two column-signature sets and classifies the difference into the drift types
 * recorded on temporedata_metadata_version (ADD_COLUMN / DROP_COLUMN / TYPE_CHANGE). A change
 * that mixes categories is tagged COMBINED; identical sets yield NONE.
 */
public final class SchemaDriftDetector {

    private SchemaDriftDetector() {
    }

    /** Drift classification result. */
    public static final class DriftResult {
        private final List<String> added;
        private final List<String> removed;
        private final List<String> typeChanged;
        private final String driftType;

        private DriftResult(List<String> added, List<String> removed, List<String> typeChanged, String driftType) {
            this.added = added;
            this.removed = removed;
            this.typeChanged = typeChanged;
            this.driftType = driftType;
        }

        public List<String> getAdded() { return added; }
        public List<String> getRemoved() { return removed; }
        public List<String> getTypeChanged() { return typeChanged; }
        public String getDriftType() { return driftType; }
        public boolean isChanged() { return !driftType.equals("NONE"); }
    }

    /**
     * @param before prior schema signatures (may be empty for first crawl)
     * @param after  newly observed schema signatures
     * @return drift classification
     */
    public static DriftResult detect(List<ColumnSig> before, List<ColumnSig> after) {
        java.util.Map<String, String> b = sigMap(before);
        java.util.Map<String, String> a = sigMap(after);

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> typeChanged = new ArrayList<>();
        for (String name : a.keySet()) {
            if (!b.containsKey(name)) {
                added.add(name);
            } else if (!normalize(a.get(name)).equals(normalize(b.get(name)))) {
                typeChanged.add(name);
            }
        }
        for (String name : b.keySet()) {
            if (!a.containsKey(name)) {
                removed.add(name);
            }
        }

        String driftType;
        int cats = (added.isEmpty() ? 0 : 1) + (removed.isEmpty() ? 0 : 1) + (typeChanged.isEmpty() ? 0 : 1);
        if (cats == 0) {
            driftType = "NONE";
        } else if (cats == 1) {
            driftType = !added.isEmpty() ? "ADD_COLUMN" : (!removed.isEmpty() ? "DROP_COLUMN" : "TYPE_CHANGE");
        } else {
            driftType = "COMBINED";
        }
        return new DriftResult(added, removed, typeChanged, driftType);
    }

    private static java.util.Map<String, String> sigMap(List<ColumnSig> list) {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        if (list != null) {
            for (ColumnSig s : list) {
                m.put(s.name(), s.type());
            }
        }
        return m;
    }

    private static String normalize(String type) {
        return type == null ? "" : type.toLowerCase().replaceAll("\\s+", "");
    }

    /** Parse "name=type" (one per line) back into signatures for latest-version comparison. */
    public static List<ColumnSig> parseSchemaJson(String schemaJson) {
        List<ColumnSig> out = new ArrayList<>();
        if (schemaJson == null || schemaJson.isBlank()) {
            return out;
        }
        for (String line : schemaJson.split("\\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            int eq = t.indexOf('=');
            if (eq > 0) {
                out.add(new ColumnSig(t.substring(0, eq), t.substring(eq + 1)));
            }
        }
        return out;
    }

    /** Serialize a signature list into the schema_json storage format. */
    public static String toSchemaJson(List<ColumnSig> sigs) {
        StringBuilder sb = new StringBuilder();
        for (ColumnSig s : sigs) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(s);
        }
        return sb.toString();
    }
}