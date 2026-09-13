package org.temporedata.asset;

/**
 * [ENT-P0] Data asset capability namespace root.
 *
 * <p>Hosts the Asset Catalog (tables / APIs / metrics / reports as unified assets), business
 * terms and tags with Data Owner, and the metric platform (atomic / derived / compound metrics
 * with pre-compute scheduling and SQL/REST publication).
 *
 * <p>Boundary contract: asset is built directly on temporedata-metadata and depends on no
 * sibling capability module except metadata.
 */
public final class Asset {

    private Asset() {
        // namespace marker only
    }
}