package org.temporedata.api.gov.lineage;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CycleAnalysisRes {

    private boolean hasCycle;
    private List<List<String>> cycles = new ArrayList<>();
    private String summary;
}