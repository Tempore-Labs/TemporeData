package org.temporedata.integration.core.datasource.driver.repository;

import org.temporedata.integration.core.datasource.driver.entity.DriverPluginEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverPluginRepository extends JpaRepository<DriverPluginEntity, String> {}