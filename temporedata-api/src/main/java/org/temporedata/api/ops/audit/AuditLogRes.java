package org.temporedata.api.ops.audit;

import lombok.Data;

/**
 * Audit log response.
 */
@Data
public class AuditLogRes {

    private String id;

    private String operator;

    private String actionType;

    private String actionDetail;

    private String targetType;

    private String targetName;

    private String ipAddress;

    private String result; // SUCCESS, FAILED

    private String errorMsg;

    private String createTime;

    private Long durationMs;
}