package org.temporedata.integration.etl;

import java.time.Instant;

/**
 * [ENT-P0] A single change (CDC) record emitted by {@link CdcReader} implementations.
 */
public final class ChangeEvent {

    private final String sourceType;
    private final String database;
    private final String table;
    private final ChangeOperation operation;
    private final DataRow before;
    private final DataRow after;
    private final Instant ts;
    private final long offset;

    private ChangeEvent(String sourceType, String database, String table, ChangeOperation operation,
                        DataRow before, DataRow after, Instant ts, long offset) {
        this.sourceType = sourceType;
        this.database = database;
        this.table = table;
        this.operation = operation;
        this.before = before;
        this.after = after;
        this.ts = ts;
        this.offset = offset;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String sourceType;
        private String database;
        private String table;
        private ChangeOperation operation;
        private DataRow before;
        private DataRow after;
        private Instant ts = Instant.now();
        private long offset;

        public Builder sourceType(String v) { this.sourceType = v; return this; }
        public Builder database(String v) { this.database = v; return this; }
        public Builder table(String v) { this.table = v; return this; }
        public Builder operation(ChangeOperation v) { this.operation = v; return this; }
        public Builder before(DataRow v) { this.before = v; return this; }
        public Builder after(DataRow v) { this.after = v; return this; }
        public Builder ts(Instant v) { this.ts = v; return this; }
        public Builder offset(long v) { this.offset = v; return this; }

        public ChangeEvent build() {
            return new ChangeEvent(sourceType, database, table, operation, before, after, ts, offset);
        }
    }

    public String sourceType() { return sourceType; }
    public String database() { return database; }
    public String table() { return table; }
    public ChangeOperation operation() { return operation; }
    public DataRow before() { return before; }
    public DataRow after() { return after; }
    public Instant ts() { return ts; }
    public long offset() { return offset; }
}