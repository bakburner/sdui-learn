package com.nba.sdui.error;

import org.springframework.http.HttpStatus;

/**
 * Base for SDUI-server exceptions that map to RFC-7807 ProblemDetail responses.
 *
 * <p>Carries an HTTP status and a stable error code string. The
 * {@link com.nba.sdui.controller.SduiExceptionHandler} translates subclasses
 * into ProblemDetail bodies; concrete subclasses choose status + code.
 */
public abstract class SduiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected SduiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    protected SduiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
