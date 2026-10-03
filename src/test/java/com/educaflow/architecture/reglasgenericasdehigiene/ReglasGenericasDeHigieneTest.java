// =====================================================================
// GENERADO por /developer-create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /developer-create-arch-tests.
// =====================================================================
package com.educaflow.architecture.reglasgenericasdehigiene;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class ReglasGenericasDeHigieneTest {

    // [C22] Verificación:
    //   - Sujeto: todas las clases del ámbito de análisis.
    //   - Condición: ninguna accede a los streams estándar (`System.out`, `System.err`, `Throwable.printStackTrace`). Usar la regla predefinida de higiene de ArchUnit para streams estándar.
    //   - Exenciones: no se aplican en esta regla (es global; la regla predefinida no admite recortar el sujeto).
    //   - Mensaje: el de la regla predefinida.
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c22_noStreamsEstandar =
        FreezingArchRule.freeze(GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS);

    // [C29] Verificación:
    //   - Sujeto: todas las clases del ámbito de análisis, **incluidas** las de `..expedientes..` y `..tramites..` (no es una regla de estructura, así que la exención de esos paquetes no se aplica).
    //   - Condición: ninguna llama a `java.time.ZoneId.systemDefault()`, a `java.util.TimeZone.getDefault()`, a `java.util.Calendar.getInstance()` sin argumentos ni al método `now()` **sin argumentos** de `LocalDate`, `LocalDateTime`, `LocalTime`, `ZonedDateTime`, `OffsetDateTime`, `OffsetTime`, `Year`, `YearMonth` o `MonthDay` (todos de `java.time`).
    //   - Exenciones: la clase `com.educaflow.base.util.Convert`, dueña de la constante.
    //   - Mensaje: `La zona horaria de la aplicación es Convert.defaultZoneId: usar now(Convert.defaultZoneId) / atZone(Convert.defaultZoneId) en vez de la zona de la JVM`.
    @ArchTest
    static final ArchRule c29_zonaHorariaEsConvertDefaultZoneId =
        noClasses()
            .that().doNotHaveFullyQualifiedName("com.educaflow.base.util.Convert")
            .should().callMethod(ZoneId.class, "systemDefault")
            .orShould().callMethod(TimeZone.class, "getDefault")
            .orShould().callMethod(Calendar.class, "getInstance")
            .orShould().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(LocalTime.class, "now")
            .orShould().callMethod(ZonedDateTime.class, "now")
            .orShould().callMethod(OffsetDateTime.class, "now")
            .orShould().callMethod(OffsetTime.class, "now")
            .orShould().callMethod(Year.class, "now")
            .orShould().callMethod(YearMonth.class, "now")
            .orShould().callMethod(MonthDay.class, "now")
            .because("La zona horaria de la aplicación es Convert.defaultZoneId: usar now(Convert.defaultZoneId) / atZone(Convert.defaultZoneId) en vez de la zona de la JVM");
}
