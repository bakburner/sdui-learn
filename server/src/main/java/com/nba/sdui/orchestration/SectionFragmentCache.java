package com.nba.sdui.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.saf.cache.CacheStrategy;
import com.nba.saf.cache.CacheStrategyName;
import com.nba.saf.cache.TwoTierCacheService;
import com.nba.saf.model.CachedValue.CacheSource;
import com.nba.saf.model.ResponseMetadata;
import com.nba.saf.model.ServiceResult;
import com.nba.sdui.metrics.SduiMetrics;
import com.nba.sdui.request.SduiRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Section-fragment cache (contract §6, plan A2c). Delegates to SAF's
 * {@link TwoTierCacheService} (the public "aggregate response caching"
 * surface per SAF's caching doc) so L1 (Caffeine), L2 (Redis), request
 * collapsing, stale-if-error, and circuit-breaker protection are all owned
 * by SAF — never reinvented here. Emits the contract §10.1
 * {@code sdui.cache.hit/miss{layer=section, key=sectionType}} counters on
 * top of SAF's own internal cache metrics.
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

    /**
     * Logical service name for SAF's per-service config + metrics dispatch.
     * Matches {@code saf.services.<name>} in {@code application.yml} so the
     * operator owns staleness/resilience policy without code changes.
     */
    static final String SERVICE_NAME = "sdui-section-fragment";

    private final SduiMetrics metrics;
    private final TwoTierCacheService cache;
    private final CacheStrategy strategy;

    public SectionFragmentCache(SduiMetrics metrics,
                                TwoTierCacheService cache,
                                Map<String, CacheStrategy> cacheStrategies) {
        this.metrics = metrics;
        this.cache = cache;
        this.strategy = cacheStrategies.get(CacheStrategyName.STALE_IF_ERROR.getKey());
        if (this.strategy == null) {
            throw new IllegalStateException(
                    "SAF stale-if-error CacheStrategy bean not registered; check SAF auto-configuration");
        }
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
     * return. Emits {@code sdui.cache.hit/miss{layer=section, key=sectionType}}
     * derived from the {@link ServiceResult} source. SAF owns the rest:
     * L1/L2 storage, request collapsing, stale-if-error fallback.
     *
     * <p>The {@code ttl} parameter is the developer-set fresh window; the
     * operator-controlled stale window comes from
     * {@code saf.services.sdui-section-fragment.cache.stale-if-error-max-stale-age}
     * in YAML.
     */
    public JsonNode getOrCompute(String key, Duration ttl, String sectionType, Supplier<JsonNode> supplier) {
        Duration effectiveTtl = ttl == null || ttl.isZero() || ttl.isNegative() ? DEFAULT_TTL : ttl;
        ServiceResult<JsonNode> result = cache.getOrCompute(
                key,
                SERVICE_NAME,
                () -> ResponseMetadata.now(supplier.get()),
                strategy,
                effectiveTtl,
                /* maxStaleAgeSeconds */ 0L);

        recordHitOrMiss(result, sectionType, key);
        return result == null ? null : result.getData();
    }

    private void recordHitOrMiss(ServiceResult<JsonNode> result, String sectionType, String key) {
        if (result == null) {
            metrics.recordCacheMiss(LAYER, safe(sectionType));
            return;
        }
        CacheSource source = result.getSource();
        boolean served = result.isStale() || (source != null && source != CacheSource.NONE);
        if (served) {
            metrics.recordCacheHit(LAYER, safe(sectionType));
            log.debug("section cache hit key={} source={} stale={}", key, source, result.isStale());
        } else {
            metrics.recordCacheMiss(LAYER, safe(sectionType));
        }
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
