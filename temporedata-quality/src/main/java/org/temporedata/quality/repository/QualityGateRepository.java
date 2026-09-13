package org.temporedata.quality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.temporedata.quality.entity.QualityGateEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for temporedata_quality_gate.
 */
@Repository
public interface QualityGateRepository extends JpaRepository<QualityGateEntity, Long> {

    List<QualityGateEntity> findByDatasetId(Long datasetId);

    Optional<QualityGateEntity> findByDatasetIdAndRuleId(Long datasetId, Long ruleId);

    Optional<QualityGateEntity> findFirstByDatasetIdOrderByIdDesc(Long datasetId);
}