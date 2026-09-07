package org.temporedata.api.dev.query;

import lombok.Data;

/**
 * Saved query log entry.
 */
@Data
public class QueryLogRes {

    private String id;

    private String datasourceName;

    private String sql;

    private String status;

    private long durationMs;

    private String createTime;

    private String errorMsg;
}