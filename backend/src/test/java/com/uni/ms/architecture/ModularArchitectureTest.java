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
}
