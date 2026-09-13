package org.temporedata.metadata.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.metadata.entity.MetadataVersionEntity;
import org.temporedata.metadata.repository.MetadataVersionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * [ENT-P0] Metadata snapshot &amp; Schema Drift versioning.
 *
 * <p>Records monotonic schema versions per dataset and exposes the latest snapshot so the
 * Schema Crawler / impact-analysis pipeline can compare and emit drift metadata.
 */
@Service
public class MetadataVersionService {

    private final MetadataVersionRepository repository;

    public MetadataVersionService(MetadataVersionRepository repository) {
        this.repository = repository;
    }

    /**
     * Persist a new schema snapshot for the dataset, bumping the per-dataset version.
     *
     * @param datasetId   target dataset
     * @param schemaJson  full schema snapshot (columns/types/DDL)
     * @param driftType   optional drift classification vs the previous snapshot
     * @return the recorded snapshot entity
     */
    @Transactional
    public MetadataVersionEntity snapshot(Long datasetId, String schemaJson, String driftType) {
        int nextVersion = 1;
        Optional<MetadataVersionEntity> latest = repository.findTopByDatasetIdOrderByVersionDesc(datasetId);
        if (latest.isPresent()) {
            nextVersion = latest.get().getVersion() + 1;
        }
        MetadataVersionEntity snapshot = MetadataVersionEntity.builder()
                .datasetId(datasetId)
                .version(nextVersion)
                .schemaJson(schemaJson)
                .driftType(driftType)
                .createdAt(LocalDateTime.now())
                .build();
        return repository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public Optional<MetadataVersionEntity> latest(Long datasetId) {
        return repository.findTopByDatasetIdOrderByVersionDesc(datasetId);
    }

    @Transactional(readOnly = true)
    public List<MetadataVersionEntity> list(Long datasetId) {
        return repository.findByDatasetIdOrderByVersionDesc(datasetId);
    }

    @Transactional(readOnly = true)
    public boolean hasVersion(Long datasetId, Integer version) {
        return repository.existsByDatasetIdAndVersion(datasetId, version);
    }
}