package com.riskhub.common.config;

import com.riskhub.common.interceptor.ApiTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiTokenInterceptor apiTokenInterceptor;

    @Value("${riskhub.auth.enabled:true}")
    private boolean authEnabled;

    public WebConfig(ApiTokenInterceptor apiTokenInterceptor) {
        this.apiTokenInterceptor = apiTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (authEnabled) {
            registry.addInterceptor(apiTokenInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns(
                            "/actuator/**",
                            "/api/v1/metrics/**",
                            "/api/v1/rules",
                            "/api/v1/policies",
                            "/api/v1/review/tasks"
                    );
        }
    }
}
