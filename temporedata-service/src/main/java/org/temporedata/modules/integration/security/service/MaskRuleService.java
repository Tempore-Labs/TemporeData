package org.temporedata.modules.integration.security.service;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.integration.security.entity.MaskRuleEntity;
import org.temporedata.modules.integration.security.repository.MaskRuleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.temporedata.common.cache.CacheConfig.CACHE_SECURITY_RULE;

@Service
public class MaskRuleService {

    private final MaskRuleRepository repository;

    public MaskRuleService(MaskRuleRepository repository) {
        this.repository = repository;
    }

    public List<MaskRuleEntity> list() {
        return repository.findAll();
    }

    public List<MaskRuleEntity> listByDatasource(String datasourceId) {
        return repository.findByDatasourceIdAndStatus(datasourceId, 1);
    }

    public MaskRuleEntity get(String id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("MaskRule not found: " + id));
    }

    @CacheEvict(cacheNames = CACHE_SECURITY_RULE, allEntries = true)
    @Transactional
    public MaskRuleEntity create(MaskRuleEntity entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        return repository.save(entity);
    }

    @CacheEvict(cacheNames = CACHE_SECURITY_RULE, allEntries = true)
    @Transactional
    public MaskRuleEntity update(String id, MaskRuleEntity entity) {
        MaskRuleEntity existing = get(id);
        existing.setName(entity.getName());
        existing.setRuleType(entity.getRuleType());
        existing.setMaskPattern(entity.getMaskPattern());
        existing.setDescription(entity.getDescription());
        existing.setDatasourceId(entity.getDatasourceId());
        existing.setTableName(entity.getTableName());
        existing.setColumnName(entity.getColumnName());
        existing.setStatus(entity.getStatus());
        return repository.save(existing);
    }

    @CacheEvict(cacheNames = CACHE_SECURITY_RULE, allEntries = true)
    @Transactional
    public void delete(String id) {
        repository.deleteById(id);
    }
}