package org.temporedata.api.ops.engine;

import lombok.Data;

/**
 * Spark job response.
 */
@Data
public class SparkJobRes {

    private String id;

    private String name;

    private String type;

    private String status; // READY, RUNNING, SUCCESS, FAILED, KILLED

    private String master;

    private String deployMode;

    private String appId; // YARN application id or Spark app id

    private String lastRunTime;

    private String duration;

    private String errorMsg;

    private String createTime;
}