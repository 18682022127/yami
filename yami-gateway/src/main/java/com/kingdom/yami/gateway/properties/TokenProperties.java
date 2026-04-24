package com.kingdom.yami.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.token")
public record TokenProperties(
	boolean enabled,
	List<String> skipPaths,
	String header,
	String bearerPrefix,
	String checkUrl
) {
	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	public TokenProperties {
		if (skipPaths == null) {
			skipPaths = List.of();
		}
		if (header == null || header.isBlank()) {
			header = "Authorization";
		}
		if (bearerPrefix == null || bearerPrefix.isBlank()) {
			bearerPrefix = "Bearer";
		}
	}

	public boolean shouldSkip(String path) {
		if (!enabled) {
			return true;
		}
		return skipPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
	}
}
