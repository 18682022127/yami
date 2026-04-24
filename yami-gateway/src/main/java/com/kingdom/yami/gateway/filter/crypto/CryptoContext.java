package com.kingdom.yami.gateway.filter.crypto;

public record CryptoContext(
    String sessionId,
    String sessionKey,
    String nonce,
    long tsSeconds,
    byte[] aad
) {
}
