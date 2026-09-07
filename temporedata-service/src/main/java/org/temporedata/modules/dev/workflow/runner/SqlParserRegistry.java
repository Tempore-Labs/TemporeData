package org.temporedata.modules.dev.workflow.runner;

import org.temporedata.api.datasource.sql.Dialect;
import org.temporedata.api.datasource.sql.SqlParser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes a {@link Dialect} to the best-fit {@link SqlParser}. Picks the first
 * parser (in bean order) that advertises support for that dialect; the parser
 * assigned to {@link Dialect#OTHER} is the fallback for unknown dialects.
 */
@Component
public class SqlParserRegistry {

    private final Map<Dialect, SqlParser> primary = new ConcurrentHashMap<>();

    public SqlParserRegistry(List<SqlParser> parsers) {
        for (SqlParser parser : parsers) {
            for (Dialect dialect : Dialect.values()) {
                if (parser.supports(dialect)) {
                    primary.putIfAbsent(dialect, parser);
                }
            }
        }
    }

    public SqlParser resolve(Dialect dialect) {
        Dialect key = dialect == null ? Dialect.OTHER : dialect;
        SqlParser parser = primary.get(key);
        if (parser != null) {
            return parser;
        }
        SqlParser fallback = primary.get(Dialect.OTHER);
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("No SqlParser available for dialect " + key);
    }
}