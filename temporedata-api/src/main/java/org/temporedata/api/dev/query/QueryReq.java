package org.temporedata.api.dev.query;

import lombok.Data;

/**
 * Ad-hoc SQL query request.
 */
@Data
public class QueryReq {

    private String datasourceId;

    private String sql;
}