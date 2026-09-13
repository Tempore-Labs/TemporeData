package org.temporedata.integration.etl;

/**
 * [ENT-P0] Batch target writer SPI.
 *
 * <p>Writes rows into a target table, optionally applying a field-type mapping. Batch sync
 * engines stream {@link DataRow}s through a writer and commit via {@code flush()}.
 */
public interface DataWriter extends Connector {

    /**
     * Prepare the writer for a target table.
     *
     * @param table qualified target table name
     * @param mapping URI/expression describing source-&gt;target column/type mapping, or null
     */
    void open(String table, String mapping);

    /** Buffer/apply one row. Caller batches rows before {@link #flush()}. */
    void write(DataRow row);

    /** Persist buffered rows (transactional if the connector supports it). */
    void flush();

    /** Optional row count written since last {@link #resetCounter()}. */
    default long written() {
        return 0L;
    }

    /** Reset the internal written counter (e.g. per slice). */
    default void resetCounter() {
        // no-op by default
    }
}