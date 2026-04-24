package com.kingdom.yami.gateway.filter.token;

import cn.hutool.json.JSONUtil;
import com.kingdom.yami.common.web.ApiResponse;
import com.kingdom.yami.gateway.properties.TokenProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(20)
public class TokenFilter extends OncePerRequestFilter {

	private final TokenProperties tokenProperties;
	private final TokenCheckClient tokenCheckClient;

	public TokenFilter(TokenProperties tokenProperties, TokenCheckClient tokenCheckClient) {
		this.tokenProperties = tokenProperties;
		this.tokenCheckClient = tokenCheckClient;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
							  HttpServletResponse response,
							  FilterChain filterChain) throws ServletException, IOException {

		String path = request.getRequestURI();
		if (tokenProperties.shouldSkip(path)) {
			filterChain.doFilter(request, response);
			return;
		}

		String raw = request.getHeader(tokenProperties.header());
		String token = extractBearerToken(raw, tokenProperties.bearerPrefix());
		if (token == null) {
			sendUnauthorized(response);
			return;
		}

		boolean ok;
		try {
			ok = tokenCheckClient.checkToken(token);
		} catch (Exception e) {
			logger.error("校验token失败",e);
			sendUnauthorized(response);
			return;
		}

		if (!ok) {
			sendUnauthorized(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String extractBearerToken(String headerValue, String bearerPrefix) {
		if (!StringUtils.hasText(headerValue)) {
			return null;
		}
		String prefix = bearerPrefix + " ";
		if (!headerValue.regionMatches(true, 0, prefix, 0, prefix.length())) {
			return null;
		}
		String token = headerValue.substring(prefix.length()).trim();
		return token.isBlank() ? null : token;
	}

	private void sendUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());

		ApiResponse<Object> fail = ApiResponse.fail("请求非法");
		response.getOutputStream().write(JSONUtil.toJsonStr(fail).getBytes(StandardCharsets.UTF_8));
	}
}
