package org.temporedata.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchUnitRules {

    void domainMustNotDependOnWeb() {
        noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web..");
    }

    void controllersMustNotAccessRepositories() {
        noClasses()
            .that().resideInAnyPackage("..controller..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..repository..");
    }
}
