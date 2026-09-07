package org.temporedata.api.dev.approval;

import lombok.Data;

/**
 * Create / update approval request.
 */
@Data
public class ApprovalReq {

    private String title;

    private String description;

    private String workflowId;

    private String applicantId;

    private String approverId;
}