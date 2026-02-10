package com.company.observability.audit;

import com.company.observability.core.MdcKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Standardized audit logger for security and compliance events.
 * <p>
 * Uses a dedicated logger name ({@code AUDIT}) so that audit events can be
 * routed to a separate appender (e.g., a dedicated Kafka topic or S3 bucket)
 * without polluting application logs.
 * <p>
 * Usage:
 * 
 * <pre>
 * {@literal @}Autowired AuditLogger auditLogger;
 * auditLogger.log("USER_LOGIN", "user@example.com", "/api/login", "SUCCESS");
 * </pre>
 * <p>
 * Output (JSON):
 * 
 * <pre>
 * {
 *   "message": "AUDIT",
 *   "audit.action": "USER_LOGIN",
 *   "audit.actor": "user@example.com",
 *   "audit.resource": "/api/login",
 *   "audit.outcome": "SUCCESS",
 *   "traceId": "abc123...",
 *   "requestId": "..."
 * }
 * </pre>
 */
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    /**
     * Log an audit event with standard fields.
     *
     * @param action   the action performed (e.g., USER_LOGIN, DATA_EXPORT,
     *                 PERMISSION_CHANGE)
     * @param actor    who performed the action (userId, service name, etc.)
     * @param resource the resource acted upon (URL, entity ID, etc.)
     * @param outcome  the result (SUCCESS, FAILURE, DENIED)
     */
    public void log(String action, String actor, String resource, String outcome) {
        MDC.put("audit.action", action);
        MDC.put("audit.actor", actor);
        MDC.put("audit.resource", resource);
        MDC.put("audit.outcome", outcome);
        try {
            auditLog.info("AUDIT event={} actor={} resource={} outcome={}",
                    action, actor, resource, outcome);
        } finally {
            MDC.remove("audit.action");
            MDC.remove("audit.actor");
            MDC.remove("audit.resource");
            MDC.remove("audit.outcome");
        }
    }

    /**
     * Log an audit event with additional detail.
     *
     * @param action   the action performed
     * @param actor    who performed the action
     * @param resource the resource acted upon
     * @param outcome  the result
     * @param detail   additional context
     */
    public void log(String action, String actor, String resource, String outcome, String detail) {
        MDC.put("audit.action", action);
        MDC.put("audit.actor", actor);
        MDC.put("audit.resource", resource);
        MDC.put("audit.outcome", outcome);
        MDC.put("audit.detail", detail);
        try {
            auditLog.info("AUDIT event={} actor={} resource={} outcome={} detail={}",
                    action, actor, resource, outcome, detail);
        } finally {
            MDC.remove("audit.action");
            MDC.remove("audit.actor");
            MDC.remove("audit.resource");
            MDC.remove("audit.outcome");
            MDC.remove("audit.detail");
        }
    }
}
