package org.temporedata.modules.dev.func.service;

import org.temporedata.modules.dev.func.entity.FuncEntity;
import org.temporedata.modules.dev.func.repository.FuncRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import static org.temporedata.common.cache.CacheConfig.CACHE_FUNC;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class FuncService {

    private final FuncRepository funcRepository;

    @Cacheable(value = CACHE_FUNC)
    public Page<FuncEntity> page(Pageable pageable) { return funcRepository.findAll(pageable); }

    @Cacheable(value = CACHE_FUNC)
    public List<FuncEntity> list() { return funcRepository.findAll(); }

    @Cacheable(value = CACHE_FUNC)
    public FuncEntity get(String id) { return funcRepository.findById(id).orElseThrow(() -> new BusinessException("Func not found: " + id)); }

    @CacheEvict(value = CACHE_FUNC, allEntries = true)
    @Transactional
    public FuncEntity create(FuncEntity entity) { return funcRepository.save(entity); }

    @CacheEvict(value = CACHE_FUNC, allEntries = true)
    @Transactional
    public FuncEntity update(FuncEntity entity) { return funcRepository.save(entity); }

    @CacheEvict(value = CACHE_FUNC, allEntries = true)
    @Transactional
    public void delete(String id) { funcRepository.deleteById(id); }
}
