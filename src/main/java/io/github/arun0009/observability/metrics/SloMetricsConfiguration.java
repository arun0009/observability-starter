package io.github.arun0009.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Automatically computes and exports SLO (Service Level Objective) metrics:
 * <ul>
 * <li>{@code slo.http.error.ratio} — ratio of 5xx responses to total</li>
 * <li>{@code slo.http.latency.p99.ms} — 99th percentile latency in ms</li>
 * </ul>
 * Computed from the standard {@code http.server.requests} timer that
 * Spring Boot Actuator provides.
 */
@Configuration(proxyBeanMethods = false)
public class SloMetricsConfiguration {

    private final MeterRegistry registry;

    public SloMetricsConfiguration(MeterRegistry registry) {
        this.registry = registry;

        Gauge.builder("slo.http.error.ratio", this::computeErrorRatio)
                .description("Ratio of 5xx responses to total HTTP requests")
                .register(registry);

        Gauge.builder("slo.http.latency.p99.ms", this::computeP99Latency)
                .description("99th percentile HTTP latency in milliseconds")
                .baseUnit("ms")
                .register(registry);
    }

    private double computeErrorRatio() {
        double total = 0;
        double errors = 0;
        for (Timer timer : registry.find("http.server.requests").timers()) {
            double count = timer.count();
            total += count;
            String status = timer.getId().getTag("status");
            if (status != null && status.startsWith("5")) {
                errors += count;
            }
        }
        return total > 0 ? errors / total : 0.0;
    }

    private double computeP99Latency() {
        double maxP99 = 0;
        for (Timer timer : registry.find("http.server.requests").timers()) {
            double p99 = timer.percentile(0.99, TimeUnit.MILLISECONDS);
            if (p99 > maxP99) {
                maxP99 = p99;
            }
        }
        return maxP99;
    }
}
