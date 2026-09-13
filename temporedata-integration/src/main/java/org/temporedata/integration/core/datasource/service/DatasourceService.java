package org.temporedata.integration.core.datasource.service;

import org.temporedata.api.integration.datasource.DatasourceReq;
import org.temporedata.api.integration.datasource.DatasourceRes;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.common.util.Crypto;
import org.temporedata.api.datasource.DatasourcePluginInfo;
import org.temporedata.api.datasource.DatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.context.DatasourcePluginContext;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.integration.core.datasource.entity.DatasourceEntity;
import org.temporedata.integration.core.datasource.repository.DatasourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.temporedata.common.cache.CacheConfig.CACHE_DATASOURCE;

@Slf4j @Service @RequiredArgsConstructor
public class DatasourceService {

    private final DatasourceRepository datasourceRepository;
    private final DatasourcePluginContext pluginContext;
    private final Crypto crypto;

    @Cacheable(value = CACHE_DATASOURCE, unless = "#result == null")
    public Page<DatasourceRes> page(Pageable pageable) {
        return datasourceRepository.findAll(pageable).map(this::toRes);
    }

    @Cacheable(value = CACHE_DATASOURCE, unless = "#result == null")
    public List<DatasourceRes> list() {
        return datasourceRepository.findAll().stream().map(this::toRes).collect(Collectors.toList());
    }

    @Cacheable(value = CACHE_DATASOURCE, unless = "#result == null")
    public DatasourceRes get(String id) {
        return toRes(findEntity(id));
    }

    @CacheEvict(value = CACHE_DATASOURCE, allEntries = true)
    @Transactional
    public DatasourceRes create(DatasourceReq req) {
        validateType(req.getType());
        DatasourceEntity entity = new DatasourceEntity();
        applyReq(entity, req);
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            entity.setPassword(crypto.encrypt(req.getPassword()));
        }
        if (entity.getTenantId() == null || entity.getTenantId().isBlank()) {
            // tenant_id is NOT NULL with no default; fall back to a safe value when the
            // caller did not supply one (consistent with UserService's "DEFAULT").
            entity.setTenantId("DEFAULT");
        }
        return toRes(datasourceRepository.save(entity));
    }

    @CacheEvict(value = CACHE_DATASOURCE, allEntries = true)
    @Transactional
    public DatasourceRes update(String id, DatasourceReq req) {
        validateType(req.getType());
        DatasourceEntity entity = findEntity(id);
        applyReq(entity, req);
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            entity.setPassword(crypto.encrypt(req.getPassword()));
        }
        return toRes(datasourceRepository.save(entity));
    }

    @CacheEvict(value = CACHE_DATASOURCE, allEntries = true)
    @Transactional
    public void delete(String id) {
        DatasourceEntity entity = findEntity(id);
        datasourceRepository.delete(entity);
    }

    public void testConnection(String id) {
        DatasourceEntity entity = findEntity(id);
        DatasourceProcessor processor = pluginContext.resolve(entity.getType());
        DatasourceParamDTO param = toParam(entity, processor);
        param.setPassword(crypto.decrypt(entity.getPassword()));
        log.info("Testing connection for datasource: {} ({})", entity.getName(), entity.getType());
        try {
            processor.check(param);
            log.info("Connection test succeeded for datasource: {}", entity.getName());
        } catch (Exception e) {
            log.error("Connection test failed for datasource: {}", entity.getName(), e);
            throw new BusinessException("Connection test failed: " + e.getMessage());
        }
    }

    /** Supported dialect type keys exposed for the frontend dictionary. */
    public List<String> pluginTypes() {
        return pluginContext.listTypes().stream()
                .map(DatasourceType::name)
                .collect(Collectors.toList());
    }

    /** Read-only list of all currently supported plugins (built-in + uploaded). */
    public List<DatasourcePluginInfo> plugins() {
        return pluginContext.listPlugins();
    }

    private void validateType(String type) {
        if (!pluginContext.supports(type)) {
            throw new BusinessException(
                    "Unsupported datasource type: " + type + ", supported: " + pluginTypes());
        }
    }

    private DatasourceEntity findEntity(String id) {
        return datasourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Datasource not found: " + id));
    }

    private void applyReq(DatasourceEntity entity, DatasourceReq req) {
        entity.setName(req.getName());
        entity.setType(req.getType().toUpperCase());
        entity.setHost(req.getHost());
        entity.setPort(req.getPort() != null
                ? req.getPort()
                : pluginContext.resolve(req.getType()).defaultPort());
        entity.setDatabase(req.getDatabase());
        entity.setUsername(req.getUsername());
        entity.setParams(req.getParams());
    }

    private DatasourceParamDTO toParam(DatasourceEntity entity, DatasourceProcessor processor) {
        try {
            DatasourceParamDTO param = processor.paramClass().getDeclaredConstructor().newInstance();
            param.setHost(entity.getHost());
            param.setPort(entity.getPort());
            param.setDatabase(entity.getDatabase());
            param.setUsername(entity.getUsername());
            param.setParams(entity.getParams());
            return param;
        } catch (ReflectiveOperationException e) {
            throw new BusinessException("Cannot build datasource param: " + e.getMessage());
        }
    }

    private DatasourceRes toRes(DatasourceEntity entity) {
        DatasourceRes res = new DatasourceRes();
        res.setId(entity.getId());
        res.setName(entity.getName());
        res.setType(entity.getType());
        res.setHost(entity.getHost());
        res.setPort(entity.getPort());
        res.setDatabase(entity.getDatabase());
        res.setUsername(entity.getUsername());
        res.setParams(entity.getParams());
        res.setCreateTime(entity.getCreateTime());
        return res;
    }
}