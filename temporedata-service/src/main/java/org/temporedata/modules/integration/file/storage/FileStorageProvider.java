package org.temporedata.modules.integration.file.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Storage abstraction for the generic FileStorage module (S4).
 *
 * <p>Business logic depends only on this interface; the concrete implementation
 * (local disk or object storage) is selected by {@code app.file.storage-type}.
 * Swap storage = swap provider + config, no business code change.</p>
 */
public interface FileStorageProvider {

    /**
     * Persist the object at {@code source} under {@code key} and return its key.
     * Implementations stream from the source file; they must not retain the caller's
     * file. The caller owns deleting {@code source} afterwards.
     */
    String put(Path source, String key);

    /**
     * Open a read stream for the given key.
     */
    InputStream stream(String key);

    /**
     * Delete an object represented by the key.
     */
    void delete(String key);

    /**
     * Storage type identifier: local | s3 | ...
     */
    String type();
}