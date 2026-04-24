package com.kingdom.yami.gateway.filter.crypto;

import com.kingdom.yami.common.exception.HeaderValidException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class CryptoContextResolver {

    private static final String NONCE_HEADER = "X-Nonce";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";

    private final StringRedisTemplate redisTemplate;
    private final long timestampSkewSeconds;

    public CryptoContextResolver(
        StringRedisTemplate redisTemplate,
        @Value("${gateway.crypto.timestamp-skew-seconds:60}") long timestampSkewSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.timestampSkewSeconds = timestampSkewSeconds;
    }

    public CryptoContext resolve(HttpServletRequest request, String sessionId, String sessionKey) {
        String nonce = requireHeader(request, NONCE_HEADER);
        long tsSeconds = requireTimestampSeconds(request);
        validateTimestamp(tsSeconds);
        checkAndStoreNonce(sessionId, nonce);

        byte[] aad = buildAad(request, sessionId, nonce, tsSeconds);
        return new CryptoContext(sessionId, sessionKey, nonce, tsSeconds, aad);
    }

    private String requireHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            throw new HeaderValidException("Missing header: " + headerName);
        }
        return value;
    }

    private long requireTimestampSeconds(HttpServletRequest request) {
        String raw = requireHeader(request, TIMESTAMP_HEADER);
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new HeaderValidException("Invalid X-Timestamp");
        }
    }

    private void validateTimestamp(long tsSeconds) {
        long now = Instant.now().getEpochSecond();
        long diff = Math.abs(now - tsSeconds);
        if (diff > timestampSkewSeconds) {
            throw new HeaderValidException("X-Timestamp out of range");
        }
    }

    private void checkAndStoreNonce(String sessionId, String nonce) {
        String key = "nonce:" + sessionId + ":" + nonce;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(timestampSkewSeconds * 2));
        if (ok == null || !ok) {
            throw new HeaderValidException("Replay detected");
        }
    }

    private byte[] buildAad(HttpServletRequest request, String sessionId, String nonce, long tsSeconds) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = (query == null || query.isBlank()) ? path : (path + "?" + query);
        String aad = method + "\n" + fullPath + "\n" + sessionId + "\n" + nonce + "\n" + tsSeconds;
        return aad.getBytes(StandardCharsets.UTF_8);
    }
}
