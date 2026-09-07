package org.temporedata.modules.dev.approval.engine;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.dev.approval.entity.ApprovalEntity;
import org.temporedata.modules.dev.approval.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for generic dev approval records (table zy_approval).
 */
@Component
@RequiredArgsConstructor
public class GenericApprovalStrategy implements ApprovalStrategy<ApprovalEntity> {

    private final ApprovalRepository approvalRepository;

    @Override
    public String type() {
        return "GENERIC";
    }

    @Override
    public ApprovalEntity get(String id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审批不存在: " + id));
    }

    @Override
    public List<ApprovalEntity> listAll() {
        return approvalRepository.findAll();
    }

    @Override
    public List<ApprovalEntity> listPending() {
        return approvalRepository.findByStatus("PENDING");
    }

    @Override
    public List<ApprovalEntity> listByApplicant(String applicantId) {
        return approvalRepository.findByApplicantId(applicantId);
    }

    @Override
    public ApprovalEntity save(ApprovalEntity entity) {
        return approvalRepository.save(entity);
    }

    @Override
    public void delete(String id) {
        approvalRepository.deleteById(id);
    }
}