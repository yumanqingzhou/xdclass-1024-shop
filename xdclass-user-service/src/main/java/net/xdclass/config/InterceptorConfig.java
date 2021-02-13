package net.xdclass.config;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class InterceptorConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/user/*/**","/api/address/*/**")
                .excludePathPatterns("/api/user/*/send_code","/api/user/*/captcha",
                "/api/user/*/register","/api/user/*/login","/api/user/*/upload");
    }
}
