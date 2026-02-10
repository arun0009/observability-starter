package io.github.arun0009.observability.propagation;

import io.github.arun0009.observability.core.MdcKeys;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Map;

/**
 * Auto-configures an interceptor for OkHttp clients to propagate MDC context
 * (requestId, userId, tenantId, correlationId) as HTTP headers.
 * <p>
 * This configuration applies to any {@link OkHttpClient.Builder} beans managed
 * by Spring.
 * Consuming services must construct their client using the builder bean:
 * 
 * <pre>
 * {@literal @}Autowired OkHttpClient.Builder builder;
 * OkHttpClient client = builder.build();
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OkHttpClient.class)
public class OkHttpPropagationConfiguration {

    @Bean
    public OkHttpBuilderCustomizer mdcPropagationOkHttpCustomizer() {
        return builder -> builder.addInterceptor(new MdcPropagationInterceptor());
    }

    /**
     * Interface for customizing OkHttp builders, similar to RestTemplateCustomizer.
     * We define it here because Spring Boot doesn't have a built-in one for OkHttp.
     * Services can just inject the builder directly, but having a customizer
     * pattern
     * is cleaner if we want to support auto-applying to existing beans.
     * <p>
     * However, for simplicity in this starter, we'll just advise users to use the
     * builder pattern. If Spring Boot's OkHttp starter is used, it might have its
     * own.
     * <p>
     * Actually, the most robust way to instrument OkHttp in Spring Boot is to
     * provide a BeanPostProcessor that intercepts OkHttpClient.Builder beans.
     */
    @Bean
    public static OkHttpBuilderBeanPostProcessor okHttpBuilderBeanPostProcessor() {
        return new OkHttpBuilderBeanPostProcessor();
    }

    static class OkHttpBuilderBeanPostProcessor implements org.springframework.beans.factory.config.BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            if (bean instanceof OkHttpClient.Builder) {
                ((OkHttpClient.Builder) bean).addInterceptor(new MdcPropagationInterceptor());
            }
            return bean;
        }
    }

    static class MdcPropagationInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request.Builder builder = chain.request().newBuilder();
            Map<String, String> contextMap = MDC.getCopyOfContextMap();

            if (contextMap != null) {
                propagateIfPresent(contextMap, MdcKeys.USER_ID, MdcKeys.HEADER_USER_ID, builder);
                propagateIfPresent(contextMap, MdcKeys.REQUEST_ID, MdcKeys.HEADER_REQUEST_ID, builder);
                propagateIfPresent(contextMap, MdcKeys.TENANT_ID, MdcKeys.HEADER_TENANT_ID, builder);
                propagateIfPresent(contextMap, MdcKeys.CORRELATION_ID, MdcKeys.HEADER_CORRELATION_ID, builder);
            }

            return chain.proceed(builder.build());
        }

        private void propagateIfPresent(Map<String, String> contextMap, String mdcKey,
                String headerName, Request.Builder builder) {
            String value = contextMap.get(mdcKey);
            if (value != null) {
                builder.header(headerName, value);
            }
        }
    }

    // interface shim not strictly needed if we use BeanPostProcessor approach
    interface OkHttpBuilderCustomizer {
        void customize(OkHttpClient.Builder builder);
    }
}
