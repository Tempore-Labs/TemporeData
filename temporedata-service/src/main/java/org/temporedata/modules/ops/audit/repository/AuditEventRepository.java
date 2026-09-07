package org.temporedata.modules.ops.audit.repository;

import org.temporedata.modules.ops.audit.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {

    Optional<AuditEventEntity> findTopByOrderBySeqDesc();

    List<AuditEventEntity> findAllByOrderBySeqDesc();

    List<AuditEventEntity> findByEventTimeGreaterThanEqualOrderBySeqAsc(String from);

    List<AuditEventEntity> findByEventTimeBetweenOrderBySeqAsc(String from, String to);

    List<AuditEventEntity> findByModule(String module);

    List<AuditEventEntity> findByOperator(String operator);
}