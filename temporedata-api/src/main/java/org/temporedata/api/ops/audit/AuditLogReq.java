package org.temporedata.api.ops.audit;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Audit log query request.
 */
@Data
public class AuditLogReq {

    private String operator; // who

    private String actionType; // LOGIN, QUERY, INSERT, UPDATE, DELETE, EXPORT, API_CALL

    private String targetType; // DATASOURCE, TABLE, WORKFLOW, API

    private String targetName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}