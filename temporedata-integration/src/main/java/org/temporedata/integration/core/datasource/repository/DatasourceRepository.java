package org.temporedata.integration.core.datasource.repository;

import org.temporedata.integration.core.datasource.entity.DatasourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasourceRepository extends JpaRepository<DatasourceEntity, String> {}