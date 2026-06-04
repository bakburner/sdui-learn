package com.nba.sdui.orchestration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry that routes parameterized refresh requests to the composer
 * responsible for each screen ID.
 *
 * <p>Composers register a resolver at startup (via {@link jakarta.annotation.PostConstruct}).
 * The controller calls {@link #refreshScreen} and receives {@link Optional#empty()} for
 * unknown screen IDs (mapped to 404 at the HTTP boundary) or a non-empty result for
 * known ones. Resolver failures propagate as unchecked exceptions (mapped to 500).
 *
 * <p>Screen IDs are exact-match keys (e.g. {@code "games"}, {@code "leaders"}), not prefixes,
 * because parameterized refresh targets a named query surface rather than a dynamic
 * content-source instance.
 */
@Service
public class ParameterizedRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ParameterizedRefreshService.class);

    @FunctionalInterface
    public interface ScreenRefreshResolver {
        ObjectNode resolve(String traceId, Map<String, String> userParams, SduiRequestContext ctx)
                throws Exception;
    }

    private final Map<String, ScreenRefreshResolver> registry = new LinkedHashMap<>();

    public void registerResolver(String screenId, ScreenRefreshResolver resolver) {
        registry.put(screenId, resolver);
        log.debug("Registered parameterized refresh resolver for screenId='{}'", screenId);
    }

    /**
     * Invoke the resolver for {@code screenId}.
     *
     * @return the composed screen node, or {@link Optional#empty()} when no resolver is
     *         registered for the given screen ID (caller should return 404).
     * @throws RuntimeException wrapping the resolver's exception on composition failure
     *         (caller should return 500).
     */
    public Optional<ObjectNode> refreshScreen(String screenId,
                                              String traceId,
                                              Map<String, String> userParams,
                                              SduiRequestContext ctx) {
        ScreenRefreshResolver resolver = registry.get(screenId);
        if (resolver == null) {
            log.warn("No resolver registered for screenId='{}' — returning 404", screenId);
            return Optional.empty();
        }
        try {
            ObjectNode result = resolver.resolve(traceId, userParams, ctx);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.error("Resolver failed for screenId='{}': {}", screenId, e.getMessage(), e);
            throw new RuntimeException("Parameterized refresh failed for screenId=" + screenId, e);
        }
    }
}
