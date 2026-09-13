package org.temporedata.integration.etl;

import java.util.Iterator;
import java.util.List;

/**
 * [ENT-P0] Batch source reader SPI.
 *
 * <p>Reads a table (optionally via a slice/shard query) as an ordered stream of rows,
 * supporting chunked/sc stepped batch sync. Implementations expose the logical schema and a
 * forward-only row iterator.
 */
public interface DataReader extends Connector {

    /** Logical columns of the target table (may be lazily resolved). */
    List<ColumnSchema> schema();

    /**
     * Open the reader for a table, optionally limited by a slice predicate.
     *
     * @param table qualified table name
     * @param slice optional slice (e.g. "id % 4 = 0") or {@code null} for full scan
     * @param sliceIndex 0-based slice ordinal used together with sliceCount
     * @param sliceCount total slice count (chunk parallelism)
     */
    void open(String table, String slice, int sliceIndex, int sliceCount);

    /** Forward-only row iterator; must be consumed before {@code close()}. */
    Iterator<DataRow> rows();

    /** High-watermark after the last {@link #open(String, String, int, int)} scan, if tracked. */
    default Object watermark() {
        return null;
    }
}