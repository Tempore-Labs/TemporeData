package org.temporedata.api.gov.meta;

import lombok.Data;

/**
 * Column metadata response.
 */
@Data
public class MetaColumnRes {

    private String id;
    private String tableId;
    private String columnName;
    private String columnType;
    private Integer columnSize;
    private Integer decimalDigits;
    private Boolean nullable;
    private String defaultValue;
    private String comment;
    private Boolean primaryKey;
    private Integer ordinalPosition;
}