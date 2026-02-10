package com.company.observability.async;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Copies the MDC context from the calling thread to the async worker thread.
 * Without this, all MDC keys (traceId, requestId, userId, etc.) are lost
 * when work is dispatched to {@code @Async} methods or thread pools.
 * <p>
 * Note: OTel trace context propagation is handled separately by the
 * {@code ContextPropagatingTaskDecorator} from micrometer-context-propagation.
 * This decorator focuses on MDC (log enrichment) specifically.
 */
public class ObservabilityTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture the MDC context on the calling thread
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();

        return () -> {
            if (callerMdc != null) {
                MDC.setContextMap(callerMdc);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
