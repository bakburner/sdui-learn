package com.nba.sdui.error;

import org.springframework.http.HttpStatus;

/**
 * 503 Service Unavailable — an upstream dependency (Stats CDN, future SAF
 * services) failed in a way that prevents composition. Mapped to RFC-7807
 * by {@link com.nba.sdui.controller.SduiExceptionHandler}.
 */
public class UpstreamServiceException extends SduiException {

    public UpstreamServiceException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "upstream_unavailable", message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "upstream_unavailable", message, cause);
    }

    public UpstreamServiceException(String code, String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, code, message, cause);
    }
}
