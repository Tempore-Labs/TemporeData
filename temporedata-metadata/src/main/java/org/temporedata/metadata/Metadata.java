package org.temporedata.metadata;

/**
 * [ENT-P0] Metadata center capability namespace root.
 *
 * <p>Hosts the metadata/data-modeling tiers that quality / asset / contract build upon:
 * Schema Crawler snapshotting, MetadataVersion + Schema Drift detection with impact analysis,
 * DataDomain/SubjectArea and the ODS-DWD-DWS-ADS mapping for logical &lt;-&gt; physical DDL.
 *
 * <p>Boundary contract: metadata is a DEPENDENCY of quality / asset / contract. It consumes
 * connectivity from temporedata-integration but is never depended on by development the other way.
 */
public final class Metadata {

    private Metadata() {
        // namespace marker only
    }
}