---
type: implementation-task
template: expediente
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-validaciones
- k-i18n

## Qué hay que hacer

Escribir los tests unitarios de las **reglas del catálogo común de validación** que la tarea 05 amplió:

- `src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt`
- `src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt`
- `src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java`

Los dos primeros son **Kotlin** (`.kt`) por el motivo que da el `test-unit-desc.md` más abajo: los tests de no-regresión invocan las reglas **sin** el segundo argumento y el diseño no declara `@JvmOverloads`, así que desde Java esas formas no compilarían. Las fuentes Kotlin conviven con las Java en los directorios `java`.

Lo que estos tests fijan, y que ningún test E2E de este trámite puede ver, es que **las llamadas existentes de otros trámites no cambian de comportamiento en castellano**.

### Decisión de descomposición documentada

Un tipo de expediente **no genera tests propios**: su conformidad la dan los tests ya existentes y escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`, que recorren automáticamente todos los tipos del árbol. La **única excepción** del contrato es que el `test-unit-desc.md` del diseño describa **clases auxiliares propias con lógica de negocio aislable**, y aquí las describe: seis ficheros de test sobre ocho clases auxiliares. Como el contrato prevé «una tarea de test por clase» y aquí hay clases en **tres paquetes distintos** (y un fichero de test que ejerce tres clases a la vez), se han agrupado en **tres tareas, una por paquete de destino**: cada una comparte skills, ubicación y especificación, y ninguna mezcla paquetes. Es la lectura más razonable del contrato para este caso; queda documentada aquí.

Reglas duras, comunes a las tres tareas de test:

- Cada test va en el **paquete espejo** de la clase que ejerce, bajo `src/test/java/...`.
- **MUST NOT** crearse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`.
- **MUST NOT** editarse, ampliarse, debilitarse ni exonerarse ningún test ya existente: los `.java` son la fuente de verdad y si uno falla, el fallo está en el trámite generado.
- **MUST NOT** escribirse tests unitarios de `InitialEventManagerImpl`, de los `PhaseEventManagerImpl` ni de los `StateEventValidatorImpl`: el propio diseño los excluye por no ser aislables.
- **MUST NOT** crearse ningún `agent_docs/*-rules.md` ni ningún skill generador para ellos.
- **La especificación del diseño es contrato fijo y la superficie es cerrada: MUST NOT crearse ningún test, método ni aserto que la especificación no liste**, ni cambiarse los literales de los mensajes esperados.
- Para el usuario autenticado, los tests mockean `SecurityUtil` (`Mockito.mockStatic`), que es exactamente para lo que el proyecto exige usar `SecurityUtil.getUser()` en vez de `AuthUtils.getUser()`.

## Filas de la tabla `## Tests nuevos a crear` del `test-unit-desc.md` (verbatim)

| Fichero | Clase que ejerce |
|---|---|
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt` | `Required` (Paso 5) |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt` | `MinLength`, `MaxLength` y `Pattern` (Paso 5) |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java` | `NoAdmitido` (Paso 5) |

Los dos ficheros de `RequiredRules.kt`/`StringRules.kt` son **Kotlin** (`.kt`) porque las clases que ejercen lo son y los tests de no-regresión las invocan **sin** el segundo argumento (`Required()`, `MinLength(5)`, `MaxLength(10)`, `Pattern("…")`): el diseño no declara `@JvmOverloads`, así que desde Java esas formas no compilarían. Las fuentes Kotlin conviven con las Java en los directorios `java`.

## Especificación de los tests de `Required`, `MinLength`/`MaxLength`/`Pattern` y `NoAdmitido`, ÍNTEGRA (verbatim de `test-unit-desc.md`)

### Clase: `com.educaflow.base.infrastructure.validation.rules.Required`

**Responsabilidad:** tras el Paso 5, `data class Required(val mensaje: String = "Es requerido")`. El parámetro sustituye **únicamente** el mensaje de la rama de **valor ausente**; las ramas «No puede estar vacío» (`MetaFile` de tamaño 0) y «No puede ser cero» (número a 0) conservan el suyo.
**Por qué sí se testea:** la modifica este diseño (tabla §6, fila `RequiredRules.kt`, con su especificación en el Paso 5); no es ninguno de los tres managers; tiene lógica propia (tres ramas de mensaje sobre el valor); y es aislable: `validate(value, bean)` es una función pura sobre valores en memoria, sin `Tramitador`, `EventContext`, base de datos, PDF ni `States`. Es además la única forma barata de comprobar lo que el Paso 5 exige y que ningún test E2E de este trámite puede ver: que **las llamadas existentes de otros trámites no cambian de comportamiento**.
**Colaboradores a mockear:** `I18n` (estático, devolviendo su argumento: lo que se comprueba es qué texto elige la regla, no cómo se traduce).

#### Método: `validate(value: Any?, bean: Any): BusinessMessages?`

- **`required_sinMensaje_valorNulo_devuelveEsRequerido`** — Tipo: happy (no regresión).
  - **Arrange:** regla construida sin argumentos; valor `null`.
  - **Act:** validar.
  - **Assert:** devuelve un único mensaje cuyo texto es exactamente `Es requerido`.
- **`required_sinMensaje_cadenaEnBlanco_devuelveEsRequerido`** — Tipo: borde (no regresión).
  - **Arrange:** regla sin argumentos; valor una cadena de solo espacios.
  - **Act:** validar.
  - **Assert:** devuelve un único mensaje `Es requerido`.
- **`required_conMensaje_valorNulo_devuelveElMensajeRecibido`** — Tipo: happy.
  - **Arrange:** regla construida con el mensaje `"Debe indicar su NIA"` (uno de los del validador de `CONTINUAR`, §10.1); valor `null`.
  - **Act:** validar.
  - **Assert:** devuelve un único mensaje cuyo texto es exactamente `Debe indicar su NIA`.
- **`required_conMensaje_metaFileDeTamanoCero_conservaNoPuedeEstarVacio`** — Tipo: borde.
  - **Arrange:** regla construida con el mensaje `"Debe firmar la solicitud antes de presentarla"` (§10.1); valor un `MetaFile` con `fileName` informado y `fileSize` 0.
  - **Act:** validar.
  - **Assert:** devuelve `No puede estar vacío`, **no** el mensaje del constructor: el parámetro solo tapa la rama de valor ausente.
- **`required_conMensaje_numeroCero_conservaNoPuedeSerCero`** — Tipo: borde.
  - **Arrange:** regla construida con un mensaje propio; valor el entero 0.
  - **Act:** validar.
  - **Assert:** devuelve `No puede ser cero`, **no** el mensaje del constructor.
- **`required_valorPresente_devuelveNull`** — Tipo: happy.
  - **Arrange:** regla con mensaje propio; valor una cadena con texto.
  - **Act:** validar.
  - **Assert:** devuelve `null` (sin mensajes).

### Clase: `com.educaflow.base.infrastructure.validation.rules.MinLength`, `MaxLength` y `Pattern`

**Responsabilidad:** tras el Paso 5, `MinLength(min, mensaje = null)` y `MaxLength(max, mensaje = null)` devuelven su mensaje **calculado** de hoy cuando `mensaje` es nulo y el literal recibido cuando no lo es; `Pattern(regex, mensaje = "El valor no cumple con el patrón especificado")` sustituye un literal constante.
**Por qué sí se testea:** las modifica este diseño (tabla §6, fila `StringRules.kt`, especificación en el Paso 5); no son ninguno de los tres managers; tienen lógica propia (comparación de longitudes y encaje del patrón, más la rama de mensaje por defecto); y son aislables: funciones puras sobre valores en memoria. Cubren la exigencia del Paso 5 de que las llamadas existentes de otros trámites sigan dando el mismo texto.
**Colaboradores a mockear:** `I18n` (estático, devolviendo su argumento).

#### Método: `MinLength.validate(value: Any?, bean: Any): BusinessMessages?`

- **`minLength_sinMensaje_textoCorto_devuelveElMensajeCalculado`** — Tipo: happy (no regresión).
  - **Arrange:** `MinLength(5)` sin mensaje; valor una cadena de 3 caracteres.
  - **Act:** validar.
  - **Assert:** devuelve exactamente `Debe tener como mínimo una longitud de 5 pero tiene 3`.
- **`minLength_conMensaje_textoCorto_devuelveElMensajeRecibido`** — Tipo: happy.
  - **Arrange:** `MinLength(5, "La dirección debe tener entre 5 y 150 caracteres")` (§10.1); valor una cadena de 3 caracteres.
  - **Act:** validar.
  - **Assert:** devuelve exactamente `La dirección debe tener entre 5 y 150 caracteres`.
- **`minLength_textoDeLongitudSuficiente_devuelveNull`** — Tipo: borde.
  - **Arrange:** `MinLength(5, "La dirección debe tener entre 5 y 150 caracteres")`; valor una cadena de exactamente 5 caracteres.
  - **Act:** validar.
  - **Assert:** devuelve `null`; el mínimo es inclusivo.

#### Método: `MaxLength.validate(value: Any?, bean: Any): BusinessMessages?`

- **`maxLength_sinMensaje_textoLargo_devuelveElMensajeCalculado`** — Tipo: happy (no regresión).
  - **Arrange:** `MaxLength(10)` sin mensaje; valor una cadena de 12 caracteres.
  - **Act:** validar.
  - **Assert:** devuelve exactamente `Debe tener como máximo una longitud de 10 pero tiene 12`.
- **`maxLength_conMensaje_textoLargo_devuelveElMensajeRecibido`** — Tipo: happy.
  - **Arrange:** `MaxLength(150, "La dirección debe tener entre 5 y 150 caracteres")` (§10.1); valor una cadena de 151 caracteres.
  - **Act:** validar.
  - **Assert:** devuelve exactamente `La dirección debe tener entre 5 y 150 caracteres`.

#### Método: `Pattern.validate(value: Any?, bean: Any): BusinessMessages?`

- **`pattern_sinMensaje_valorQueNoEncaja_devuelveElLiteralPorDefecto`** — Tipo: happy (no regresión).
  - **Arrange:** `Pattern("^\\d{5}$")` sin mensaje; valor `"123"`.
  - **Act:** validar.
  - **Assert:** devuelve exactamente `El valor no cumple con el patrón especificado`.
- **`pattern_conMensaje_valorQueNoEncaja_devuelveElMensajeRecibido`** — Tipo: happy.
  - **Arrange:** `Pattern("^\\d{8}$", "El NIA debe tener 8 dígitos")` (§10.1); valor `"1234"`.
  - **Act:** validar.
  - **Assert:** devuelve exactamente `El NIA debe tener 8 dígitos`.
- **`pattern_valorQueEncaja_devuelveNull`** — Tipo: borde.
  - **Arrange:** `Pattern("^[6789]\\d{8}$", "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9")` (§10.1); valor `"612345678"`.
  - **Act:** validar.
  - **Assert:** devuelve `null`.

### Clase: `com.educaflow.base.infrastructure.validation.rules.NoAdmitido`

**Responsabilidad:** regla **nueva** del catálogo común que el Paso 5 añade a `ConditionalRules.kt`: `data class NoAdmitido(val mensaje: String)`, que falla **siempre** con ese mensaje sin mirar el valor. Se usa solo dentro de una rama `ifValueIn`/`ifValueNotIn`: la rama pone la condición, la regla el mensaje.
**Por qué sí se testea:** la crea este diseño (tabla §6, fila `ConditionalRules.kt`, especificación en el Paso 5); no es ninguno de los tres managers; tiene lógica propia (es la que produce el mensaje de rechazo de los sentidos no admitidos de `ENVIAR_A_FIRMA` y `SUBSANAR`, §10.2); y es aislable: función pura sobre el valor. El test fija justamente lo que la hace peligrosa fuera de una rama —que rechaza cualquier valor, incluido `null`—, que es lo que su KDoc advierte.
**Colaboradores a mockear:** `I18n` (estático, devolviendo su argumento).

#### Método: `validate(value: Any?, bean: Any): BusinessMessages?`

- **`noAdmitido_valorInformado_devuelveSiempreSuMensaje`** — Tipo: happy.
  - **Arrange:** `NoAdmitido("Para pedir una subsanación use el botón «Pedir subsanación al alumno»")` (§10.2); valor el ítem `SUBSANAR` del enum `SentidoRevisionAnulacionMatriculaCicloFormativoV1`.
  - **Act:** validar.
  - **Assert:** devuelve un único mensaje cuyo texto es exactamente `Para pedir una subsanación use el botón «Pedir subsanación al alumno»`.
- **`noAdmitido_valorNulo_devuelveIgualmenteSuMensaje`** — Tipo: borde.
  - **Arrange:** `NoAdmitido("Para pedir una subsanación elija el sentido «Pedir subsanación»")` (§10.2); valor `null` (el caso «sentido sin elegir», que la rama `ifValueNotIn` también alcanza).
  - **Act:** validar.
  - **Assert:** devuelve exactamente `Para pedir una subsanación elija el sentido «Pedir subsanación»`; la regla no deja pasar el valor ausente.
- **`noAdmitido_valorDeOtroTipo_devuelveIgualmenteSuMensaje`** — Tipo: borde.
  - **Arrange:** `NoAdmitido("Para pedir una subsanación use el botón «Pedir subsanación al alumno»")` (§10.2); valor una cadena cualquiera.
  - **Act:** validar.
  - **Assert:** devuelve exactamente `Para pedir una subsanación use el botón «Pedir subsanación al alumno»`; la regla **no** mira el valor, y por eso solo tiene sentido dentro de una rama condicional.

