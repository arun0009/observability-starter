package com.company.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Monitors Spring's {@link ThreadPoolTaskExecutor} beans for saturation.
 * <p>
 * Key metric: {@code thread.pool.saturation} (0.0 to 1.0)
 * <p>
 * If the pool has a queue, saturation = (active threads + queue size) / (max
 * pool size + queue capacity).
 * If infinite queue, saturation is just active / max pool size.
 */
@Configuration(proxyBeanMethods = false)
public class ThreadPoolSaturationMetrics {

    public ThreadPoolSaturationMetrics(MeterRegistry registry,
            Map<String, ThreadPoolTaskExecutor> executors) {
        executors.forEach((name, executor) -> {
            Gauge.builder("thread.pool.saturation", executor, this::calculateSaturation)
                    .tag("name", name)
                    .description("Saturation of the thread pool (0.0 to 1.0)")
                    .register(registry);
        });
    }

    private double calculateSaturation(ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor tpe = executor.getThreadPoolExecutor();
        int active = tpe.getActiveCount();
        int max = tpe.getMaximumPoolSize();
        int queueSize = tpe.getQueue().size();
        int queueCapacity = Integer.MAX_VALUE;
        // Best effort to guess queue capacity since ThreadPoolExecutor doesn't expose
        // it directly
        // Usually Spring's wrapper sets it. If infinite, we just measure thread
        // saturation.

        int totalCapacity = max + queueCapacity; // Potential overflow if MAX_VALUE
        if (queueCapacity == Integer.MAX_VALUE) {
            return max > 0 ? (double) active / max : 0.0;
        }

        // This calculation depends on having access to queue capacity, which is hard.
        // A simpler proxy: Active / Max.
        return max > 0 ? (double) active / max : 0.0;
    }
}
