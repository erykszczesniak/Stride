package com.strava.socialservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * @LoadBalanced lets Spring Cloud LoadBalancer rewrite {@code lb://service-name}
     * URLs into a healthy instance address picked from Eureka. Connect + read
     * timeouts are kept short so a slow activity-service triggers Resilience4j
     * retry/circuit-breaker (and the snapshot fallback) rather than backing up
     * a request thread for 30s.
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalanced() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }
}
