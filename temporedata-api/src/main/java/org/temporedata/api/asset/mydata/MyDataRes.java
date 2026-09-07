package org.temporedata.api.asset.mydata;

import lombok.Data;

/**
 * My data response DTO.
 */
@Data
public class MyDataRes {

    private String id;

    private String userId;

    private String resourceType;

    private String resourceId;

    private String resourceName;

    private String accessType;

    private String grantedBy;

    private String expireTime;

    private String tenantId;

    private String createTime;
}