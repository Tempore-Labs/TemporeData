package org.temporedata.api.ops.real;

import lombok.Data;

/**
 * Create / update realtime job request.
 */
@Data
public class RealJobReq {

    private String name;

    private String type; // FLINK_SQL, FLINK_JAR

    private String script;

    private String clusterId;
}