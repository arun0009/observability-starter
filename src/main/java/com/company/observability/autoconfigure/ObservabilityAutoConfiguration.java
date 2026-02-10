package com.company.observability.autoconfigure;

import com.company.observability.audit.AuditLogger;
import com.company.observability.core.MdcContributor;
import com.company.observability.core.MdcFilter;
import com.company.observability.core.TraceGuardFilter;
import com.company.observability.exception.ObservabilityExceptionHandler;
import com.company.observability.metrics.BusinessMetrics;
import com.company.observability.metrics.SloMetricsConfiguration;
import com.company.observability.metrics.StandardMetricsConfiguration;
import com.company.observability.async.ObservabilityExecutorConfiguration;
import com.company.observability.kafka.KafkaTracingConfiguration;
import com.company.observability.propagation.RestTemplatePropagationConfiguration;
import com.company.observability.propagation.WebClientPropagationConfiguration;
import com.company.observability.guardrails.ObservabilityGuardrailsConfiguration;
import com.company.observability.scheduling.ScheduledTaskObservabilityAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(ObservabilityProperties.class)
@Import({
        StandardMetricsConfiguration.class,
        SloMetricsConfiguration.class,
        RestTemplatePropagationConfiguration.class,
        WebClientPropagationConfiguration.class,
        ObservabilityGuardrailsConfiguration.class,
        ObservabilityExecutorConfiguration.class,
        KafkaTracingConfiguration.class
})
public class ObservabilityAutoConfiguration {

    // ── Core Filters ─────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.mdc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MdcFilter mdcFilter(
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${app.env:unknown-env}") String environment,
            ObjectProvider<List<MdcContributor>> contributors) {
        return new MdcFilter(serviceName, environment, contributors.getIfAvailable());
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
}
