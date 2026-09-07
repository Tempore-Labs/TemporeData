package org.temporedata.modules.ops.alarm.repository;

import org.temporedata.modules.ops.alarm.entity.AlarmBaselineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlarmBaselineRepository extends JpaRepository<AlarmBaselineEntity, String> {

    List<AlarmBaselineEntity> findByEnabledTrue();
}