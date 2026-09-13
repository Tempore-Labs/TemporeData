package org.temporedata.metadata.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.metadata.drift.ColumnSig;
import org.temporedata.metadata.drift.SchemaDriftDetector;
import org.temporedata.metadata.entity.MetadataVersionEntity;

import java.util.List;
import java.util.Optional;

/**
 * [ENT-P0] Schema Crawler service.
 *
 * <p>Given an observed schema (column signatures) for a dataset, snapshots it into a new
 * temporedata_metadata_version and reports the Schema Drift relative to the previous snapshot
 * (ADD_COLUMN / DROP_COLUMN / TYPE_CHANGE / COMBINED). The observed signatures come from a
 * {@code SchemaReader} (etl SPI) or any crawler; this service owns versioning + drift detection.
 */
@Service
public class SchemaCrawlerService {

    private final MetadataVersionService metadataVersionService;

    public SchemaCrawlerService(MetadataVersionService metadataVersionService) {
        this.metadataVersionService = metadataVersionService;
    }

    @Transactional
    public CrawlResult crawl(Long datasetId, List<ColumnSig> observed) {
        List<ColumnSig> before = Optional.ofNullable(metadataVersionService.latest(datasetId).orElse(null))
                .map(MetadataVersionEntity::getSchemaJson)
                .map(SchemaDriftDetector::parseSchemaJson)
                .orElse(List.of());

        List<ColumnSig> observedSafe = observed == null ? List.of() : observed;

        SchemaDriftDetector.DriftResult dr = SchemaDriftDetector.detect(before, observedSafe);
        MetadataVersionEntity saved =
                metadataVersionService.snapshot(datasetId, SchemaDriftDetector.toSchemaJson(observedSafe), dr.getDriftType());

        return new CrawlResult(datasetId, saved.getId(), saved.getVersion(), dr.getDriftType(),
                dr.isChanged(), dr.getAdded(), dr.getRemoved(), dr.getTypeChanged());
    }
}