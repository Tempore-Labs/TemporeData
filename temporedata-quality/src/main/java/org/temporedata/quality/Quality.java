package org.temporedata.quality;

/**
 * [ENT-P0] Data quality capability namespace root.
 *
 * <p>Hosts the multi-dimension validation engine (Completeness / Uniqueness / Validity /
 * Accuracy / Timeliness) and the Quality Gate that couples with temporedata-service WorkflowEngine
 * to auto-block downstream publishing when a score drops below threshold.
 *
 * <p>Boundary contract: quality evaluates DATASET via temporedata-metadata; it never performs I/O.
 */
public final class Quality {

    private Quality() {
        // namespace marker only
    }
}