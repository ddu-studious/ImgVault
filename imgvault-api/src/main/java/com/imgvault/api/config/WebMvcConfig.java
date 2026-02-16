package com.imgvault.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - 注册 Admin 认证拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${admin.jwt.secret:imgvault-jwt-secret-key-2026}")
    private String jwtSecret;

    @Value("${admin.jwt.expire-hours:24}")
    private int expireHours;

    @Bean
    public AdminTokenUtil adminTokenUtil() {
        return new AdminTokenUtil(jwtSecret, expireHours);
    }

    @Bean
    public AdminAuthInterceptor adminAuthInterceptor() {
        return new AdminAuthInterceptor(adminTokenUtil());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor())
                .addPathPatterns("/api/v1/admin/**")
                .excludePathPatterns("/api/v1/admin/login");
    }
}
