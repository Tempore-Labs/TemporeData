package org.temporedata.modules.asset.permapproval.repository;

import org.temporedata.modules.asset.permapproval.entity.PermapprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermapprovalRepository extends JpaRepository<PermapprovalEntity, String> {

    List<PermapprovalEntity> findByStatus(String status);

    List<PermapprovalEntity> findByApplicantId(String applicantId);
}
