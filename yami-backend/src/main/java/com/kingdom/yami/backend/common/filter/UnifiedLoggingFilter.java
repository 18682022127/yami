package com.kingdom.yami.backend.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class UnifiedLoggingFilter extends OncePerRequestFilter {

	private static final int MAX_PAYLOAD_LENGTH = 5000;
	private static final int CACHE_LIMIT = 64 * 1024;
	private static final String TRACE_ID = "traceId";
	private static final String TRACE_HEADER = "X-Trace-Id";
	private static final String SESSION_HEADER = "X-Session-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, CACHE_LIMIT);
		ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

		String traceId = getOrCreateTraceId(request);
		response.setHeader(TRACE_HEADER, traceId);
		MDC.put(TRACE_ID, traceId);

		long start = System.currentTimeMillis();
		try {
			filterChain.doFilter(req, res);
		} catch (Exception e) {
			log.error("Exception | traceId={}", traceId, e);
			throw e;
		} finally {
			long cost = System.currentTimeMillis() - start;
			try {
				logRequestAndResponse(req, res, cost, traceId);
			} catch (Exception logEx) {
				log.error("LogError | traceId={}", traceId, logEx);
			}
			res.copyBodyToResponse();
			MDC.remove(TRACE_ID);
		}
	}

	private void logRequestAndResponse(ContentCachingRequestWrapper req,
									  ContentCachingResponseWrapper res,
									  long cost,
									  String traceId) {

		String uri = req.getRequestURI();
		if (uri.contains("/health") || uri.contains("/metrics")) {
			return;
		}

		String sessionId = firstHeader(req, SESSION_HEADER);
		String requestBody = buildRequestBody(req);
		String responseBody = buildResponseBody(res);

        log.info("traceId={} | sessionId={} | {} {} | {}ms | status={} | req={} | resp={}",
                traceId, sessionId, req.getMethod(), uri, cost, res.getStatus(), requestBody, responseBody);
	}

	private String buildRequestBody(ContentCachingRequestWrapper req) {
		String contentType = req.getContentType();
		if (isTextContent(contentType)) {
			return "[non-text]";
		}

		byte[] buf = req.getContentAsByteArray();
		if (buf.length == 0) {
			return "";
		}

		Charset charset = charsetOrUtf8(req.getCharacterEncoding());
		String body = new String(buf, charset);
		return mask(truncate(body));
	}

	private String buildResponseBody(ContentCachingResponseWrapper res) {
		String contentType = res.getContentType();
		if (isTextContent(contentType)) {
			return "[non-text]";
		}

		byte[] buf = res.getContentAsByteArray();
		if (buf.length == 0) {
			return "";
		}

		Charset charset = charsetOrUtf8(res.getCharacterEncoding());
		String body = new String(buf, charset);
		return mask(truncate(body));
	}

	private boolean isTextContent(String contentType) {
		if (!StringUtils.hasText(contentType)) {
			return true;
		}
		String ct = contentType.toLowerCase();
		if (ct.contains("multipart")) {
			return true;
		}
		return !ct.contains("json")
				&& !ct.contains("xml")
				&& !ct.startsWith("text/")
				&& !ct.contains("application/x-www-form-urlencoded");
	}

	private String truncate(String str) {
		if (str.length() > MAX_PAYLOAD_LENGTH) {
			return str.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
		}
		return str;
	}

	private String mask(String str) {
        return str;
	}

	private String getOrCreateTraceId(HttpServletRequest request) {
		String traceId = firstHeader(request, TRACE_HEADER);
		return StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString();
	}

	private String firstHeader(HttpServletRequest request, String name) {
		return Optional.ofNullable(request.getHeader(name)).orElse("");
	}

	private Charset charsetOrUtf8(String encoding) {
		if (!StringUtils.hasText(encoding)) {
			return StandardCharsets.UTF_8;
		}
		try {
			return Charset.forName(encoding);
		} catch (Exception e) {
			return StandardCharsets.UTF_8;
		}
	}
}
