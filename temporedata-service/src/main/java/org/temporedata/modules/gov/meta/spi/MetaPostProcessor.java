package org.temporedata.modules.gov.meta.spi;

/**
 * MetaPostProcessor (P2, design §6): a pluggable post-processing step executed by the
 * MetaPipeline after a metadata collection. Implementations derive business capabilities
 * such as sensitivity/level classification, default desensitization rules, lineage links.
 */
public interface MetaPostProcessor {

    /** Run over the freshly collected metadata for the given datasource. */
    void process(String datasourceId);
}