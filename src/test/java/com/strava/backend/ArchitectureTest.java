package com.strava.backend;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Package-dependency rules for the monolith, enforced in the build. The
 * codebase is organised by layer (controller / service / repository /
 * domain), so the rules pin the layer boundaries.
 *
 * {@code controllers_do_not_use_repositories} is a {@link FreezingArchRule}:
 * three controllers (Admin, Export, Search) predate the rule and are recorded
 * in {@code src/test/resources/archunit_store}. Existing violations pass, any
 * new one fails the build, and fixing one of the frozen cases shrinks the
 * store on the next run (never grows it).
 */
@AnalyzeClasses(packages = "com.strava.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String BASE = "com.strava.backend";

    @ArchTest
    static final ArchRule entities_live_in_domain =
            classes().that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage(BASE + ".domain..")
                    .because("JPA entities are the persistence model; every other package works with DTOs");

    @ArchTest
    static final ArchRule repositories_live_in_repository_or_search =
            classes().that().areAssignableTo(Repository.class)
                    .should().resideInAnyPackage(BASE + ".repository..", BASE + ".search..")
                    .because("JPA repositories are the only way to the schema (reviewed with the migrations); "
                            + "the Elasticsearch repository lives with the index it owns");

    @ArchTest
    static final ArchRule controllers_do_not_use_repositories = FreezingArchRule.freeze(
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .because("controllers map HTTP to services; persistence decisions belong in the service layer"));

    @ArchTest
    static final ArchRule domain_does_not_depend_on_web_or_services =
            noClasses().that().resideInAPackage(BASE + ".domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            BASE + ".controller..", BASE + ".service..", BASE + ".search..",
                            "org.springframework.web..", "org.springframework.data.elasticsearch..")
                    .because("the persistence model must not know how it is exposed or indexed");

    @ArchTest
    static final ArchRule controllers_are_named_as_such =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule services_do_not_read_the_servlet_request =
            noClasses().that().resideInAPackage(BASE + ".service..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.servlet..")
                    .because("services take identity and parameters from the controller, not from the request");
}
