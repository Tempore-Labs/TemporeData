package org.temporedata.modules.integration.datasource.driver.service;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.datasource.context.PluginManager;
import org.temporedata.modules.integration.datasource.driver.entity.DriverPluginEntity;
import org.temporedata.modules.integration.datasource.driver.repository.DriverPluginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages uploaded datasource driver-jar plugins: persists records in
 * zy_datasource_plugin and delegates isolated loading/unloading to
 * {@link PluginManager}.
 */
@Slf4j @Service @RequiredArgsConstructor
public class DriverPluginService {

    private final DriverPluginRepository repository;
    private final PluginManager pluginManager;
    private final MavenDependencyResolver mavenDependencyResolver;

    public List<DriverPluginRes> list() {
        return repository.findAll().stream().map(this::toRes).collect(Collectors.toList());
    }

    /**
     * Load a driver plugin either by Maven GAV (resolve transitive deps) or by a
     * local jar path, verify it is a valid DatasourceProcessor, then persist the
     * record (builtin=false) with the resolved dbType.
     */
    @Transactional
    public DriverPluginRes create(DriverPluginReq req) {
        boolean byGav = hasText(req.getMvnGroup()) && hasText(req.getMvnArtifact()) && hasText(req.getMvnVersion());
        PluginManager.PluginLoadResult result;
        try {
            if (byGav) {
                List<File> files = mavenDependencyResolver.resolve(req.getMvnGroup(), req.getMvnArtifact(), req.getMvnVersion());
                result = pluginManager.loadFiles(files);
            } else {
                if (!hasText(req.getDriverPath())) {
                    throw new BusinessException("需提供 Maven 坐标(GAV) 或 driverPath");
                }
                result = pluginManager.load(req.getDriverPath());
            }
        } catch (Exception e) {
            log.warn("Failed to load plugin {}: {}", byGav ? mavenDependencyResolverKey(req) : req.getDriverPath(), e.getMessage());
            throw new BusinessException("加载插件失败: " + e.getMessage());
        }
        DriverPluginEntity entity = DriverPluginEntity.builder()
                .name(req.getName())
                .dbType(result.getType())
                .version(req.getVersion())
                .driverPath(req.getDriverPath())
                .mvnGroup(req.getMvnGroup())
                .mvnArtifact(req.getMvnArtifact())
                .mvnVersion(req.getMvnVersion())
                .status("ENABLED")
                .builtin(false)
                .build();
        return toRes(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        DriverPluginEntity entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Datasource plugin not found: " + id));
        pluginManager.unloadByType(entity.getDbType());
        repository.delete(entity);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String mavenDependencyResolverKey(DriverPluginReq req) {
        return req.getMvnGroup() + ":" + req.getMvnArtifact() + ":" + req.getMvnVersion();
    }

    private DriverPluginRes toRes(DriverPluginEntity e) {
        DriverPluginRes res = new DriverPluginRes();
        res.setId(e.getId());
        res.setDbType(e.getDbType());
        res.setName(e.getName());
        res.setVersion(e.getVersion());
        res.setDriverPath(e.getDriverPath());
        res.setMvnGroup(e.getMvnGroup());
        res.setMvnArtifact(e.getMvnArtifact());
        res.setMvnVersion(e.getMvnVersion());
        res.setStatus(e.getStatus());
        res.setBuiltin(e.getBuiltin());
        res.setCreateTime(e.getCreateTime() == null ? null : e.getCreateTime().toString().replace('T', ' '));
        return res;
    }
}