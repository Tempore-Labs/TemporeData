package org.temporedata.api.asset.mydata;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * My data request DTO.
 */
@Data
public class MyDataReq {

    private String resourceType;

    private String resourceId;

    private String resourceName;

    private String accessType;

    private LocalDateTime expireTime;
}