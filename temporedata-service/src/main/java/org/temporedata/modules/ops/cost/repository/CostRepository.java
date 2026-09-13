package org.temporedata.modules.ops.cost.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.temporedata.modules.ops.cost.entity.CostEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for temporedata_data_cost.
 */
@Repository
public interface CostRepository extends JpaRepository<CostEntity, Long> {

    Optional<CostEntity> findByDatasetIdAndRecordDate(Long datasetId, LocalDate recordDate);

    List<CostEntity> findByDatasetIdOrderByRecordDateDesc(Long datasetId);

    List<CostEntity> findByDatasetIdAndRecordDateBetween(Long datasetId, LocalDate from, LocalDate to);
}