package org.temporedata.api.ops.sync;

import lombok.Data;

/**
 * DB sync task request.
 */
@Data
public class SyncTaskReq {

    private String name;

    private String sourceDatasourceId;

    private String sourceTable;

    private String targetDatasourceId;

    private String targetTable;

    private String syncMode; // FULL, INCREMENTAL

    private String incrementalColumn; // column for incremental sync, e.g. update_time, id

    private String incrementalValue; // last sync value for incremental

    private String batchSize; // rows per batch, default 1000

    private String config; // extra config as JSON
}