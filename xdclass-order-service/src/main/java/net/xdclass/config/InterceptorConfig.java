package net.xdclass.config;

import net.xdclass.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/order/*/**").excludePathPatterns("/api/callback/*/**",
                "/api/order/*/query_state",
                "/api/order/*/test_pay");
        WebMvcConfigurer.super.addInterceptors(registry);

    }
}
