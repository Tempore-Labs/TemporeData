package org.temporedata.api.asset.permapproval;

import lombok.Data;

/**
 * Permission approval response DTO.
 */
@Data
public class PermApprovalRes {

    private String id;

    private String applicantId;

    private String applicantName;

    private String resourceType;

    private String resourceId;

    private String resourceName;

    private String accessType;

    private String reason;

    private String status;

    private String approverId;

    private String approverName;

    private String approveComment;

    private String expireTime;

    private String tenantId;

    private String createTime;

    private String updateTime;
}