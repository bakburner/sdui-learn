package com.nba.sdui.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Central Micrometer wrapper for the contract §10.1 metric vocabulary.
 *
 * <p>Every metric name and tag set referenced here is part of the wire-level
 * observability contract; renames must be coordinated with dashboards and
 * alerting before changing constants.
 */
@Component
public class SduiMetrics {

    public static final String COMPOSITION_DURATION = "sdui.composition.duration";
    public static final String UPSTREAM_DURATION = "sdui.upstream.duration";
    public static final String CACHE_HIT = "sdui.cache.hit";
    public static final String CACHE_MISS = "sdui.cache.miss";
    public static final String VERSION_MISMATCH = "sdui.version.mismatch";
    public static final String SECTION_REFRESH = "sdui.section.refresh";

    private final MeterRegistry registry;

    @org.springframework.beans.factory.annotation.Autowired
    public SduiMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        this.registry = registryProvider.getIfAvailable(SimpleMeterRegistry::new);
    }

    /**
     * Test-only constructor that accepts an explicit {@link MeterRegistry}.
     * Production wiring uses the {@link ObjectProvider} constructor so the
     * bean is creatable in a {@code @WebMvcTest} slice that does not
     * auto-configure metrics.
     */
    public SduiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordComposition(String screenId, String sectionType, boolean cached, Duration elapsed) {
        Timer.builder(COMPOSITION_DURATION)
                .tags(Tags.of(
                        "screenId", nullSafe(screenId),
                        "sectionType", nullSafe(sectionType),
                        "cached", Boolean.toString(cached)))
                .register(registry)
                .record(elapsed);
    }

    public void recordUpstream(String serviceName, String status, Duration elapsed) {
        Timer.builder(UPSTREAM_DURATION)
                .tags(Tags.of(
                        "serviceName", nullSafe(serviceName),
                        "status", nullSafe(status)))
                .register(registry)
                .record(elapsed);
    }

    public void recordCacheHit(String layer, String key) {
        Counter.builder(CACHE_HIT)
                .tags(Tags.of("layer", nullSafe(layer), "key", nullSafe(key)))
                .register(registry)
                .increment();
    }

    public void recordCacheMiss(String layer, String key) {
        Counter.builder(CACHE_MISS)
                .tags(Tags.of("layer", nullSafe(layer), "key", nullSafe(key)))
                .register(registry)
                .increment();
    }

    public void recordVersionMismatch(String clientVersion) {
        Counter.builder(VERSION_MISMATCH)
                .tags(Tags.of("clientVersion", nullSafe(clientVersion)))
                .register(registry)
                .increment();
    }

    public void recordSectionRefresh(String sectionId, String status) {
        Counter.builder(SECTION_REFRESH)
                .tags(Tags.of("sectionId", nullSafe(sectionId), "status", nullSafe(status)))
                .register(registry)
                .increment();
    }

    private static String nullSafe(String v) {
        return v == null ? "unknown" : v;
    }
}
