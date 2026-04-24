package com.kingdom.yami.backend.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, long ttlMs) {

	public SecretKey secretKey() {
		return io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
}
