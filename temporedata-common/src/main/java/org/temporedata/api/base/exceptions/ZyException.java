package org.temporedata.api.base.exceptions;

/**
 * Base runtime exception carrying a business error code & message.
 */
public class ZyException extends RuntimeException {

    private final int code;

    public ZyException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}