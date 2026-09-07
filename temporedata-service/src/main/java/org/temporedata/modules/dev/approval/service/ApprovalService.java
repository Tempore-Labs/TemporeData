package org.temporedata.modules.dev.approval.service;

import org.temporedata.modules.dev.approval.entity.ApprovalEntity;
import org.temporedata.modules.dev.approval.repository.ApprovalRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;

    public Page<ApprovalEntity> page(Pageable pageable) { return approvalRepository.findAll(pageable); }

    public List<ApprovalEntity> list() { return approvalRepository.findAll(); }

    public ApprovalEntity get(String id) { return approvalRepository.findById(id).orElseThrow(() -> new BusinessException("Approval not found: " + id)); }

    @Transactional
    public ApprovalEntity create(ApprovalEntity entity) { return approvalRepository.save(entity); }

    @Transactional
    public ApprovalEntity update(ApprovalEntity entity) { return approvalRepository.save(entity); }

    @Transactional
    public void delete(String id) { approvalRepository.deleteById(id); }
}
