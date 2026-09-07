package org.temporedata.api.asset.indicator;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Indicator lineage: tables referenced by the indicator SQL and upstream
 * indicators that read the same tables.
 */
@Data
public class IndicatorLineageRes {

    private List<String> tables = new ArrayList<>();

    private List<UpstreamIndicator> upstream = new ArrayList<>();

    @Data
    public static class UpstreamIndicator {
        private String id;
        private String name;
        private String code;
        private String type;
        private String status;
    }
}