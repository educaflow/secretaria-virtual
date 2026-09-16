---
type: implementation-task
template: expediente
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-validaciones

Escribe el test unitario

```
src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt
```

que cubre las **tres** reglas que viven en `StringRules.kt`: `MinLength`, `MaxLength` y `Pattern` (en Kotlin, por el mismo motivo).

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

### Clase: `com.educaflow.base.infrastructure.validation.rules.MinLength`, `MaxLength` y `Pattern`

**Responsabilidad:** exigir longitud mínima, longitud máxima y que el valor case con una expresión regular. El diseño les añade un mensaje opcional: constante con valor por defecto en `Pattern`, y `mensaje: String? = null` en `MinLength`/`MaxLength`, cuyo texto actual **se calcula con el valor** y por eso no es expresable como valor por defecto (Paso 5).
**Por qué sí se testea:** las define el diseño (tabla §6 y Paso 5), no son ninguno de los tres managers, deciden con ramas y son aislables — `validate(value, bean)` es pura. Los casos «sin mensaje» son los que garantizan que ninguna llamada existente cambia de texto.
**Colaboradores a mockear:** `I18n` (estático, devolviendo su argumento). El `bean` es irrelevante.

#### Método: `MinLength.validate(value: Any?, bean: Any): BusinessMessages?`

- **`minLength_longitudSuficiente_devuelveNull`** — Tipo: happy.
  - **Arrange:** `MinLength(5)` y `"123456"`.
  - **Act:** `validate("123456", bean)`.
  - **Assert:** `null`.
- **`minLength_conMensaje_textoCorto_devuelveElMensajeRecibido`** — Tipo: error.
  - **Arrange:** `MinLength(5, "La dirección debe tener entre 5 y 150 caracteres")` y `"Av"`.
  - **Act:** `validate("Av", bean)`.
  - **Assert:** un único mensaje, exactamente `"La dirección debe tener entre 5 y 150 caracteres"`.
- **`minLength_sinMensaje_textoCorto_devuelveElMensajeCalculado`** — Tipo: borde (**no regresión**).
  - **Arrange:** `MinLength(5)` y `"123"`.
  - **Act:** `validate("123", bean)`.
  - **Assert:** el mensaje es exactamente `"Debe tener como mínimo una longitud de 5 pero tiene 3"`.
- **`minLength_valorQueNoEsTexto_devuelveNull`** — Tipo: borde.
  - **Arrange:** `MinLength(5)` y un valor que no es `String` (por ejemplo `null` o un número).
  - **Act:** `validate(valor, bean)`.
  - **Assert:** `null` — la regla solo opina sobre cadenas; la obligatoriedad la declara `Required` aparte.

#### Método: `MaxLength.validate(value: Any?, bean: Any): BusinessMessages?`

- **`maxLength_longitudDentroDelLimite_devuelveNull`** — Tipo: happy.
  - **Arrange:** `MaxLength(10)` y `"123"`.
  - **Act:** `validate("123", bean)`.
  - **Assert:** `null`.
- **`maxLength_conMensaje_textoLargo_devuelveElMensajeRecibido`** — Tipo: error.
  - **Arrange:** `MaxLength(1000, "El motivo del rechazo debe tener entre 10 y 1000 caracteres")` y un texto de 1001 caracteres.
  - **Act:** `validate(texto, bean)`.
  - **Assert:** un único mensaje, exactamente `"El motivo del rechazo debe tener entre 10 y 1000 caracteres"`.
- **`maxLength_sinMensaje_textoLargo_devuelveElMensajeCalculado`** — Tipo: borde (**no regresión**).
  - **Arrange:** `MaxLength(10)` y un texto de 12 caracteres.
  - **Act:** `validate(texto, bean)`.
  - **Assert:** el mensaje es exactamente `"Debe tener como máximo una longitud de 10 pero tiene 12"`.

#### Método: `Pattern.validate(value: Any?, bean: Any): BusinessMessages?`

- **`pattern_valorQueCasa_devuelveNull`** — Tipo: happy.
  - **Arrange:** `Pattern("^\\d{8}$")` y `"12345678"`.
  - **Act:** `validate("12345678", bean)`.
  - **Assert:** `null`.
- **`pattern_conMensaje_valorQueNoCasa_devuelveElMensajeRecibido`** — Tipo: error.
  - **Arrange:** `Pattern("^\\d{8}$", "El NIA debe tener 8 dígitos")` y `"12A45678"`.
  - **Act:** `validate("12A45678", bean)`.
  - **Assert:** un único mensaje, exactamente `"El NIA debe tener 8 dígitos"`.
- **`pattern_sinMensaje_valorQueNoCasa_devuelveElLiteralPorDefecto`** — Tipo: borde (**no regresión**).
  - **Arrange:** `Pattern("^\\d{8}$")` y `"12A45678"`.
  - **Act:** `validate("12A45678", bean)`.
  - **Assert:** el mensaje es exactamente `"El valor no cumple con el patrón especificado"`.
- **`pattern_valorConEspaciosAlrededor_seComparaSobreElTextoRecortado`** — Tipo: borde.
  - **Arrange:** `Pattern("^\\d{8}$")` y `" 12345678 "`.
  - **Act:** `validate(" 12345678 ", bean)`.
  - **Assert:** `null` — el comportamiento actual, que el mensaje opcional no cambia.

