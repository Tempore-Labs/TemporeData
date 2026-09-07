package org.temporedata.datasource.doris;

import org.temporedata.api.datasource.AbstractDatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.annotation.DatasourcePlugin;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.api.datasource.param.GenericDatasourceParamDTO;
import org.springframework.stereotype.Component;

/**
 * Apache Doris dialect processor. Connects to a Doris FE through the MySQL
 * protocol, hence the MySQL JDBC driver.
 */
@Component
@DatasourcePlugin(value = "DORIS", name = "Doris")
public class DorisProcessor extends AbstractDatasourceProcessor {

    private static final String URL = "jdbc:mysql://{host}:{port}/{db}";

    @Override
    public DatasourceType type() {
        return DatasourceType.DORIS;
    }

    @Override
    public String dialectName() {
        return "Doris";
    }

    @Override
    public int defaultPort() {
        return 9030;
    }

    @Override
    public Class<? extends DatasourceParamDTO> paramClass() {
        return GenericDatasourceParamDTO.class;
    }

    @Override
    protected String driverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    protected String urlTemplate() {
        return URL;
    }
}