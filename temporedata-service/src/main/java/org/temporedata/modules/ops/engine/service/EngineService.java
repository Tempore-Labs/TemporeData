package org.temporedata.modules.ops.engine.service;

import org.temporedata.modules.ops.engine.entity.EngineEntity;
import org.temporedata.modules.ops.engine.repository.EngineRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.temporedata.common.cache.CacheConfig.CACHE_ENGINE;

@Slf4j @Service @RequiredArgsConstructor
public class EngineService {

    private final EngineRepository engineRepository;

    /**
     * List all engines of a given type.
     */
    @Cacheable(value = CACHE_ENGINE)
    public List<EngineEntity> listByType(String type) {
        return engineRepository.findByType(type);
    }

    /**
     * Get engine by id.
     */
    @Cacheable(value = CACHE_ENGINE)
    public EngineEntity get(String id) {
        return engineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Engine not found: " + id));
    }

    /**
     * Create a new engine.
     */
    @CacheEvict(value = CACHE_ENGINE, allEntries = true)
    @Transactional
    public EngineEntity create(EngineEntity entity) {
        log.info("Creating engine: name={}, type={}", entity.getName(), entity.getType());
        return engineRepository.save(entity);
    }

    /**
     * Update an existing engine.
     */
    @CacheEvict(value = CACHE_ENGINE, allEntries = true)
    @Transactional
    public EngineEntity update(String id, EngineEntity entity) {
        EngineEntity existing = get(id);
        // Preserve base fields
        entity.setId(id);
        entity.setCreateDateTime(existing.getCreateDateTime());
        entity.setCreateBy(existing.getCreateBy());
        entity.setTenantId(existing.getTenantId());
        log.info("Updating engine: id={}, name={}", id, entity.getName());
        return engineRepository.save(entity);
    }

    /**
     * Delete an engine by id.
     */
    @CacheEvict(value = CACHE_ENGINE, allEntries = true)
    @Transactional
    public void delete(String id) {
        EngineEntity entity = get(id);
        engineRepository.delete(entity);
        log.info("Deleted engine: id={}", id);
    }
}