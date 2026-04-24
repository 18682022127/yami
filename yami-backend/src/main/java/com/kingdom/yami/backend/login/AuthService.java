package com.kingdom.yami.backend.login;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.kingdom.yami.backend.common.config.JwtProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;

@Service
public class AuthService {

	private static final String FIXED_CODE = "123456";
	private static final Duration SESSION_KEY_TTL = Duration.ofDays(7);
	private static final String TOKEN_PREFIX = "token:";
	private static final String SESSION_KEY_PREFIX = "sess:";
	private static final String SESSION_KEY_SUFFIX = ":key";

	private final StringRedisTemplate redisTemplate;
	private final JwtProperties jwtProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
		this.redisTemplate = redisTemplate;
		this.jwtProperties = jwtProperties;
	}

	public LoginResponse loginWithPhoneCode(String phone, String code) {
		if (phone == null || phone.isBlank() || code == null || code.isBlank()) {
			throw new IllegalArgumentException("invalid");
		}
		if (!FIXED_CODE.equals(code)) {
			throw new IllegalArgumentException("invalid");
		}

		String userId = phone;
		Instant now = Instant.now();
		Instant exp = now.plusMillis(jwtProperties.ttlMs());

		String token = Jwts.builder()
				.subject(userId)
				.issuedAt(java.util.Date.from(now))
				.expiration(java.util.Date.from(exp))
				.signWith(jwtProperties.secretKey(), Jwts.SIG.HS256)
				.compact();

		String sessionId = UUID.randomUUID().toString();
		byte[] key = new byte[32];
		secureRandom.nextBytes(key);
		String sessionEncKey = Base64.getEncoder().encodeToString(key);

		recordSessionKey(sessionId, sessionEncKey);
		recordToken(token, userId);

		return new LoginResponse(token, sessionId, sessionEncKey);
	}

	public boolean checkToken(String token) {
		if (token == null || token.isBlank()) {
			return false;
		}

		try {
			Jwts.parser()
					.verifyWith(jwtProperties.secretKey())
					.build()
					.parseSignedClaims(token);
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}

		String redisKey = TOKEN_PREFIX + token;
		String stored = redisTemplate.opsForValue().get(redisKey);
		return stored != null && !stored.isBlank();
	}

	public String getSessionEncKeyBySessionId(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			throw new IllegalArgumentException("invalid");
		}
		String redisKey = SESSION_KEY_PREFIX + sessionId + SESSION_KEY_SUFFIX;
		String key = redisTemplate.opsForValue().get(redisKey);
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("invalid");
		}
		return key;
	}

	private void recordSessionKey(String sessionId, String sessionEncKey) {
		redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId + SESSION_KEY_SUFFIX, sessionEncKey, SESSION_KEY_TTL);
	}

	private void recordToken(String token, String userId) {
		redisTemplate.opsForValue().set(TOKEN_PREFIX + token, userId, Duration.ofMillis(jwtProperties.ttlMs()));
	}

	public record LoginResponse(String token, String sessionId, String sessionEncKey) {
	}
}
