package org.temporedata.datasource.hive;

import org.temporedata.api.datasource.AbstractDatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.annotation.DatasourcePlugin;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.api.datasource.param.GenericDatasourceParamDTO;
import org.springframework.stereotype.Component;

/**
 * Hive dialect processor (HiveServer2 / Beeline).
 */
@Component
@DatasourcePlugin(value = "HIVE", name = "Hive")
public class HiveProcessor extends AbstractDatasourceProcessor {

    private static final String URL = "jdbc:hive2://{host}:{port}/{db}";

    @Override
    public DatasourceType type() {
        return DatasourceType.HIVE;
    }

    @Override
    public String dialectName() {
        return "Hive";
    }

    @Override
    public int defaultPort() {
        return 10000;
    }

    @Override
    public Class<? extends DatasourceParamDTO> paramClass() {
        return GenericDatasourceParamDTO.class;
    }

    @Override
    protected String driverClass() {
        return "org.apache.hive.jdbc.HiveDriver";
    }

    @Override
    protected String urlTemplate() {
        return URL;
    }
}