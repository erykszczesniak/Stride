package com.strava.webbff.config;

import com.strava.webbff.client.IdentityForwardingInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    /**
     * {@code @LoadBalanced} lets Spring Cloud LoadBalancer rewrite {@code lb://service-name}
     * URLs into a healthy instance address picked from Eureka. Connect + read
     * timeouts are kept short so a slow dependency triggers Resilience4j retry
     * /circuit-breaker rather than backing up a thread for 30s.
     *
     * <p>The {@link IdentityForwardingInterceptor} re-attaches the calling user's
     * X-User-* identity headers on every outbound call, so downstream services
     * authenticate the composition calls exactly like gateway traffic.
     * (LoadBalancerAutoConfiguration appends its own interceptor after ours.)
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalanced() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(List.of(new IdentityForwardingInterceptor()));
        return restTemplate;
    }

    /** Injectable clock so time-window logic (admin stats) is deterministic in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
