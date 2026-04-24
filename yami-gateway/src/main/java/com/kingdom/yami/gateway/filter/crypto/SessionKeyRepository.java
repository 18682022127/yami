package com.kingdom.yami.gateway.filter.crypto;

import com.kingdom.yami.common.web.ApiResponse;
import com.kingdom.yami.gateway.client.LBClient;
import com.kingdom.yami.gateway.properties.CryptoProperties;
import org.springframework.stereotype.Component;

@Component
public class SessionKeyRepository {

	private final LBClient lbClient;
	private final CryptoProperties cryptoProperties;

	public SessionKeyRepository(LBClient lbClient, CryptoProperties cryptoProperties) {
		this.lbClient = lbClient;
		this.cryptoProperties = cryptoProperties;
	}

	public String getSessionKey(String sessionId) {
		ApiResponse<?> resp = lbClient.call(new GetEncKeyBySessionIdRequest(sessionId), cryptoProperties.sessionKeyUrl(), ApiResponse.class);
		if (resp == null || resp.data() == null) {
			return null;
		}
		return String.valueOf(resp.data());
	}
}
