package org.temporedata.api.datasource.param;

import lombok.Data;

/**
 * Base data source parameters shared by all dialects.
 *
 * <p>Abstract so that a dialect may declare its own sub-class to add
 * dialect-specific fields (e.g. Oracle connection type, ClickHouse http
 * schema). The manager maps a persisted entity to a concrete instance through
 * {@code processor.paramClass()}.</p>
 */
@Data
public abstract class DatasourceParamDTO {

    protected String host;

    protected Integer port;

    protected String database;

    protected String username;

    /** Plaintext password used only transiently to build/test a connection; never persisted. */
    protected String password;

    /** Extra JDBC params appended to the URL. */
    protected String params;

    /** Dialect-specific validation rule. Throw IllegalArgumentException on failure. */
    public abstract void validate();
}