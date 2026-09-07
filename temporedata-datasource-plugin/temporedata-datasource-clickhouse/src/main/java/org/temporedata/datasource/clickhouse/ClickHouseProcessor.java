package org.temporedata.datasource.clickhouse;

import org.temporedata.api.datasource.AbstractDatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.annotation.DatasourcePlugin;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.api.datasource.param.GenericDatasourceParamDTO;
import org.springframework.stereotype.Component;

/**
 * ClickHouse dialect processor.
 */
@Component
@DatasourcePlugin(value = "CLICKHOUSE", name = "ClickHouse")
public class ClickHouseProcessor extends AbstractDatasourceProcessor {

    private static final String URL = "jdbc:clickhouse://{host}:{port}/{db}";

    @Override
    public DatasourceType type() {
        return DatasourceType.CLICKHOUSE;
    }

    @Override
    public String dialectName() {
        return "ClickHouse";
    }

    @Override
    public int defaultPort() {
        return 8123;
    }

    @Override
    public Class<? extends DatasourceParamDTO> paramClass() {
        return GenericDatasourceParamDTO.class;
    }

    @Override
    protected String driverClass() {
        return "com.clickhouse.jdbc.ClickHouseDriver";
    }

    @Override
    protected String urlTemplate() {
        return URL;
    }
}