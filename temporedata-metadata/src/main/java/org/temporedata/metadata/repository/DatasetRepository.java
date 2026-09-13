package org.temporedata.metadata.repository;

import org.temporedata.metadata.entity.DatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for temporedata_dataset.
 */
@Repository
public interface DatasetRepository extends JpaRepository<DatasetEntity, Long> {

    Optional<DatasetEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<DatasetEntity> findByDomainId(Long domainId);

    List<DatasetEntity> findByLayer(String layer);
}