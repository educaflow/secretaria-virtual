---
type: implementation-task
template: expediente
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-validaciones

## Qué hay que hacer

Escribe los tests unitarios de:

- `src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt` — ejerce `Required` (tarea 05)
- `src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt` — ejerce `MinLength`, `MaxLength` y `Pattern` (tarea 05)
- `src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java` — ejerce `NoAdmitido` (tarea 05)

**Por qué existe esta tarea.** Un tipo de expediente **no genera tests propios**: su conformidad la dan los tests ya existentes y escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`. La **única excepción** es que el diseño describa el test de una pieza de **lógica de negocio pura y aislable** que no viva en el `PhaseEventManagerImpl`, el `StateEventValidatorImpl` ni el `InitialEventManagerImpl`, y `design/test-unit-desc.md` describe seis ficheros así. Se reparten en **tres** tareas de test, una por cada tarea de implementación que produce la lógica que ejercen (las clases auxiliares de la versión, el campo calculado del catálogo y el catálogo común de reglas de `base`), para que cada test se escriba junto al criterio de «bien hecho» de lo que prueba. Esta decisión la toma la descomposición y queda documentada aquí.

**Reglas duras:**

- Cada test va en el **paquete espejo** de la clase que ejerce, bajo `src/test/java/…`, con exactamente el nombre de fichero que la tabla «Tests nuevos a crear» declara.
- **MUST NOT** crearse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`, ni ningún `agent_docs/*-rules.md`, ni ningún skill generador.
- **MUST NOT** editarse, ampliarse, debilitarse ni exonerarse ningún test existente: los `.java` de `tiposexpedientes` y de `views` son fuente de verdad escrita a mano. Si uno falla, el fallo está en el código de esta iniciativa.
- **MUST NOT** escribirse tests de los `PhaseEventManagerImpl`, de los `StateEventValidatorImpl`, del `InitialEventManagerImpl`, de `SinOtraSolicitudEnCursoParaElMismoCiclo` ni de `FirmaPdf`: `test-unit-desc.md` los excluye con su motivo.
- La descripción de los tests es **contrato fijo** y la **superficie es cerrada**: se escriben **los tests descritos**, con su nombre, su tipo y sus asertos; **MUST NOT** inventarse otros ni omitirse ninguno.
- Para mockear el usuario autenticado se mockea `SecurityUtil` (`Mockito.mockStatic`), nunca `AuthUtils`.

## Tabla «Tests nuevos a crear» de `design/test-unit-desc.md` (verbatim)

## Tests nuevos a crear

Seis ficheros, todos bajo `src/test/java/`, en el paquete espejo de la clase que ejercen:

| Fichero | Clase que ejerce |
|---|---|
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAccesoTest.java` | `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ControlDeAcceso` |
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirectorTest.java` | `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.DevolucionDelDirector` |
| `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java` | el campo calculado `Ciclo.gradoNivel` (Paso 4) |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt` | `Required` (Paso 5) |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt` | `MinLength`, `MaxLength` y `Pattern` (Paso 5) |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java` | `NoAdmitido` (Paso 5) |

**MUST NOT** crearse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`, ni ningún `agent_docs/*-rules.md`, ni ningún skill generador.

## Descripción de los tests de esta tarea, en `design/test-unit-desc.md` (verbatim)

### Clase: `com.educaflow.base.infrastructure.validation.rules.Required`

**Responsabilidad:** tras el Paso 5, `data class Required(val mensaje: String = "Es requerido")`. El parámetro sustituye **únicamente** el mensaje de la rama de **valor ausente**; las ramas «No puede estar vacío» (`MetaFile` de tamaño 0) y «No puede ser cero» (número a 0) conservan el suyo.
**Por qué sí se testea:** la modifica este diseño (tabla §6, fila `RequiredRules.kt`, con su especificación en el Paso 5); no es ninguno de los tres managers; tiene lógica propia (tres ramas de mensaje sobre el valor); y es aislable: `validate(value, bean)` es una función pura sobre valores en memoria, sin `Tramitador`, `EventContext`, base de datos, PDF ni `States`. Es además la única forma barata de comprobar lo que el Paso 5 exige y que ningún test E2E de este trámite puede ver: que **las llamadas existentes de otros trámites no cambian de comportamiento**.
**Colaboradores a mockear:** `I18n` (estático, devolviendo su argumento).

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
  - **Arrange:** regla construida con el mensaje `"Debe firmar la solicitud antes de presentarla"`; valor un `MetaFile` con `fileName` informado y `fileSize` 0.
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
  - **Arrange:** `MinLength(5, "…")`; valor una cadena de exactamente 5 caracteres.
  - **Act:** validar.
  - **Assert:** devuelve `null`; el mínimo es inclusivo.

#### Método: `MaxLength.validate(value: Any?, bean: Any): BusinessMessages?`

- **`maxLength_sinMensaje_textoLargo_devuelveElMensajeCalculado`** — Tipo: happy (no regresión).
  - **Arrange:** `MaxLength(10)` sin mensaje; valor una cadena de 12 caracteres.
  - **Act:** validar.
  - **Assert:** devuelve exactamente `Debe tener como máximo una longitud de 10 pero tiene 12`.
- **`maxLength_conMensaje_textoLargo_devuelveElMensajeRecibido`** — Tipo: happy.
  - **Arrange:** `MaxLength(150, "La dirección debe tener entre 5 y 150 caracteres")`; valor una cadena de 151 caracteres.
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
  - **Arrange:** `Pattern("^[6789]\\d{8}$", "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9")`; valor `"612345678"`.
  - **Act:** validar.
  - **Assert:** devuelve `null`.

### Clase: `com.educaflow.base.infrastructure.validation.rules.NoAdmitido`

**Responsabilidad:** regla **nueva** del catálogo común que el Paso 5 añade a `ConditionalRules.kt`: `data class NoAdmitido(val mensaje: String)`, que falla **siempre** con ese mensaje sin mirar el valor. Se usa solo dentro de una rama `ifValueIn`/`ifValueNotIn`: la rama pone la condición, la regla el mensaje.
**Por qué sí se testea:** la crea este diseño (tabla §6, fila `ConditionalRules.kt`, especificación en el Paso 5); no es ninguno de los tres managers; tiene lógica propia (es la que produce el mensaje de rechazo de los dos sentidos no admitidos de `ENVIAR_A_FIRMA` y `SUBSANAR`); y es aislable: función pura sobre el valor. El test fija justamente lo que la hace peligrosa fuera de una rama —que rechaza cualquier valor, incluido `null`—, que es lo que su KDoc advierte.
**Colaboradores a mockear:** `I18n` (estático, devolviendo su argumento).

#### Método: `validate(value: Any?, bean: Any): BusinessMessages?`

- **`noAdmitido_valorInformado_devuelveSiempreSuMensaje`** — Tipo: happy.
  - **Arrange:** `NoAdmitido("Para pedir una subsanación use el botón «Pedir subsanación al alumno»")` (§10.2); valor el ítem `SUBSANAR` del enum de sentido de la revisión.
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


## `## Cobertura` de `design/test-unit-desc.md` (verbatim)

## Cobertura

- Clases auxiliares descritas: **8**, agrupadas en **6** secciones (`ControlDeAcceso`, `DevolucionDelDirector`, el campo calculado `Ciclo.gradoNivel`, `Required`, el trío `MinLength`/`MaxLength`/`Pattern` de `StringRules.kt` y `NoAdmitido`), con **35** tests descritos en total.
- Clases del tipo excluidas: `InitialEventManagerImpl`; los tres `PhaseEventManagerImpl` (`solicitud`, `revision`, `resolucion`); los tres `StateEventValidatorImpl` (`solicitud`, `revision`, `resolucion`); además de `SinOtraSolicitudEnCursoParaElMismoCiclo` y de la ampliación de `FirmaPdf`, que no son aislables.
- Tests nuevos a crear: **6 ficheros** bajo `src/test/java/…` (los de la tabla «Tests nuevos a crear»). Ninguno bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`.
