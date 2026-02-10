package io.github.arun0009.observability.core;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            HttpServletResponse httpResponse = (response instanceof jakarta.servlet.http.HttpServletResponse)
                    ? (jakarta.servlet.http.HttpServletResponse) response
                    : null;

            try {
                // 1. Static / service-level context
                MDC.put(MdcKeys.SERVICE_NAME, serviceName);
                MDC.put(MdcKeys.ENVIRONMENT, environment);

                // 2. Request-scoped identity
                // Request ID
                String requestId = httpRequest.getHeader(MdcKeys.HEADER_REQUEST_ID);
                if (requestId == null || requestId.isEmpty()) {
                    requestId = UUID.randomUUID().toString();
                }
                MDC.put(MdcKeys.REQUEST_ID, requestId);

                // Response Injection: X-Request-ID
                if (httpResponse != null) {
                    httpResponse.setHeader(MdcKeys.HEADER_REQUEST_ID, requestId);
                }

                putHeaderIfPresent(httpRequest, MdcKeys.HEADER_USER_ID, MdcKeys.USER_ID);
                putHeaderIfPresent(httpRequest, MdcKeys.HEADER_TENANT_ID, MdcKeys.TENANT_ID);
                putHeaderIfPresent(httpRequest, MdcKeys.HEADER_CORRELATION_ID, MdcKeys.CORRELATION_ID);

                // 3. Trace ID (Micrometer/OTel specific)
                // Try to grab it if already populated by OTel filter upstream
                String traceId = MDC.get(MdcKeys.TRACE_ID);
                if (traceId != null && httpResponse != null) {
                    httpResponse.setHeader("X-Trace-ID", traceId);
                }

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

    private void putHeaderIfPresent(HttpServletRequest request, String headerName, String mdcKey) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isEmpty()) {
            MDC.put(mdcKey, value);
        }
    }
}
