package org.temporedata.modules.ops.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.temporedata.modules.ops.incident.entity.IncidentEntity;

import java.util.List;

/**
 * JPA repository for temporedata_incident.
 */
@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {

    List<IncidentEntity> findByDatasetIdOrderByCreatedAtDesc(Long datasetId);

    List<IncidentEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<IncidentEntity> findBySeverityOrderByCreatedAtDesc(String severity);

    long countByStatus(String status);
}