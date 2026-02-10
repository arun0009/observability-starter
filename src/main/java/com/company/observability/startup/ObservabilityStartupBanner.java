package com.company.observability.startup;

import com.company.observability.autoconfigure.ObservabilityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

/**
 * Prints a beautiful banner on startup showing observability status.
 */
public class ObservabilityStartupBanner implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityStartupBanner.class);

    private final ObservabilityProperties properties;
    private final Environment env;
    private final String serviceName;

    public ObservabilityStartupBanner(ObservabilityProperties properties,
            Environment env,
            @Value("${spring.application.name:unknown}") String serviceName) {
        this.properties = properties;
        this.env = env;
        this.serviceName = serviceName;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null)
            version = "dev";

        log.info("\n" +
                "========================================================================================\n" +
                "   🚀 OBSERVABILITY ACTIVE   ::   ServiceName: {}\n" +
                "========================================================================================\n" +
                "   Profile    : {}\n" +
                "   Tracing    : [Sample Rate: {}] {}\n" +
                "   Async Prop : {}\n" +
                "   TraceGuard : [Fail-Fast: {}]\n" +
                "   Logging    : [JSON + PiiMasking]\n" +
                "========================================================================================",
                serviceName,
                String.join(",", env.getActiveProfiles()),
                properties.getSampling().getProbability(),
                properties.getSampling().getProbability() == 1.0 ? "(Full)" : "(Sampled)",
                properties.getAsync().isPropagationEnabled() ? "ENABLED" : "DISABLED",
                properties.getTraceGuard().isFailOnMissing());
    }
}
