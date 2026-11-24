package com.empay.mars_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Bean
    RouteLocator customRoutes(RouteLocatorBuilder builder) {

        return builder.routes()
        		.route("auth", r -> r
                        .path("/api/auth/**")
                        .uri("http://localhost:8081"))

                .route("parameter-management", r -> r
                        .path("/api/params/**")
                        .uri("http://localhost:8082"))

                .route("tlf-processing", r-> r
                        .path("/api/tlf/**")
                        .uri("http://localhost:8083"))

                .route("user-management", r -> r
                        .path("/api/**")
                        .uri("http://localhost:8081"))

                .build();
    }
}