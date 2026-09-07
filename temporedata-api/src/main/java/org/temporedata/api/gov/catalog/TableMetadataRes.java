package org.temporedata.api.gov.catalog;

import lombok.Data;

import java.util.List;

/**
 * Table metadata response.
 */
@Data
public class TableMetadataRes {

    private String id;

    private String datasourceId;

    private String datasourceName;

    private String schemaName;

    private String tableName;

    private String comment;

    private Long rowCount;

    private String dataLevelId;

    private String dataCategoryId;

    private List<ColumnMetadataRes> columns;

    private String syncTime;
}