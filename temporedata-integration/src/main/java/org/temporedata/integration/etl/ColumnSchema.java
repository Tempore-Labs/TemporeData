package org.temporedata.integration.etl;

/**
 * [ENT-P0] Logical column metadata read by a {@link SchemaReader}.
 */
public final class ColumnSchema {

    private final String name;
    private final String typeName;
    private final boolean nullable;
    private final Integer precision;
    private final Integer scale;
    private final Integer ordinal;

    public ColumnSchema(String name, String typeName, boolean nullable,
                        Integer precision, Integer scale, Integer ordinal) {
        this.name = name;
        this.typeName = typeName;
        this.nullable = nullable;
        this.precision = precision;
        this.scale = scale;
        this.ordinal = ordinal;
    }

    public String name() {
        return name;
    }

    public String typeName() {
        return typeName;
    }

    public boolean nullable() {
        return nullable;
    }

    public Integer precision() {
        return precision;
    }

    public Integer scale() {
        return scale;
    }

    public Integer ordinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return name + ":" + typeName;
    }
}