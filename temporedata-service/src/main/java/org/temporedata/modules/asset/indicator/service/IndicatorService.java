package org.temporedata.modules.asset.indicator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.temporedata.modules.asset.indicator.entity.IndicatorEntity;
import org.temporedata.modules.asset.indicator.repository.IndicatorRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import static org.temporedata.common.cache.CacheConfig.CACHE_INDICATOR;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorService {

    private final IndicatorRepository indicatorRepository;
    private final ObjectMapper objectMapper;

    /**
     * Paginated indicator list.
     */
    @Cacheable(value = CACHE_INDICATOR)
    public Page<IndicatorEntity> page(Pageable pageable) {
        return indicatorRepository.findAll(pageable);
    }

    /**
     * Get indicator by ID.
     */
    @Cacheable(value = CACHE_INDICATOR)
    public IndicatorEntity get(String id) {
        return indicatorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Indicator not found: " + id));
    }

    /**
     * Statistics overview: total, published, running, failed counts.
     */
    @Cacheable(value = CACHE_INDICATOR)
    public Map<String, Object> stats() {
        List<IndicatorEntity> all = indicatorRepository.findAll();
        long total = all.size();
        long published = all.stream().filter(e -> "PUBLISHED".equals(e.getStatus())).count();
        long running = all.stream().filter(e -> "RUNNING".equals(e.getStatus())).count();
        long failed = all.stream().filter(e -> "FAILED".equals(e.getLatestStatus())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("published", published);
        result.put("running", running);
        result.put("failed", failed);
        return result;
    }

    /**
     * Create a new indicator with default values.
     */
    @CacheEvict(value = CACHE_INDICATOR, allEntries = true)
    @Transactional
    public IndicatorEntity create(IndicatorEntity entity) {
        entity.setStatus(entity.getStatus() != null ? entity.getStatus() : "DRAFT");
        entity.setTotalRuns(0);
        entity.setSuccessRuns(0);
        entity.setFailedRuns(0);
        entity.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("Creating indicator: name={}, code={}", entity.getName(), entity.getCode());
        return indicatorRepository.save(entity);
    }

    /**
     * Update an existing indicator by ID.
     */
    @CacheEvict(value = CACHE_INDICATOR, allEntries = true)
    @Transactional
    public IndicatorEntity update(String id, IndicatorEntity entity) {
        IndicatorEntity existing = get(id);
        // Preserve immutable fields
        entity.setId(id);
        entity.setCreateDateTime(existing.getCreateDateTime());
        entity.setCreateBy(existing.getCreateBy());
        entity.setTotalRuns(existing.getTotalRuns());
        entity.setSuccessRuns(existing.getSuccessRuns());
        entity.setFailedRuns(existing.getFailedRuns());
        entity.setLatestStatus(existing.getLatestStatus());
        entity.setLatestValue(existing.getLatestValue());
        entity.setLatestExecuteTime(existing.getLatestExecuteTime());
        entity.setLatestDurationMs(existing.getLatestDurationMs());
        entity.setLatestErrorMsg(existing.getLatestErrorMsg());
        entity.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("Updating indicator: id={}, name={}", id, entity.getName());
        return indicatorRepository.save(entity);
    }

    /**
     * Delete an indicator by ID.
     */
    @CacheEvict(value = CACHE_INDICATOR, allEntries = true)
    @Transactional
    public void delete(String id) {
        get(id);
        indicatorRepository.deleteById(id);
        log.info("Deleted indicator: id={}", id);
    }

    /**
     * Execute indicator calculation (simulate SQL execution and record result).
     */
    @CacheEvict(value = CACHE_INDICATOR, allEntries = true)
    @Transactional
    public IndicatorEntity execute(String id) {
        IndicatorEntity entity = get(id);
        if (entity.getQuerySql() == null || entity.getQuerySql().isBlank()) {
            throw new BusinessException("Indicator has no querySql configured: " + id);
        }

        long startMs = System.currentTimeMillis();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try {
            // Simulate SQL execution — in production this would run against the actual datasource
            log.info("Executing indicator: id={}, sql={}", id, entity.getQuerySql());
            Thread.sleep((long) (50 + Math.random() * 200)); // simulated delay

            String simulatedValue = String.format("%.2f", Math.random() * 10000);
            entity.setLatestStatus("SUCCESS");
            entity.setLatestValue(simulatedValue);
            entity.setLatestErrorMsg(null);
            entity.setSuccessRuns(entity.getSuccessRuns() + 1);

            // Also populate the per-run fields
            entity.setResultValue(simulatedValue);
            entity.setErrorMsg(null);
            entity.setDurationMs(System.currentTimeMillis() - startMs);
            entity.setExecuteTime(now);

            log.info("Indicator execution SUCCESS: id={}, value={}", id, simulatedValue);
        } catch (Exception e) {
            log.error("Indicator execution FAILED: id={}", id, e);
            entity.setLatestStatus("FAILED");
            entity.setLatestErrorMsg(e.getMessage());
            entity.setFailedRuns(entity.getFailedRuns() + 1);

            entity.setResultValue(null);
            entity.setErrorMsg(e.getMessage());
            entity.setDurationMs(System.currentTimeMillis() - startMs);
            entity.setExecuteTime(now);
        }

        entity.setTotalRuns(entity.getTotalRuns() + 1);
        entity.setLatestDurationMs(System.currentTimeMillis() - startMs);
        entity.setLatestExecuteTime(now);
        entity.setStatus("RUNNING");

        return indicatorRepository.save(entity);
    }

    /**
     * Get execution history (simulated from stored run fields).
     * Returns a list of run records built from the indicator's run fields.
     */
    public List<Map<String, Object>> getRuns(String id) {
        IndicatorEntity entity = get(id);
        List<Map<String, Object>> runs = new ArrayList<>();

        // Build simulated run records from the entity's run data
        if (entity.getTotalRuns() > 0) {
            Map<String, Object> latestRun = new LinkedHashMap<>();
            latestRun.put("indicatorId", entity.getIndicatorId() != null ? entity.getIndicatorId() : id);
            latestRun.put("status", entity.getLatestStatus());
            latestRun.put("value", entity.getLatestValue());
            latestRun.put("resultValue", entity.getResultValue());
            latestRun.put("executeTime", entity.getLatestExecuteTime());
            latestRun.put("durationMs", entity.getLatestDurationMs());
            latestRun.put("errorMsg", entity.getLatestErrorMsg());
            latestRun.put("totalRuns", entity.getTotalRuns());
            latestRun.put("successRuns", entity.getSuccessRuns());
            latestRun.put("failedRuns", entity.getFailedRuns());
            runs.add(latestRun);
        }

        // If there are no runs yet, return an empty list
        if (runs.isEmpty()) {
            Map<String, Object> placeholder = new LinkedHashMap<>();
            placeholder.put("indicatorId", id);
            placeholder.put("status", "NO_DATA");
            placeholder.put("value", null);
            placeholder.put("executeTime", null);
            placeholder.put("durationMs", 0);
            placeholder.put("errorMsg", null);
            placeholder.put("totalRuns", 0);
            placeholder.put("successRuns", 0);
            placeholder.put("failedRuns", 0);
            runs.add(placeholder);
        }

        return runs;
    }

    /**
     * Get indicator lineage (upstream / downstream dependencies).
     * Returns a simulated lineage graph.
     */
    public Map<String, Object> getLineage(String id) {
        IndicatorEntity entity = get(id);
        Map<String, Object> lineage = new LinkedHashMap<>();
        lineage.put("indicatorId", id);
        lineage.put("indicatorName", entity.getName());
        lineage.put("indicatorCode", entity.getCode());

        // Simulated upstream indicators
        List<Map<String, String>> upstream = new ArrayList<>();
        Map<String, String> up1 = new LinkedHashMap<>();
        up1.put("id", "upstream-001");
        up1.put("name", "上游指标-数据源接入量");
        up1.put("type", "SOURCE");
        upstream.add(up1);
        Map<String, String> up2 = new LinkedHashMap<>();
        up2.put("id", "upstream-002");
        up2.put("name", "上游指标-数据清洗率");
        up2.put("type", "TRANSFORM");
        upstream.add(up2);
        lineage.put("upstream", upstream);

        // Simulated downstream indicators
        List<Map<String, String>> downstream = new ArrayList<>();
        Map<String, String> down1 = new LinkedHashMap<>();
        down1.put("id", "downstream-001");
        down1.put("name", "下游指标-报表汇总");
        down1.put("type", "REPORT");
        downstream.add(down1);
        lineage.put("downstream", downstream);

        lineage.put("querySql", entity.getQuerySql());
        lineage.put("datasourceId", entity.getDatasourceId());

        return lineage;
    }
}