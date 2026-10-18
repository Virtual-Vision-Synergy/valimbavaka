package org.vvs.valimbavaka;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.springframework.beans.factory.annotation.Autowired;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Règles Feature-Based Layering vérifiées à chaque build (convention §2.5 et §4.3). */
@AnalyzeClasses(packagesOf = ValimbavakaCoreApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_do_not_touch_repositories =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule controllers_do_not_expose_entities =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().areAnnotatedWith(jakarta.persistence.Entity.class);

    @ArchTest
    static final ArchRule transverse_does_not_depend_on_features =
            noClasses().that().resideInAnyPackage("..valimbavaka.core..", "..valimbavaka.security..",
                            "..valimbavaka.shared..")
                    .should().dependOnClassesThat().resideInAPackage("..valimbavaka.feature..");

    @ArchTest
    static final ArchRule no_cycles_between_features =
            slices().matching("..valimbavaka.feature.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule repositories_are_used_by_service_implementations_only =
            classes().that().resideInAPackage("..repository..")
                    .should().onlyBeAccessed().byAnyPackage("..service.impl..", "..repository..");

    @ArchTest
    static final ArchRule no_field_injection =
            noFields().should().beAnnotatedWith(Autowired.class);
}
