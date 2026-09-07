package org.temporedata.datasource.oceanbase;

import org.temporedata.api.datasource.AbstractDatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.annotation.DatasourcePlugin;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.api.datasource.param.GenericDatasourceParamDTO;
import org.springframework.stereotype.Component;

/**
 * OceanBase dialect processor. Speaks the MySQL protocol; the JDBC driver is
 * {@code com.oceanbase.jdbc.Driver} via the {@code jdbc:oceanbase://} scheme.
 */
@Component
@DatasourcePlugin(value = "OCEANBASE", name = "OceanBase")
public class OceanBaseProcessor extends AbstractDatasourceProcessor {

    private static final String URL = "jdbc:oceanbase://{host}:{port}/{db}"
            + "?useUnicode=true&characterEncoding=utf-8&useSSL=false";

    @Override
    public DatasourceType type() {
        return DatasourceType.OCEANBASE;
    }

    @Override
    public String dialectName() {
        return "OceanBase";
    }

    @Override
    public int defaultPort() {
        return 2881;
    }

    @Override
    public Class<? extends DatasourceParamDTO> paramClass() {
        return GenericDatasourceParamDTO.class;
    }

    @Override
    protected String driverClass() {
        return "com.oceanbase.jdbc.Driver";
    }

    @Override
    protected String urlTemplate() {
        return URL;
    }
}