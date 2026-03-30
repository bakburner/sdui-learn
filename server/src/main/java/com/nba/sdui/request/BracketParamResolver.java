package com.nba.sdui.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.*;

/**
 * Resolves {@link SduiRequestContext} from bracket-notation query params (GET)
 * or a JSON request body (POST).
 *
 * <h3>Bracket notation</h3>
 * <pre>
 *   platform[name]=android
 *   platform[capabilities][sse]=true
 *   device[countryCode]=US
 *   experiments[gd_tab_order_v2]=variant_b
 * </pre>
 *
 * <h3>POST JSON body</h3>
 * <pre>
 *   { "platform": { "name": "android" }, "locale": "es" }
 * </pre>
 */
public class BracketParamResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(BracketParamResolver.class);

    private final ObjectMapper objectMapper;

    public BracketParamResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return SduiRequestContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return new SduiRequestContext();
        }

        // POST with JSON body → deserialize directly
        if ("POST".equalsIgnoreCase(request.getMethod())
                && request.getContentType() != null
                && request.getContentType().contains("application/json")) {
            try {
                SduiRequestContext ctx = objectMapper.readValue(
                        request.getInputStream(), SduiRequestContext.class);
                applyHeaderFallbacks(ctx, webRequest);
                return ctx;
            } catch (Exception e) {
                log.warn("Failed to parse POST body as SduiRequestContext, falling back to query params: {}",
                        e.getMessage());
            }
        }

        // GET (or POST fallback) → parse bracket-notation query params
        Map<String, String[]> paramMap = request.getParameterMap();
        Map<String, Object> nested = parseBracketParams(paramMap);

        SduiRequestContext ctx = objectMapper.convertValue(nested, SduiRequestContext.class);
        applyHeaderFallbacks(ctx, webRequest);
        return ctx;
    }

    /**
     * During transition: accept deprecated headers as fallback if query params are absent.
     */
    private void applyHeaderFallbacks(SduiRequestContext ctx, NativeWebRequest webRequest) {
        // X-Platform → platform.name (deprecated header fallback)
        if (ctx.getPlatformName() == null) {
            String headerPlatform = webRequest.getHeader("X-Platform");
            if (headerPlatform != null) {
                log.debug("Using deprecated X-Platform header; migrate to platform[name] query param");
                if (ctx.getPlatform() == null) {
                    ctx.setPlatform(new SduiRequestContext.Platform());
                }
                ctx.getPlatform().setName(headerPlatform);
            }
        }

        // X-Schema-Version → schemaVersion (deprecated header fallback)
        if ("1.0".equals(ctx.getSchemaVersion())) {
            String headerSchema = webRequest.getHeader("X-Schema-Version");
            if (headerSchema != null && !"1.0".equals(headerSchema)) {
                log.debug("Using deprecated X-Schema-Version header; migrate to schemaVersion query param");
                ctx.setSchemaVersion(headerSchema);
            }
        }

        // X-Trace-Id → traceId
        if (ctx.getTraceId() == null) {
            String headerTraceId = webRequest.getHeader("X-Trace-Id");
            if (headerTraceId != null) {
                ctx.setTraceId(headerTraceId);
            }
        }
    }

    /**
     * Parse bracket-notation query parameters into a nested map.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code platform[name]=android} → {@code {platform: {name: "android"}}}</li>
     *   <li>{@code platform[capabilities][sse]=true} → {@code {platform: {capabilities: {sse: "true"}}}}</li>
     *   <li>{@code locale=en} → {@code {locale: "en"}}</li>
     *   <li>{@code experiments[gd_tab_order]=B} → {@code {experiments: {gd_tab_order: "B"}}}</li>
     * </ul>
     */
    static Map<String, Object> parseBracketParams(Map<String, String[]> paramMap) {
        Map<String, Object> root = new LinkedHashMap<>();

        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().length > 0 ? entry.getValue()[0] : "";

            List<String> segments = parseBracketKey(key);
            if (segments.isEmpty()) continue;

            setNestedValue(root, segments, value);
        }

        return root;
    }

    /**
     * Parse a bracket-notation key into path segments.
     * <ul>
     *   <li>{@code "locale"} → {@code ["locale"]}</li>
     *   <li>{@code "platform[name]"} → {@code ["platform", "name"]}</li>
     *   <li>{@code "platform[capabilities][sse]"} → {@code ["platform", "capabilities", "sse"]}</li>
     * </ul>
     */
    static List<String> parseBracketKey(String key) {
        List<String> segments = new ArrayList<>();
        int bracketStart = key.indexOf('[');

        if (bracketStart == -1) {
            // Simple key: "locale"
            segments.add(key);
        } else {
            // First segment is before the first bracket
            segments.add(key.substring(0, bracketStart));

            // Extract each [segment]
            int pos = bracketStart;
            while (pos < key.length()) {
                if (key.charAt(pos) == '[') {
                    int end = key.indexOf(']', pos);
                    if (end == -1) break;
                    segments.add(key.substring(pos + 1, end));
                    pos = end + 1;
                } else {
                    pos++;
                }
            }
        }

        return segments;
    }

    /**
     * Set a value in a nested map structure, creating intermediate maps as needed.
     */
    @SuppressWarnings("unchecked")
    private static void setNestedValue(Map<String, Object> root, List<String> segments, String value) {
        Map<String, Object> current = root;

        for (int i = 0; i < segments.size() - 1; i++) {
            String segment = segments.get(i);
            Object existing = current.get(segment);
            if (existing instanceof Map) {
                current = (Map<String, Object>) existing;
            } else {
                Map<String, Object> child = new LinkedHashMap<>();
                current.put(segment, child);
                current = child;
            }
        }

        current.put(segments.get(segments.size() - 1), value);
    }
}
