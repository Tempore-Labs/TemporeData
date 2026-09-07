package org.temporedata.api.svc.service;

import lombok.Data;

/**
 * Data API publishing request.
 */
@Data
public class DataApiReq {

    private String name;

    private String description;

    private String datasourceId;

    private String sql; // SQL template, supports ${param} placeholders

    private String method; // GET, POST

    private String path; // unique API path, e.g. /data/users

    private String cacheTtl; // cache TTL in seconds, 0 = no cache
}