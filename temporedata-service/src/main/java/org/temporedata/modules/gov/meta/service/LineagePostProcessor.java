package org.temporedata.modules.gov.meta.service;

import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.dev.workflow.entity.WorkflowLineageEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowLineageRepository;
import org.temporedata.modules.gov.meta.entity.MetaTableEntity;
import org.temporedata.modules.gov.meta.repository.MetaTableRepository;
import org.temporedata.modules.gov.meta.spi.MetaPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LineagePostProcessor (P2-followup, design §6): auto-links lineage to metadata by table
 * name. Counts how many workflow lineage edges reference each metadata table and writes the
 * count back ({@code lineage_count}), so table metadata reflects its role in the data flow.
 */
@Slf4j
@Component
@Order(4)
public class LineagePostProcessor implements MetaPostProcessor {

    private final WorkflowLineageRepository lineageRepo;
    private final MetaTableRepository tableRepo;

    public LineagePostProcessor(WorkflowLineageRepository lineageRepo, MetaTableRepository tableRepo) {
        this.lineageRepo = lineageRepo;
        this.tableRepo = tableRepo;
    }

    @Override
    public void process(String datasourceId) {
        List<WorkflowLineageEntity> all = lineageRepo.findAll();
        if (all.isEmpty()) {
            return;
        }
        Map<String, Integer> cnt = new HashMap<>();
        for (WorkflowLineageEntity l : all) {
            touch(cnt, l.getSourceTable());
            touch(cnt, l.getTargetTable());
        }

        List<MetaTableEntity> updated = new ArrayList<>();
        for (MetaTableEntity t : tableRepo.findByDatasourceId(datasourceId)) {
            Integer c = cnt.get(t.getTableName());
            if (c == null) {
                continue;
            }
            Integer cur = t.getLineageCount();
            if (cur == null || cur != c) {
                t.setLineageCount(c);
                updated.add(t);
            }
        }
        if (!updated.isEmpty()) {
            tableRepo.saveAll(updated);
            log.info("LineageLink: linked {} tables in metadata for {}",
                    updated.size(), datasourceId);
        }
    }

    private void touch(Map<String, Integer> cnt, String table) {
        if (table == null || table.isBlank()) {
            return;
        }
        cnt.merge(table, 1, Integer::sum);
    }
}