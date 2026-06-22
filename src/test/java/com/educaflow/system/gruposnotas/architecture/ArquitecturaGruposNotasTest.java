package com.educaflow.system.gruposnotas.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import com.tngtech.archunit.library.GeneralCodingRules;

import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelService;

@AnalyzeClasses(packages = "com.educaflow.system.gruposnotas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaGruposNotasTest {

    @ArchTest
    static final ArchRule c4_systemNoDependeDeSecretariaVirtual =
        noClasses()
            .that().resideInAPackage("com.educaflow.system..")
            .should().dependOnClassesThat()
                .resideInAPackage("com.educaflow.secretariavirtual..")
            .because("un system nunca depende del ensamblaje secretariavirtual (capa más alta)");

    @ArchTest
    static final ArchRule c5_secretariaVirtualNoEsAccedidaPorNadie =
        noClasses()
            .that().resideInAnyPackage(
                    "com.educaflow.base..",
                    "com.educaflow.subsystem..",
                    "com.educaflow.system..")
            .should().dependOnClassesThat()
                .resideInAPackage("com.educaflow.secretariavirtual..")
            .because("secretariavirtual es la capa más alta: ninguna otra capa puede depender de ella");

    @ArchTest
    static final ArchRule c8_sistemasIndependientesEntreSi =
        slices().matching("com.educaflow.system.(*)..")
            .should().notDependOnEachOther()
            // 'tiposexpedientes' y 'tramites' tienen arquitectura propia: se excluyen.
            .ignoreDependency(resideInAPackage("..tiposexpedientes.."), DescribedPredicate.alwaysTrue())
            .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..tiposexpedientes.."))
            .ignoreDependency(resideInAPackage("..tramites.."), DescribedPredicate.alwaysTrue())
            .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..tramites.."));

    @ArchTest
    static final ArchRule c9_controladorNoAccedeARepositorio =
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..db.repo..")
            .because("el controlador delega el acceso a datos en el servicio, nunca usa el repositorio directamente");

    @ArchTest
    static final ArchRule c10_controladorNoUsaJpaRepository =
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.axelor.db.JpaRepository")
            .because("cargar entidades es del servicio; el controlador no usa JpaRepository");

    @ArchTest
    static final ArchRule c11_repositorioNoDependeDeServicioNiControlador =
        noClasses()
            .that().resideInAPackage("..db.repo..")
            .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..")
            .because("el repositorio es capa de datos: no conoce servicios ni controladores");

    @ArchTest
    static final ArchRule c12_servicioNoDependeDeControlador =
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("la dependencia es Controller→Service, nunca Service→Controller");

    @ArchTest
    static final ArchRule c13_entidadesDominioSonPojos =
        noClasses()
            .that().resideInAPackage("..db..")
                .and().resideOutsideOfPackages("..db.repo..")
            .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..")
            .because("las entidades de dominio son POJOs; la lógica de negocio vive en el servicio");

    @ArchTest
    static final ArchRule c14_noBeansGetEnControladorNiServiceImpl =
        noClasses()
            .that().resideInAnyPackage("..controller..", "..service.impl..")
            .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.axelor.inject.Beans")
            .because("Beans.get es service-locator; se usa inyección / ModelServiceFactory");

    @ArchTest
    static final ArchRule c15a_clasesEnControllerTerminanEnController =
        classes()
            .that().resideInAPackage("..controller..")
            .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule c15b_controllersResidenEnPaqueteController =
        classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule c16_implServicioNombreYUbicacion =
        classes()
            .that().areAssignableTo(DefaultModelService.class)
                .and().doNotHaveFullyQualifiedName("com.axelor.db.modelservice.DefaultModelService")
            .should().haveSimpleNameEndingWith("ServiceImpl")
            .andShould().resideInAPackage("..service.impl..")
            .because("ModelServiceFactory descubre la impl por el nombre <Entidad>ServiceImpl en service.impl");

    @ArchTest
    static final ArchRule c17_interfazServicioNombreYUbicacion =
        classes()
            .that().areInterfaces()
                .and().areAssignableTo(ModelService.class)
                .and().doNotHaveFullyQualifiedName("com.axelor.db.modelservice.ModelService")
            .should().haveSimpleNameEndingWith("Service")
            .andShould().resideInAPackage("..service..")
            .because("la interfaz de servicio se llama <Entidad>Service y vive en service");

    @ArchTest
    static final ArchRule c18_repositoriosNombre =
        classes()
            .that().resideInAPackage("..db.repo..")
            .should().haveSimpleNameEndingWith("Repository")
            .orShould().haveSimpleNameEndingWith("Listener")
            .because("en db/repo solo hay repositorios (*Repository) y, excepcionalmente, listeners (*Listener)");

    @ArchTest
    static final ArchRule c21_modelServiceNoSeInyecta =
        noFields()
            .that().haveRawType(assignableTo(ModelService.class))
            .should().beAnnotatedWith("com.google.inject.Inject")
            .orShould().beAnnotatedWith("jakarta.inject.Inject")
            .as("Ningún campo de tipo ModelService debe llevar @Inject (se usa ModelServiceFactory)")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule c22_noStreamsEstandar =
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}
