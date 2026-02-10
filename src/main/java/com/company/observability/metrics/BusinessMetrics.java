package com.company.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.function.Supplier;

/**
 * Simplified metrics facade for business teams.
 * <p>
 * Instead of wiring {@link MeterRegistry} directly, teams use this class
 * which enforces naming conventions (dot-separated lowercase) and
 * automatically adds common tags.
 * <p>
 * Usage:
 * 
 * <pre>
 * {@literal @}Autowired BusinessMetrics metrics;
 *
 * // Count business events
 * metrics.count("orders.placed", "region", "us-east");
 *
 * // Time business operations
 * Order order = metrics.timed("orders.processing", "type", "express",
 *     () -> orderService.processOrder(request));
 * </pre>
 */
public class BusinessMetrics {

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Increment a counter. Use dot-separated lowercase names.
     *
     * @param name the metric name (e.g., "orders.placed")
     * @param tags key-value pairs (e.g., "region", "us-east")
     */
    public void count(String name, String... tags) {
        Counter.builder(name)
                .tags(tags)
                .register(registry)
                .increment();
    }

    /**
     * Increment a counter by a specific amount.
     */
    public void count(String name, double amount, String... tags) {
        Counter.builder(name)
                .tags(tags)
                .register(registry)
                .increment(amount);
    }

    /**
     * Time a block of code and return its result.
     *
     * @param name     the metric name (e.g., "orders.processing")
     * @param tags     key-value pairs
     * @param supplier the code to time
     * @return the result of the supplier
     */
    public <T> T timed(String name, String[] tags, Supplier<T> supplier) {
        return Timer.builder(name)
                .tags(tags)
                .register(registry)
                .record(supplier);
    }

    /**
     * Time a Runnable (no return value).
     */
    public void timed(String name, String[] tags, Runnable runnable) {
        Timer.builder(name)
                .tags(tags)
                .register(registry)
                .record(runnable);
    }

    /**
     * Register a gauge backed by a supplier.
     *
     * @param name     the metric name (e.g., "queue.depth")
     * @param supplier provides the current value
     * @param tags     key-value pairs
     */
    public void gauge(String name, Supplier<Number> supplier, String... tags) {
        io.micrometer.core.instrument.Gauge.builder(name, supplier)
                .tags(tags)
                .register(registry);
    }

    /**
     * Access the underlying MeterRegistry for advanced use cases.
     */
    public MeterRegistry getRegistry() {
        return registry;
    }
}
