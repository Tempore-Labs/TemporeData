package org.temporedata.modules.dev.schedule.repository;

import org.temporedata.modules.dev.schedule.entity.TaskDefineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskDefineRepository extends JpaRepository<TaskDefineEntity, String> {

    /** Enabled, NORMAL task definitions eligible for scheduling. */
    List<TaskDefineEntity> findByEnabledTrueAndStatus(String status);
}