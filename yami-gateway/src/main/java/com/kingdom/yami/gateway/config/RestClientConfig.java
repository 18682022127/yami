package com.kingdom.yami.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced // 核心：赋予它去 Nacos 解析服务名和负载均衡的能力
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}