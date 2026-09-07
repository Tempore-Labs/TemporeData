package org.temporedata.modules.sys.org.service;

import org.temporedata.modules.sys.org.entity.OrgEntity;
import org.temporedata.modules.sys.org.repository.OrgRepository;
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

import static org.temporedata.common.cache.CacheConfig.CACHE_ORG;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class OrgService {

    private final OrgRepository orgRepository;

    @Cacheable(value = CACHE_ORG)
    public Page<OrgEntity> page(Pageable pageable) { return orgRepository.findAll(pageable); }

    @Cacheable(value = CACHE_ORG)
    public List<OrgEntity> list() { return orgRepository.findAll(); }

    @Cacheable(value = CACHE_ORG)
    public OrgEntity get(String id) { return orgRepository.findById(id).orElseThrow(() -> new BusinessException("Org not found: " + id)); }

    @CacheEvict(value = CACHE_ORG, allEntries = true)
    @Transactional
    public OrgEntity create(OrgEntity entity) { return orgRepository.save(entity); }

    @CacheEvict(value = CACHE_ORG, allEntries = true)
    @Transactional
    public OrgEntity update(OrgEntity entity) { return orgRepository.save(entity); }

    @CacheEvict(value = CACHE_ORG, allEntries = true)
    @Transactional
    public void delete(String id) { orgRepository.deleteById(id); }
}
