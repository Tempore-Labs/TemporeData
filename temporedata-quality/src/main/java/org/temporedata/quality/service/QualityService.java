package org.temporedata.quality.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.quality.entity.QualityGateEntity;
import org.temporedata.quality.entity.QualityRuleEntity;
import org.temporedata.quality.repository.QualityGateRepository;
import org.temporedata.quality.repository.QualityRuleRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * [ENT-P0] Quality rule &amp; gate orchestration over temporedata_quality_*.
 *
 * <p>Manages rule definitions and per-dataset gate configs. The evaluate/assess entry point
 * is the future coupling point with the WorkflowEngine's Quality Gate node.
 * Distinct bean name to coexist with the legacy gov QualityService.
 */
@Service("enterpriseQualityService")
public class QualityService {

    private final QualityRuleRepository ruleRepository;
    private final QualityGateRepository gateRepository;

    public QualityService(QualityRuleRepository ruleRepository, QualityGateRepository gateRepository) {
        this.ruleRepository = ruleRepository;
        this.gateRepository = gateRepository;
    }

    // ---- rules ----

    @Transactional
    public QualityRuleEntity createRule(QualityRuleEntity rule) {
        if (rule.getDatasetId() == null) {
            throw new BusinessException("rule.datasetId is required");
        }
        rule.setId(null);
        return ruleRepository.save(rule);
    }

    @Transactional
    public QualityRuleEntity updateRule(Long id, QualityRuleEntity patch) {
        QualityRuleEntity existing = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("quality rule not found: " + id));
        if (patch.getDimension() != null) existing.setDimension(patch.getDimension());
        if (patch.getRuleType() != null) existing.setRuleType(patch.getRuleType());
        if (patch.getExpression() != null) existing.setExpression(patch.getExpression());
        if (patch.getThresholdScore() != null) existing.setThresholdScore(patch.getThresholdScore());
        if (patch.getIsBlocking() != null) existing.setIsBlocking(patch.getIsBlocking());
        return ruleRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Page<QualityRuleEntity> pageRules(Long datasetId, Pageable pageable) {
        return datasetId == null ? ruleRepository.findAll(pageable) : ruleRepository.findByDatasetId(datasetId, pageable);
    }

    @Transactional(readOnly = true)
    public List<QualityRuleEntity> listRules(Long datasetId) {
        return datasetId == null ? ruleRepository.findAll() : ruleRepository.findByDatasetId(datasetId);
    }

    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    // ---- gates ----

    @Transactional
    public QualityGateEntity createGate(QualityGateEntity gate) {
        if (gate.getDatasetId() == null) {
            throw new BusinessException("gate.datasetId is required");
        }
        if (gate.getRuleId() != null && gateRepository.findByDatasetIdAndRuleId(gate.getDatasetId(), gate.getRuleId()).isPresent()) {
            throw new BusinessException("quality gate already exists for dataset+rule");
        }
        gate.setId(null);
        if (gate.getStatus() == null) {
            gate.setStatus("PASSED");
        }
        return gateRepository.save(gate);
    }

    @Transactional
    public QualityGateEntity updateGate(Long id, QualityGateEntity patch) {
        QualityGateEntity existing = gateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("quality gate not found: " + id));
        if (patch.getMinScore() != null) existing.setMinScore(patch.getMinScore());
        if (patch.getBlocking() != null) existing.setBlocking(patch.getBlocking());
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        return gateRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<QualityGateEntity> listGates(Long datasetId) {
        return datasetId == null ? gateRepository.findAll() : gateRepository.findByDatasetId(datasetId);
    }

    /**
     * Evaluate a measured score against the dataset's (latest) gate and set its status.
     * score &lt; minScore &amp;&amp; blocking =&gt; BLOCKED, else PASSED.
     */
    @Transactional
    public QualityGateEntity assess(Long datasetId, BigDecimal score) {
        QualityGateEntity gate = gateRepository.findFirstByDatasetIdOrderByIdDesc(datasetId)
                .orElseThrow(() -> new BusinessException("no quality gate for dataset: " + datasetId));
        boolean shouldBlock = gate.getBlocking() != null && gate.getBlocking()
                && gate.getMinScore() != null && score != null && score.compareTo(gate.getMinScore()) < 0;
        gate.setStatus(shouldBlock ? "BLOCKED" : "PASSED");
        return gateRepository.save(gate);
    }
}