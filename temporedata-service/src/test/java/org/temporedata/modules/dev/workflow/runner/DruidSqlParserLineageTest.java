package org.temporedata.modules.dev.workflow.runner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.temporedata.api.datasource.sql.ColumnLineage;
import org.temporedata.api.datasource.sql.Dialect;
import org.temporedata.api.datasource.sql.SqlParseResult;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DruidSqlParserLineageTest {

    static DruidSqlParser parser;

    @BeforeAll
    static void init() {
        SqlDialectRegistry reg = new SqlDialectRegistry();
        parser = new DruidSqlParser(reg);
    }

    @Test
    void singleSourceInsertSelectPositional() {
        SqlParseResult r = parser.parse(
                "INSERT INTO target_t (acct, total) SELECT account, tx_count FROM fund_flow_stats",
                Dialect.MYSQL);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getTargetTable()).isEqualTo("target_t");
        assertThat(r.getSources()).containsExactly("fund_flow_stats");
        Map<String, String> byTarget = r.getColumnLineage().stream()
                .collect(Collectors.toMap(ColumnLineage::getTargetColumn, ColumnLineage::getSourceColumn));
        assertThat(byTarget).containsEntry("acct", "account").containsEntry("total", "tx_count");
        for (ColumnLineage cl : r.getColumnLineage()) {
            assertThat(cl.getSourceTable()).isEqualTo("fund_flow_stats");
            assertThat(cl.getTargetTable()).isEqualTo("target_t");
        }
    }

    @Test
    void joinMultiSourceQualifiedColumnsResolveToTables() {
        SqlParseResult r = parser.parse(
                "INSERT INTO agg_t (acct, region) " +
                "SELECT f.account, o.region FROM fund_flow_stats f " +
                "LEFT JOIN org_t o ON f.org_id = o.id",
                Dialect.MYSQL);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getTargetTable()).isEqualTo("agg_t");
        // both sources present
        assertThat(r.getSources()).contains("fund_flow_stats", "org_t");
        assertThat(r.getColumnLineage()).hasSize(2);

        Map<String, String> srcTableByTarget = r.getColumnLineage().stream()
                .collect(Collectors.toMap(ColumnLineage::getTargetColumn, ColumnLineage::getSourceTable));
        assertThat(srcTableByTarget).containsEntry("acct", "fund_flow_stats")
                .containsEntry("region", "org_t");

        Map<String, String> srcColByTarget = r.getColumnLineage().stream()
                .collect(Collectors.toMap(ColumnLineage::getTargetColumn, ColumnLineage::getSourceColumn));
        assertThat(srcColByTarget).containsEntry("acct", "account").containsEntry("region", "region");
    }
}