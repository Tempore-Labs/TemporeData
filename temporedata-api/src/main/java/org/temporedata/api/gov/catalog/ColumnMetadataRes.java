package org.temporedata.api.gov.catalog;

import lombok.Data;

/**
 * Column metadata response.
 */
@Data
public class ColumnMetadataRes {

    private String id;

    private String columnName;

    private String dataType;

    private Integer columnSize;

    private Integer decimalDigits;

    private String defaultValue;

    private Integer ordinalPosition;

    private Boolean isNullable;

    private String comment;

    private Boolean isPrimaryKey;

    private String dataLevelId;
}