package com.company.observability.guardrails;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Sampler.class, Resource.class })
public class ObservabilityGuardrailsConfiguration {

    @Bean
    public Sampler defaultSampler(@Value("${observability.sampling.probability:1.0}") double probability) {
        // Enforce ParentBased(TraceIdRatioBased) sampler
        // This ensures if a parent trace exists, it is respected.
        // If it's a new trace, it samples based on probability.
        return Sampler.parentBased(Sampler.traceIdRatioBased(probability));
    }

    // Resource Attributes are usually handled by Spring Boot's
    // OtelResourceAttributes,
    // but if we want to enforce specific ones or merge them:
    @Bean
    public Resource openTelemetryResource(@Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${app.env:unknown-env}") String environment) {
        return Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.DEPLOYMENT_ENVIRONMENT, environment)));
    }
}
