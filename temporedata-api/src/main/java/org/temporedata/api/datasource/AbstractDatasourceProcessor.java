package org.temporedata.api.datasource;

import org.temporedata.api.datasource.conn.DatasourceConnection;
import org.temporedata.api.datasource.param.DatasourceParamDTO;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Common implementation of {@link DatasourceProcessor} for JDBC dialects.
 *
 * <p>A concrete dialect only needs to supply {@code type()}, {@code dialectName()},
 * {@code defaultPort()}, {@code driverClass()} and {@code urlTemplate()} (with
 * {host}/{port}/{db} placeholders). URL building, driver loading, connection
 * and connectivity checks are provided here.</p>
 */
public abstract class AbstractDatasourceProcessor implements DatasourceProcessor {

    /** JDBC driver class name. */
    protected abstract String driverClass();

    /** JDBC URL template containing {host}, {port} and {db} placeholders. */
    protected abstract String urlTemplate();

    @Override
    public DatasourceConnection buildConnection(DatasourceParamDTO param) {
        int port = param.getPort() != null ? param.getPort() : defaultPort();
        String url = urlTemplate()
                .replace("{host}", param.getHost())
                .replace("{port}", String.valueOf(port))
                .replace("{db}", param.getDatabase() == null ? "" : param.getDatabase());
        if (param.getParams() != null && !param.getParams().isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + param.getParams();
        }
        return new DatasourceConnection(driverClass(), url, param.getUsername(), param.getPassword());
    }

    @Override
    public Connection getConnection(DatasourceParamDTO param) throws Exception {
        DatasourceConnection conn = buildConnection(param);
        Class.forName(conn.getDriverClass());
        return DriverManager.getConnection(conn.getJdbcUrl(), conn.getUsername(), conn.getPassword());
    }

    @Override
    public boolean check(DatasourceParamDTO param) throws Exception {
        try (Connection conn = getConnection(param)) {
            return conn.isValid(3);
        }
    }
}