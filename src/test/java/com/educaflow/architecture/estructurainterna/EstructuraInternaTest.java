// =====================================================================
// GENERADO por /developer-create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /developer-create-arch-tests.
// =====================================================================
package com.educaflow.architecture.estructurainterna;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
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

    // [C23] Verificación:
    //   - Sujeto: métodos **declarados** (no heredados) en interfaces asignables a `com.axelor.db.modelservice.ModelService`, excluidos los paquetes exentos y excluidos los propios métodos de infraestructura del contrato: los que empiezan por `validate` o por `allowProperties`.
    //   - Condición: para cada método `m` del sujeto existe en la misma interfaz un método llamado `validate` + el nombre de `m` con la inicial en mayúscula, con **la misma lista de tipos de parámetros en el mismo orden** y con tipo de retorno `java.util.Optional`.
    //   - Vacuidad: una interfaz sin acciones propias cumple la regla (no debe fallar por sujeto vacío).
    //   - Nota: el argumento genérico de `Optional<BusinessMessages>` se borra en bytecode, así que la condición de retorno solo puede comprobar `Optional`. Es suficiente: ningún otro método del contrato devuelve `Optional`.
    //   - Mensaje: «cada acción propia de un *Service declara su validador validate<Accion> con la misma firma de parámetros».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c23_accionDeServicioDeclaraSuValidador =
        FreezingArchRule.freeze(
            methods()
                .that().areDeclaredInClassesThat().areInterfaces()
                    .and().areDeclaredInClassesThat()
                        .areAssignableTo("com.axelor.db.modelservice.ModelService")
                    .and().areDeclaredInClassesThat().resideOutsideOfPackages(PAQUETES_EXENTOS)
                    .and().haveNameNotStartingWith("validate")
                    .and().haveNameNotStartingWith("allowProperties")
                .should(declararSuValidador())
                .because("cada acción propia de un *Service declara su validador validate<Accion> con la misma firma de parámetros")
                .allowEmptyShould(true));

    private static ArchCondition<JavaMethod> declararSuValidador() {
        return new ArchCondition<JavaMethod>(
                "declarar su validador validate<Accion> con la misma firma de parámetros") {
            @Override
            public void check(JavaMethod accion, ConditionEvents events) {
                String nombreValidador = "validate"
                    + Character.toUpperCase(accion.getName().charAt(0))
                    + accion.getName().substring(1);
                List<String> parametrosAccion = tiposDe(accion);

                boolean declarado = accion.getOwner().getMethods().stream()
                    .anyMatch(candidato ->
                        candidato.getName().equals(nombreValidador)
                            && tiposDe(candidato).equals(parametrosAccion)
                            && candidato.getRawReturnType().getName().equals("java.util.Optional"));

                events.add(new SimpleConditionEvent(accion, declarado,
                    declarado
                        ? accion.getFullName() + " declara " + nombreValidador
                        : "falta " + nombreValidador + " para la acción " + accion.getFullName()));
            }

            private List<String> tiposDe(JavaMethod metodo) {
                return metodo.getRawParameterTypes().stream().map(JavaClass::getName).toList();
            }
        };
    }
}
