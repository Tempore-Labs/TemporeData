package org.temporedata.api.datasource.param;

/**
 * Generic RDBMS parameters (MySQL / PostgreSQL) with no extra fields beyond
 * the base ones.
 */
public class GenericDatasourceParamDTO extends DatasourceParamDTO {

    @Override
    public void validate() {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host is required");
        }
    }
}