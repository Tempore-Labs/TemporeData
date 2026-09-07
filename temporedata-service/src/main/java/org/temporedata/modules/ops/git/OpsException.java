package org.temporedata.modules.ops.git;

/**
 * Raised for Ops provider failures.
 */
public class OpsException extends RuntimeException {
    public OpsException(String message) {
        super(message);
    }
}