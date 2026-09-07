package org.temporedata.modules.integration.security.repository;

import org.temporedata.modules.integration.security.entity.MaskRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaskRuleRepository extends JpaRepository<MaskRuleEntity, String> {

    List<MaskRuleEntity> findByDatasourceIdAndTableNameAndStatus(String datasourceId, String tableName, Integer status);

    List<MaskRuleEntity> findByDatasourceIdAndStatus(String datasourceId, Integer status);
}