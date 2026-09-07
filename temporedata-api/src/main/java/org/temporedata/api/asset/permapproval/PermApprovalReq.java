package org.temporedata.api.asset.permapproval;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Permission approval request DTO.
 */
@Data
public class PermApprovalReq {

    private String resourceType;

    private String resourceId;

    private String resourceName;

    private String accessType;

    private String reason;

    private LocalDateTime expireTime;
}