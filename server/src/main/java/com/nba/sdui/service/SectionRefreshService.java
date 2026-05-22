package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.sdui.request.SduiRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SectionRefreshService {

    private static final Logger log = LoggerFactory.getLogger(SectionRefreshService.class);

    @FunctionalInterface
    public interface SectionResolver {
        JsonNode resolve(String sectionId, SduiRequestContext ctx) throws Exception;
    }

    private final Map<String, SectionResolver> registry = new LinkedHashMap<>();

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
            return Optional.empty();
        }

        try {
            JsonNode result = registry.get(bestPrefix).resolve(sectionId, ctx);
            return Optional.ofNullable(result);
        } catch (UnsupportedSectionException e) {
            // Resolver recognized the prefix but cannot refresh this specific section in isolation.
            // Propagate so the controller can translate to 400 (vs 404 for "no resolver registered").
            throw e;
        } catch (Exception e) {
            log.error("Resolver failed for sectionId='{}': {}", sectionId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
