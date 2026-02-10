package io.github.arun0009.observability.exception;

import io.github.arun0009.observability.core.MdcKeys;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global exception handler that enriches unhandled exceptions with
 * full MDC context and marks the active OTel span as ERROR.
 * <p>
 * This ensures that:
 * <ul>
 * <li>Every unhandled exception is logged with traceId, userId, requestId</li>
 * <li>The span in your tracing backend is marked as failed</li>
 * <li>The client receives a standard RFC 7807 Problem Detail response</li>
 * </ul>
 */
@RestControllerAdvice
public class ObservabilityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex) {
        // 1. Enrich the current span
        Span span = Span.current();
        span.setStatus(StatusCode.ERROR, ex.getMessage());
        span.recordException(ex);

        // 2. Log with full MDC context (traceId, userId, requestId are already in MDC)
        log.error("Unhandled exception [user={}, request={}, correlation={}]: {}",
                MDC.get(MdcKeys.USER_ID),
                MDC.get(MdcKeys.REQUEST_ID),
                MDC.get(MdcKeys.CORRELATION_ID),
                ex.getMessage(),
                ex);

        // 3. Return RFC 7807 Problem Detail (no internal details leaked)
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred. Reference: " + MDC.get(MdcKeys.REQUEST_ID));
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("urn:error:internal"));
        // Include requestId so the caller can quote it to support
        problem.setProperty("requestId", MDC.get(MdcKeys.REQUEST_ID));
        problem.setProperty("traceId", MDC.get(MdcKeys.TRACE_ID));

        return problem;
    }
}
