package org.temporedata.modules.gov.catalog.repository;

import org.temporedata.modules.gov.catalog.entity.CatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogRepository extends JpaRepository<CatalogEntity, String> {

    List<CatalogEntity> findByDatasourceId(String datasourceId);
}
