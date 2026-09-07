package org.temporedata.modules.gov.quality.service;

import org.temporedata.modules.gov.quality.entity.QualityEntity;
import org.temporedata.modules.gov.quality.repository.QualityRepository;
import org.temporedata.modules.ops.audit.entity.AuditEventEntity;
import org.temporedata.modules.ops.audit.service.AuditService;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.gov.quality.QualityRunRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_QUALITY_RULE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityService {

    private final QualityRepository qualityRepository;
    private final AuditService auditService;
    private final QualityRuleEngine qualityRuleEngine;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * List all quality rules.
     */
    public List<QualityEntity> list() {
        return qualityRepository.findAll();
    }

    /**
     * Create a quality rule.
     */
    @Transactional
    @CacheEvict(value = CACHE_QUALITY_RULE, allEntries = true)
    public QualityEntity create(QualityEntity entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new BusinessException("Quality rule name is required");
        }
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        entity.setStatus(entity.getStatus() != null ? entity.getStatus() : "active");
        entity.setLastStatus("pending");
        entity.setCreateTime(LocalDateTime.now().format(DTF));
        QualityEntity saved = qualityRepository.save(entity);
        log.info("Created quality rule: {}", saved.getName());
        return saved;
    }

    /**
     * Update a quality rule.
     */
    @Transactional
    @CacheEvict(value = CACHE_QUALITY_RULE, allEntries = true)
    public QualityEntity update(String id, QualityEntity entity) {
        QualityEntity existing = qualityRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Quality rule not found: " + id));

        if (entity.getName() != null) existing.setName(entity.getName());
        if (entity.getRuleName() != null) existing.setRuleName(entity.getRuleName());
        if (entity.getRuleType() != null) existing.setRuleType(entity.getRuleType());
        if (entity.getRuleConfig() != null) existing.setRuleConfig(entity.getRuleConfig());
        if (entity.getDatasourceId() != null) existing.setDatasourceId(entity.getDatasourceId());
        if (entity.getDatasourceName() != null) existing.setDatasourceName(entity.getDatasourceName());
        if (entity.getTableName() != null) existing.setTableName(entity.getTableName());
        if (entity.getColumnName() != null) existing.setColumnName(entity.getColumnName());
        if (entity.getStatus() != null) existing.setStatus(entity.getStatus());
        if (entity.getDescription() != null) existing.setDescription(entity.getDescription());

        QualityEntity saved = qualityRepository.save(existing);
        log.info("Updated quality rule: {}", id);
        return saved;
    }

    /**
     * Delete a quality rule.
     */
    @Transactional
    @CacheEvict(value = CACHE_QUALITY_RULE, allEntries = true)
    public void delete(String id) {
        QualityEntity entity = qualityRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Quality rule not found: " + id));
        qualityRepository.delete(entity);
        log.info("Deleted quality rule: {}", id);
    }

    /**
     * Execute a quality check for a specific rule (real engine, P1-6).
     */
    @Transactional
    @CacheEvict(value = CACHE_QUALITY_RULE, allEntries = true)
    public QualityEntity execute(String id) {
        QualityEntity rule = findActive(id);
        QualityRunRes res = qualityRuleEngine.run(rule);
        applyResult(rule, res);
        QualityEntity saved = qualityRepository.save(rule);
        persistAudit(rule, res);
        log.info("Executed quality rule {}: {}", id, res.getStatus());
        return saved;
    }

    /**
     * Run a rule and return the rich result (pass rate + samples).
     */
    @Transactional
    public QualityRunRes run(String id) {
        QualityEntity rule = findActive(id);
        QualityRunRes res = qualityRuleEngine.run(rule);
        applyResult(rule, res);
        qualityRepository.save(rule);
        persistAudit(rule, res);
        log.info("Ran quality rule {}: {}", id, res.getStatus());
        return res;
    }

    private QualityEntity findActive(String id) {
        QualityEntity rule = qualityRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Quality rule not found: " + id));
        if (!"active".equals(rule.getStatus())) {
            throw new BusinessException("Rule is not active, cannot execute: " + id);
        }
        return rule;
    }

    private void applyResult(QualityEntity rule, QualityRunRes res) {
        rule.setLastStatus(rule.getResult());
        rule.setResult(res.getStatus().equals("PASS") ? "pass" : "fail");
        rule.setStatus("completed");
        rule.setCheckTime(res.getCheckTime());
        rule.setDurationMs(res.getDurationMs());
        rule.setErrorMsg(res.getErrorMsg());
    }

    private static final String QUALITY_MODULE = "QUALITY";

    private void persistAudit(QualityEntity rule, QualityRunRes res) {
        try {
            String json = "{\"datasourceId\":\"" + nvl(rule.getDatasourceId())
                    + "\",\"tableName\":\"" + nvl(rule.getTableName())
                    + "\",\"ruleType\":\"" + nvl(rule.getRuleType())
                    + "\",\"result\":\"" + nvl(res.getResult() != null ? res.getResult()
                    : (res.getErrorMsg() != null ? res.getErrorMsg() : res.getStatus()))
                    + "\",\"checkTime\":\"" + nvl(res.getCheckTime()) + "\"}";
            auditService.record(QUALITY_MODULE, "EXECUTE", "quality",
                    rule.getRuleName() != null ? rule.getRuleName() : rule.getName(),
                    "system", null, null, res.getStatus(), json);
        } catch (Exception e) {
            log.warn("Failed to persist quality audit", e);
        }
    }

    private List<Map<String, Object>> qualityAuditHistory(String datasourceId, String tableName) {
        List<Map<String, Object>> audits = new ArrayList<>();
        List<AuditEventEntity> events = auditService.list(QUALITY_MODULE, null);
        for (AuditEventEntity e : events) {
            String detail = e.getDetailJson() == null ? "" : e.getDetailJson();
            String dsId = jsonField(detail, "datasourceId");
            if (datasourceId != null && !datasourceId.isEmpty() && !datasourceId.equals(dsId)) continue;
            String tName = jsonField(detail, "tableName");
            if (tableName != null && !tableName.isEmpty() && !tableName.equals(tName)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("datasourceId", dsId);
            m.put("tableName", tName);
            m.put("ruleName", e.getResourceKey());
            m.put("ruleType", jsonField(detail, "ruleType"));
            m.put("status", e.getStatus());
            m.put("result", jsonField(detail, "result"));
            m.put("checkTime", jsonField(detail, "checkTime"));
            audits.add(m);
        }
        audits.sort((a, b) -> {
            String ta = a.get("checkTime") == null ? "" : a.get("checkTime").toString();
            String tb = b.get("checkTime") == null ? "" : b.get("checkTime").toString();
            return tb.compareTo(ta);
        });
        return audits;
    }

    private String jsonField(String json, String key) {
        try {
            String k = "\"" + key + "\":\"";
            int i = json.indexOf(k);
            if (i < 0) return "";
            int s = i + k.length();
            int e = json.indexOf('"', s);
            return e < 0 ? "" : json.substring(s, e);
        } catch (Exception ex) {
            return "";
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    /**
     * Get quality report filtered by datasource and/or table.
     */
    @Cacheable(value = CACHE_QUALITY_RULE)
    public Map<String, Object> getReport(String datasourceId, String tableName) {
        List<QualityEntity> allRules = qualityRepository.findAll();

        // Filter by datasourceId
        if (datasourceId != null && !datasourceId.isEmpty()) {
            allRules = allRules.stream()
                    .filter(e -> datasourceId.equals(e.getDatasourceId()))
                    .collect(Collectors.toList());
        }

        // Filter by tableName
        if (tableName != null && !tableName.isEmpty()) {
            allRules = allRules.stream()
                    .filter(e -> tableName.equals(e.getTableName()))
                    .collect(Collectors.toList());
        }

        long totalRules = allRules.size();
        long passCount = allRules.stream().filter(e -> "pass".equals(e.getResult()) || "pass".equals(e.getLastStatus())).count();
        long failCount = allRules.stream().filter(e -> "fail".equals(e.getResult()) || "fail".equals(e.getLastStatus())).count();
        long pendingCount = totalRules - passCount - failCount;
        String passRate = totalRules > 0 ? String.format("%.1f%%", (double) passCount / totalRules * 100) : "0%";

        // Also get audit records from the unified audit engine
        List<Map<String, Object>> audits = qualityAuditHistory(datasourceId, tableName);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalRules", totalRules);
        report.put("passCount", passCount);
        report.put("failCount", failCount);
        report.put("pendingCount", pendingCount);
        report.put("passRate", passRate);
        report.put("rules", allRules);
        report.put("auditHistory", audits);
        return report;
    }

    /**
     * Batch execute quality checks for a datasource/table.
     */
    @Transactional
    @CacheEvict(value = CACHE_QUALITY_RULE, allEntries = true)
    public List<QualityEntity> batchExecute(String datasourceId, String tableName) {
        List<QualityEntity> rules = qualityRepository.findAll().stream()
                .filter(e -> "active".equals(e.getStatus()))
                .collect(Collectors.toList());

        if (datasourceId != null && !datasourceId.isEmpty()) {
            rules = rules.stream()
                    .filter(e -> datasourceId.equals(e.getDatasourceId()))
                    .collect(Collectors.toList());
        }
        if (tableName != null && !tableName.isEmpty()) {
            rules = rules.stream()
                    .filter(e -> tableName.equals(e.getTableName()))
                    .collect(Collectors.toList());
        }

        if (rules.isEmpty()) {
            throw new BusinessException("No active quality rules found to execute");
        }

        List<QualityEntity> executed = new ArrayList<>();
        for (QualityEntity rule : rules) {
            QualityRunRes res = qualityRuleEngine.run(rule);
            applyResult(rule, res);
            executed.add(qualityRepository.save(rule));
            persistAudit(rule, res);
        }

        log.info("Batch executed {} quality rules", executed.size());
        return executed;
    }
}