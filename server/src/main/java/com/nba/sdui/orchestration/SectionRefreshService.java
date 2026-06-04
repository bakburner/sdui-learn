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
    private final SectionFragmentCache sectionCache;

    public SectionRefreshService(SduiMetrics metrics, SectionFragmentCache sectionCache) {
        this.metrics = metrics;
        this.sectionCache = sectionCache;
    }

    /**
     * Test-only constructor for unit tests that build the service without a
     * Spring context. Production wiring always uses the {@link SduiMetrics}
     * constructor.
     */
    public SectionRefreshService() {
        this(new SduiMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()), null);
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
            String sectionType = sectionTypeFromId(sectionId);
            JsonNode result;
            if (sectionCache != null) {
                String key = SectionFragmentCache.key(sectionType, sectionId, ctx);
                final String resolverPrefix = bestPrefix;
                result = sectionCache.getOrCompute(key, null, sectionType, () -> {
                    try {
                        return registry.get(resolverPrefix).resolve(sectionId, ctx);
                    } catch (UnsupportedSectionException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                result = registry.get(bestPrefix).resolve(sectionId, ctx);
            }
            metrics.recordSectionRefresh(sectionId, result == null ? "empty" : "success");
            return Optional.ofNullable(result);
        } catch (UnsupportedSectionException e) {
            metrics.recordSectionRefresh(sectionId, "unsupported");
            throw e;
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnsupportedSectionException uns) {
                metrics.recordSectionRefresh(sectionId, "unsupported");
                throw uns;
            }
            log.error("Resolver failed for sectionId='{}': {}", sectionId, e.getMessage(), e);
            metrics.recordSectionRefresh(sectionId, "error");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Resolver failed for sectionId='{}': {}", sectionId, e.getMessage(), e);
            metrics.recordSectionRefresh(sectionId, "error");
            return Optional.empty();
        }
    }

    /**
     * Best-effort sectionType derivation from a sectionId. Most ids are of the
     * form {@code "<type>-<discriminator>"} (e.g. {@code "boxscore-0022400123"}).
     * Falls back to the full id when no discriminator is present. Used only as
     * a metric tag and a fragment-cache label, never as a routing decision.
     */
    private static String sectionTypeFromId(String sectionId) {
        if (sectionId == null) return "unknown";
        int dash = sectionId.indexOf('-');
        return dash > 0 ? sectionId.substring(0, dash) : sectionId;
    }
}
