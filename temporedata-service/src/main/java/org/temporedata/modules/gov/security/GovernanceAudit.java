package org.temporedata.modules.gov.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.ops.audit.service.AuditService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Asynchronous governance audit fan-out (P3, 数据访问控制与动态脱敏执行链 v1.0 §4.6).
 *
 * <p>Every data-access governance decision (allowed / denied / masked) is written to the
 * unified tamper-evident audit log ({@link AuditService}). The write runs on
 * {@code governanceAuditExecutor} (single-threaded) so it never blocks the query/store hot
 * path and keeps the hash chain ordering deterministic. Caller identity arrives explicitly
 * because the async thread has no request SecurityContext/TenantContext.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GovernanceAudit {

    private final AuditService auditService;

    /**
     * Fire-and-forget async audit of a governed data access. Never throws into the caller.
     *
     * @param module    "DATA_ACCESS"
     * @param action    "QUERY"/"INVOKE"/"EXPORT"
     * @param resource  "datasource:table" resource key
     * @param operator  authenticated caller identity
     * @param ip        caller IP
     * @param status    "ALLOW"/"DENY"/"FAILED"
     * @param maskCols  count of columns under an active mask rule
     * @param pushed    whether masking was pushed down to the data source
     * @param truncated whether the result was capped by the circuit breaker
     */
    @Async("governanceAuditExecutor")
    public void auditAsync(String action, String resource, String operator, String ip,
                           String status, int maskCols, boolean pushed, boolean truncated) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("maskCols", maskCols);
            detail.put("pushed", pushed);
            detail.put("truncated", truncated);
            auditService.record("DATA_ACCESS", action, "DATA", resource, operator, ip, null, status,
                    toJson(detail));
        } catch (Exception e) {
            log.warn("Governance audit record failed (non-blocking): resource={}, status={}", resource, status, e);
        }
    }

    private String toJson(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(e.getKey()).append("\":").append(e.getValue());
        }
        return sb.append('}').toString();
    }
}