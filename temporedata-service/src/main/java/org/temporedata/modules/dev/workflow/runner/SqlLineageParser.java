package org.temporedata.modules.dev.workflow.runner;

import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.sql.Dialect;
import org.temporedata.api.datasource.sql.SqlParseResult;
import org.temporedata.api.dev.workflow.SqlParseRes;
import org.springframework.stereotype.Component;

/**
 * Facade for SQL lineage parsing. Keeps the legacy {@link #parse(String)} contract
 * that downstream services rely on, while delegating actual parsing to the dialect-aware
 * {@link SqlParser} resolved from {@link SqlParserRegistry}.
 */
@Component
public class SqlLineageParser {

    private final SqlParserRegistry registry;

    public SqlLineageParser(SqlParserRegistry registry) {
        this.registry = registry;
    }

    /** Parse without a known datasource; defaults to the OTHER dialect strategy. */
    public SqlParseRes parse(String sql) {
        return map(registry.resolve(Dialect.OTHER).parse(sql, Dialect.OTHER));
    }

    /** Parse for a specific datasource, routing to the best-fit dialect parser. */
    public SqlParseRes parse(String sql, DatasourceType type) {
        Dialect dialect = Dialect.from(type);
        return map(registry.resolve(dialect).parse(sql, dialect));
    }

    private SqlParseRes map(SqlParseResult r) {
        SqlParseRes res = new SqlParseRes();
        res.setSuccess(r.isSuccess());
        res.setMessage(r.getMessage());
        res.setSqlType(r.getSqlType());
        res.setTargetTable(r.getTargetTable());
        res.setSources(r.getSources());
        res.setTables(r.getTables());
        res.setColumns(r.getColumns());
        res.setColumnLineage(r.getColumnLineage());
        return res;
    }
}