package org.temporedata.development;

/**
 * [ENT-P0] Data development capability namespace root.
 *
 * <p>Hosts the SQL-IDE server-side tiers and heterogeneous job executors:
 * AST parse / format / pre-explain (Calcite &amp; JSqlParser), pre-submit field-level lineage
 * candidates, and the {@code JobExecutor} SPI (Spark Submit / Flink / Python / Shell / UDF).
 *
 * <p>Boundary contract: development orchestrates execution via temporedata-integration SPI for
 * connectivity and delegates lineage-base computation to temporedata-metadata; it never owns I/O.
 */
public final class Development {

    private Development() {
        // namespace marker only
    }
}