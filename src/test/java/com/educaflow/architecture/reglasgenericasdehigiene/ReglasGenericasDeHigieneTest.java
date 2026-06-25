// =====================================================================
// GENERADO por /create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /create-arch-tests.
// =====================================================================
package com.educaflow.architecture.reglasgenericasdehigiene;

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

import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasGenericasDeHigieneTest {

    private static final String[] PAQUETES_EXENTOS = {
        "..expedientes..", "..tiposexpedientes..", "..tramites.."
    };

    // frozen: incumplimiento conocido (ver "Estado actual" en architecture-rules.md)
    @ArchTest
    static final ArchRule c22_noStreamsEstandar =
        FreezingArchRule.freeze(GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS);
}
