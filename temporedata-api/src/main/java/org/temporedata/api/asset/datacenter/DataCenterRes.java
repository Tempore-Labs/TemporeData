package org.temporedata.api.asset.datacenter;

import lombok.Data;

/**
 * Data center response DTO.
 */
@Data
public class DataCenterRes {

    private String id;

    private String name;

    private String description;

    private String category;

    private String config;

    private String datasourceId;

    private Integer refreshInterval;

    private String status;

    private String tenantId;

    private String createTime;

    private String updateTime;
}