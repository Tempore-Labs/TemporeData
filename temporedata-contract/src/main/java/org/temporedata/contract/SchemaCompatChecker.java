package org.temporedata.contract;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * [ENT-P1] Column-signature-based schema compatibility validator.
 *
 * <p>Both schemas are normalized into {@code colName=type} signatures (case-insensitively),
 * then compared according to {@link CompatibilityMode}:
 * <ul>
 *   <li>BACKWARD: the promised signature set must be a subset of the provided one (the change
 *       did not drop or rename any promised column);</li>
 *   <li>FULL: the two signature sets must be equal.</li>
 * </ul>
 * Type narrowing is treated as a breaking change when the type token changes. This is a lightweight
 * skeleton; real connectors may supply richer semantics.
 */
public class SchemaCompatChecker {

    private SchemaCompatChecker() {
    }

    /**
     * @param promised schema the producer committed to (from the contract)
     * @param provided actual/proposed schema
     * @param mode     compatibility mode
     * @return true when the provided schema is still allowed under {@code mode}
     */
    public static boolean isCompatible(String promised, String provided, CompatibilityMode mode) {
        Set<String> promisedSig = signatures(promised);
        Set<String> providedSig = signatures(provided);
        if (mode == CompatibilityMode.FULL) {
            return promisedSig.equals(providedSig);
        }
        // BACKWARD: everything promised must still be provided.
        return providedSig.containsAll(promisedSig);
    }

    /** Parse "col1:int,col2:varchar(10)" or one-signature-per-line into a normalized set. */
    private static Set<String> signatures(String schema) {
        Set<String> out = new LinkedHashSet<>();
        if (schema == null || schema.isBlank()) {
            return out;
        }
        String[] parts = schema.split("[,;\n]");
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            out.add(t.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    /** First line of a normalized schema, for UI hints. */
    public static List<String> asList(String schema) {
        List<String> out = new ArrayList<>();
        if (schema == null || schema.isBlank()) {
            return out;
        }
        for (String p : schema.split("[,;\n]")) {
            if (!p.isBlank()) {
                out.add(p.trim());
            }
        }
        return out;
    }
}