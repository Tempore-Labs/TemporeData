package org.temporedata.api.gov.catalog;

import lombok.Data;

import java.util.List;

/**
 * Catalog tree node: datasource -> schema -> table hierarchy.
 */
@Data
public class CatalogTreeRes {

    private String id;
    private String label;
    private String type; // DATASOURCE, SCHEMA, TABLE
    private String datasourceName;
    private String schemaName;
    private String tableName;
    private Long rowCount;
    private String comment;
    private Boolean isLeaf;
    private List<CatalogTreeRes> children;
}