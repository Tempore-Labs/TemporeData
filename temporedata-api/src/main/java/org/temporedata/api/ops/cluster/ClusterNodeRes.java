package org.temporedata.api.ops.cluster;

import lombok.Data;

/**
 * Cluster node response.
 */
@Data
public class ClusterNodeRes {

    private String id;

    private String host;

    private Integer port;

    private String agentStatus;

    private Double cpuUsage;

    private Double memoryUsage;

    private Double diskUsage;
}