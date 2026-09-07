package org.temporedata.modules.ops.alarm.repository;

import org.temporedata.modules.ops.alarm.entity.AlarmRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlarmRecordRepository extends JpaRepository<AlarmRecordEntity, String> {

    List<AlarmRecordEntity> findByBaselineIdOrderByCreateTimeDesc(String baselineId);

    List<AlarmRecordEntity> findAllByOrderByCreateTimeDesc();

    boolean existsByBaselineIdAndBizDateAndAlarmTypeAndStatusNot(
            String baselineId, String bizDate, String alarmType, String status);

    List<AlarmRecordEntity> findByBaselineIdAndBizDateAndStatusNot(String baselineId, String bizDate, String status);
}