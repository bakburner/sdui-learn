package com.nba.sdui.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * SDUI composition health.
 *
 * <p>Exposed under the {@code /actuator/health/sdui} group. For now this
 * always reports UP — Phase A2b will tie this to upstream readiness
 * (Stats CDN reachability, SAF-backed services). Liveness/readiness
 * probes use the standard {@code /actuator/health/liveness} and
 * {@code /actuator/health/readiness} endpoints, which Spring Boot wires
 * automatically.
 */
@Component("sdui")
public class SduiHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
                .withDetail("composers", "ready")
                .build();
    }
}
