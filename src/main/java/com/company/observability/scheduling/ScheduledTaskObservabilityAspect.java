package com.company.observability.scheduling;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * AOP aspect that instruments {@code @Scheduled} methods with:
 * <ul>
 * <li>MDC population (service context + a synthetic requestId for
 * correlation)</li>
 * <li>Duration metric ({@code scheduled.task.duration})</li>
 * <li>Error counting ({@code scheduled.task.errors})</li>
 * </ul>
 */
@Aspect
public class ScheduledTaskObservabilityAspect {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskObservabilityAspect.class);

    private final MeterRegistry meterRegistry;
    private final String serviceName;
    private final String environment;

    public ScheduledTaskObservabilityAspect(MeterRegistry meterRegistry,
            String serviceName,
            String environment) {
        this.meterRegistry = meterRegistry;
        this.serviceName = serviceName;
        this.environment = environment;
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object instrumentScheduledTask(ProceedingJoinPoint joinPoint) throws Throwable {
        String taskName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();

        // Populate MDC for the scheduled thread
        MDC.put("service", serviceName);
        MDC.put("env", environment);
        MDC.put("requestId", "scheduled-" + UUID.randomUUID());
        MDC.put("scheduledTask", taskName);

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.info("Scheduled task started: {}", taskName);
            Object result = joinPoint.proceed();
            log.info("Scheduled task completed: {}", taskName);
            return result;
        } catch (Throwable t) {
            meterRegistry.counter("scheduled.task.errors", "task", taskName).increment();
            log.error("Scheduled task failed: {}", taskName, t);
            throw t;
        } finally {
            sample.stop(Timer.builder("scheduled.task.duration")
                    .tag("task", taskName)
                    .register(meterRegistry));
            MDC.clear();
        }
    }
}
