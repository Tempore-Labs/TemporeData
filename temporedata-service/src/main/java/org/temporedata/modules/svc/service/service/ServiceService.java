package org.temporedata.modules.svc.service.service;

import org.temporedata.modules.svc.service.entity.ServiceEntity;
import org.temporedata.modules.svc.service.repository.ServiceRepository;
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
import static org.temporedata.common.cache.CacheConfig.CACHE_DATA_API;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;

    @Cacheable(value = CACHE_DATA_API)
    public Page<ServiceEntity> page(Pageable pageable) { return serviceRepository.findAll(pageable); }

    @Cacheable(value = CACHE_DATA_API)
    public List<ServiceEntity> list() { return serviceRepository.findAll(); }

    @Cacheable(value = CACHE_DATA_API)
    public ServiceEntity get(String id) { return serviceRepository.findById(id).orElseThrow(() -> new BusinessException("Service not found: " + id)); }

    @CacheEvict(value = CACHE_DATA_API, allEntries = true)
    @Transactional
    public ServiceEntity create(ServiceEntity entity) { return serviceRepository.save(entity); }

    @CacheEvict(value = CACHE_DATA_API, allEntries = true)
    @Transactional
    public ServiceEntity update(ServiceEntity entity) { return serviceRepository.save(entity); }

    @CacheEvict(value = CACHE_DATA_API, allEntries = true)
    @Transactional
    public void delete(String id) { serviceRepository.deleteById(id); }
}
