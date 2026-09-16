---
type: implementation-task
template: expediente
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-secure-coding
- k-i18n

## Qué hay que hacer

Escribe los tests unitarios de:

- `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAccesoTest.java` — ejerce `…v1.ControlDeAcceso` (tarea 09)
- `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirectorTest.java` — ejerce `…v1.DevolucionDelDirector` (tarea 09)

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

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ControlDeAcceso`

**Responsabilidad:** concentrar las comprobaciones de **quién es el usuario autenticado** que los `trigger*` de las tres fases usan como guarda en su primera línea (§9.0.1 del diseño). Dos métodos estáticos, totales: o dejan pasar, o lanzan `BusinessException` con el mensaje recibido por parámetro.
**Por qué sí se testea:** la define el diseño (tabla §6, especificación §9.0.1); no es ninguno de los tres managers; tiene lógica propia (comparación de identificadores con ramas para los casos nulos y decisión de lanzar o no); y es aislable: no necesita `Tramitador`, `EventContext`, base de datos, PDF ni la clase `States` —solo la entidad como POJO y el usuario autenticado—.
**Colaboradores a mockear:** `SecurityUtil` (estático, `Mockito.mockStatic`, tal como autoriza el `CLAUDE.md` del proyecto) e `I18n` (estático, programado para devolver su argumento, según la convención ya usada en `CicloServiceImplTest`).

#### Método: `static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException`

- **`exigeSerElCreador_usuarioAutenticadoEsElRegistrador_noLanza`** — Tipo: happy.
  - **Arrange:** expediente con `usuarioRegistrador` de `id = 7`; `SecurityUtil.getUser()` devuelve **otra instancia** de usuario con el mismo `id = 7` (la comparación es por identificador, no por identidad de objeto).
  - **Act:** invocar el método con el mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Assert:** no se lanza ninguna excepción.
- **`exigeSerElCreador_usuarioAutenticadoDistintoDelRegistrador_lanzaConElMensajeRecibido`** — Tipo: error.
  - **Arrange:** expediente con `usuarioRegistrador` de `id = 7`; `SecurityUtil.getUser()` devuelve un usuario de `id = 8`.
  - **Act:** invocar el método con el mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Assert:** se lanza `BusinessException` cuyo mensaje es exactamente `Solo puede modificar sus propias solicitudes`.
- **`exigeSerElCreador_mensajeDeBorrado_seDevuelveElMensajeRecibido`** — Tipo: error.
  - **Arrange:** mismo desajuste de usuarios que el test anterior.
  - **Act:** invocar el método con el mensaje `"Solo puede borrar sus propias solicitudes"` (el que usa `triggerDelete`, §9.1).
  - **Assert:** se lanza `BusinessException` cuyo mensaje es exactamente `Solo puede borrar sus propias solicitudes`; el mensaje sale del parámetro y no de un literal interno.
- **`exigeSerElCreador_expedienteSinUsuarioRegistrador_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `usuarioRegistrador` a `null`; `SecurityUtil.getUser()` devuelve un usuario cualquiera.
  - **Act:** invocar el método con el mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Assert:** se lanza `BusinessException` con ese mensaje exacto; la regla es total y **no** deja pasar ante un dato ausente.
- **`exigeSerElCreador_sinUsuarioAutenticado_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `usuarioRegistrador` de `id = 7`; `SecurityUtil.getUser()` devuelve `null`.
  - **Act:** invocar el método con el mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Assert:** se lanza `BusinessException` con ese mensaje exacto (nunca `NullPointerException`).

#### Método: `static void exigeMismoCentroQueElExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException`

- **`exigeMismoCentro_centroActivoIgualAlDelExpediente_noLanza`** — Tipo: happy.
  - **Arrange:** expediente con `centro` de `id = 3`; `SecurityUtil.getUser()` devuelve un usuario cuyo `centroActivo` es **otra instancia** con el mismo `id = 3`.
  - **Act:** invocar el método con el mensaje `"Solo puede revisar solicitudes de su propio centro"`.
  - **Assert:** no se lanza ninguna excepción.
- **`exigeMismoCentro_centroActivoDistinto_lanzaConElMensajeRecibido`** — Tipo: error.
  - **Arrange:** expediente con `centro` de `id = 3`; usuario autenticado con `centroActivo` de `id = 4`.
  - **Act:** invocar el método con el mensaje `"Solo puede revisar solicitudes de su propio centro"`.
  - **Assert:** se lanza `BusinessException` cuyo mensaje es exactamente `Solo puede revisar solicitudes de su propio centro`.
- **`exigeMismoCentro_expedienteSinCentro_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` a `null`; usuario autenticado con `centroActivo` de `id = 3`.
  - **Act:** invocar el método con el mensaje `"Solo puede firmar resoluciones de su propio centro"`.
  - **Assert:** se lanza `BusinessException` con ese mensaje exacto.
- **`exigeMismoCentro_usuarioSinCentroActivo_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` de `id = 3`; usuario autenticado con `centroActivo` a `null` (el caso del administrador al que no se le ha asignado centro, §14 nota 13).
  - **Act:** invocar el método con el mensaje `"Solo puede devolver resoluciones de su propio centro"`.
  - **Assert:** se lanza `BusinessException` con ese mensaje exacto.
- **`exigeMismoCentro_sinUsuarioAutenticado_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` de `id = 3`; `SecurityUtil.getUser()` devuelve `null`.
  - **Act:** invocar el método con el mensaje `"Solo puede firmar resoluciones de su propio centro"`.
  - **Assert:** se lanza `BusinessException` con ese mensaje exacto (nunca `NullPointerException`).

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.DevolucionDelDirector`

**Responsabilidad:** ser el dueño único de qué campos forman «la devolución del director» —`motivoDevolucion`, `fechaDevolucion` y `devueltoPor`— y dejarlos limpios; la invocan `triggerPresentar`, `triggerEnviarAFirma`, `triggerSubsanar` y `triggerFirmar` (§9.0.2).
**Por qué sí se testea:** la define el diseño (tabla §6, especificación §9.0.2); no es ninguno de los tres managers; tiene lógica propia (una transformación del expediente, declarada idempotente y total); y es aislable por completo: opera sobre la entidad como POJO, sin `Tramitador`, `EventContext`, base de datos, PDF ni `States`. El test es además lo que **fija la lista de los tres campos**, que es la razón de existir de la clase: si alguien añadiera un cuarto dato a la devolución sin limpiarlo aquí, el fallo sería silencioso.
**Colaboradores a mockear:** ninguno.

#### Método: `static void borrar(AnulacionMatriculaCicloFormativoV1 expediente)`

- **`borrar_expedienteQueVieneDeUnaDevolucion_dejaLosTresCamposANull`** — Tipo: happy.
  - **Arrange:** expediente con `motivoDevolucion` con texto, `fechaDevolucion` con una fecha y `devueltoPor` con un usuario.
  - **Act:** invocar `borrar` con ese expediente.
  - **Assert:** `motivoDevolucion`, `fechaDevolucion` y `devueltoPor` quedan los tres a `null`.
- **`borrar_expedienteSinDevolucionPrevia_esIdempotenteYNoLanza`** — Tipo: borde.
  - **Arrange:** expediente recién construido, con los tres campos ya a `null`.
  - **Act:** invocar `borrar` dos veces seguidas.
  - **Assert:** no se lanza ninguna excepción y los tres campos siguen a `null`; ningún llamante necesita decidir si le toca limpiar.
- **`borrar_noTocaNingunOtroCampoDelExpediente`** — Tipo: borde.
  - **Arrange:** expediente con la terna de la devolución informada y, además, `sentidoRevision`, `motivoRechazo`, `textoSubsanacion`, `fechaRevision` y `revisadoPor` informados.
  - **Act:** invocar `borrar`.
  - **Assert:** los cinco campos ajenos a la devolución conservan su valor; la clase es dueña solo de la terna y no limpia la decisión de secretaría.


## `## Cobertura` de `design/test-unit-desc.md` (verbatim)

## Cobertura

- Clases auxiliares descritas: **8**, agrupadas en **6** secciones (`ControlDeAcceso`, `DevolucionDelDirector`, el campo calculado `Ciclo.gradoNivel`, `Required`, el trío `MinLength`/`MaxLength`/`Pattern` de `StringRules.kt` y `NoAdmitido`), con **35** tests descritos en total.
- Clases del tipo excluidas: `InitialEventManagerImpl`; los tres `PhaseEventManagerImpl` (`solicitud`, `revision`, `resolucion`); los tres `StateEventValidatorImpl` (`solicitud`, `revision`, `resolucion`); además de `SinOtraSolicitudEnCursoParaElMismoCiclo` y de la ampliación de `FirmaPdf`, que no son aislables.
- Tests nuevos a crear: **6 ficheros** bajo `src/test/java/…` (los de la tabla «Tests nuevos a crear»). Ninguno bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`.
