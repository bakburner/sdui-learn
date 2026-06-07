package com.nba.sdui.orchestration;

import com.nba.saf.dto.ServiceMetadata;
import com.nba.saf.dto.ServiceMetadata.ServiceCallInfo;
import com.nba.saf.dto.ServiceMetadata.ServiceCallStatus;
import com.nba.sdui.controller.ResponseMeta;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-request accumulator for upstream {@link ServiceCallInfo} entries. Adapters
 * push call metadata after each {@code orchestrator.executeAll()}; the
 * controller projects the accumulated entries into a {@link ResponseMeta} at
 * envelope time.
 *
 * <p>Mapping rules (plan A2c, contract §10.2):
 * <ul>
 *   <li>{@code failedSections} = call ids whose status is FAILED or TIMEOUT.</li>
 *   <li>{@code staleSections} = call ids whose {@code servedStale == true}
 *       (upstream actually returned stale-if-error data — a fresh L1/L2 hit is
 *       not stale).</li>
 *   <li>{@code degraded} = any partialFailure observed across the request, or
 *       any failed/stale call id present.</li>
 * </ul>
 *
 * <p>The collector is a request-scoped bean so concurrent requests cannot
 * contaminate each other's metadata. Tests that build adapters without a
 * Spring context skip the collector path entirely.
 */
@Component
@RequestScope
public class ResponseMetaCollector {

    private final Map<String, ServiceCallInfo> calls = new LinkedHashMap<>();
    private boolean partialFailure;

    /**
     * Record a single call's metadata. Subsequent calls with the same id
     * overwrite the previous entry (refreshes / retries within one request).
     */
    public synchronized void record(ServiceMetadata metadata) {
        if (metadata == null) return;
        if (Boolean.TRUE.equals(metadata.getPartialFailure())) {
            partialFailure = true;
        }
        Map<String, ServiceCallInfo> services = metadata.getServices();
        if (services == null) return;
        for (Map.Entry<String, ServiceCallInfo> e : services.entrySet()) {
            calls.put(e.getKey(), e.getValue());
        }
    }

    /**
     * Build the {@link ResponseMeta} for the current request. Safe to call
     * multiple times; produces an immutable snapshot each time.
     */
    public synchronized ResponseMeta build() {
        if (calls.isEmpty() && !partialFailure) {
            return ResponseMeta.fresh();
        }
        List<String> failed = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, ServiceCallInfo> e : calls.entrySet()) {
            ServiceCallInfo info = e.getValue();
            ServiceCallStatus status = info.getStatus();
            if (status == ServiceCallStatus.FAILED || status == ServiceCallStatus.TIMEOUT) {
                failed.add(e.getKey());
                continue;
            }
            if (Boolean.TRUE.equals(info.getServedStale())) {
                stale.add(e.getKey());
            }
        }
        boolean degraded = partialFailure || !failed.isEmpty() || !stale.isEmpty();
        return new ResponseMeta(degraded,
                Collections.unmodifiableList(stale),
                Collections.unmodifiableList(failed));
    }
}
