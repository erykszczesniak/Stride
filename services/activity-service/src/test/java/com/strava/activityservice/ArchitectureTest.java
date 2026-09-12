package com.strava.activityservice;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Package-dependency rules for activity-service, enforced in the build. The
 * module is organised by feature (activity, pr, gear, importactivity, ...)
 * rather than by layer, so the rules pin down the few boundaries that matter
 * for a database-per-service module: persistence stays in {@code domain},
 * controllers never bypass services, Kafka consumers are only where the
 * runbooks say they are, and the domain never learns about transports.
 */
@AnalyzeClasses(packages = "com.strava.activityservice", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String BASE = "com.strava.activityservice";

    @ArchTest
    static final ArchRule entities_live_in_domain =
            classes().that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage(BASE + ".domain..")
                    .because("JPA entities are the persistence model; every other package works with DTOs");

    @ArchTest
    static final ArchRule repositories_live_in_domain =
            classes().that().areAssignableTo(Repository.class)
                    .should().resideInAPackage(BASE + ".domain..")
                    .because("repositories are the only way to the schema and are reviewed with the migrations");

    @ArchTest
    static final ArchRule controllers_do_not_use_repositories =
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .because("controllers map HTTP to services; persistence decisions belong in the service layer");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_transports =
            noClasses().that().resideInAPackage(BASE + ".domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            BASE + ".grpc..", BASE + ".kafka..", BASE + ".client..",
                            "org.springframework.web..", "org.springframework.kafka..", "io.grpc..")
                    .because("the persistence model must not know how it is exposed or which events touch it");

    @ArchTest
    static final ArchRule kafka_listeners_only_in_consumer_packages =
            methods().that().areAnnotatedWith(KafkaListener.class)
                    .should().beDeclaredInClassesThat().resideInAnyPackage(BASE + ".kafka..", BASE + ".event..")
                    .because("consumers are the at-least-once boundary (docs/saga.md); keeping them in two packages keeps idempotency reviewable");

    @ArchTest
    static final ArchRule controllers_are_named_as_such =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule no_service_uses_servlet_request_directly =
            noClasses().that().haveSimpleNameEndingWith("Service")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.servlet..")
                    .because("services take identity and parameters from the controller, not from the request");
}
