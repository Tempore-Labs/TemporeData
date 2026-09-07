package org.temporedata.api.svc.service;

import lombok.Data;

/**
 * Data API response.
 */
@Data
public class DataApiRes {

    private String id;

    private String name;

    private String description;

    private String datasourceId;

    private String sql;

    private String method; // GET, POST

    private String path;

    private String apiKey; // masked, only shown on create

    private String status; // ACTIVE, DISABLED

    private String cacheTtl;

    private Long callCount;

    private String lastCallTime;

    private String createTime;

    private String updateTime;
}