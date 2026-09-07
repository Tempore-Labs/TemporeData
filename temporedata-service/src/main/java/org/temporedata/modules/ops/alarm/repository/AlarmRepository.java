package org.temporedata.modules.ops.alarm.repository;

import org.temporedata.modules.ops.alarm.entity.AlarmEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmRepository extends JpaRepository<AlarmEntity, String> {}
