package org.temporedata.metadata.service;

import java.util.List;

/**
 * [ENT-P0] Result of a schema crawl + drift evaluation for one dataset.
 */
public final class CrawlResult {

    private final Long datasetId;
    private final Long versionId;
    private final Integer version;
    private final String driftType;
    private final boolean changed;
    private final List<String> added;
    private final List<String> removed;
    private final List<String> typeChanged;

    public CrawlResult(Long datasetId, Long versionId, Integer version, String driftType, boolean changed,
                       List<String> added, List<String> removed, List<String> typeChanged) {
        this.datasetId = datasetId;
        this.versionId = versionId;
        this.version = version;
        this.driftType = driftType;
        this.changed = changed;
        this.added = added;
        this.removed = removed;
        this.typeChanged = typeChanged;
    }

    public Long getDatasetId() { return datasetId; }
    public Long getVersionId() { return versionId; }
    public Integer getVersion() { return version; }
    public String getDriftType() { return driftType; }
    public boolean isChanged() { return changed; }
    public List<String> getAdded() { return added; }
    public List<String> getRemoved() { return removed; }
    public List<String> getTypeChanged() { return typeChanged; }
}