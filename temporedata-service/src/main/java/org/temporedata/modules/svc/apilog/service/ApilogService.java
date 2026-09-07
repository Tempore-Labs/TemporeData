package org.temporedata.modules.svc.apilog.service;

import org.temporedata.modules.svc.apilog.entity.ApilogEntity;
import org.temporedata.modules.svc.apilog.repository.ApilogRepository;
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
public class ApilogService {

    private final ApilogRepository apilogRepository;

    public Page<ApilogEntity> page(Pageable pageable) { return apilogRepository.findAll(pageable); }

    public List<ApilogEntity> list() { return apilogRepository.findAll(); }

    public ApilogEntity get(String id) { return apilogRepository.findById(id).orElseThrow(() -> new BusinessException("Apilog not found: " + id)); }

    @Transactional
    public ApilogEntity create(ApilogEntity entity) { return apilogRepository.save(entity); }

    @Transactional
    public ApilogEntity update(ApilogEntity entity) { return apilogRepository.save(entity); }

    @Transactional
    public void delete(String id) { apilogRepository.deleteById(id); }
}
