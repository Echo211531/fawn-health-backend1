package com.ljh.fawnhealth.config;

import com.ljh.fawnhealth.interceptor.JwtTokenAdminInterceptor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOriginPatterns("*") // 明确指定允许的来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只拦截后台接口，例如 /admin/** 或后台相关接口
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/foodDiary/**", "/file/**", "/coupons/**", "/userCoupon/**","/communityPosts/**",
                        "/foodLibrary/**","/vip/**", "/comment/**", "/ai/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/send",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/doc.html",
                        "/favicon.ico"
                );
    }
}
