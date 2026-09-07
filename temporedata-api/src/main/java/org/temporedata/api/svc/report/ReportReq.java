package org.temporedata.api.svc.report;

import lombok.Data;

/**
 * Create / update report request.
 */
@Data
public class ReportReq {

    private String name;

    private String description;

    private String type; // TABLE, CHART, DASHBOARD

    private String config; // JSON config

    private String datasourceId;
}