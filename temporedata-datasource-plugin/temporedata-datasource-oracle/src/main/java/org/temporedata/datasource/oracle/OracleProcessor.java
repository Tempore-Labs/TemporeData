package org.temporedata.datasource.oracle;

import org.temporedata.api.datasource.AbstractDatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.annotation.DatasourcePlugin;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.api.datasource.param.GenericDatasourceParamDTO;
import org.springframework.stereotype.Component;

/**
 * Oracle dialect processor. Uses the thin JDBC driver with service-name syntax.
 */
@Component
@DatasourcePlugin(value = "ORACLE", name = "Oracle")
public class OracleProcessor extends AbstractDatasourceProcessor {

    private static final String URL = "jdbc:oracle:thin:@//{host}:{port}/{db}";

    @Override
    public DatasourceType type() {
        return DatasourceType.ORACLE;
    }

    @Override
    public String dialectName() {
        return "Oracle";
    }

    @Override
    public int defaultPort() {
        return 1521;
    }

    @Override
    public Class<? extends DatasourceParamDTO> paramClass() {
        return GenericDatasourceParamDTO.class;
    }

    @Override
    protected String driverClass() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    protected String urlTemplate() {
        return URL;
    }
}