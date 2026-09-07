package org.temporedata.modules.gov.lineage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Data lineage module configuration (temporedata.lineage.*), bound via Spring's
 * relaxed mapping (upstream-depth -> upstreamDepth). Mirrors the FileProperties
 * binding convention (Component + ConfigurationProperties).
 * <p>Controls precise assembly depth, layered exploration page size, the in-memory
 * graph cache cap and the small/medium/large/streaming strategy thresholds used to
 * avoid whole-graph materialization/OOM on large lineage graphs.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "temporedata.lineage")
public class LineageProperties {

    /** Max upstream depth for precise assembly (1-5). */
    private int upstreamDepth = 3;

    /** Max downstream depth for precise assembly (1-5). */
    private int downstreamDepth = 3;

    /** Number of nodes per exploration layer page. */
    private int layerSize = 200;

    /** Cap of the dedicated Caffeine graph cache. */
    private int graphCacheMax = 100000;

    /** Whether time-window hard pruning is enabled. */
    private boolean timeWindowEnabled = false;

    /** Small/medium/large strategy thresholds (by estimated node count). */
    private StrategyThreshold strategyThreshold = new StrategyThreshold();

    @Data
    public static class StrategyThreshold {
        /** Estimated node count below which SMALL strategy is used. */
        private int small = 1000;

        /** Estimated node count below which MEDIUM strategy is used (above = LARGE/STREAMING). */
        private int medium = 10000;
    }
}