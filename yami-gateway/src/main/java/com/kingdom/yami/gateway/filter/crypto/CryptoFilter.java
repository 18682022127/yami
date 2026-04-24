package com.kingdom.yami.gateway.filter.crypto;

import cn.hutool.json.JSONUtil;
import com.kingdom.yami.common.exception.CryptoException;
import com.kingdom.yami.common.exception.HeaderValidException;
import com.kingdom.yami.common.web.ApiResponse;
import com.kingdom.yami.gateway.properties.CryptoProperties;
import com.kingdom.yami.gateway.tools.AesGcmCrypto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(10)
public class CryptoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CryptoFilter.class);

    private final CryptoProperties cryptoProperties;
    private final SessionKeyRepository sessionKeyRepository;
    private final CryptoContextResolver contextResolver;

    public CryptoFilter(CryptoProperties cryptoProperties,
                       SessionKeyRepository sessionKeyRepository,
                       CryptoContextResolver contextResolver) {
        this.cryptoProperties = cryptoProperties;
        this.sessionKeyRepository = sessionKeyRepository;
        this.contextResolver = contextResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (cryptoProperties.shouldSkip(path)) {
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(request, wrappedResponse);
            wrapAndWriteJsonResponse(wrappedResponse);
            return;
        }

        String sessionId = request.getHeader(cryptoProperties.sessionIdHeader());
        if (sessionId == null || sessionId.isBlank()) {
            sendError(response, HttpStatus.UNAUTHORIZED, "请求非法");
            return;
        }

        String sessionKey = sessionKeyRepository.getSessionKey(sessionId);
        if (sessionKey == null) {
            sendError(response, HttpStatus.UNAUTHORIZED, "请求非法");
            return;
        }

        try {
            CryptoContext context = contextResolver.resolve(request, sessionId, sessionKey);

            HttpServletRequest requestToPass = maybeDecryptRequest(context, request, response);
            if (requestToPass == null) {
                return;
            }

            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(requestToPass, wrappedResponse);
            encryptAndWriteResponse(wrappedResponse, context.sessionKey(), context.aad());

        } catch (HeaderValidException e) {
            sendError(response, HttpStatus.BAD_REQUEST, "请求非法");
        } catch (CryptoException e) {
            log.error("Decryption failed for session: {}", sessionId, e);
            sendError(response, HttpStatus.BAD_REQUEST, "请求非法");
        } catch (Exception e) {
            log.error("Unexpected error in crypto filter", e);
            sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, "系统错误");
        }
    }


    private HttpServletRequest maybeDecryptRequest(CryptoContext context,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) throws IOException {
        if (!shouldDecryptBody(request)) {
            return request;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String encryptedBody = StreamUtils.copyToString(cachedRequest.getInputStream(), StandardCharsets.UTF_8);

        if (encryptedBody.isBlank()) {
            sendEncryptedError(response, "请求非法", context.sessionKey(), context.aad());
            return null;
        }

        String decryptedJson = AesGcmCrypto.decrypt(encryptedBody, context.sessionKey(), context.aad());

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(
            cachedRequest,
            decryptedJson.getBytes(StandardCharsets.UTF_8)
        );
        wrappedRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return wrappedRequest;
    }


    private void encryptAndWriteResponse(ContentCachingResponseWrapper responseWrapper,
                                        String sessionKey,
                                        byte[] aad) throws IOException {
        byte[] responseBody = responseWrapper.getContentAsByteArray();

        if (responseBody.length == 0) {
            responseWrapper.copyBodyToResponse();
            return;
        }

        String plainResponse = new String(responseBody, StandardCharsets.UTF_8);
        String encryptedResponse = AesGcmCrypto.encrypt(plainResponse, sessionKey, aad);

        responseWrapper.resetBuffer();
        responseWrapper.setContentType(MediaType.TEXT_PLAIN_VALUE);
        responseWrapper.setCharacterEncoding(StandardCharsets.UTF_8.name());
        responseWrapper.getResponse().getOutputStream()
            .write(encryptedResponse.getBytes(StandardCharsets.UTF_8));
    }


    private void wrapAndWriteJsonResponse(ContentCachingResponseWrapper responseWrapper) throws IOException {
        int status = responseWrapper.getStatus();

        Object wrapped;
        if (status < 200 || status >= 300) {
            wrapped = ApiResponse.fail("系统错误");
        } else {
            responseWrapper.copyBodyToResponse();
            return;
        }

        responseWrapper.resetBuffer();
        responseWrapper.setStatus(HttpStatus.OK.value());
        responseWrapper.setCharacterEncoding(StandardCharsets.UTF_8.name());
        responseWrapper.setContentType(MediaType.APPLICATION_JSON_VALUE);
        responseWrapper.getOutputStream().write(JSONUtil.toJsonStr(wrapped).getBytes(StandardCharsets.UTF_8));
        responseWrapper.copyBodyToResponse();
    }

    private boolean shouldDecryptBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
            || "PUT".equalsIgnoreCase(method)
            || "PATCH".equalsIgnoreCase(method);
    }

    private void sendEncryptedError(HttpServletResponse response,
                                    String message,
                                    String sessionKey,
                                    byte[] aad) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Object> body = ApiResponse.fail(message);
        String encrypted = AesGcmCrypto.encrypt(JSONUtil.toJsonStr(body), sessionKey, aad);
        response.getOutputStream().write(encrypted.getBytes(StandardCharsets.UTF_8));
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Object> fail = ApiResponse.fail(message);
        response.getOutputStream().write(JSONUtil.toJsonStr(fail).getBytes(StandardCharsets.UTF_8));
    }
}
