package org.temporedata.contract;

/**
 * [ENT-P1] Schema compatibility mode for data contracts.
 *
 * <p>BACKWARD - new schema must not drop or break anything the current schema promises to
 * existing consumers (added columns/types are safe, removals/narrowings are not).
 * FULL      - new schema must be identical in column set &amp; types to the promised one.
 */
public enum CompatibilityMode {
    BACKWARD,
    FULL;

    public static CompatibilityMode from(String mode) {
        return mode == null ? BACKWARD : valueOf(mode.toUpperCase());
    }
}