package org.temporedata.api.svc.blacklist;

import lombok.Data;

/**
 * Blacklist / whitelist response DTO.
 */
@Data
public class BlacklistRes {

    private String id;

    private String ipAddress;

    private String listType;

    private String reason;

    private String status;

    private String tenantId;

    private String createTime;

    private String updateTime;
}