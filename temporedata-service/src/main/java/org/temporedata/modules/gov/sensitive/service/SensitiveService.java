package org.temporedata.modules.gov.sensitive.service;

import org.temporedata.modules.gov.sensitive.entity.SensitiveEntity;
import org.temporedata.modules.gov.sensitive.repository.SensitiveRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_SENSITIVE_RULE;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class SensitiveService {

    private final SensitiveRepository sensitiveRepository;

    @Cacheable(value = CACHE_SENSITIVE_RULE)
    public Page<SensitiveEntity> page(Pageable pageable) { return sensitiveRepository.findAll(pageable); }

    @Cacheable(value = CACHE_SENSITIVE_RULE)
    public List<SensitiveEntity> list() { return sensitiveRepository.findAll(); }

    @Cacheable(value = CACHE_SENSITIVE_RULE)
    public SensitiveEntity get(String id) { return sensitiveRepository.findById(id).orElseThrow(() -> new BusinessException("Sensitive not found: " + id)); }

    @Transactional
    @CacheEvict(value = CACHE_SENSITIVE_RULE, allEntries = true)
    public SensitiveEntity create(SensitiveEntity entity) { return sensitiveRepository.save(entity); }

    @Transactional
    @CacheEvict(value = CACHE_SENSITIVE_RULE, allEntries = true)
    public SensitiveEntity update(SensitiveEntity entity) { return sensitiveRepository.save(entity); }

    @Transactional
    @CacheEvict(value = CACHE_SENSITIVE_RULE, allEntries = true)
    public void delete(String id) { sensitiveRepository.deleteById(id); }
}
