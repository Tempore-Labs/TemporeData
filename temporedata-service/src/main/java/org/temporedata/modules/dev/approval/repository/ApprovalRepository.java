package org.temporedata.modules.dev.approval.repository;

import org.temporedata.modules.dev.approval.entity.ApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRepository extends JpaRepository<ApprovalEntity, String> {

    List<ApprovalEntity> findByStatus(String status);

    List<ApprovalEntity> findByApplicantId(String applicantId);
}
