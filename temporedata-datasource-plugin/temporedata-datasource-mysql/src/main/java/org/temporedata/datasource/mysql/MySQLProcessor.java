package org.temporedata.datasource.mysql;

import org.temporedata.api.datasource.AbstractDatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.annotation.DatasourcePlugin;
import org.temporedata.api.datasource.param.DatasourceParamDTO;
import org.temporedata.api.datasource.param.GenericDatasourceParamDTO;
import org.springframework.stereotype.Component;

/**
 * MySQL dialect processor.
 */
@Component
@DatasourcePlugin(value = "MYSQL", name = "MySQL")
public class MySQLProcessor extends AbstractDatasourceProcessor {

    private static final String URL = "jdbc:mysql://{host}:{port}/{db}"
            + "?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false";

    @Override
    public DatasourceType type() {
        return DatasourceType.MYSQL;
    }

    @Override
    public String dialectName() {
        return "MySQL";
    }

    @Override
    public int defaultPort() {
        return 3306;
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