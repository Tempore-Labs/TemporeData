package org.temporedata.modules.dev.schedule.calendar.repository;

import org.temporedata.modules.dev.schedule.calendar.entity.BizDateQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BizDateQueueRepository extends JpaRepository<BizDateQueueEntity, String> {

    List<BizDateQueueEntity> findByStateOrderByBizDateAsc(String state);

    Optional<BizDateQueueEntity> findByTaskIdAndBizDate(String taskId, String bizDate);
}