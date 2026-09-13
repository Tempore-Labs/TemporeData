package org.temporedata.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.temporedata.asset.entity.AssetCatalogEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for temporedata_asset_catalog.
 */
@Repository
public interface AssetCatalogRepository extends JpaRepository<AssetCatalogEntity, Long> {

    Optional<AssetCatalogEntity> findByDatasetId(Long datasetId);

    boolean existsByDatasetId(Long datasetId);

    List<AssetCatalogEntity> findByTagsContaining(String tag);

    List<AssetCatalogEntity> findByOwner(String owner);
}