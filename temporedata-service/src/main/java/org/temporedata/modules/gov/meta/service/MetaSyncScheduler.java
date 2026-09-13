package org.temporedata.modules.gov.meta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.common.util.Crypto;
import org.temporedata.integration.core.datasource.entity.DatasourceEntity;
import org.temporedata.integration.core.datasource.repository.DatasourceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MetaSyncScheduler (P1, design §5): periodically collects metadata for registered
 * MySQL datasources. Runnable standalone (no-op when no MySQL datasource is present).
 */
@Slf4j
@Component
public class MetaSyncScheduler {

    private final DatasourceRepository datasourceRepository;
    private final MetaCollector collector;
    private final Crypto crypto;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetaSyncScheduler(DatasourceRepository datasourceRepository, MetaCollector collector, Crypto crypto) {
        this.datasourceRepository = datasourceRepository;
        this.collector = collector;
        this.crypto = crypto;
    }

    @Scheduled(cron = "${temporedata.meta.sync.cron:0 30 3 * * *}")
    public void syncAll() {
        List<DatasourceEntity> mysqlList = datasourceRepository.findAll().stream()
                .filter(d -> d.getType() != null && "MYSQL".equalsIgnoreCase(d.getType()))
                .collect(java.util.stream.Collectors.toList());
        if (mysqlList.isEmpty()) {
            log.info("MetaSyncScheduler: no MySQL datasource registered, skip");
            return;
        }
        for (DatasourceEntity ds : mysqlList) {
            try {
                if (ds.getHost() == null || ds.getDatabase() == null) {
                    continue;
                }
                String password = ds.getPassword() == null
                        ? "" : crypto.decrypt(ds.getPassword());
                java.util.Map<String, Object> m = collector.collect(ds.getId(), ds.getHost(),
                        port(ds), ds.getDatabase(), ds.getUsername(), password);
                String summary = m.get("tables") + "/" + m.get("columns") + " (unchanged=" + m.get("unchangedTables") + ")";
                log.info("MetaSyncScheduler: collected {} , {}", ds.getName(), summary);
            } catch (Exception e) {
                log.error("MetaSyncScheduler: failed for datasource {}", ds.getId(), e);
            }
        }
    }

    private int port(DatasourceEntity ds) {
        return ds.getPort() != null ? ds.getPort() : 3306;
    }
}