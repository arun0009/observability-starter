package com.company.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Exports Git version info as metrics so you can correlate performance
 * with specific deployments/commits on your dashboard.
 * <p>
 * Metric: {@code app.info}
 * Tags: {@code version}, {@code commit}, {@code branch}
 * Value: 1.0
 */
@Configuration(proxyBeanMethods = false)
public class GitInfoMetricsConfiguration {

    public GitInfoMetricsConfiguration(MeterRegistry registry, GitProperties gitProperties) {
        Gauge.builder("app.info", () -> 1)
                .description("Application version and git commit info")
                .tag("version",
                        gitProperties.get("build.version") != null ? gitProperties.get("build.version") : "unknown")
                .tag("commit", gitProperties.getShortCommitId())
                .tag("branch", gitProperties.getBranch() != null ? gitProperties.getBranch() : "unknown")
                .register(registry);
    }
}
