---
type: implementation-task
template: expediente
---

# Tarea 21 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-validaciones

Escribe el test unitario

```
src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt
```

que cubre la regla `Required` del catálogo común de validación (en Kotlin, porque necesita nombrar el parámetro opcional y la nulabilidad de la propia regla).

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

### Clase: `com.educaflow.base.infrastructure.validation.rules.Required`

**Responsabilidad:** exigir que el campo tenga valor. El diseño la convierte en `data class Required(val mensaje: String = "Es requerido")`, donde el parámetro sustituye **únicamente** el texto de la rama de **valor ausente** (Paso 5).
**Por qué sí se testea:** la define el diseño (tabla §6 y Paso 5), no es ninguno de los tres managers, decide con ramas según el tipo y el valor recibidos, y es aislable: `validate(value, bean)` es una función pura sobre su argumento. Es además el test que protege a los trámites ya existentes del cambio, porque la regla la comparte todo el catálogo.
**Colaboradores a mockear:** `I18n` (estático, devolviendo su propio argumento: sin el contexto de Axelor arrancado no hay traducción, y lo que se comprueba es **qué texto elige** la regla). El `bean` es irrelevante para esta regla.

#### Método: `override fun validate(value: Any?, bean: Any): BusinessMessages?`

- **`valorPresente_devuelveNull`** — Tipo: happy.
  - **Arrange:** `Required("Debe indicar su NIA")` y un valor `"12345678"`.
  - **Act:** `validate(valor, bean)`.
  - **Assert:** devuelve `null` (sin mensajes).
- **`valorNulo_devuelveElMensajeRecibido`** — Tipo: error.
  - **Arrange:** `Required("Debe indicar su NIA")` y `value` a `null`.
  - **Act:** `validate(null, bean)`.
  - **Assert:** un único mensaje, exactamente `"Debe indicar su NIA"`.
- **`textoEnBlanco_devuelveElMensajeRecibido`** — Tipo: error.
  - **Arrange:** `Required("Debe indicar su NIA")` y `value` = `"   "`.
  - **Act:** `validate("   ", bean)`.
  - **Assert:** un único mensaje, exactamente `"Debe indicar su NIA"`.
- **`ficheroSinNombre_devuelveElMensajeRecibido`** — Tipo: error.
  - **Arrange:** `Required("Debe firmar la solicitud antes de presentarla")` y un `MetaFile` con `fileName` vacío.
  - **Act:** `validate(metaFile, bean)`.
  - **Assert:** un único mensaje, exactamente `"Debe firmar la solicitud antes de presentarla"`.
- **`ficheroDeTamanoCero_conservaSuPropioMensaje`** — Tipo: borde (**no regresión**).
  - **Arrange:** `Required("Debe firmar la solicitud antes de presentarla")` y un `MetaFile` con `fileName` informado y `fileSize` 0.
  - **Act:** `validate(metaFile, bean)`.
  - **Assert:** el mensaje es exactamente `"No puede estar vacío"`, **no** el recibido por parámetro: el dato está, pero vacío, y taparlo sería una regresión en las llamadas existentes.
- **`numeroACero_conservaSuPropioMensaje`** — Tipo: borde (**no regresión**).
  - **Arrange:** `Required("Debe indicar el importe")` y `value` = `0` (y la misma comprobación con un `BigDecimal.ZERO`).
  - **Act:** `validate(0, bean)`.
  - **Assert:** el mensaje es exactamente `"No puede ser cero"`, no el recibido por parámetro.
- **`sinMensaje_valorNulo_devuelveElLiteralPorDefecto`** — Tipo: borde (**no regresión**).
  - **Arrange:** `Required()`, sin argumento, y `value` a `null`.
  - **Act:** `validate(null, bean)`.
  - **Assert:** el mensaje es exactamente `"Es requerido"` — las llamadas existentes siguen diciendo lo mismo.

