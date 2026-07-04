// =====================================================================
// GENERADO por /create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /create-arch-tests.
// =====================================================================
package com.educaflow.architecture.dependenciasentrecapas;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
class DependenciasEntreCapasTest {

    private static final String[] PAQUETES_EXENTOS = {
        "..expedientes..", "..tiposexpedientes..", "..tramites.."
    };

    @ArchTest
    static final ArchRule c1_baseUtilNoDependeDeOtrosPaquetesEducaflow =
        noClasses()
            .that().resideInAPackage("com.educaflow.base.util..")
            .should().dependOnClassesThat(
                resideInAPackage("com.educaflow..")
                    .and(DescribedPredicate.not(resideInAPackage("com.educaflow.base.util.."))))
            .because("base/util es la capa más baja: no puede depender de ningún otro paquete com.educaflow");

    @ArchTest
    static final ArchRule c2_baseInfrastructureSoloDependeDeBaseUtil =
        noClasses()
            .that().resideInAPackage("com.educaflow.base.infrastructure..")
            .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "com.educaflow.subsystem..",
                    "com.educaflow.system..",
                    "com.educaflow.secretariavirtual..")
            .because("base/infrastructure solo puede depender, dentro de com.educaflow, de base/util");

    @ArchTest
    static final ArchRule c3_subsystemNoDependeDeSystemNiSecretariaVirtual =
        noClasses()
            .that().resideInAPackage("com.educaflow.subsystem..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "com.educaflow.system..",
                    "com.educaflow.secretariavirtual..")
            .because("un subsystem nunca depende de un system ni del ensamblaje secretariavirtual");

    @ArchTest
    static final ArchRule c4_systemNoDependeDeSecretariaVirtual =
        noClasses()
            .that().resideInAPackage("com.educaflow.system..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
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
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().dependOnClassesThat()
                .resideInAPackage("com.educaflow.secretariavirtual..")
            .because("secretariavirtual es la capa más alta: ninguna otra capa puede depender de ella");

    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c7_subsistemasSinCiclos =
        FreezingArchRule.freeze(
            slices().matching("com.educaflow.subsystem.(*)..")
                .should().beFreeOfCycles()
                // 'expedientes' tiene arquitectura propia: se excluye del análisis de ciclos (origen y destino).
                .ignoreDependency(resideInAPackage("..expedientes.."), DescribedPredicate.alwaysTrue())
                .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..expedientes..")));

    @ArchTest
    static final ArchRule c8_sistemasIndependientesEntreSi =
        slices().matching("com.educaflow.system.(*)..")
            .should().notDependOnEachOther()
            // 'tiposexpedientes' y 'tramites' tienen arquitectura propia: se excluyen (origen y destino).
            .ignoreDependency(resideInAPackage("..tiposexpedientes.."), DescribedPredicate.alwaysTrue())
            .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..tiposexpedientes.."))
            .ignoreDependency(resideInAPackage("..tramites.."), DescribedPredicate.alwaysTrue())
            .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..tramites.."));
}
