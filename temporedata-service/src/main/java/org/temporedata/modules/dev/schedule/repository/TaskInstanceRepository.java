package org.temporedata.modules.dev.schedule.repository;

import org.temporedata.modules.dev.schedule.entity.TaskInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskInstanceRepository extends JpaRepository<TaskInstanceEntity, String> {

    Long countByTaskId(String taskId);

    boolean existsByTaskIdAndTriggerTime(String taskId, LocalDateTime triggerTime);

    boolean existsByTaskIdAndBizDate(String taskId, String bizDate);

    List<TaskInstanceEntity> findByTaskIdOrderByTriggerTimeDesc(String taskId);

    Optional<TaskInstanceEntity> findTopByTaskIdOrderByInstanceNoDesc(String taskId);
}