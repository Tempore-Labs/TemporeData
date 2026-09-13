package org.temporedata.metadata.drift;

/**
 * [ENT-P0] A column signature (name + logical type token) used for drift comparison.
 */
public final class ColumnSig {

    private final String name;
    private final String type;

    public ColumnSig(String name, String type) {
        this.name = name == null ? "" : name.trim();
        this.type = type == null ? "" : type.trim();
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    /** Stable serialization used in temporedata_metadata_version.schema_json. */
    @Override
    public String toString() {
        return name + "=" + type;
    }
}