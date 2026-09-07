package org.temporedata.modules.integration.datasource.driver.repository;

import org.temporedata.modules.integration.datasource.driver.entity.DriverPluginEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverPluginRepository extends JpaRepository<DriverPluginEntity, String> {}