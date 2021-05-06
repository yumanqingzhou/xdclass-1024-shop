package net.xdclass.config;

import feign.RequestInterceptor;
import feign.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;

@Configuration
@Slf4j
public class FeignInterceptorConfig implements WebMvcConfigurer {

    /**
     * 解决feign请求头丢失问题
     * @return
     */
    @Bean
    public RequestInterceptor requestInterceptor(){
        //RequestInterceptor 是一个接口 如果要定义feign请求的拦截器就要重写里面的apply 方法  template是apply的入参
        return template->{
            //获取容器请求上下文
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                if (request != null) {
                    return ;
                }
                String token = request.getHeader("token");
                template.header("token",token);
            }
        };
    }
}
