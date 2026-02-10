package io.github.arun0009.observability.autoconfigure;

import io.github.arun0009.observability.audit.AuditLogger;
import io.github.arun0009.observability.core.MdcContributor;
import io.github.arun0009.observability.core.MdcFilter;
import io.github.arun0009.observability.core.TraceGuardFilter;
import io.github.arun0009.observability.exception.ObservabilityExceptionHandler;
import io.github.arun0009.observability.metrics.BusinessMetrics;
import io.github.arun0009.observability.metrics.SloMetricsConfiguration;
import io.github.arun0009.observability.metrics.StandardMetricsConfiguration;
import io.github.arun0009.observability.async.ObservabilityExecutorConfiguration;
import io.github.arun0009.observability.kafka.KafkaTracingConfiguration;
import io.github.arun0009.observability.propagation.OkHttpPropagationConfiguration;
import io.github.arun0009.observability.propagation.RestTemplatePropagationConfiguration;
import io.github.arun0009.observability.propagation.WebClientPropagationConfiguration;
import io.github.arun0009.observability.guardrails.ObservabilityGuardrailsConfiguration;
import io.github.arun0009.observability.scheduling.ScheduledTaskObservabilityAspect;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.github.arun0009.observability.logging.PiiMaskingConverter;
import io.github.arun0009.observability.metrics.GitInfoMetricsConfiguration;
import io.github.arun0009.observability.metrics.ThreadPoolSaturationMetrics;
import io.github.arun0009.observability.startup.ObservabilityStartupBanner;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;

@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(ObservabilityProperties.class)
@Import({
        StandardMetricsConfiguration.class,
        SloMetricsConfiguration.class,
        RestTemplatePropagationConfiguration.class,
        WebClientPropagationConfiguration.class,
        OkHttpPropagationConfiguration.class,
        ObservabilityGuardrailsConfiguration.class,
        ObservabilityExecutorConfiguration.class,
        KafkaTracingConfiguration.class
})
public class ObservabilityAutoConfiguration {

    // ── Core Filters ─────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.mdc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MdcFilter mdcFilter(ObservabilityProperties properties,
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${app.env:unknown-env}") String environment,
            List<MdcContributor> contributors) {
        return new MdcFilter(serviceName, environment, contributors);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.trace-guard", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceGuardFilter traceGuardFilter(
            MeterRegistry meterRegistry,
            ObservabilityProperties properties) {
        return new TraceGuardFilter(meterRegistry, properties.getTraceGuard().isFailOnMissing());
    }

    // ── Metrics ──────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public BusinessMetrics businessMetrics(MeterRegistry meterRegistry) {
        return new BusinessMetrics(meterRegistry);
    }

    // ── Exception Handling ───────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ObservabilityExceptionHandler observabilityExceptionHandler() {
        return new ObservabilityExceptionHandler();
    }

    // ── Audit ────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuditLogger auditLogger() {
        return new AuditLogger();
    }

    // ── Scheduling ───────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public ScheduledTaskObservabilityAspect scheduledTaskObservabilityAspect(
            MeterRegistry meterRegistry,
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${app.env:unknown-env}") String environment) {
        return new ScheduledTaskObservabilityAspect(meterRegistry, serviceName, environment);
    }

    // ── Extras / WOW Features ────────────────────────────────

    @Bean
    @ConditionalOnBean(GitProperties.class)
    public GitInfoMetricsConfiguration gitInfoMetricsConfiguration(MeterRegistry registry,
            GitProperties gitProperties) {
        return new GitInfoMetricsConfiguration(registry, gitProperties);
    }

    @Bean
    public ThreadPoolSaturationMetrics threadPoolSaturationMetrics(MeterRegistry registry,
            Map<String, ThreadPoolTaskExecutor> executors) {
        return new ThreadPoolSaturationMetrics(registry, executors);
    }

    @Bean
    public ObservabilityStartupBanner observabilityStartupBanner(ObservabilityProperties properties,
            Environment env,
            @Value("${spring.application.name:unknown}") String serviceName) {
        return new ObservabilityStartupBanner(properties, env, serviceName);
    }
}
