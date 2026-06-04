package com.nba.sdui.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown by a section resolver when the sectionId is recognized by a resolver
 * (the content-source prefix matched), but the specific section variant or
 * slug is not supported by that resolver.
 *
 * <p>This is distinct from "no resolver registered for this prefix" (the
 * service returns {@link java.util.Optional#empty()} for that, which the
 * controller translates to 404). UnsupportedSectionException is translated to
 * HTTP 400 because the client referenced a section the server knows about but
 * cannot refresh in isolation.
 */
public class UnsupportedSectionException extends SduiException {

    public UnsupportedSectionException(String message) {
        super(HttpStatus.BAD_REQUEST, "unsupported_section", message);
    }
}
