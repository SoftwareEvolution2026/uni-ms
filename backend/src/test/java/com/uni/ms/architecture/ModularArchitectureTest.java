package com.uni.ms.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.uni.ms")
class ModularArchitectureTest {

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule shared_must_not_contain_academic_catalog_rules = noClasses()
            .that().resideInAnyPackage("..common..", "..shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..academiccatalog.department..",
                    "..academiccatalog.course..",
                    "..course..");

    @ArchTest
    static final ArchRule identity_domain_must_not_depend_on_outer_identity_layers = noClasses()
            .that().resideInAPackage("..identity.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..identity.api..",
                    "..identity.application..",
                    "..identity.infrastructure..");

    @ArchTest
    static final ArchRule identity_api_must_not_access_infrastructure = noClasses()
            .that().resideInAPackage("..identity.api..")
            .should().dependOnClassesThat().resideInAPackage("..identity.infrastructure..");

    @ArchTest
    static final ArchRule identity_repositories_are_not_used_outside_identity = noClasses()
            .that().resideOutsideOfPackage("..identity..")
            .should().dependOnClassesThat().resideInAPackage("..identity.infrastructure..");

    @ArchTest
    static final ArchRule department_api_must_not_access_infrastructure = noClasses()
            .that().resideInAPackage("..academiccatalog.department.api..")
            .should().dependOnClassesThat().resideInAPackage(
                    "..academiccatalog.department.infrastructure..");

    @ArchTest
    static final ArchRule department_domain_must_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage("..academiccatalog.department.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..academiccatalog.department.api..",
                    "..academiccatalog.department.application..",
                    "..academiccatalog.department.infrastructure..");

    @ArchTest
    static final ArchRule department_must_not_depend_on_course_implementation = noClasses()
            .that().resideInAPackage("..academiccatalog.department..")
            .and().haveSimpleNameNotEndingWith("Test")
            .should().dependOnClassesThat().resideInAPackage("..academiccatalog.course..");

    @ArchTest
    static final ArchRule department_infrastructure_is_internal = noClasses()
            .that().resideOutsideOfPackage("..academiccatalog.department..")
            .and().haveSimpleNameNotEndingWith("Test")
            .should().dependOnClassesThat().resideInAPackage(
                    "..academiccatalog.department.infrastructure..");

    @ArchTest
    static final ArchRule course_api_must_not_access_infrastructure = noClasses()
            .that().resideInAPackage("..academiccatalog.course.api..")
            .should().dependOnClassesThat().resideInAPackage(
                    "..academiccatalog.course.infrastructure..");

    @ArchTest
    static final ArchRule course_domain_must_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage("..academiccatalog.course.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..academiccatalog.course.api..",
                    "..academiccatalog.course.application..",
                    "..academiccatalog.course.infrastructure..");

    @ArchTest
    static final ArchRule course_must_not_depend_on_department_implementation = noClasses()
            .that().resideInAPackage("..academiccatalog.course..")
            .and().haveSimpleNameNotEndingWith("Test")
            .should().dependOnClassesThat().resideInAPackage(
                    "..academiccatalog.department..");

    @ArchTest
    static final ArchRule course_infrastructure_is_internal = noClasses()
            .that().resideOutsideOfPackage("..academiccatalog.course..")
            .and().haveSimpleNameNotEndingWith("Test")
            .should().dependOnClassesThat().resideInAPackage(
                    "..academiccatalog.course.infrastructure..");

    @ArchTest
    static final ArchRule dashboard_api_must_not_access_infrastructure = noClasses()
            .that().resideInAPackage("..dashboard.api..")
            .should().dependOnClassesThat().resideInAPackage("..dashboard.infrastructure..");
}
