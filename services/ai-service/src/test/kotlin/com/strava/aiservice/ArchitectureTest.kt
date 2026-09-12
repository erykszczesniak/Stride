package com.strava.aiservice

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.springframework.web.bind.annotation.RestController

/**
 * Package-dependency rules for ai-service. The module has one hard boundary
 * worth enforcing: everything that talks to an LLM provider or to
 * activity-service lives in `client`, is reachable only through the service
 * layer, and knows nothing about the HTTP surface. That is what lets the
 * standalone profile swap providers for mocks and what keeps the guardrails
 * (prompt guard, PII masking, rate limits) in one place.
 */
@AnalyzeClasses(packages = ["com.strava.aiservice"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {

    companion object {
        private const val BASE = "com.strava.aiservice"

        @ArchTest
        @JvmField
        val controllers_never_reach_clients: ArchRule =
            noClasses().that().areAnnotatedWith(RestController::class.java)
                .should().dependOnClassesThat().resideInAPackage("$BASE.client..")
                .because("providers are reached through AiChatService / GroqService, where guardrails and metrics live")

        @ArchTest
        @JvmField
        val clients_know_nothing_about_features: ArchRule =
            noClasses().that().resideInAPackage("$BASE.client..")
                .should().dependOnClassesThat().resideInAnyPackage("$BASE.chat..", "$BASE.goal..")
                .because("client is the bottom of the dependency graph")

        @ArchTest
        @JvmField
        val only_clients_talk_to_providers: ArchRule =
            noClasses().that().resideOutsideOfPackages("$BASE.client..", "$BASE.config..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "com.anthropic..", "org.springframework.web.client..", "io.grpc.."
                )
                .because("provider SDKs and transports are an implementation detail of client/ (config wires them)")

        @ArchTest
        @JvmField
        val domain_is_framework_free: ArchRule =
            noClasses().that().resideInAPackage("$BASE.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "$BASE.client..", "$BASE.chat..", "$BASE.goal..", "org.springframework.web..", "com.anthropic.."
                )

        @ArchTest
        @JvmField
        val controllers_are_named_as_such: ArchRule =
            classes().that().areAnnotatedWith(RestController::class.java)
                .should().haveSimpleNameEndingWith("Controller")
    }
}
