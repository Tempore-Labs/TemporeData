package org.temporedata.api.dev.approval;

import lombok.Data;

/**
 * Approval list / detail response.
 */
@Data
public class ApprovalRes {

    private String id;

    private String workflowId;

    private String title;

    private String description;

    private String status;

    private String applicantId;

    private String approverId;

    private String comment;

    private String tenantId;

    private String createTime;

    private String updateTime;
}