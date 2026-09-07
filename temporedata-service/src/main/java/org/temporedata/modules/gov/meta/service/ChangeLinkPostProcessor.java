package org.temporedata.modules.gov.meta.service;

import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.gov.meta.entity.MetaChangeEntity;
import org.temporedata.modules.gov.meta.entity.MetaTableEntity;
import org.temporedata.modules.gov.meta.repository.MetaChangeRepository;
import org.temporedata.modules.gov.meta.repository.MetaTableRepository;
import org.temporedata.modules.gov.meta.spi.MetaPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ChangeLinkPostProcessor (P3, design §6): consumes the latest MetaChange events for a
 * datasource and auto-links business states: marks changed tables as STRUCT_CHANGED and
 * removed tables as REMOVED on the metadata, and invalidates matching quality rules.
 * Quality invalidation is done via plain SQL to avoid loading the legacy entity
 * (whose nullable columns conflict with primitive-typed fields).
 */
@Slf4j
@Component
@Order(3)
public class ChangeLinkPostProcessor implements MetaPostProcessor {

    private final MetaChangeRepository changeRepo;
    private final MetaTableRepository tableRepo;
    private final JdbcTemplate jdbcTemplate;

    public ChangeLinkPostProcessor(MetaChangeRepository changeRepo,
                                   MetaTableRepository tableRepo,
                                   DataSource dataSource) {
        this.changeRepo = changeRepo;
        this.tableRepo = tableRepo;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void process(String datasourceId) {
        List<MetaChangeEntity> changes = changeRepo.findByDatasourceIdOrderBySyncedAtDesc(datasourceId);
        if (changes.isEmpty()) {
            return;
        }
        String latestSync = changes.get(0).getSyncedAt();
        Map<String, String> typeByTable = new LinkedHashMap<>();
        for (MetaChangeEntity c : changes) {
            if (latestSync.equals(c.getSyncedAt())) {
                typeByTable.putIfAbsent(c.getTableName(), c.getChangeType());
            }
        }

        // 1) mark table status (ADD keeps ACTIVE; CHANGE -> STRUCT_CHANGED; REMOVE -> REMOVED)
        List<MetaTableEntity> updated = new ArrayList<>();
        for (MetaTableEntity t : tableRepo.findByDatasourceId(datasourceId)) {
            String type = typeByTable.get(t.getTableName());
            if (type == null) {
                continue;
            }
            if ("REMOVE".equals(type)) {
                t.setStatus("REMOVED");
                updated.add(t);
            } else if ("CHANGE".equals(type)) {
                if (!"STRUCT_CHANGED".equals(t.getStatus())) {
                    t.setStatus("STRUCT_CHANGED");
                    updated.add(t);
                }
            }
        }
        if (!updated.isEmpty()) {
            tableRepo.saveAll(updated);
        }

        // 2) invalidate quality rules bound to changed/removed tables (native SQL)
        int invalidated = 0;
        for (Map.Entry<String, String> e : typeByTable.entrySet()) {
            if ("ADD".equals(e.getValue()) || "REMOVE".equals(e.getValue())) {
                continue;
            }
            invalidated += jdbcTemplate.update(
                    "UPDATE zy_quality SET last_status = status, status = 'INVALID', " +
                            "error_msg = 'metadata structure changed' " +
                            "WHERE datasource_id = ? AND table_name = ? AND status <> 'INVALID'",
                    datasourceId, e.getKey());
        }
        log.info("ChangeLink: linked {} tables, invalidated {} quality rules for {}",
                typeByTable.size(), invalidated, datasourceId);
    }
}