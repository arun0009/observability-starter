package com.company.observability.core;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Populates the SLF4J/Log4j2 MDC with standard keys on every request.
 * Runs early in the filter chain so that all downstream logging automatically
 * includes tracing, identity, and service metadata.
 * <p>
 * Extension: register {@link MdcContributor} beans to add custom keys.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class MdcFilter implements Filter {

    private final String serviceName;
    private final String environment;
    private final List<MdcContributor> contributors;

    public MdcFilter(String serviceName, String environment, List<MdcContributor> contributors) {
        this.serviceName = serviceName;
        this.environment = environment;
        this.contributors = contributors != null ? contributors : List.of();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            try {
                // 1. Static / service-level context
                MDC.put(MdcKeys.SERVICE_NAME, serviceName);
                MDC.put(MdcKeys.ENVIRONMENT, environment);

                // 2. Request-scoped identity
                putHeaderOrGenerate(httpRequest, MdcKeys.HEADER_REQUEST_ID, MdcKeys.REQUEST_ID);
                putHeaderIfPresent(httpRequest, MdcKeys.HEADER_USER_ID, MdcKeys.USER_ID);
                putHeaderIfPresent(httpRequest, MdcKeys.HEADER_TENANT_ID, MdcKeys.TENANT_ID);
                putHeaderIfPresent(httpRequest, MdcKeys.HEADER_CORRELATION_ID, MdcKeys.CORRELATION_ID);

                // 3. TraceId / SpanId are populated by micrometer-tracing / OTel bridge.
                // We do NOT set them manually to avoid conflicts.

                // 4. Custom extensions
                for (MdcContributor contributor : contributors) {
                    contributor.contribute(httpRequest);
                }

                chain.doFilter(request, response);
            } finally {
                MDC.clear();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void putHeaderOrGenerate(HttpServletRequest request, String headerName, String mdcKey) {
        String value = request.getHeader(headerName);
        MDC.put(mdcKey, (value != null && !value.isEmpty()) ? value : UUID.randomUUID().toString());
    }

    private void putHeaderIfPresent(HttpServletRequest request, String headerName, String mdcKey) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isEmpty()) {
            MDC.put(mdcKey, value);
        }
    }
}
