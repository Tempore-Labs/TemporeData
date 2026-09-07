package org.temporedata.modules.gov.meta.repository;

import org.temporedata.modules.gov.meta.entity.MetaSyncLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetaSyncLogRepository extends JpaRepository<MetaSyncLogEntity, String> {

    java.util.Optional<MetaSyncLogEntity> findFirstByDatasourceIdOrderByStartedAtDesc(String datasourceId);
}