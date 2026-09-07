package org.temporedata.api.asset.mydata;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * My data grant request DTO.
 */
@Data
public class MyDataGrantReq {

    private String userId;

    private String resourceType;

    private String resourceId;

    private String resourceName;

    private LocalDateTime expireTime;
}