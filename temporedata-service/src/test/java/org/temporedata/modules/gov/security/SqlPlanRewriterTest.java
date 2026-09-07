package org.temporedata.modules.gov.security;

import org.junit.jupiter.api.Test;
import org.temporedata.api.gov.security.AccessPolicy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SqlPlanRewriterTest {

    private final SqlPlanRewriter rewriter = new SqlPlanRewriter();

    private AccessPolicy policy(String col, String rule, String pattern) {
        AccessPolicy p = new AccessPolicy();
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put(col, rule);
        p.setDesensitizeRules(rules);
        if (pattern != null) {
            Map<String, String> mp = new LinkedHashMap<>();
            mp.put(col, pattern);
            p.setMaskPatterns(mp);
        }
        return p;
    }

    @Test
    void selectStarUnderRuleFallsBack() {
        // Fail-closed: a bare SELECT * with an active rule cannot be pushed down,
        // so we must return empty and let application-side masking take over.
        Optional<String> r = rewriter.rewrite("SELECT * FROM fund_flow_stats",
                policy("account", "CUSTOM", "****"));
        assertThat(r).isEmpty();
    }

    @Test
    void explicitMaskedColumnIsRewritten() {
        Optional<String> r = rewriter.rewrite("SELECT account, amount FROM fund_flow_stats",
                policy("account", "CUSTOM", "****"));
        assertThat(r).isNotEmpty();
        String sql = r.get();
        assertThat(sql).containsIgnoringCase("CONCAT");
        assertThat(sql).containsIgnoringCase("account");
    }

    @Test
    void nonMaskedColumnKeepsOriginalSelect() {
        Optional<String> r = rewriter.rewrite("SELECT name, amount FROM fund_flow_stats",
                policy("account", "CUSTOM", "****"));
        // account is masked but not selected; nothing to mask, safe pass-through.
        assertThat(r).isPresent();
    }

    @Test
    void noRulesReturnsEmpty() {
        assertThat(rewriter.rewrite("SELECT * FROM t", new AccessPolicy())).isEmpty();
    }

    @Test
    void phoneRuleRewritesConcat() {
        Optional<String> r = rewriter.rewrite("SELECT phone FROM user", policy("phone", "PHONE", null));
        assertThat(r).isNotEmpty();
        assertThat(r.get()).containsIgnoringCase("CONCAT(SUBSTRING(`phone`, 1, 3), '****'");
    }
}