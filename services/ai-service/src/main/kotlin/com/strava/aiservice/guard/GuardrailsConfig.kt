package com.strava.aiservice.guard

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Explicit beans for the guard building blocks so wiring never depends on Kotlin default arguments. */
@Configuration
class GuardrailsConfig {

    @Bean
    fun aiRateLimiter(): AiRateLimiter = AiRateLimiter()

    @Bean
    fun promptGuard(properties: AiGuardProperties): PromptGuard = PromptGuard(properties.guard.maxRepeatedChars)

    @Bean
    fun piiMasker(): PiiMasker = PiiMasker()
}
