package org.temporedata.api.ops.real;

import lombok.Data;

/**
 * Realtime job list / detail response.
 */
@Data
public class RealJobRes {

    private String id;

    private String name;

    private String type;

    private String status;

    private String clusterId;

    private String createTime;
}