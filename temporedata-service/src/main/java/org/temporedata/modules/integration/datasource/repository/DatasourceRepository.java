package org.temporedata.modules.integration.datasource.repository;

import org.temporedata.modules.integration.datasource.entity.DatasourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasourceRepository extends JpaRepository<DatasourceEntity, String> {}