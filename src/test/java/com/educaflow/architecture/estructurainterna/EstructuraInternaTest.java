// =====================================================================
// GENERADO por /developer-create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /developer-create-arch-tests.
// =====================================================================
package com.educaflow.architecture.estructurainterna;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class EstructuraInternaTest {

    private static final String[] PAQUETES_EXENTOS = {
        "..expedientes..", "..tramites.."
    };

    // [C9] Verificación:
    //   - Sujeto: clases de `..controller..`, excluidos los paquetes exentos.
    //   - Condición: ninguna depende de clases de `..db.repo..`.
    //   - Mensaje: «el controlador delega el acceso a datos en el servicio, nunca usa el repositorio directamente».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c9_controladorNoAccedeARepositorio =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("..controller..")
                    .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
                .should().dependOnClassesThat().resideInAPackage("..db.repo..")
                .because("el controlador delega el acceso a datos en el servicio, nunca usa el repositorio directamente"));

    // [C10] Verificación:
    //   - Sujeto: clases de `..controller..`, excluidos los paquetes exentos.
    //   - Condición: ninguna depende de la clase `com.axelor.db.JpaRepository`.
    //   - Mensaje: «cargar entidades es del servicio; el controlador no usa JpaRepository».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c10_controladorNoUsaJpaRepository =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("..controller..")
                    .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
                .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.axelor.db.JpaRepository")
                .because("cargar entidades es del servicio; el controlador no usa JpaRepository"));

    // [C11] Verificación:
    //   - Sujeto: clases de `..db.repo..`, excluidos los paquetes exentos.
    //   - Condición: ninguna depende de clases de `..service..` ni `..controller..`.
    //   - Mensaje: «el repositorio es capa de datos: no conoce servicios ni controladores».
    @ArchTest
    static final ArchRule c11_repositorioNoDependeDeServicioNiControlador =
        noClasses()
            .that().resideInAPackage("..db.repo..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..")
            .because("el repositorio es capa de datos: no conoce servicios ni controladores");

    // [C12] Verificación:
    //   - Sujeto: clases de `..service..`, excluidos los paquetes exentos.
    //   - Condición: ninguna depende de clases de `..controller..`.
    //   - Mensaje: «la dependencia es Controller→Service, nunca Service→Controller».
    @ArchTest
    static final ArchRule c12_servicioNoDependeDeControlador =
        noClasses()
            .that().resideInAPackage("..service..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("la dependencia es Controller→Service, nunca Service→Controller");

    // [C13] Verificación:
    //   - Sujeto: clases de `..db..`, excluidas las de `..db.repo..` y los paquetes exentos.
    //   - Condición: ninguna depende de clases de `..service..` ni `..controller..`.
    //   - Mensaje: «las entidades de dominio son POJOs; la lógica de negocio vive en el servicio».
    @ArchTest
    static final ArchRule c13_entidadesDominioSonPojos =
        noClasses()
            .that().resideInAPackage("..db..")
                .and().resideOutsideOfPackages("..db.repo..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..")
            .because("las entidades de dominio son POJOs; la lógica de negocio vive en el servicio");

    // [C14] Verificación:
    //   - Sujeto: clases de `..controller..` y `..service.impl..`, excluidos los paquetes exentos. `base/util` queda deliberadamente **fuera** del sujeto: allí algún `Beans.get` de infraestructura puede ser legítimo.
    //   - Condición: ninguna depende de la clase `com.axelor.inject.Beans`.
    //   - Mensaje: «Beans.get es service-locator; se usa inyección / ModelServiceFactory».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c14_noBeansGetEnControladorNiServiceImpl =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAnyPackage("..controller..", "..service.impl..")
                    .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
                .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.axelor.inject.Beans")
                .because("Beans.get es service-locator; se usa inyección / ModelServiceFactory"));
}
