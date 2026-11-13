package com.xingguang.delivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 跨域配置（H3 修复）：白名单化，不再允许任意来源携带凭据跨域。
 *
 * <p>默认放行本机四个前端 dev server；生产环境通过
 * {@code XG_CORS_ORIGINS}（逗号分隔）注入正式域名，默认白名单自动失效。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] DEFAULT_ORIGINS = {
            "http://localhost:5173", "http://127.0.0.1:5173",
            "http://localhost:8081", "http://127.0.0.1:8081",
            "http://localhost:8082", "http://127.0.0.1:8082",
            "http://localhost:8086", "http://127.0.0.1:8086"
    };

    @Value("${xingguang.cors.allowed-origins:}")
    private String configuredOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(origins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Idempotency-Key")
                .allowCredentials(true)
                .maxAge(3600);
    }

    private String[] origins() {
        if (configuredOrigins != null && !configuredOrigins.isBlank()) {
            return Arrays.stream(configuredOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        }
        return DEFAULT_ORIGINS;
    }
}
