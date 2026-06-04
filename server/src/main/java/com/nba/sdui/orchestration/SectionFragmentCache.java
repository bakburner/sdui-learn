package com.nba.sdui.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nba.sdui.metrics.SduiMetrics;
import com.nba.sdui.request.SduiRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Section-fragment cache (contract §6, plan A2c). Caches composed section
 * JsonNodes by a deterministic key shape and emits
 * {@code sdui.cache.hit/miss{layer=section, key=sectionType}} for every
 * lookup. Screen assembly stays uncached; this layer sits below screen
 * composition and above upstream data caching.
 *
 * <h2>Cache key shape</h2>
 *
 * Keys carry exactly:
 * <ul>
 *   <li>{@code sectionType} — the section's wire {@code type}.</li>
 *   <li>{@code contentHash} — composer-supplied fingerprint of the upstream
 *       inputs that drove the composition (e.g. gameId for a boxscore).</li>
 *   <li>{@code deviceClass} — server composes differently for phone vs tablet
 *       vs desktop.</li>
 *   <li>{@code schemaVersion} — version-aware field stripping changes bytes
 *       per version.</li>
 *   <li>{@code experimentBucket} — experiment-driven branches.</li>
 * </ul>
 *
 * Keys must <strong>not</strong> carry {@code theme}, {@code density},
 * {@code fontScale}, {@code formFactor}, {@code deviceId}, or any time-of-day
 * component. Token resolution stays client-side per AGENTS.md §3.6, so theme
 * / density / form-factor never branch composer output.
 *
 * <h2>i18n {@code stringTable} stamp policy</h2>
 *
 * Stamping happens <strong>after</strong> the cache (composers stamp the
 * assembled response in {@code SduiUtils.stampStringTableOnSections()} as a
 * post-section pass). Cached fragments are therefore locale-neutral and one
 * fragment serves all locales — {@code locale} is intentionally absent from
 * the key.
 *
 * <p>Phase A2c provides only the seam plus the section-channel integration
 * in {@link SectionRefreshService}; per-composer caching (plan-server-section-caching)
 * follows.
 */
@Component
public class SectionFragmentCache {

    private static final Logger log = LoggerFactory.getLogger(SectionFragmentCache.class);

    private static final String LAYER = "section";
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(10);

    private final SduiMetrics metrics;
    private final Map<Duration, Cache<String, JsonNode>> tiers = new ConcurrentHashMap<>();

    public SectionFragmentCache(SduiMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Compute a cache key from the canonical components. Components are
     * normalized to a stable string form; {@code null} or blank values
     * collapse to {@code "_"} so missing inputs do not silently collide with
     * the literal value {@code "null"}.
     */
    public static String key(String sectionType,
                             String contentHash,
                             String deviceClass,
                             String schemaVersion,
                             String experimentBucket) {
        return new StringBuilder("section:")
                .append(safe(sectionType)).append(':')
                .append(safe(contentHash)).append(':')
                .append(safe(deviceClass)).append(':')
                .append(safe(schemaVersion)).append(':')
                .append(safe(experimentBucket))
                .toString();
    }

    /**
     * Convenience overload that derives {@code deviceClass} / {@code schemaVersion}
     * / {@code experimentBucket} from the request context. {@code experiments}
     * are concatenated in sorted order so two requests with the same
     * variant-set produce identical keys.
     */
    public static String key(String sectionType, String contentHash, SduiRequestContext ctx) {
        String deviceClass = ctx == null || ctx.getPlatform() == null ? null
                : ctx.getPlatform().getDeviceClass();
        String schemaVersion = ctx == null ? null : ctx.getSchemaVersion();
        String experimentBucket = experimentBucket(ctx);
        return key(sectionType, contentHash, deviceClass, schemaVersion, experimentBucket);
    }

    /**
     * Look up {@code key}; on miss, compute via {@code supplier}, cache, and
     * return. Emits the appropriate {@code sdui.cache.hit/miss} counter.
     * A {@code null} from the supplier is not cached.
     */
    public JsonNode getOrCompute(String key, Duration ttl, String sectionType, Supplier<JsonNode> supplier) {
        Duration effectiveTtl = ttl == null || ttl.isZero() || ttl.isNegative() ? DEFAULT_TTL : ttl;
        Cache<String, JsonNode> tier = tiers.computeIfAbsent(effectiveTtl, this::buildTier);
        JsonNode cached = tier.getIfPresent(key);
        if (cached != null) {
            metrics.recordCacheHit(LAYER, safe(sectionType));
            log.debug("section cache hit key={} ttl={}", key, effectiveTtl);
            return cached;
        }
        metrics.recordCacheMiss(LAYER, safe(sectionType));
        JsonNode computed = supplier.get();
        if (computed != null) {
            tier.put(key, computed);
        }
        return computed;
    }

    private Cache<String, JsonNode> buildTier(Duration ttl) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(10_000)
                .build();
    }

    private static String experimentBucket(SduiRequestContext ctx) {
        if (ctx == null) return null;
        Map<String, String> experiments = ctx.getExperiments();
        if (experiments == null || experiments.isEmpty()) return "_";
        TreeMap<String, String> sorted = new TreeMap<>(experiments);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "_" : value;
    }
}
