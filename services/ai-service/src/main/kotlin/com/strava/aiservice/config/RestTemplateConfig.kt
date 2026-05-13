package com.strava.aiservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    /**
     * Connect/read timeouts deliberately short — Groq is fast on average
     * (~500ms-2s) and a slow tail beyond 5s usually means a partial outage
     * we'd rather fail-fast on. Resilience4j @TimeLimiter caps the whole
     * call at 8s anyway.
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
