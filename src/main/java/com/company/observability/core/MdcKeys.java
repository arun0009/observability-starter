package com.company.observability.core;

/**
 * Centralized, versioned MDC key constants for the enterprise.
 * <p>
 * All services MUST use these constants rather than raw strings.
 * This class serves as the single source-of-truth for log field names.
 */
public final class MdcKeys {

    private MdcKeys() {
    }

    // ── Identity ──────────────────────────────────────────────
    public static final String USER_ID = "userId";
    public static final String TENANT_ID = "tenantId";

    // ── Request ───────────────────────────────────────────────
    public static final String REQUEST_ID = "requestId";
    public static final String CORRELATION_ID = "correlationId";

    // ── Tracing (READ-ONLY — populated by Micrometer, not by us) ─────
    // These constants exist so other code can reference the canonical
    // key names (e.g., in JSON layout, exception handler) without
    // hard-coding strings. Do NOT call MDC.put() with these keys.
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";

    // ── Service ───────────────────────────────────────────────
    public static final String SERVICE_NAME = "service";
    public static final String ENVIRONMENT = "env";

    // ── Inbound Header Names ──────────────────────────────────
    public static final String HEADER_USER_ID = "X-User-ID";
    public static final String HEADER_TENANT_ID = "X-Tenant-ID";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
}
