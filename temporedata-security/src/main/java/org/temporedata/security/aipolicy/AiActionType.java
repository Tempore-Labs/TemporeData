package org.temporedata.security.aipolicy;

/**
 * [ENT-P2] Kind of AI-generated action submitted to the safety gate.
 */
public enum AiActionType {
    TEXT2SQL,
    DDL,
    SCRIPT
}