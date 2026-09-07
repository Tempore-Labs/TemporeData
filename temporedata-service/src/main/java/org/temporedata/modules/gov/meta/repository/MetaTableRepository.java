package org.temporedata.modules.gov.meta.repository;

import org.temporedata.modules.gov.meta.entity.MetaTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetaTableRepository extends JpaRepository<MetaTableEntity, String> {

    void deleteByDatasourceId(String datasourceId);

    List<MetaTableEntity> findByDatasourceId(String datasourceId);

    long countByDatasourceId(String datasourceId);
}