---
type: implementation-task
template: expediente
---

# Tarea 23 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-validaciones

Escribe el test unitario

```
src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java
```

que cubre la regla **nueva** `NoAdmitido` del catálogo común (en Java, porque solo construye la regla y lee su mensaje).

**Por qué esta tarea existe.** Un tipo de expediente **no genera tests propios**: su conformidad la dan los tests ya existentes y escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`. La **única excepción** es la de una pieza de **lógica de negocio pura y aislable** que **no** vive en el `PhaseEventManagerImpl`, el `StateEventValidatorImpl` ni el `InitialEventManagerImpl`, y `design/test-unit-desc.md` describe exactamente eso para esta clase. El test va en el **mismo paquete** de la clase bajo `src/test/java/...`.

**CRITICAL — la descripción de `test-unit-desc.md` que va abajo es contrato fijo y la superficie es cerrada: MUST NOT escribirse ningún test que no liste, ni omitirse ninguno de los que lista, ni cambiarse su Arrange/Act/Assert.**

- **MUST NOT** editarse, ampliarse, debilitarse ni exonerarse ningún test existente de `src/test/java/com/educaflow/tiposexpedientes/` ni de `src/test/java/com/educaflow/views/`.
- **MUST NOT** añadirse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com/educaflow/views`.
- **MUST NOT** editarse ningún fichero de `src/test/e2e/**`.
- **MUST NOT** modificarse el código de producción para que el test pase: si el test no pasa, el fallo se reporta.

## `test-unit-desc.md` — «Tests nuevos a crear» (verbatim)

## Tests nuevos a crear

Siete ficheros, todos bajo `src/test/java/`, uno por clase o por fichero de reglas:

| Fichero de test | Cubre |
|---|---|
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAccesoTest.java` | `exigeSerElCreador` y `exigeMismoCentroQueElExpediente` |
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirectorTest.java` | `borrar` |
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatriculaTest.java` | `ReglasAnulacionMatricula.esRechazo` (desde Java, que es además la forma de llamada que exige el `@JvmStatic`) |
| `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java` | el campo derivado `Ciclo.gradoNivel` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt` | `Required` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt` | `MinLength`, `MaxLength` y `Pattern` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java` | `NoAdmitido` |

`StringRulesTest` cubre las tres reglas que viven en `StringRules.kt`. Los tests de las reglas de `base` van en Kotlin cuando necesitan nombrar los parámetros opcionales y la nulabilidad de la propia regla (`Required`, `MinLength`, `MaxLength`, `Pattern`) y en Java cuando solo construyen la regla y leen su mensaje (`NoAdmitido`). **MUST NOT** añadirse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`.

## Descripción del test (verbatim, íntegra)

### Clase: `com.educaflow.base.infrastructure.validation.rules.NoAdmitido`

**Responsabilidad:** rechazar **siempre** el campo con el mensaje indicado. Es la única regla **nueva** del catálogo común, y se usa solo dentro de una rama `ifValueIn`/`ifValueNotIn`: la rama dice cuándo aplica y la regla con qué mensaje se rechaza (Paso 5, `decisiones.md` D7).
**Por qué sí se testea:** la define el diseño (tabla §6 y Paso 5), no es ninguno de los tres managers, tiene comportamiento propio (rechazar sea cual sea el valor, incluido el nulo) y es aislable: `validate(value, bean)` no mira nada más que su propio mensaje.
**Colaboradores a mockear:** `I18n` (estático, devolviendo su argumento).

#### Método: `override fun validate(value: Any?, bean: Any): BusinessMessages?`

- **`noAdmitido_conValor_devuelveSiempreElMensaje`** — Tipo: happy.
  - **Arrange:** `NoAdmitido("Para pedir una subsanación use el botón «Pedir subsanación al alumno»")` y un valor cualquiera.
  - **Act:** `validate(valor, bean)`.
  - **Assert:** un único mensaje, exactamente `"Para pedir una subsanación use el botón «Pedir subsanación al alumno»"`.
- **`noAdmitido_valorNulo_devuelveElMismoMensaje`** — Tipo: borde.
  - **Arrange:** la misma regla con `value` a `null`.
  - **Act:** `validate(null, bean)`.
  - **Assert:** un único mensaje, exactamente `"Para pedir una subsanación use el botón «Pedir subsanación al alumno»"`: la regla **no** mira el valor, y por eso su KDoc declara que suelta, fuera de una rama, rechazaría cualquier cosa.
- **`noAdmitido_dosInstanciasConDistintoMensaje_devuelveCadaUnaElSuyo`** — Tipo: borde.
  - **Arrange:** las dos instancias que el diseño declara: `NoAdmitido("Para pedir una subsanación use el botón «Pedir subsanación al alumno»")` y `NoAdmitido("Para pedir una subsanación elija el sentido «Pedir subsanación»")`.
  - **Act:** invocar `validate` en cada una.
  - **Assert:** cada una devuelve su propio texto, literal: la primera `"Para pedir una subsanación use el botón «Pedir subsanación al alumno»"` y la segunda `"Para pedir una subsanación elija el sentido «Pedir subsanación»"`.

