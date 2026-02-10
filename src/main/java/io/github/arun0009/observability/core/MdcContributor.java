package io.github.arun0009.observability.core;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SPI for teams to add custom MDC keys.
 * <p>
 * Register a bean implementing this interface to have your custom keys
 * automatically populated on every request by the {@link MdcFilter}.
 * <p>
 * Example:
 * 
 * <pre>
 * {@literal @}Component
 * public class MyMdcContributor implements MdcContributor {
 *     {@literal @}Override
 *     public void contribute(HttpServletRequest request) {
 *         String region = request.getHeader("X-Region");
 *         if (region != null) {
 *             MDC.put("region", region);
 *         }
 *     }
 * }
 * </pre>
 */
@FunctionalInterface
public interface MdcContributor {

    /**
     * Populate additional MDC keys from the incoming request.
     * Called after standard keys (traceId, spanId, userId, etc.) are set.
     * Do NOT call {@code MDC.clear()} — that is handled by the filter.
     *
     * @param request the current HTTP request
     */
    void contribute(HttpServletRequest request);
}
