package org.temporedata.modules.gov.meta.repository;

import org.temporedata.modules.gov.meta.entity.MetaColumnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetaColumnRepository extends JpaRepository<MetaColumnEntity, String> {

    void deleteByDatasourceId(String datasourceId);

    List<MetaColumnEntity> findByDatasourceId(String datasourceId);

    List<MetaColumnEntity> findByTableId(String tableId);

    long countByDatasourceId(String datasourceId);
}