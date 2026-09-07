package org.temporedata.modules.ops.audit.repository;

import org.temporedata.modules.ops.audit.entity.AuditPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditPolicyRepository extends JpaRepository<AuditPolicyEntity, String> {

    Optional<AuditPolicyEntity> findByModuleAndAction(String module, String action);

    Optional<AuditPolicyEntity> findByModuleAndActionIsNull(String module);
}