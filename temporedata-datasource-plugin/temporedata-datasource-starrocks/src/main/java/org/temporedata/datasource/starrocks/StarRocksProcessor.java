package org.temporedata.datasource.starrocks;

import org.temporedata.api.datasource.AbstractDatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.annotation.DatasourcePlugin;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.api.datasource.param.GenericDatasourceParamDTO;
import org.springframework.stereotype.Component;

/**
 * StarRocks dialect processor. Connects to a StarRocks FE through the MySQL
 * protocol, hence the MySQL JDBC driver.
 */
@Component
@DatasourcePlugin(value = "STARROCKS", name = "StarRocks")
public class StarRocksProcessor extends AbstractDatasourceProcessor {

    private static final String URL = "jdbc:mysql://{host}:{port}/{db}";

    @Override
    public DatasourceType type() {
        return DatasourceType.STARROCKS;
    }

    @Override
    public String dialectName() {
        return "StarRocks";
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