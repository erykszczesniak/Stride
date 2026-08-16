package com.strava.aiservice.config

import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    /**
     * Plain RestTemplate for the outbound Groq call. Connect/read timeouts
     * deliberately short — Groq is fast on average (~500ms-2s) and a slow tail
     * beyond 7s usually means a partial outage we'd rather fail-fast on, letting
     * Resilience4j retry / circuit-breaker do their job.
     */
    @Bean
    fun rest(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3000)
            setReadTimeout(7000)
        }
        return RestTemplate(factory)
    }

    /**
     * @LoadBalanced lets Spring Cloud LoadBalancer rewrite lb://service-name
     * URLs into a healthy instance address picked from Eureka — used for the
     * activity-service internal API that grounds the AI chat. Timeouts kept
     * short so a slow peer trips Resilience4j instead of backing up a thread.
     */
    @Bean
    @LoadBalanced
    fun loadBalanced(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(2000)
            setReadTimeout(3000)
        }
        return RestTemplate(factory)
    }
}
