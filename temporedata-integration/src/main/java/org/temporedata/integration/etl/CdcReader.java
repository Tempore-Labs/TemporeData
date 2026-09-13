package org.temporedata.integration.etl;

import java.util.List;

/**
 * [ENT-P0] Change-data-capture reader SPI.
 *
 * <p>Watches a database change log (MySQL binlog / PostgreSQL WAL / Oracle LogMiner) and emits
 * {@link ChangeEvent}s. Offset/checkpoint must be persisted by the caller through the config
 * ("offset" key); DLQ routing is the caller's concern.
 */
public interface CdcReader extends Connector {

    /** Start the change listener against the configured database. Idempotent. */
    void start();

    /**
     * Block up to {@code timeoutMs} and return whatever change events are available.
     *
     * @return possibly empty list of events
     */
    List<ChangeEvent> poll(long timeoutMs);

    /** Stop listening and release the log reader. Idempotent. */
    void stop();
}