// =====================================================================
// GENERADO por /create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /create-arch-tests.
// =====================================================================
package com.educaflow.architecture.nomenclaturayubicacion;

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
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import com.tngtech.archunit.library.GeneralCodingRules;

// Tipos del framework Axelor referenciados por las reglas:
import com.axelor.app.AxelorModule;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelService;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
class NomenclaturaYUbicacionTest {

    private static final String[] PAQUETES_EXENTOS = {
        "..expedientes..", "..tiposexpedientes..", "..tramites.."
    };

    @ArchTest
    static final ArchRule c15a_clasesEnControllerTerminanEnController =
        classes()
            .that().resideInAPackage("..controller..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule c15b_controllersResidenEnPaqueteController =
        classes()
            .that().haveSimpleNameEndingWith("Controller")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule c16_implServicioNombreYUbicacion =
        classes()
            .that().areAssignableTo(DefaultModelService.class)
                .and().doNotHaveFullyQualifiedName("com.axelor.db.modelservice.DefaultModelService")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().haveSimpleNameEndingWith("ServiceImpl")
            .andShould().resideInAPackage("..service.impl..")
            .because("ModelServiceFactory descubre la impl por el nombre <Entidad>ServiceImpl en service.impl");

    @ArchTest
    static final ArchRule c17_interfazServicioNombreYUbicacion =
        classes()
            .that().areInterfaces()
                .and().areAssignableTo(ModelService.class)
                .and().doNotHaveFullyQualifiedName("com.axelor.db.modelservice.ModelService")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().haveSimpleNameEndingWith("Service")
            .andShould().resideInAPackage("..service..")
            .because("la interfaz de servicio se llama <Entidad>Service y vive en service");

    @ArchTest
    static final ArchRule c18_repositoriosNombre =
        classes()
            .that().resideInAPackage("..db.repo..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().haveSimpleNameEndingWith("Repository")
            .orShould().haveSimpleNameEndingWith("Listener")
            .because("en db/repo solo hay repositorios (*Repository) y, excepcionalmente, listeners (*Listener)");

    @ArchTest
    static final ArchRule c19_modulosGuiceNombreYUbicacion =
        classes()
            .that().areAssignableTo(AxelorModule.class)
                .and().doNotHaveFullyQualifiedName("com.axelor.app.AxelorModule")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().haveSimpleNameEndingWith("Module")
            .andShould().resideInAPackage("..module..")
            .because("los módulos Guice se llaman <Subsistema>Module y viven en module/");

    @ArchTest
    static final ArchRule c20_dtosSonRecordsEnService =
        classes()
            .that().haveSimpleNameEndingWith("DTO")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().beAssignableTo(Record.class)   // todo record extiende java.lang.Record
            .andShould().resideInAPackage("..service..")
            .because("los DTOs del proyecto son records de Java y viven junto a la interfaz del servicio");
}
