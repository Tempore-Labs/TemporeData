package org.temporedata.modules.dev.approval.engine;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.asset.permapproval.entity.PermapprovalEntity;
import org.temporedata.modules.asset.permapproval.repository.PermapprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for asset permission-request approvals (table zy_permapproval).
 */
@Component
@RequiredArgsConstructor
public class ResourcePermApprovalStrategy implements ApprovalStrategy<PermapprovalEntity> {

    private final PermapprovalRepository permapprovalRepository;

    @Override
    public String type() {
        return "RESOURCE_PERM";
    }

    @Override
    public PermapprovalEntity get(String id) {
        return permapprovalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("权限审批不存在: " + id));
    }

    @Override
    public List<PermapprovalEntity> listAll() {
        return permapprovalRepository.findAll();
    }

    @Override
    public List<PermapprovalEntity> listPending() {
        return permapprovalRepository.findByStatus("PENDING");
    }

    @Override
    public List<PermapprovalEntity> listByApplicant(String applicantId) {
        return permapprovalRepository.findByApplicantId(applicantId);
    }

    @Override
    public PermapprovalEntity save(PermapprovalEntity entity) {
        return permapprovalRepository.save(entity);
    }

    @Override
    public void delete(String id) {
        permapprovalRepository.deleteById(id);
    }
}