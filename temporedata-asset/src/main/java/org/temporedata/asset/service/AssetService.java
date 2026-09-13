package org.temporedata.asset.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.asset.entity.AssetCatalogEntity;
import org.temporedata.asset.entity.MetricEntity;
import org.temporedata.asset.repository.AssetCatalogRepository;
import org.temporedata.asset.repository.MetricRepository;

import java.util.List;
import java.util.Optional;

/**
 * [ENT-P0] Asset catalog &amp; metric platform orchestration over temporedata_asset_*.
 *
 * <p>Catalog binds a single row per dataset; metric supports atomic/derived/compound
 * definitions that can later be published as scheduled pre-compute + SQL/REST API.
 */
@Service
public class AssetService {

    private final AssetCatalogRepository catalogRepository;
    private final MetricRepository metricRepository;

    public AssetService(AssetCatalogRepository catalogRepository, MetricRepository metricRepository) {
        this.catalogRepository = catalogRepository;
        this.metricRepository = metricRepository;
    }

    // ---- catalog ----

    @Transactional
    public AssetCatalogEntity upsertCatalog(AssetCatalogEntity catalog) {
        if (catalog.getDatasetId() == null) {
            throw new BusinessException("asset.datasetId is required");
        }
        Optional<AssetCatalogEntity> existing = catalogRepository.findByDatasetId(catalog.getDatasetId());
        if (existing.isPresent()) {
            AssetCatalogEntity e = existing.get();
            if (catalog.getBusinessTerm() != null) e.setBusinessTerm(catalog.getBusinessTerm());
            if (catalog.getTags() != null) e.setTags(catalog.getTags());
            if (catalog.getOwner() != null) e.setOwner(catalog.getOwner());
            if (catalog.getPopularity() != null) e.setPopularity(catalog.getPopularity());
            return catalogRepository.save(e);
        }
        catalog.setId(null);
        if (catalog.getPopularity() == null) {
            catalog.setPopularity(0);
        }
        return catalogRepository.save(catalog);
    }

    @Transactional(readOnly = true)
    public Optional<AssetCatalogEntity> getCatalog(Long datasetId) {
        return catalogRepository.findByDatasetId(datasetId);
    }

    @Transactional(readOnly = true)
    public Page<AssetCatalogEntity> pageCatalog(Pageable pageable) {
        return catalogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<AssetCatalogEntity> searchByTag(String tag) {
        return catalogRepository.findByTagsContaining(tag);
    }

    /** Simple popularity bump on asset access (call from browse/hit paths). */
    @Transactional
    public AssetCatalogEntity touch(Long datasetId) {
        AssetCatalogEntity e = catalogRepository.findByDatasetId(datasetId)
                .orElseThrow(() -> new BusinessException("asset not found for dataset: " + datasetId));
        e.setPopularity((e.getPopularity() == null ? 0 : e.getPopularity()) + 1);
        return catalogRepository.save(e);
    }

    // ---- metrics ----

    @Transactional
    public MetricEntity createMetric(MetricEntity metric) {
        if (metric.getCode() == null || metric.getCode().isBlank()) {
            throw new BusinessException("metric.code is required");
        }
        if (metricRepository.existsByCode(metric.getCode())) {
            throw new BusinessException("metric.code already exists: " + metric.getCode());
        }
        metric.setId(null);
        return metricRepository.save(metric);
    }

    @Transactional
    public MetricEntity updateMetric(Long id, MetricEntity patch) {
        MetricEntity existing = metricRepository.findById(id)
                .orElseThrow(() -> new BusinessException("metric not found: " + id));
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getMetricType() != null) existing.setMetricType(patch.getMetricType());
        if (patch.getDefinitionSql() != null) existing.setDefinitionSql(patch.getDefinitionSql());
        if (patch.getOwner() != null) existing.setOwner(patch.getOwner());
        return metricRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Optional<MetricEntity> getMetricByCode(String code) {
        return metricRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public Page<MetricEntity> pageMetrics(Pageable pageable) {
        return metricRepository.findAll(pageable);
    }

    @Transactional
    public void deleteMetric(Long id) {
        metricRepository.deleteById(id);
    }
}