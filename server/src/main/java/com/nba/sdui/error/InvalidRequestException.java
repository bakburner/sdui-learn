package com.nba.sdui.error;

import org.springframework.http.HttpStatus;

/**
 * 400 Bad Request — caller-facing input validation failure
 * (malformed param, unknown screen id, unsupported section variant, etc.).
 */
public class InvalidRequestException extends SduiException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "invalid_request", message);
    }

    public InvalidRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
