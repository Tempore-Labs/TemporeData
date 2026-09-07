package org.temporedata.api.integration.datasource;

import lombok.Data;

/**
 * Datasource list / detail response.
 */
@Data
public class DatasourceRes {

    private String id;

    private String name;

    private String type;

    private String host;

    private Integer port;

    private String database;

    private String username;

    private String params;

    private String createTime;
}