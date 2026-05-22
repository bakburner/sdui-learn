package com.nba.sdui.service;

/**
 * Thrown by a {@link SectionRefreshService.SectionResolver} when the sectionId
 * is recognized by a resolver (the content-source prefix matched), but the
 * specific section variant or slug is not supported by that resolver.
 *
 * <p>This is distinct from "no resolver registered for this prefix" (the
 * service returns {@link java.util.Optional#empty()} for that, which the
 * controller translates to 404). An UnsupportedSectionException is translated
 * to HTTP 400 Bad Request because the client referenced a section the server
 * knows about but cannot refresh in isolation.
 */
public class UnsupportedSectionException extends RuntimeException {
    public UnsupportedSectionException(String message) {
        super(message);
    }
}
