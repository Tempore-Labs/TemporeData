package org.temporedata.modules.dev.schedule.repository;

import org.temporedata.modules.dev.schedule.entity.TaskLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLogEntity, String> {

    List<TaskLogEntity> findByInstanceIdOrderByCreatedAtAsc(String instanceId);
}