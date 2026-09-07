package org.temporedata.api.ops.cluster;

import lombok.Data;

/**
 * Create / update cluster request.
 */
@Data
public class ClusterReq {

    private String name;

    private String type; // SPARK_STANDALONE, YARN, K8S

    private String masterUrl;
}