package org.vvs.valimbavaka;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.Arrays;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** Règles Feature-Based Layering vérifiées à chaque build (convention §2.5 et §4.3). */
@AnalyzeClasses(packagesOf = ValimbavakaCoreApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final DescribedPredicate<JavaAnnotation<?>> API_V1_MAPPING = DescribedPredicate.describe(
            "@RequestMapping(\"/api/v1...\")",
            annotation -> annotation.getRawType().isEquivalentTo(RequestMapping.class)
                    && Arrays.stream(annotation.as(RequestMapping.class).value())
                            .allMatch(path -> path.startsWith("/api/v1")));

    private static final DescribedPredicate<JavaField> ENUM_TYPED = DescribedPredicate.describe(
            "of enum type", field -> field.getRawType().isEnum());

    private static final DescribedPredicate<JavaAnnotation<?>> ENUMERATED_STRING = DescribedPredicate.describe(
            "@Enumerated(EnumType.STRING)",
            annotation -> annotation.getRawType().isEquivalentTo(Enumerated.class)
                    && annotation.as(Enumerated.class).value() == EnumType.STRING);

    private static final DescribedPredicate<JavaAnnotation<?>> READ_ONLY_TRANSACTION = DescribedPredicate.describe(
            "@Transactional(readOnly = true)",
            annotation -> annotation.getRawType().isEquivalentTo(Transactional.class)
                    && annotation.as(Transactional.class).readOnly());

    @ArchTest
    static final ArchRule controllers_do_not_touch_repositories = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule controllers_do_not_expose_entities = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(jakarta.persistence.Entity.class);

    @ArchTest
    static final ArchRule transverse_does_not_depend_on_features = noClasses()
            .that()
            .resideInAnyPackage("..valimbavaka.core..", "..valimbavaka.security..", "..valimbavaka.shared..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..valimbavaka.feature..");

    @ArchTest
    static final ArchRule no_cycles_between_features =
            slices().matching("..valimbavaka.feature.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule repositories_are_used_by_service_implementations_only = classes()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .onlyBeAccessed()
            .byAnyPackage("..service.impl..", "..repository..");

    @ArchTest
    static final ArchRule no_field_injection = noFields().should().beAnnotatedWith(Autowired.class);

    // §7 : contrat REST — /api/v1, validation et autorisation explicites.
    @ArchTest
    static final ArchRule rest_controllers_follow_api_contract = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage("..controller..")
            .andShould()
            .beAnnotatedWith(Validated.class)
            .andShould()
            .beAnnotatedWith(API_V1_MAPPING);

    // Les endpoints de connexion (feature auth) précèdent l'authentification : pas de permission possible.
    @ArchTest
    static final ArchRule endpoints_require_permission = methods()
            .that()
            .arePublic()
            .and()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .and()
            .areDeclaredInClassesThat()
            .resideOutsideOfPackage("..feature.auth..")
            .should()
            .beAnnotatedWith(HasPermission.class);

    // §7 : DTO = record ; jamais d'id ni de rôle dans une entrée utilisateur (mass assignment).
    @ArchTest
    static final ArchRule dtos_are_records = classes()
            .that()
            .haveSimpleNameEndingWith("RDto")
            .or()
            .haveSimpleNameEndingWith("WDto")
            .should()
            .beRecords();

    @ArchTest
    static final ArchRule input_dtos_have_no_id_or_role = noFields()
            .that()
            .areDeclaredInClassesThat()
            .haveSimpleNameEndingWith("WDto")
            .should()
            .haveName("id")
            .orShould()
            .haveName("role");

    // §8 : enums persistés par nom, jamais par ordinal (défaut JPA sans @Enumerated).
    @ArchTest
    static final ArchRule entity_enums_are_stored_as_string = fields().that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Entity.class)
            .and(ENUM_TYPED)
            .should()
            .beAnnotatedWith(ENUMERATED_STRING);

    // §2 / D8 : pas d'association JPA entre features, on référence l'autre entité par son UUID.
    @ArchTest
    static final ArchRule feature_domains_do_not_depend_on_each_other =
            slices().matching("..valimbavaka.feature.(*).domain..").should().notDependOnEachOther();

    // §7 : lecture seule par défaut sur la classe, @Transactional explicite sur les écritures.
    @ArchTest
    static final ArchRule feature_services_are_read_only_by_default = classes()
            .that()
            .resideInAPackage("..feature..service.impl..")
            .and()
            .areAnnotatedWith(Service.class)
            .should()
            .beAnnotatedWith(READ_ONLY_TRANSACTION);

    // JNL-02 : aucune suppression définitive d'opération validée. Seul le référentiel utilisateur expose un DELETE
    // (un utilisateur n'est pas une opération, et sa suppression est tracée au journal).
    @ArchTest
    static final ArchRule delete_endpoints_are_limited_to_the_user_reference = noMethods()
            .that()
            .areAnnotatedWith(DeleteMapping.class)
            .should()
            .beDeclaredInClassesThat()
            .resideOutsideOfPackage("..feature.user..");

    // JNL-02 : un repository de feature métier n'hérite ni de JpaRepository ni de CrudRepository (delete*) ;
    // il passe par OperationRepository. Le référentiel et l'authentification sont des données, pas des opérations.
    @ArchTest
    static final ArchRule operation_features_have_no_deleting_repository = noClasses()
            .that()
            .resideInAPackage("..feature..repository..")
            .and()
            .resideOutsideOfPackages("..feature.user..", "..feature.referential..", "..feature.auth..")
            .should()
            .beAssignableTo(CrudRepository.class);

    @ArchTest
    static final ArchRule operation_repositories_declare_no_delete = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .areAssignableTo(OperationRepository.class)
            .should()
            .haveNameMatching("(delete|remove).*");
}
