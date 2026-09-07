package org.temporedata.modules.gov.security;

import org.temporedata.api.gov.security.AccessPolicy;
import org.temporedata.modules.integration.security.entity.MaskRuleEntity;
import org.temporedata.modules.integration.security.repository.MaskRuleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.temporedata.common.cache.CacheConfig.CACHE_SECURITY_RULE;

/**
 * AccessPolicyResolver (P1/P3, 数据访问控制与动态脱敏执行链 v1.0 §3/§7).
 * Builds an {@link AccessPolicy} for a (datasourceId, table) from the enabled
 * {@code zy_mask_rule} rows: each enabled column rule becomes a desensitization rule.
 *
 * <p>P3 two-level caching (§4.5): the decision is {@link Cacheable} on the
 * {@code security_rule} region (Caffeine L1 by default, Redis L2 in distributed mode).
 * Cache invalidation is driven by mask-rule writes (see {@code MaskRuleService}).
 * Column-level grants (MyData) and row-filter push-down are wired in later phases.</p>
 */
@Component
public class AccessPolicyResolver {

    private final MaskRuleRepository maskRuleRepository;

    public AccessPolicyResolver(MaskRuleRepository maskRuleRepository) {
        this.maskRuleRepository = maskRuleRepository;
    }

    @Cacheable(cacheNames = CACHE_SECURITY_RULE, key = "#datasourceId + ':' + #tableName")
    public AccessPolicy resolve(String datasourceId, String tableName) {
        AccessPolicy policy = new AccessPolicy();
        if (datasourceId == null || tableName == null) {
            return policy;
        }
        List<MaskRuleEntity> rules = maskRuleRepository
                .findByDatasourceIdAndTableNameAndStatus(datasourceId, tableName, 1);
        for (MaskRuleEntity rule : rules) {
            if (rule.getColumnName() == null || rule.getColumnName().isBlank()) {
                continue;
            }
            policy.getDesensitizeRules().put(rule.getColumnName(), rule.getRuleType());
            if (rule.getMaskPattern() != null && !rule.getMaskPattern().isBlank()) {
                policy.getMaskPatterns().put(rule.getColumnName(), rule.getMaskPattern());
            }
        }
        return policy;
    }
}