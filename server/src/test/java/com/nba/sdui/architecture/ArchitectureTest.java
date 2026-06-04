package com.nba.sdui.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architectural boundary tests for the SDUI server.
 *
 * <p>Phase A1 contract:
 * <ul>
 *   <li>{@code ..domain..} is the composition core. It may use Spring stereotype/
 *       injection annotations during the transition (composers are still
 *       {@code @Component} / {@code @Autowired} until A3 wraps them in a
 *       composition root). It must <b>not</b> reach into Spring's web/http
 *       stack, OkHttp, or SAF — those are remote/transport concerns.</li>
 *   <li>{@code @RestController} may only appear in {@code ..controller..}.</li>
 *   <li>SAF ({@code com.nba.serviceaggregation..}) is forbidden in
 *       {@code ..domain..}; A2b lands SAF in {@code ..remote..} only.</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.nba.sdui",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotImportSpringWeb =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.web..")
                    .because("Domain composers must not depend on Spring MVC types; controllers own HTTP.");

    @ArchTest
    static final ArchRule domainMustNotImportSpringHttp =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.http..")
                    .because("Domain composers must not depend on HTTP types; controllers/remote own transport.");

    @ArchTest
    static final ArchRule domainMustNotImportOkHttp =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("okhttp3..")
                    .because("Domain composers depend on ports; OkHttp lives behind StatsApiAdapter in ..remote..");

    @ArchTest
    static final ArchRule domainMustNotImportSaf =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.nba.saf..")
                    .because("SAF lives in ..remote.. (Phase A2b); domain stays SAF-free.");

    @ArchTest
    static final ArchRule restClientOnlyInRemotePackage =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.web.client..")
                    .because("Spring RestClient lives behind ..remote.. adapters; domain stays transport-free.");

    @ArchTest
    static final ArchRule okHttpForbiddenEverywhere =
            noClasses()
                    .that().resideInAPackage("com.nba.sdui..")
                    .should().dependOnClassesThat().resideInAPackage("okhttp3..")
                    .because("OkHttp was removed in Phase A2b; all HTTP goes through Spring RestClient in ..remote..");

    @ArchTest
    static final ArchRule restControllersOnlyInControllerPackage =
            classes()
                    .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().resideInAPackage("..controller..")
                    .because("HTTP entry points belong in ..controller..");
}
