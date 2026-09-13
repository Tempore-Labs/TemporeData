package org.temporedata.quality.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.temporedata.quality.entity.QualityRuleEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for temporedata_quality_rule.
 */
@Repository
public interface QualityRuleRepository extends JpaRepository<QualityRuleEntity, Long> {

    Page<QualityRuleEntity> findByDatasetId(Long datasetId, Pageable pageable);

    List<QualityRuleEntity> findByDatasetId(Long datasetId);

    Optional<QualityRuleEntity> findByDatasetIdAndId(Long datasetId, Long id);

    boolean existsByDatasetIdAndRuleType(Long datasetId, String ruleType);
}