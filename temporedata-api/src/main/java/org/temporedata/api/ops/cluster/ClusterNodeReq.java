package org.temporedata.api.ops.cluster;

import lombok.Data;

/**
 * Create / update cluster node request.
 */
@Data
public class ClusterNodeReq {

    private String host;

    private Integer port;

    private String username;

    private String password;
}