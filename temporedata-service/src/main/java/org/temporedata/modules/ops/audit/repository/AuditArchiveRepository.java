package org.temporedata.modules.ops.audit.repository;

import org.temporedata.modules.ops.audit.entity.AuditArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditArchiveRepository extends JpaRepository<AuditArchiveEntity, String> {

    List<AuditArchiveEntity> findAllByOrderByCreateTimeDesc();
}