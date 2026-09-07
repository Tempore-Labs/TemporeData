package org.temporedata.api.ops.ingestion;

import lombok.Data;

/**
 * Ingestion task create request.
 */
@Data
public class IngestionTaskReq {

    private String name;

    private String type; // CSV_FILE, JSON_FILE, API

    private String datasourceId;

    private String targetTable;

    private String config; // JSON: delimiter, hasHeader, encoding
}