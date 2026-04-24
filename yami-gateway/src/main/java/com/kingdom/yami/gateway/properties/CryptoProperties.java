package com.kingdom.yami.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.crypto")
public record CryptoProperties(
    boolean enabled,
    List<String> skipPaths,
    String sessionIdHeader,
    String sessionKeyUrl
) {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public CryptoProperties {
        if (sessionIdHeader == null || sessionIdHeader.isBlank()) {
            sessionIdHeader = "X-Session-Id";
        }
        if (skipPaths == null) {
            skipPaths = List.of();
        }
    }

    public boolean shouldSkip(String path) {
        if (!enabled) {
            return true;
        }
        return skipPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
