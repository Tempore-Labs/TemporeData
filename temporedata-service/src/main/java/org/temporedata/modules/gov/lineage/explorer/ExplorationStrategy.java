package org.temporedata.modules.gov.lineage.explorer;

/**
 * Exploration strategy selected by graph size, mirroring the design doc's
 * small/medium/large/streaming split. Larger strategies avoid materializing the
 * whole graph and rely on layered pagination.
 */
public enum ExplorationStrategy {
    /** Small graph: full in-memory assembly is safe. */
    SMALL,
    /** Medium graph: assemble but cap with layered pagination. */
    MEDIUM,
    /** Large graph: only layer pages are produced, skip whole-graph materialization. */
    LARGE,
    /** Extremely large graph: force streaming/layered output only. */
    STREAMING
}