package org.temporedata.api.asset.datacenter;

import lombok.Data;

/**
 * Data center overview statistics response DTO.
 */
@Data
public class DataCenterOverviewRes {

    private long datasourceCount;

    private long tableCount;

    private long workflowCount;

    private long apiCount;

    private long syncTaskCount;

    private long qualityRuleCount;
}