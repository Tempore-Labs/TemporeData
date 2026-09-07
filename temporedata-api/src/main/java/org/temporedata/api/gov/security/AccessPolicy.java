package org.temporedata.api.gov.security;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * Data access decision for a principal over a dataset (table) at a given
 * query/export/API action. Produced by the policy resolver and consumed by the
 * {@code DataAccessGovernor} to prune columns and apply desensitization
 * (§数据访问控制与动态脱敏执行链设计 v1.0).
 */
@Data
public class AccessPolicy {

    /** Whole-access denied (fail-closed). */
    private boolean denied;

    /**
     * Columns the principal may read. Empty/null means "all columns allowed"
     * (subject to desensitization); otherwise unlisted columns are pruned.
     */
    private Set<String> grantedColumns = Collections.emptySet();

    /** columnName -> desensitization ruleType (PHONE/EMAIL/NAME/ID_CARD/BANK_CARD/CUSTOM/HASH/DROP). */
    private Map<String, String> desensitizeRules = new LinkedHashMap<>();

    /** columnName -> custom maskPattern (used for CUSTOM rule). */
    private Map<String, String> maskPatterns = new LinkedHashMap<>();

    /** Optional push-down row filter predicate (future: injected into WHERE). */
    private String rowFilter;
}