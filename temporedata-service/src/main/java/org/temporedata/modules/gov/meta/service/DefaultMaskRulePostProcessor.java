package org.temporedata.modules.gov.meta.service;

import org.temporedata.modules.gov.meta.entity.MetaColumnEntity;
import org.temporedata.modules.gov.meta.repository.MetaColumnRepository;
import org.temporedata.modules.gov.meta.spi.MetaPostProcessor;
import org.temporedata.modules.integration.security.entity.MaskRuleEntity;
import org.temporedata.modules.integration.security.repository.MaskRuleRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DefaultMaskRulePostProcessor (P2, design §6): for columns auto-flagged sensitive, create a
 * default desensitization rule in {@code zy_mask_rule} (skipping existing rules), so the
 * query governor (AccessPolicyResolver) automatically masks them — closing the loop between
 * metadata discovery and dynamic masking.
 */
@Component
@Order(2)
public class DefaultMaskRulePostProcessor implements MetaPostProcessor {

    private final MetaColumnRepository columnRepo;
    private final MaskRuleRepository maskRepo;

    public DefaultMaskRulePostProcessor(MetaColumnRepository columnRepo, MaskRuleRepository maskRepo) {
        this.columnRepo = columnRepo;
        this.maskRepo = maskRepo;
    }

    @Override
    public void process(String datasourceId) {
        List<MetaColumnEntity> cols = columnRepo.findByDatasourceId(datasourceId);
        for (MetaColumnEntity c : cols) {
            if (Boolean.TRUE != c.getSensitiveFlag()) {
                continue;
            }
            String ruleType = inferType(c.getColumnName());
            if (ruleType == null) {
                continue;
            }
            if (exists(datasourceId, c.getTableName(), c.getColumnName())) {
                continue;
            }
            maskRepo.save(MaskRuleEntity.builder()
                    .name("auto-" + c.getColumnName() + "-" + ruleType)
                    .ruleType(ruleType)
                    .description("auto-generated from meta collection")
                    .datasourceId(datasourceId)
                    .tableName(c.getTableName())
                    .columnName(c.getColumnName())
                    .status(1)
                    .build());
        }
    }

    private boolean exists(String dsId, String table, String column) {
        return maskRepo.findByDatasourceIdAndTableNameAndStatus(dsId, table, 1).stream()
                .anyMatch(r -> column.equals(r.getColumnName()));
    }

    private String inferType(String column) {
        if (column == null) {
            return null;
        }
        String c = column.toLowerCase();
        if (c.contains("phone") || c.contains("mobile")) {
            return "PHONE";
        }
        if (c.contains("email")) {
            return "EMAIL";
        }
        if (c.contains("id_card") || c.contains("idcard") || c.contains("identity")) {
            return "ID_CARD";
        }
        if (c.contains("bank_card") || c.contains("bankcard")) {
            return "BANK_CARD";
        }
        if (c.contains("password") || c.contains("passwd") || c.contains("pwd")
                || c.contains("token") || c.contains("secret")) {
            return "HASH";
        }
        if (c.contains("name") && !c.contains("username")) {
            return "NAME";
        }
        return null;
    }
}