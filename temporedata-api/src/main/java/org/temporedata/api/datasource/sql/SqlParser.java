package org.temporedata.api.datasource.sql;

/**
 * Strategy interface for SQL parsing (table-level lineage extraction).
 * Implementations advertise which dialects they support; a
 * {@code SqlParserRegistry} routes a SQL statement to the best-fit parser.
 */
public interface SqlParser {

    /**
     * Whether this parser can parse statements for the given dialect.
     */
    boolean supports(Dialect dialect);

    /**
     * Parse a SQL statement in the given dialect into table-level lineage.
     * Must never throw for a syntactically unsupported statement — degrade
     * gracefully ({@code success=false}) instead.
     */
    SqlParseResult parse(String sql, Dialect dialect);
}