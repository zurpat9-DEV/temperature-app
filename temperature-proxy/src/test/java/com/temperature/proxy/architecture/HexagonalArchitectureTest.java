package com.temperature.proxy.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Hexagonal Architecture")
class HexagonalArchitectureTest {

    private static final String BASE_PACKAGE = "com.temperature.proxy";
    private static final String DOMAIN_PACKAGE = BASE_PACKAGE + ".domain..";
    private static final String APPLICATION_PACKAGE = BASE_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE_PACKAGE = BASE_PACKAGE + ".infrastructure..";

    private static com.tngtech.archunit.core.domain.JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Nested
    @DisplayName("Domain layer rules")
    class DomainLayerRules {

        @Test
        void should_not_depend_on_application_layer() {
            noClasses()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .check(classes);
        }

        @Test
        void should_not_depend_on_infrastructure_layer() {
            noClasses()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                    .check(classes);
        }

        @Test
        void should_not_depend_on_spring_framework() {
            noClasses()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Application layer rules")
    class ApplicationLayerRules {

        @Test
        void should_not_depend_on_infrastructure_layer() {
            noClasses()
                    .that()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                    .check(classes);
        }

        @Test
        void should_implement_use_cases() {
            classes()
                    .that()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .and()
                    .haveSimpleNameEndingWith("Service")
                    .should()
                    .implement(com.temperature.proxy.domain.port.in.GetCurrentWeatherUseCase.class)
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Infrastructure layer rules")
    class InfrastructureLayerRules {

        @Test
        void adapters_should_implement_ports_or_use_use_cases() {
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".infrastructure.adapter.out..")
                    .and()
                    .haveSimpleNameEndingWith("Adapter")
                    .should()
                    .implement(com.temperature.proxy.domain.port.out.WeatherDataProvider.class)
                    .check(classes);
        }

        @Test
        @DisplayName("Input adapters should not depend on output adapters")
        void input_adapters_should_not_depend_on_output_adapters() {
            noClasses()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".infrastructure.adapter.in..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(BASE_PACKAGE + ".infrastructure.adapter.out..")
                    .check(classes);
        }

        @Test
        @DisplayName("Output adapters should not depend on input adapters")
        void output_adapters_should_not_depend_on_input_adapters() {
            noClasses()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".infrastructure.adapter.out..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(BASE_PACKAGE + ".infrastructure.adapter.in..")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Package dependency rules")
    class PackageDependencyRules {

        @Test
        void dependency_direction_should_be_inward() {
            noClasses()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(APPLICATION_PACKAGE, INFRASTRUCTURE_PACKAGE)
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Port rules")
    class PortRules {

        @Test
        @DisplayName("Input ports should be interfaces")
        void input_ports_should_be_interfaces() {
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".domain.port.in..")
                    .should()
                    .beInterfaces()
                    .check(classes);
        }

        @Test
        @DisplayName("Output ports should be interfaces")
        void output_ports_should_be_interfaces() {
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".domain.port.out..")
                    .should()
                    .beInterfaces()
                    .check(classes);
        }
    }
}
