package com.kingdom.yami.gateway.filter.token;

import com.kingdom.yami.gateway.client.LBClient;
import com.kingdom.yami.gateway.properties.TokenProperties;
import org.springframework.stereotype.Component;

@Component
public class TokenCheckClient {

	private final LBClient client;
	private final TokenProperties tokenProperties;

	public TokenCheckClient(LBClient client, TokenProperties tokenProperties) {
		this.client = client;
		this.tokenProperties = tokenProperties;
	}

	public boolean checkToken(String token) {
		Boolean ok = client.call(new CheckTokenRequest(token), tokenProperties.checkUrl(), Boolean.class);
		return Boolean.TRUE.equals(ok);
	}
}
