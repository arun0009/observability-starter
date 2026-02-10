package com.company.observability.core;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.List;

/**
 * Guardrail: Detects if incoming requests are missing trace propagation
 * headers.
 * Useful for finding broken upstream callers.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 101) // Run just after MdcFilter
public class TraceGuardFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TraceGuardFilter.class);
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String B3_HEADER = "X-B3-TraceId";

    private final MeterRegistry meterRegistry;
    private final boolean failOnMissingTrace;

    public TraceGuardFilter(MeterRegistry meterRegistry, boolean failOnMissingTrace) {
        this.meterRegistry = meterRegistry;
        this.failOnMissingTrace = failOnMissingTrace;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            boolean hasTrace = httpRequest.getHeader(TRACEPARENT_HEADER) != null
                    || httpRequest.getHeader(B3_HEADER) != null;

            if (!hasTrace) {
                // Record metric
                meterRegistry.counter("observability.trace.missing",
                        List.of(Tag.of("path", httpRequest.getRequestURI()))).increment();

                if (failOnMissingTrace) {
                    throw new ServletException("Missing required trace propagation headers. " +
                            "Ensure upstream service propagates context.");
                }

                log.warn("Missing trace headers for request: {}", httpRequest.getRequestURI());
            }
        }

        chain.doFilter(request, response);
    }
}
