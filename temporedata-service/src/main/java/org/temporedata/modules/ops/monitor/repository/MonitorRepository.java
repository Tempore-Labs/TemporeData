package org.temporedata.modules.ops.monitor.repository;

import org.temporedata.modules.ops.monitor.entity.MonitorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitorRepository extends JpaRepository<MonitorEntity, String> {}
