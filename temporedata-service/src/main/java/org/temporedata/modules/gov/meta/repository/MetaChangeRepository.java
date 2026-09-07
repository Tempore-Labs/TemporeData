package org.temporedata.modules.gov.meta.repository;

import org.temporedata.modules.gov.meta.entity.MetaChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetaChangeRepository extends JpaRepository<MetaChangeEntity, String> {

    List<MetaChangeEntity> findByDatasourceId(String datasourceId);

    List<MetaChangeEntity> findByDatasourceIdOrderBySyncedAtDesc(String datasourceId);
}