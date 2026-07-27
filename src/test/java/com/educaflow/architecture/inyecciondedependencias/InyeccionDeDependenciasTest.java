// =====================================================================
// GENERADO por /developer-create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /developer-create-arch-tests.
// =====================================================================
package com.educaflow.architecture.inyecciondedependencias;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

// Tipos del framework Axelor referenciados por las reglas:
import com.axelor.db.modelservice.ModelService;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
class InyeccionDeDependenciasTest {

    // [C21] Verificación:
    //   - Sujeto: **campos** (no clases) cuyo tipo es asignable a `com.axelor.db.modelservice.ModelService`.
    //   - Condición: ninguno está anotado con `@Inject` (ni `com.google.inject.Inject` ni `jakarta.inject.Inject`).
    //   - Vacuidad: si no existe ningún campo de ese tipo, la regla se considera **cumplida** (no debe fallar por sujeto vacío).
    //   - Mensaje (descripción de la regla, literal): «Ningún campo de tipo ModelService debe llevar @Inject (se usa ModelServiceFactory)».
    @ArchTest
    static final ArchRule c21_modelServiceNoSeInyecta =
        noFields()
            .that().haveRawType(assignableTo(ModelService.class))
            .should().beAnnotatedWith("com.google.inject.Inject")
            .orShould().beAnnotatedWith("jakarta.inject.Inject")
            .as("Ningún campo de tipo ModelService debe llevar @Inject (se usa ModelServiceFactory)")
            .allowEmptyShould(true);
}
