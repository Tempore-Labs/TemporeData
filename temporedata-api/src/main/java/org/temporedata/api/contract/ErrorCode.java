package org.temporedata.api.contract;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * v1 error codes (refactor/02-backend-code-architecture.md §7). Groups:
 * SYS_ CLUSTER_ COMPUTE_ JOB_ MONITOR_ LOG_ INCIDENT_ AUTO_ DATA_ AUTH_.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode {

    // --- SYS ---
    SYS_INTERNAL(50000, "system error"),
    SYS_PARAM(40000, "invalid parameter"),
    SYS_NOT_FOUND(40400, "resource not found"),
    SYS_NOT_IMPLEMENTED(50100, "not implemented"),

    // --- AUTH / common ---
    AUTH_UNAUTHORIZED(40100, "unauthorized"),
    AUTH_FORBIDDEN(40300, "forbidden"),
    AUTH_APPROVAL_REQUIRED(40301, "approval required"),
    AUTH_ACTION_DENIED(40302, "action denied"),

    // --- Domain groups (kept generic here; refine per module on demand) ---
    CLUSTER_NOT_FOUND(40410, "cluster not found"),
    CLUSTER_NODE_UNREACHABLE(50310, "cluster node unreachable"),
    JOB_NOT_FOUND(40420, "job not found"),
    JOB_EXECUTION_NOT_ALLOWED(40320, "job execution not allowed"),
    JOB_EXECUTOR_UNAVAILABLE(50320, "job executor unavailable"),
    INCIDENT_NOT_FOUND(40430, "incident not found"),
    AUTO_ACTION_DENIED(40340, "automation action denied"),
    DATA_NOT_FOUND(40450, "data resource not found");

    private final int code;
    private final String msg;

    public String asCode() {
        return name();
    }
}