package com.kingdom.yami.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
class InternalRouteBlockConfig {

	@Bean
	@ConditionalOnProperty(prefix = "gateway.routes", name = "blockInternal", havingValue = "true", matchIfMissing = true)
	RouterFunction<ServerResponse> blockInternalRoutes() {
		return route(path("/ymb/internal/**"), req -> ServerResponse.notFound().build());
	}
}
