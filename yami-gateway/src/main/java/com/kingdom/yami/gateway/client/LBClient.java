package com.kingdom.yami.gateway.client;

import com.kingdom.yami.common.web.ApiResponse;
import com.kingdom.yami.gateway.filter.crypto.GetEncKeyBySessionIdRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Component
public class LBClient {

    private final RestClient restClient;

    public LBClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.build();
    }

    public <T> T call(Object params,String url,Class<T> clazz) {

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .retrieve()
                .body(clazz);
    }
}