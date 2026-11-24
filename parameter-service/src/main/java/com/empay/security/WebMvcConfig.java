package com.empay.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(jwtAuthenticationInterceptor)

                // Apply authentication to all controller APIs
                .addPathPatterns("/**")

                // Exclude public and documentation endpoints
                .excludePathPatterns(
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}