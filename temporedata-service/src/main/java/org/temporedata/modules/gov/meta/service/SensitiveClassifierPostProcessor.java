package org.temporedata.modules.gov.meta.service;

import org.temporedata.modules.gov.meta.entity.MetaColumnEntity;
import org.temporedata.modules.gov.meta.repository.MetaColumnRepository;
import org.temporedata.modules.gov.meta.spi.MetaPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SensitiveClassifierPostProcessor (P2, design §6): auto-derives data level code
 * ({@code P0..P3}) per column by a name/type dictionary and ensures the sensitive flag.
 */
@Component
@Order(1)
public class SensitiveClassifierPostProcessor implements MetaPostProcessor {

    private final MetaColumnRepository columnRepo;

    public SensitiveClassifierPostProcessor(MetaColumnRepository columnRepo) {
        this.columnRepo = columnRepo;
    }

    @Override
    public void process(String datasourceId) {
        List<MetaColumnEntity> cols = columnRepo.findByDatasourceId(datasourceId);
        List<MetaColumnEntity> updated = new ArrayList<>();
        for (MetaColumnEntity c : cols) {
            String level = classify(c.getColumnName());
            if (level == null) {
                continue;
            }
            boolean changed = false;
            if (!level.equals(c.getDataLevelCode())) {
                c.setDataLevelCode(level);
                changed = true;
            }
            boolean sensitive = !"P0".equals(level);
            if (Boolean.TRUE != c.getSensitiveFlag() && sensitive) {
                c.setSensitiveFlag(true);
                changed = true;
            }
            if (changed) {
                updated.add(c);
            }
        }
        columnRepo.saveAll(updated);
    }

    private String classify(String column) {
        if (column == null) {
            return null;
        }
        String c = column.toLowerCase();
        if (c.contains("password") || c.contains("passwd") || c.contains("pwd")
                || c.contains("token") || c.contains("secret") || c.contains("api_key")
                || c.contains("id_card") || c.contains("idcard") || c.contains("identity")
                || c.contains("bank_card") || c.contains("bankcard") || c.contains("account_no")) {
            return "P3";
        }
        if (c.contains("phone") || c.contains("mobile") || c.contains("telephone")
                || c.contains("email") || c.contains("salary") || c.contains("balance")) {
            return "P2";
        }
        if (c.contains("name") && !c.contains("username") && !c.contains("table_name")
                && !c.contains("column_name")) {
            return "P2";
        }
        return null;
    }
}