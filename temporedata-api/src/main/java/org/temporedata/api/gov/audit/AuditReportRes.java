package org.temporedata.api.gov.audit;

import lombok.Data;

import java.util.List;

/**
 * Audit report response.
 */
@Data
public class AuditReportRes {

    private String datasourceId;

    private String tableName;

    private int totalRules;

    private int passCount;

    private int failCount;

    private String passRate; // e.g. "85.7%"

    private String reportTime;

    private List<AuditRuleResult> ruleResults;

    @Data
    public static class AuditRuleResult {
        private String ruleName;
        private String ruleType;
        private String status; // PASS, FAIL
        private String result;
        private String checkTime;
    }
}