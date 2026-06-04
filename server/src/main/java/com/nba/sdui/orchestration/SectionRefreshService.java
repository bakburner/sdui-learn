package com.nba.sdui.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.sdui.metrics.SduiMetrics;
import com.nba.sdui.request.SduiRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import com.nba.sdui.error.UnsupportedSectionException;

@Service
public class SectionRefreshService {

    private static final Logger log = LoggerFactory.getLogger(SectionRefreshService.class);

    @FunctionalInterface
    public interface SectionResolver {
        JsonNode resolve(String sectionId, SduiRequestContext ctx) throws Exception;
    }

    private final Map<String, SectionResolver> registry = new LinkedHashMap<>();
    private final SduiMetrics metrics;

    public SectionRefreshService(SduiMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Test-only constructor for unit tests that build the service without a
     * Spring context. Production wiring always uses the {@link SduiMetrics}
     * constructor.
     */
    public SectionRefreshService() {
        this(new SduiMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    }

    public void registerResolver(String prefix, SectionResolver resolver) {
        registry.put(prefix, resolver);
        log.debug("Registered section resolver for prefix '{}'", prefix);
    }

    public Optional<JsonNode> refreshSection(String sectionId, SduiRequestContext ctx) {
        String bestPrefix = null;
        for (String prefix : registry.keySet()) {
            if (sectionId.startsWith(prefix)) {
                if (bestPrefix == null || prefix.length() > bestPrefix.length()) {
                    bestPrefix = prefix;
                }
            }
        }

        if (bestPrefix == null) {
            log.warn("No resolver registered for sectionId='{}' — returning 404", sectionId);
            metrics.recordSectionRefresh(sectionId, "not_found");
            return Optional.empty();
        }

        try {
            JsonNode result = registry.get(bestPrefix).resolve(sectionId, ctx);
            metrics.recordSectionRefresh(sectionId, result == null ? "empty" : "success");
            return Optional.ofNullable(result);
        } catch (UnsupportedSectionException e) {
            metrics.recordSectionRefresh(sectionId, "unsupported");
            throw e;
        } catch (Exception e) {
            log.error("Resolver failed for sectionId='{}': {}", sectionId, e.getMessage(), e);
            metrics.recordSectionRefresh(sectionId, "error");
            return Optional.empty();
        }
    }
}
