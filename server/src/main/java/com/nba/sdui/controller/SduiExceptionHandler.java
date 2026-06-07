package com.nba.sdui.controller;

import com.nba.sdui.error.SduiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global RFC-7807 exception handler for SDUI controllers.
 *
 * <p>Translates {@link SduiException} subclasses into Spring
 * {@link ProblemDetail} bodies; all other exceptions fall back to a 500
 * ProblemDetail with {@code code=internal_error}. Limited to controllers in
 * {@code com.nba.sdui.controller} so this advice does not silently swallow
 * exceptions from unrelated controllers (e.g. legacy demo endpoints) until
 * they opt in.
 */
@RestControllerAdvice(basePackages = "com.nba.sdui.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SduiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SduiExceptionHandler.class);

    private static final String PROBLEM_TYPE_BASE = "https://nba.example/sdui/problems/";

    @ExceptionHandler(SduiException.class)
    public ResponseEntity<ProblemDetail> handleSdui(SduiException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        body.setTitle(ex.getStatus().getReasonPhrase());
        body.setType(URI.create(PROBLEM_TYPE_BASE + ex.getCode()));
        body.setProperty("code", ex.getCode());
        if (ex.getStatus().is5xxServerError()) {
            log.error("SDUI {} ({}): {}", ex.getStatus().value(), ex.getCode(), ex.getMessage(), ex);
        } else {
            log.warn("SDUI {} ({}): {}", ex.getStatus().value(), ex.getCode(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnknown(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Internal server error");
        body.setTitle(status.getReasonPhrase());
        body.setType(URI.create(PROBLEM_TYPE_BASE + "internal_error"));
        body.setProperty("code", "internal_error");
        log.error("SDUI 500 (internal_error): {}", ex.getMessage(), ex);
        return ResponseEntity.status(status).body(body);
    }
}
