package org.temporedata.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.temporedata.asset.entity.MetricEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for temporedata_metric.
 */
@Repository
public interface MetricRepository extends JpaRepository<MetricEntity, Long> {

    Optional<MetricEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<MetricEntity> findByDatasetId(Long datasetId);

    List<MetricEntity> findByMetricType(String metricType);
}