package com.strava.notificationservice.config

import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    /**
     * @LoadBalanced lets Spring Cloud LoadBalancer rewrite lb://service-name
     * URLs into a healthy instance address picked from Eureka. Connect + read
     * timeouts are kept short so a slow dependency triggers Resilience4j
     * retry / circuit-breaker rather than backing up a thread.
     */
    @Bean
    @LoadBalanced
    fun loadBalanced(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(2000)
        factory.setReadTimeout(3000)
        return RestTemplate(factory)
    }
}
