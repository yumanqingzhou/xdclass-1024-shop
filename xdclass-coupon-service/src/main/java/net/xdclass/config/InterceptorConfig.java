package net.xdclass.config;

import net.xdclass.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/coupon_record/*/**")
                .addPathPatterns("/api/coupon/*/**")
                .excludePathPatterns("/api/coupon/*/page_coupon","/api/coupon/*/new_user_coupon"
                       );//查看所有优惠券接口不拦截
        WebMvcConfigurer.super.addInterceptors(registry);

    }
}
