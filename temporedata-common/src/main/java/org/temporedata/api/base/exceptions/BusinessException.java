package org.temporedata.api.base.exceptions;

/**
 * Business-level exception thrown for expected/controllable failures.
 */
public class BusinessException extends ZyException {

    public BusinessException(String message) {
        super(400, message);
    }

    public BusinessException(int code, String message) {
        super(code, message);
    }
}