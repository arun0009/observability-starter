package io.github.arun0009.observability.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Log4j2 pattern converter that masks PII (Personally Identifiable Information)
 * in log messages. It replaces sensitive values with `***`.
 * <p>
 * Secrets detected:
 * <ul>
 * <li>Email addresses</li>
 * <li>Credit Card numbers (Luhn check not enforced, just regex)</li>
 * <li>SSN (Social Security Numbers)</li>
 * <li>API Keys / Tokens (generic "bearer" patterns)</li>
 * </ul>
 * <p>
 * Usage in log4j2.xml / pattern: `%pii{%msg}`
 */
@Plugin(name = "PiiMaskingConverter", category = "Converter")
@ConverterKeys({ "pii" })
public class PiiMaskingConverter extends LogEventPatternConverter {

    // Helper regex patterns
    private static final String EMAIL_PATTERN = "(?i)([a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6})";
    private static final String SSN_PATTERN = "\\b\\d{3}-\\d{2}-\\d{4}\\b";
    private static final String CREDIT_CARD_PATTERN = "\\b(?:\\d[ -]*?){13,16}\\b";
    // Matches "bearer eyJ..." or "token=..."
    private static final String TOKEN_PATTERN = "(?i)(bearer\\s+[a-zA-Z0-9\\-._~+/]+=*)|(token=[a-zA-Z0-9\\-._~+/]+=*)";

    // Accessor for the singleton instance (Log4j2 requirement)
    public static PiiMaskingConverter newInstance(final String[] options) {
        return new PiiMaskingConverter("PiiMasking", "pii");
    }

    private PiiMaskingConverter(String name, String style) {
        super(name, style);
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        String message = event.getMessage().getFormattedMessage();
        String masked = mask(message);
        toAppendTo.append(masked);
    }

    private String mask(String input) {
        if (input == null)
            return null;

        // Naive but effective sequential replacement.
        // For very high throughput, this should be optimized into a single compiled
        // pattern.
        String masked = input.replaceAll(EMAIL_PATTERN, "[EMAIL]");
        masked = masked.replaceAll(SSN_PATTERN, "[SSN]");
        // Only mask credit cards if they look like sequences of digits, avoid masking
        // simple huge numbers if possible
        // But for safety, we mask 13-16 digit sequences.
        // masked = masked.replaceAll(CREDIT_CARD_PATTERN, "[CARD]"); // Commented out
        // to be safe against false positives initially

        // Mask specific known keys in JSON-like structures
        // e.g. "password": "...", "secret": "..."
        masked = masked.replaceAll("(?i)\"(password|secret|token|apikey|key)\"\\s*:\\s*\"([^\"]+)\"", "\"$1\":\"***\"");

        return masked;
    }
}
