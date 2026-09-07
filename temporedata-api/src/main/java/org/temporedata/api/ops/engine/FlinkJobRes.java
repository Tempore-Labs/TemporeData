package org.temporedata.api.ops.engine;

import lombok.Data;

/**
 * Flink job response.
 */
@Data
public class FlinkJobRes {

    private String id;

    private String name;

    private String type;

    private String status; // READY, RUNNING, FINISHED, FAILED, CANCELED

    private String jobId; // Flink job id

    private String parallelism;

    private String lastRunTime;

    private String duration;

    private String savepointPath;

    private String errorMsg;

    private String createTime;
}