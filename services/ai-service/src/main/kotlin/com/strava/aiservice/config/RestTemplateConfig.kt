package com.strava.aiservice.config

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
     *
     * (The @LoadBalanced lb:// RestTemplate that used to live here fetched the
     * chat's activity context over REST; that call moved to gRPC in Phase 23c —
     * see client/ActivityClient.)
     */
    @Bean
    fun rest(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3000)
            setReadTimeout(7000)
        }
        return RestTemplate(factory)
    }
}
