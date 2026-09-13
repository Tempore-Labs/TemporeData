package org.temporedata.integration.etl;

/**
 * [ENT-P0] CDC change-operation type.
 */
public enum ChangeOperation {
    INSERT,
    UPDATE,
    DELETE,
    NA
}