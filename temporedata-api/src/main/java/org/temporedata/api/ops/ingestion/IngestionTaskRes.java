package org.temporedata.api.ops.ingestion;

import lombok.Data;

/**
 * Ingestion task response.
 */
@Data
public class IngestionTaskRes {

    private String id;

    private String name;

    private String type;

    private String datasourceId;

    private String datasourceName;

    private String targetTable;

    private String status;

    private Integer rowCount;

    private String lastRunTime;

    private String errorMsg;

    private String createTime;
}