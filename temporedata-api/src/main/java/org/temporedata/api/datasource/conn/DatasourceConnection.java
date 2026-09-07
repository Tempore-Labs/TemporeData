package org.temporedata.api.datasource.conn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resolved connection configuration produced by a {@code DatasourceProcessor}:
 * the JDBC driver class, the fully built URL and the credentials. The manager
 * only reads this value object - it never builds dialect URLs itself.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceConnection {

    /** JDBC driver class name, e.g. com.mysql.cj.jdbc.Driver. */
    private String driverClass;

    /** Fully built JDBC URL. */
    private String jdbcUrl;

    private String username;

    /** Plaintext password (transient, only used to establish the connection). */
    private String password;
}