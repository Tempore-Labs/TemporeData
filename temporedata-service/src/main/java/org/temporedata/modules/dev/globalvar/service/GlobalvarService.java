package org.temporedata.modules.dev.globalvar.service;

import org.temporedata.modules.dev.globalvar.entity.GlobalvarEntity;
import org.temporedata.modules.dev.globalvar.repository.GlobalvarRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import static org.temporedata.common.cache.CacheConfig.CACHE_GLOBALVAR;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class GlobalvarService {
    private final GlobalvarRepository globalvarRepository;

    @Cacheable(value = CACHE_GLOBALVAR)
    public Page<GlobalvarEntity> page(Pageable p) { return globalvarRepository.findAll(p); }

    @Cacheable(value = CACHE_GLOBALVAR)
    public List<GlobalvarEntity> list() { return globalvarRepository.findAll(); }

    @Cacheable(value = CACHE_GLOBALVAR)
    public GlobalvarEntity get(String id) { return globalvarRepository.findById(id).orElseThrow(() -> new BusinessException("Globalvar not found: "+id)); }

    @CacheEvict(value = CACHE_GLOBALVAR, allEntries = true)
    @Transactional
    public GlobalvarEntity create(GlobalvarEntity e) { return globalvarRepository.save(e); }

    @CacheEvict(value = CACHE_GLOBALVAR, allEntries = true)
    @Transactional
    public GlobalvarEntity update(GlobalvarEntity e) { return globalvarRepository.save(e); }

    @CacheEvict(value = CACHE_GLOBALVAR, allEntries = true)
    @Transactional
    public void delete(String id) { globalvarRepository.deleteById(id); }
}
