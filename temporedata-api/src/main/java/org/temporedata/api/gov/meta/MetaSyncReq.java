package org.temporedata.api.gov.meta;

import lombok.Data;

/**
 * Request to sync metadata from a datasource.
 */
@Data
public class MetaSyncReq {

    private String datasourceId;
    private String schemaName;
    private String tableName;
}