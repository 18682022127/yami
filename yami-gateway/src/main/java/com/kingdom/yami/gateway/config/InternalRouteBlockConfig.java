package com.kingdom.yami.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Minimal in-code routing guard so tests can validate that internal endpoints are not exposed
 * via the gateway, even though production routes are configured in Nacos.
 */
@Configuration
public class InternalRouteBlockConfig {

    @Bean
    public RouterFunction<ServerResponse> internalRouteBlockRouter() {
        return RouterFunctions.route()
                .path("/ymb/internal", builder -> builder
                        .GET("/**", request -> ServerResponse.notFound().build())
                        .POST("/**", request -> ServerResponse.notFound().build())
                        .PUT("/**", request -> ServerResponse.notFound().build())
                        .PATCH("/**", request -> ServerResponse.notFound().build())
                        .DELETE("/**", request -> ServerResponse.notFound().build())
                        .OPTIONS("/**", request -> ServerResponse.notFound().build())
                        .build())
                .build();
    }
}
