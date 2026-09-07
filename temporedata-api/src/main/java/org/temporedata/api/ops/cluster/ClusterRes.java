package org.temporedata.api.ops.cluster;

import lombok.Data;

/**
 * Cluster list / detail response.
 */
@Data
public class ClusterRes {

    private String id;

    private String name;

    private String type;

    private String masterUrl;

    private String status;

    private Integer nodeCount;

    private String createTime;
}