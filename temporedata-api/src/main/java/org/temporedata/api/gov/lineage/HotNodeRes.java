package org.temporedata.api.gov.lineage;

import lombok.Data;

@Data
public class HotNodeRes {

    private String id;
    private String name;
    private String nodeType;
    private int inDegree;
    private int outDegree;
    private int total;
}