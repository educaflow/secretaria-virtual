// =====================================================================
// GENERADO por /create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /create-arch-tests.
// =====================================================================
package com.educaflow.architecture.reglasgenericasdehigiene;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasGenericasDeHigieneTest {

    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c22_noStreamsEstandar =
        FreezingArchRule.freeze(GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS);
}
