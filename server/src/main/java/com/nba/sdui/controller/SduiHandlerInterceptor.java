package com.nba.sdui.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * SDUI request/response interceptor.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Reads {@code X-Schema-Version}; if present and not equal to the server's
 *       configured {@code sdui.schema.version}, sets response header
 *       {@code X-Schema-Version-Mismatch} with the server version. This is a
 *       soft gate — requests still proceed. Hard enforcement is a future
 *       phase.</li>
 *   <li>Sets {@code Cache-Control: no-store} as the default. Composer-level
 *       endpoints can override this per-response (Phase A2c hardens the
 *       per-channel cache policy).</li>
 * </ul>
 */
@Component
public class SduiHandlerInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SduiHandlerInterceptor.class);

    private static final String SCHEMA_VERSION_HEADER = "X-Schema-Version";
    private static final String SCHEMA_VERSION_MISMATCH_HEADER = "X-Schema-Version-Mismatch";

    @Value("${sdui.schema.version:1.0}")
    private String serverSchemaVersion;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String clientVersion = request.getHeader(SCHEMA_VERSION_HEADER);
        if (clientVersion != null && !clientVersion.equals(serverSchemaVersion)) {
            response.setHeader(SCHEMA_VERSION_MISMATCH_HEADER, serverSchemaVersion);
            log.debug("Schema version mismatch: client={} server={}", clientVersion, serverSchemaVersion);
        }
        if (response.getHeader("Cache-Control") == null) {
            response.setHeader("Cache-Control", "no-store");
        }
        return true;
    }
}
