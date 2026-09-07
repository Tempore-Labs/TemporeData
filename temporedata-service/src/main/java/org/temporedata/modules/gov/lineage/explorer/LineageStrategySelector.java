package org.temporedata.modules.gov.lineage.explorer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.temporedata.modules.gov.lineage.config.LineageProperties;

/**
 * Picks an {@link ExplorationStrategy} from the estimated linearized node count,
 * using the {@code strategy-threshold} config (small / medium). Above {@code medium}
 * the graph is treated as large/streaming: only layered pages are produced to avoid
 * whole-graph materialization (and OOM) on big lineage graphs.
 */
@Component
@RequiredArgsConstructor
public class LineageStrategySelector {

    private final LineageProperties properties;

    public ExplorationStrategy select(long estNodeCount) {
        LineageProperties.StrategyThreshold threshold = properties.getStrategyThreshold();
        if (estNodeCount <= threshold.getSmall()) {
            return ExplorationStrategy.SMALL;
        }
        if (estNodeCount <= threshold.getMedium()) {
            return ExplorationStrategy.MEDIUM;
        }
        // Above medium: prefer layered output; beyond graph-cache cap force streaming.
        if (estNodeCount > properties.getGraphCacheMax()) {
            return ExplorationStrategy.STREAMING;
        }
        return ExplorationStrategy.LARGE;
    }

    /** Whether the strategy should skip whole-graph materialization in the cache. */
    public boolean isPagedOnly(ExplorationStrategy strategy) {
        return strategy == ExplorationStrategy.LARGE || strategy == ExplorationStrategy.STREAMING;
    }
}