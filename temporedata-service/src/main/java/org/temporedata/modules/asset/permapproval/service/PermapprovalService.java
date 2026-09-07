package org.temporedata.modules.asset.permapproval.service;

import org.temporedata.modules.asset.permapproval.entity.PermapprovalEntity;
import org.temporedata.modules.asset.permapproval.repository.PermapprovalRepository;
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
public class PermapprovalService {

    private final PermapprovalRepository permapprovalRepository;

    public Page<PermapprovalEntity> page(Pageable pageable) { return permapprovalRepository.findAll(pageable); }

    public List<PermapprovalEntity> list() { return permapprovalRepository.findAll(); }

    public PermapprovalEntity get(String id) { return permapprovalRepository.findById(id).orElseThrow(() -> new BusinessException("Permapproval not found: " + id)); }

    @Transactional
    public PermapprovalEntity create(PermapprovalEntity entity) { return permapprovalRepository.save(entity); }

    @Transactional
    public PermapprovalEntity update(PermapprovalEntity entity) { return permapprovalRepository.save(entity); }

    @Transactional
    public void delete(String id) { permapprovalRepository.deleteById(id); }
}
