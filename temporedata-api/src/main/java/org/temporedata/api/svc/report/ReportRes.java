package org.temporedata.api.svc.report;

import lombok.Data;

/**
 * Report list / detail response.
 */
@Data
public class ReportRes {

    private String id;

    private String name;

    private String description;

    private String type;

    private String config;

    private String datasourceId;

    private String status;

    private String tenantId;

    private String createTime;

    private String updateTime;
}