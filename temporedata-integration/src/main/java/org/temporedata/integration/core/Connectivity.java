package org.temporedata.integration.core;

/**
 * [ENT-P0] Connectivity capability marker.
 *
 * <p>Namespace root for the migrated low-level connection layer (datasource connections,
 * secret management, file connectivity) that used to live under
 * {@code temporedata-service .../modules/integration}. Kept as an isolated package so the
 * new ETL/CDC engine (org.temporedata.integration.etl) never mixes with connectivity concerns.
 *
 * <p>Boundary contract: this package owns ATOMIC connection handling only; orchestration,
 * cost analysis and AI reasoning belong to temporedata-service.
 */
public final class Connectivity {

    private Connectivity() {
        // utility/marker only
    }

    /** Marker for a resolvable, opened or pooled physical connection. */
    public interface Connection {
        String id();
    }
}