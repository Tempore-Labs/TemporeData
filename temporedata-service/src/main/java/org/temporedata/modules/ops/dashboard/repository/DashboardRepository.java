package org.temporedata.modules.ops.dashboard.repository;

import org.temporedata.modules.ops.dashboard.entity.DashboardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardRepository extends JpaRepository<DashboardEntity, String> {}
