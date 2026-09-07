package org.temporedata.modules.gov.meta.service;

import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.gov.meta.spi.MetaPostProcessor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * MetaPipeline (P2, design §6): orchestrates all registered {@link MetaPostProcessor}s
 * (ordered) after a metadata collection, auto-linking business capabilities.
 */
@Slf4j
@Service
public class MetaPipeline {

    private final List<MetaPostProcessor> processors;

    public MetaPipeline(List<MetaPostProcessor> processors) {
        this.processors = processors.stream()
                .sorted(Comparator.comparingInt(p -> {
                    org.springframework.core.annotation.Order o =
                            org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation(
                                    p.getClass(), org.springframework.core.annotation.Order.class);
                    return o == null ? 0 : o.value();
                }))
                .collect(java.util.stream.Collectors.toList());
    }

    public void process(String datasourceId) {
        for (MetaPostProcessor p : processors) {
            try {
                p.process(datasourceId);
            } catch (Exception e) {
                log.warn("MetaPipeline: processor {} failed for {}: {}",
                        p.getClass().getSimpleName(), datasourceId, e.getMessage());
            }
        }
    }
}