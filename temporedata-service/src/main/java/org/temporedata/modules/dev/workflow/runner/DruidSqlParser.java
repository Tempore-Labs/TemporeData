package org.temporedata.modules.dev.workflow.runner;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import org.temporedata.api.datasource.sql.ColumnLineage;
import org.temporedata.api.datasource.sql.Dialect;
import org.temporedata.api.datasource.sql.SqlParseResult;
import org.temporedata.api.datasource.sql.SqlParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single {@link SqlParser} implementation backed by Druid's SQL AST, covering every
 * dialect registered in {@link SqlDialectRegistry} (MySQL/PG/Oracle/ClickHouse and
 * the MySQL-protocol Doris/StarRocks). Degrades gracefully to success=false on
 * statements it cannot parse; never throws.
 */
@Component
public class DruidSqlParser implements SqlParser {

    private final SqlDialectRegistry registry;

    public DruidSqlParser(SqlDialectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean supports(Dialect dialect) {
        // a single Druid-based parser covers every registered dialect (all 6 built-in)
        return registry.exists(dialect);
    }

    @Override
    public SqlParseResult parse(String sql, Dialect dialect) {
        SqlParseResult res = new SqlParseResult();
        Dialect effective = dialect == null ? Dialect.OTHER : dialect;
        res.setDialect(effective);
        if (sql == null || sql.isBlank()) {
            res.setSuccess(false);
            res.setMessage("SQL is empty");
            return res;
        }
        try {
            DbType dbType = registry.dbType(effective);
            SQLStatement stmt = SQLParserUtils.createSQLStatementParser(sql, dbType).parseStatement();
            if (stmt == null) {
                // e.g. a comment-only / empty payload: nothing to infer - degrade gracefully.
                res.setSuccess(false);
                res.setMessage("未解析出可识别的 SQL 语句");
                return res;
            }
            LineageSchemaVisitor visitor = new LineageSchemaVisitor();
            stmt.accept(visitor);
            String target = resolveTarget(stmt);
            Set<String> all = new LinkedHashSet<>(visitor.tables);
            Set<String> sources = new LinkedHashSet<>(all);
            if (target != null) {
                sources.remove(target);
            }
            res.setSqlType(typeName(stmt));
            res.setTargetTable(target);
            res.setSources(new ArrayList<>(sources));
            res.setTables(new ArrayList<>(all));
            res.setColumns(new ArrayList<>(visitor.columnNames()));
            List<ColumnLineage> cl = new ArrayList<>();
            collectColumnLineage(stmt, cl);
            res.setColumnLineage(cl);
            // success = the statement parsed. A target/source-less SELECT (e.g. "SELECT 1")
            // is still a valid parse; the empty table/column lists reflect that there is no
            // lineage, rather than being a failure.
            res.setSuccess(true);
            res.setMessage("OK");
        } catch (IndexOutOfBoundsException e) {
            // Druid internal indexing issue on unusual input - do not leak an internal error.
            res.setSuccess(false);
            res.setMessage("无法解析该 SQL，可能缺少可执行的语句");
        } catch (Exception e) {
            res.setSuccess(false);
            res.setMessage("Parse error: " + e.getMessage());
        }
        return res;
    }

    private String resolveTarget(SQLStatement stmt) {
        if (stmt instanceof SQLInsertStatement) {
            return sqlName(((SQLInsertStatement) stmt).getTableName());
        }
        if (stmt instanceof SQLUpdateStatement) {
            return sqlName(((SQLUpdateStatement) stmt).getTableName());
        }
        if (stmt instanceof SQLDeleteStatement) {
            return sqlName(((SQLDeleteStatement) stmt).getTableName());
        }
        if (stmt instanceof SQLCreateTableStatement) {
            return ((SQLCreateTableStatement) stmt).getName().toString();
        }
        return null;
    }

    private String sqlName(SQLName name) {
        return name == null ? null : name.getSimpleName();
    }

    private String typeName(SQLStatement stmt) {
        if (stmt instanceof SQLInsertStatement) return "INSERT";
        if (stmt instanceof SQLUpdateStatement) return "UPDATE";
        if (stmt instanceof SQLDeleteStatement) return "DELETE";
        if (stmt instanceof SQLCreateTableStatement) return "CREATE_TABLE";
        if (stmt instanceof SQLSelectStatement) return "SELECT";
        return stmt.getClass().getSimpleName();
    }

    /** Unified lineage traversal via Druid {@link SchemaStatVisitor} (§4.3): tables + column refs. */
    private static final class LineageSchemaVisitor extends SchemaStatVisitor {
        final Set<String> tables = new LinkedHashSet<>();

        @Override
        public boolean visit(SQLExprTableSource x) {
            String name = x.getTableName();
            if (name != null && !name.isBlank()) {
                tables.add(name);
            }
            return super.visit(x);
        }

        Set<String> columnNames() {
            Set<String> out = new LinkedHashSet<>();
            for (TableStat.Column c : columns.values()) {
                if (c.getName() != null && !c.getName().isBlank()) {
                    out.add(c.getName());
                }
            }
            return out;
        }
    }

    /**
     * Best-effort column mapping for {@code INSERT ... SELECT} (P3 列级血缘).
     * Resolves the SELECT's source columns positionally against the INSERT target
     * columns. Supports single-table, INNER/LEFT JOIN (multi-source) and qualified
     * references ({@code alias.col}) via an alias→table map. Complex expressions
     * (functions/subqueries/literals) are skipped rather than mis-attributed.
     */
    private void collectColumnLineage(SQLStatement stmt, List<ColumnLineage> out) {
        if (!(stmt instanceof SQLInsertStatement)) {
            return;
        }
        SQLInsertStatement ins = (SQLInsertStatement) stmt;
        SQLSelect sel = ins.getQuery();
        if (sel == null) {
            return;
        }
        SQLSelectQueryBlock block = sel.getFirstQueryBlock();
        if (block == null) {
            return;
        }
        java.util.List<? extends com.alibaba.druid.sql.ast.SQLExpr> cols = ins.getColumns();
        List<String> targetCols = columnNames(cols);
        java.util.List<com.alibaba.druid.sql.ast.statement.SQLSelectItem> items = block.getSelectList();
        if (items == null) {
            return;
        }

        Map<String, String> aliasToTable = new LinkedHashMap<>();
        String mainTable = firstTableName(block.getFrom(), aliasToTable);
        String targetTable = sqlName(ins.getTableName());

        for (int i = 0; i < items.size(); i++) {
            com.alibaba.druid.sql.ast.SQLExpr e = items.get(i).getExpr();
            String sourceCol = null;
            String sourceTable = null;
            if (e instanceof SQLIdentifierExpr) {
                sourceCol = ((SQLIdentifierExpr) e).getName();
                sourceTable = mainTable;
            } else if (e instanceof SQLPropertyExpr) {
                SQLPropertyExpr p = (SQLPropertyExpr) e;
                Object owner = p.getOwner();
                sourceCol = p.getName();
                if (owner instanceof SQLIdentifierExpr) {
                    String alias = ((SQLIdentifierExpr) owner).getName();
                    sourceTable = aliasToTable.getOrDefault(alias, alias);
                } else {
                    sourceTable = mainTable;
                }
            } else {
                // function / subquery / literal: best-effort, do not mis-attribute
                continue;
            }
            if (sourceCol == null) {
                continue;
            }
            // target column: explicit column list position, else select item alias, else ordinal placeholder
            String tc = tcAt(targetCols, items.get(i), i);
            ColumnLineage cl = new ColumnLineage();
            cl.setSourceTable(sourceTable);
            cl.setSourceColumn(sourceCol);
            cl.setTargetTable(targetTable);
            cl.setTargetColumn(tc);
            out.add(cl);
        }
    }

    /** Extract simple column names from the INSERT target column list (null-safe). */
    private List<String> columnNames(java.util.List<? extends com.alibaba.druid.sql.ast.SQLExpr> cols) {
        List<String> out = new ArrayList<>();
        if (cols == null) {
            return out;
        }
        for (com.alibaba.druid.sql.ast.SQLExpr e : cols) {
            String n = null;
            if (e instanceof SQLIdentifierExpr) {
                n = ((SQLIdentifierExpr) e).getName();
            } else if (e instanceof SQLName) {
                n = ((SQLName) e).getSimpleName();
            }
            out.add(n);
        }
        return out;
    }

    private String tcAt(List<String> targetCols, com.alibaba.druid.sql.ast.statement.SQLSelectItem item, int i) {
        if (targetCols != null && i < targetCols.size()) {
            return targetCols.get(i);
        }
        return item != null && item.getAlias() != null ? item.getAlias() : null;
    }

    /**
     * Recursively walk the FROM/JOIN tree, recording alias→physical-table, and return
     * the primary (left-most) table name.
     */
    private String firstTableName(SQLTableSource source, Map<String, String> aliasToTable) {
        if (source == null) {
            return null;
        }
        if (source instanceof SQLJoinTableSource) {
            SQLJoinTableSource join = (SQLJoinTableSource) source;
            String left = firstTableName(join.getLeft(), aliasToTable);
            firstTableName(join.getRight(), aliasToTable);
            return left;
        }
        if (source instanceof SQLExprTableSource) {
            SQLExprTableSource t = (SQLExprTableSource) source;
            String table = t.getTableName();
            String alias = t.getAlias();
            if (table != null && alias != null && !alias.isBlank()) {
                aliasToTable.put(alias, table);
            }
            return table != null ? table : alias;
        }
        // subquery / lateral: cannot map to a physical table; keep null / just the alias
        return source.getAlias();
    }

    private String columnName(com.alibaba.druid.sql.ast.SQLExpr expr) {
        if (expr instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) expr).getName();
        }
        if (expr instanceof SQLName) {
            return ((SQLName) expr).getSimpleName();
        }
        return null;
    }
}