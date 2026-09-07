package org.temporedata.modules.gov.meta.service;

import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.common.util.Crypto;
import org.temporedata.modules.gov.meta.entity.MetaSyncLogEntity;
import org.temporedata.modules.gov.meta.repository.MetaColumnRepository;
import org.temporedata.modules.gov.meta.repository.MetaSyncLogRepository;
import org.temporedata.modules.gov.meta.repository.MetaTableRepository;
import org.temporedata.modules.integration.datasource.entity.DatasourceEntity;
import org.temporedata.modules.integration.datasource.repository.DatasourceRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MetaCoordinator (design §7): central orchestration for the real metadata pipeline that
 * works off the *registered* datasources (not the POC connection params). It drives a
 * full collection of a datasource via its stored credentials, aggregates a per-datasource
 * overview, and supports one-shot collection of all MYSQL datasources. Currently only MYSQL
 * datasources carry a real JDBC collector; other types are listed in the overview but not
 * collected.
 */
@Slf4j
@Service
public class MetaCoordinator {

    private final DatasourceRepository datasourceRepo;
    private final MetaCollector collector;
    private final MetaTableRepository tableRepo;
    private final MetaColumnRepository columnRepo;
    private final MetaSyncLogRepository syncLogRepo;
    private final Crypto crypto;

    public MetaCoordinator(DatasourceRepository datasourceRepo,
                           MetaCollector collector,
                           MetaTableRepository tableRepo,
                           MetaColumnRepository columnRepo,
                           MetaSyncLogRepository syncLogRepo,
                           Crypto crypto) {
        this.datasourceRepo = datasourceRepo;
        this.collector = collector;
        this.tableRepo = tableRepo;
        this.columnRepo = columnRepo;
        this.syncLogRepo = syncLogRepo;
        this.crypto = crypto;
    }

    /** Collect real metadata for one registered datasource (MYSQL only). */
    public Map<String, Object> collectDatasource(String datasourceId) {
        DatasourceEntity ds = datasourceRepo.findById(datasourceId)
                .orElseThrow(() -> new BusinessException("Datasource not found: " + datasourceId));
        if (!"MYSQL".equalsIgnoreCase(ds.getType())) {
            throw new BusinessException("Metadata collection currently supports MYSQL only: " + datasourceId);
        }
        if (ds.getHost() == null || ds.getDatabase() == null || ds.getUsername() == null) {
            throw new BusinessException("Datasource missing host/database/username: " + datasourceId);
        }
        String password = ds.getPassword() == null ? "" : crypto.decrypt(ds.getPassword());
        int port = ds.getPort() != null ? ds.getPort() : 3306;
        return collector.collect(ds.getId(), ds.getHost(), port, ds.getDatabase(), ds.getUsername(), password);
    }

    /** Collect all MYSQL datasources, aggregating per-source summaries (failures isolated). */
    public List<Map<String, Object>> collectAll() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (DatasourceEntity ds : datasourceRepo.findAll()) {
            if (!"MYSQL".equalsIgnoreCase(ds.getType())) {
                continue;
            }
            try {
                out.add(collectDatasource(ds.getId()));
            } catch (Exception e) {
                log.warn("MetaCoordinator: collect failed for {}: {}", ds.getId(), e.getMessage());
                out.add(Collections.singletonMap("error", ds.getId() + ": " + e.getMessage()));
            }
        }
        if (out.isEmpty()) {
            throw new BusinessException("No MYSQL datasource available to collect");
        }
        return out;
    }

    /** Per-datasource overview (registered datasources + collected counts + last sync time). */
    public Map<String, Object> overview() {
        List<DatasourceEntity> all = datasourceRepo.findAll();
        List<Map<String, Object>> dsInfo = new ArrayList<>();
        long totalTables = 0;
        long totalColumns = 0;
        int collected = 0;
        for (DatasourceEntity ds : all) {
            long tc = tableRepo.countByDatasourceId(ds.getId());
            long cc = columnRepo.countByDatasourceId(ds.getId());
            if (tc > 0) {
                collected++;
            }
            totalTables += tc;
            totalColumns += cc;
            String lastSync = syncLogRepo
                    .findFirstByDatasourceIdOrderByStartedAtDesc(ds.getId())
                    .map(MetaSyncLogEntity::getStartedAt).orElse(null);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("datasourceId", ds.getId());
            m.put("datasourceName", ds.getName());
            m.put("type", ds.getType());
            m.put("tableCount", tc);
            m.put("columnCount", cc);
            m.put("collected", tc > 0);
            m.put("lastSyncTime", lastSync);
            dsInfo.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalTables", totalTables);
        out.put("totalColumns", totalColumns);
        out.put("datasourceCount", all.size());
        out.put("collectedCount", collected);
        out.put("datasources", dsInfo);
        return out;
    }
}