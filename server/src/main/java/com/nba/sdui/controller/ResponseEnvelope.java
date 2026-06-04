package com.nba.sdui.controller;

/**
 * Transport-framing wrapper for every SDUI screen-channel and section-channel
 * response. The wire body is always {@code { "data": <Screen|Section>, "meta": {...} }}.
 *
 * <p>This wrapper is hand-written and intentionally lives outside
 * {@code schema/sdui-schema.json} and the codegen pipeline. Schema describes
 * the composed UI tree; transport framing (envelope, correlation, freshness
 * metadata) evolves independently of UI semantics. See AGENTS.md §1.2
 * "Transport-framing exception" and ADR-017.
 */
public record ResponseEnvelope<T>(T data, ResponseMeta meta) {
    public static <T> ResponseEnvelope<T> of(T data) {
        return new ResponseEnvelope<>(data, ResponseMeta.fresh());
    }
}
