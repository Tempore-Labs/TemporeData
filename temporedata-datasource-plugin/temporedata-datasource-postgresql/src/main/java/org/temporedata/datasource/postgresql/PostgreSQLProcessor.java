package org.temporedata.datasource.postgresql;

import org.temporedata.api.datasource.AbstractDatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.annotation.DatasourcePlugin;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.api.datasource.param.GenericDatasourceParamDTO;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL dialect processor.
 */
@Component
@DatasourcePlugin(value = "POSTGRESQL", name = "PostgreSQL")
public class PostgreSQLProcessor extends AbstractDatasourceProcessor {

    private static final String URL = "jdbc:postgresql://{host}:{port}/{db}";

    @Override
    public DatasourceType type() {
        return DatasourceType.POSTGRESQL;
    }

    @Override
    public String dialectName() {
        return "PostgreSQL";
    }

    @Override
    public int defaultPort() {
        return 5432;
    }

    @Override
    public Class<? extends DatasourceParamDTO> paramClass() {
        return GenericDatasourceParamDTO.class;
    }

    @Override
    protected String driverClass() {
        return "org.postgresql.Driver";
    }

    @Override
    protected String urlTemplate() {
        return URL;
    }
}