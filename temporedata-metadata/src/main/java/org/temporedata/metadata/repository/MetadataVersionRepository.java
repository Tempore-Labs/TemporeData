package org.temporedata.metadata.repository;

import org.temporedata.metadata.entity.MetadataVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for temporedata_metadata_version.
 */
@Repository
public interface MetadataVersionRepository extends JpaRepository<MetadataVersionEntity, Long> {

    Optional<MetadataVersionEntity> findTopByDatasetIdOrderByVersionDesc(Long datasetId);

    List<MetadataVersionEntity> findByDatasetIdOrderByVersionDesc(Long datasetId);

    boolean existsByDatasetIdAndVersion(Long datasetId, Integer version);
}