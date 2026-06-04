package com.nba.sdui.controller;

import java.util.List;

/**
 * Transport-framing metadata sibling to {@code data} in {@link ResponseEnvelope}.
 *
 * <p>Static stub for A2a — every response currently reports {@code fresh()}.
 * A2c populates these fields from real orchestrator metadata (degraded paths,
 * stale fragments served from cache, sections that failed to compose).
 */
public record ResponseMeta(boolean degraded,
                           List<String> staleSections,
                           List<String> failedSections) {
    public static ResponseMeta fresh() {
        return new ResponseMeta(false, List.of(), List.of());
    }
}
