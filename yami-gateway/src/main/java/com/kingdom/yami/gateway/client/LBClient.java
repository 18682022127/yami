package com.kingdom.yami.gateway.client;

import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LBClient {

    private final RestClient restClient;

    public LBClient(RestClient.Builder defaultBuilder, LoadBalancerInterceptor lbInterceptor) {
        this.restClient = defaultBuilder.clone()
                .requestInterceptor(lbInterceptor)
                .build();
    }

    public <T> T postJson(Object body, String url, Class<T> clazz) {
        return restClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(clazz);
    }

    public <T> T call(Object params, String url, Class<T> clazz) {
        return postJson(params, url, clazz);
    }
}
