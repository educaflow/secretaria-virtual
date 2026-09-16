---
type: implementation-task
template: expediente
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-tipo-expediente
- k-secure-coding

## Qué hay que hacer

Escribir los tests unitarios de las **dos clases auxiliares de la raíz de la versión** que creó la tarea 09:

- `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAccesoTest.java`
- `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirectorTest.java`

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

## Tabla `## Tests nuevos a crear` del `test-unit-desc.md` (verbatim)

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

Los dos ficheros de `RequiredRules.kt`/`StringRules.kt` son **Kotlin** (`.kt`) porque las clases que ejercen lo son y los tests de no-regresión las invocan **sin** el segundo argumento (`Required()`, `MinLength(5)`, `MaxLength(10)`, `Pattern("…")`): el diseño no declara `@JvmOverloads`, así que desde Java esas formas no compilarían. Las fuentes Kotlin conviven con las Java en los directorios `java`.

**MUST NOT** crearse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`, ni ningún `agent_docs/*-rules.md`, ni ningún skill generador.


## Especificación de los tests de `ControlDeAcceso` y `DevolucionDelDirector`, ÍNTEGRA (verbatim de `test-unit-desc.md`)

## Clases auxiliares con lógica propia

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ControlDeAcceso`

**Responsabilidad:** concentrar las comprobaciones de **quién es el usuario autenticado** que los `trigger*` de las tres fases usan como guarda en su primera línea (§9.0.1 del diseño). Dos métodos estáticos, totales: o dejan pasar, o lanzan `BusinessException` con el mensaje recibido por parámetro. `exigeSerElCreador` tiene cuatro llamantes con cuatro mensajes distintos (`CONTINUAR`, `VOLVER`, `PRESENTAR` y `DELETE`, §11 «Las cuatro guardas de autoría») y `exigeMismoCentroQueElExpediente` tiene cuatro con tres mensajes (`ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR` y `DEVOLVER`).
**Por qué sí se testea:** la define el diseño (tabla §6, especificación §9.0.1); no es ninguno de los tres managers; tiene lógica propia (comparación de identificadores con ramas para los casos nulos y decisión de lanzar o no); y es aislable: no necesita `Tramitador`, `EventContext`, base de datos, PDF ni la clase `States` —solo la entidad como POJO y el usuario autenticado—.
**Colaboradores a mockear:** `SecurityUtil` (estático, `Mockito.mockStatic`, que es exactamente para lo que el `CLAUDE.md` del proyecto exige usarlo en vez de `AuthUtils.getUser()`) e `I18n` (estático, programado para devolver su argumento, según la convención ya usada en `CicloServiceImplTest`).

#### Método: `static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException`

- **`exigeSerElCreador_usuarioAutenticadoEsElRegistrador_noLanza`** — Tipo: happy.
  - **Arrange:** expediente con `usuarioRegistrador` de `id = 7`; `SecurityUtil.getUser()` devuelve **otra instancia** de usuario con el mismo `id = 7` (la comparación es por identificador, no por identidad de objeto).
  - **Act:** invocar el método con el mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Assert:** no se lanza ninguna excepción.
- **`exigeSerElCreador_usuarioAutenticadoDistintoDelRegistrador_lanzaConElMensajeRecibido`** — Tipo: error.
  - **Arrange:** expediente con `usuarioRegistrador` de `id = 7`; `SecurityUtil.getUser()` devuelve un usuario de `id = 8`.
  - **Act:** invocar el método con el mensaje `"Solo puede modificar sus propias solicitudes"` (el de `triggerContinuar`, §9.1).
  - **Assert:** se lanza `BusinessException` cuyo mensaje es exactamente `Solo puede modificar sus propias solicitudes`.
- **`exigeSerElCreador_cadaLlamanteRecibeSuPropioMensaje`** — Tipo: error.
  - **Arrange:** mismo desajuste de usuarios que el test anterior.
  - **Act:** invocar el método una vez por cada uno de los otros tres mensajes de los `trigger*` de la fase `SOLICITUD` (§9.1): `"Solo puede volver atrás en sus propias solicitudes"`, `"Solo puede presentar sus propias solicitudes"` y `"Solo puede borrar sus propias solicitudes"`.
  - **Assert:** cada invocación lanza `BusinessException` con exactamente ese mensaje; el texto sale del parámetro y no de un literal interno.
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
  - **Act:** invocar el método con el mensaje `"Solo puede revisar solicitudes de su propio centro"` (el de `triggerEnviarAFirma` y `triggerSubsanar`, §9.2).
  - **Assert:** se lanza `BusinessException` cuyo mensaje es exactamente `Solo puede revisar solicitudes de su propio centro`.
- **`exigeMismoCentro_expedienteSinCentro_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` a `null`; usuario autenticado con `centroActivo` de `id = 3`.
  - **Act:** invocar el método con el mensaje `"Solo puede firmar resoluciones de su propio centro"` (el de `triggerFirmar`, §9.3).
  - **Assert:** se lanza `BusinessException` con ese mensaje exacto.
- **`exigeMismoCentro_usuarioSinCentroActivo_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` de `id = 3`; usuario autenticado con `centroActivo` a `null` (el caso del administrador al que no se le ha asignado centro, §14 nota 13).
  - **Act:** invocar el método con el mensaje `"Solo puede devolver resoluciones de su propio centro"` (el de `triggerDevolver`, §9.3).
  - **Assert:** se lanza `BusinessException` con ese mensaje exacto.
- **`exigeMismoCentro_sinUsuarioAutenticado_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` de `id = 3`; `SecurityUtil.getUser()` devuelve `null`.
  - **Act:** invocar el método con el mensaje `"Solo puede firmar resoluciones de su propio centro"`.
  - **Assert:** se lanza `BusinessException` con ese mensaje exacto (nunca `NullPointerException`).

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.DevolucionDelDirector`

**Responsabilidad:** ser el dueño único de qué campos forman «la devolución del director» —`motivoDevolucion`, `fechaDevolucion` y `devueltoPor`— y dejarlos limpios; la invocan `triggerPresentar` (RN-007), `triggerEnviarAFirma` (RN-011), `triggerSubsanar` (RN-015) y `triggerFirmar` (RN-020), según §9.0.2.
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
  - **Assert:** los cinco campos ajenos a la devolución conservan su valor; la clase es dueña solo de la terna y no limpia la decisión de secretaría (§9.0.2 prohíbe expresamente añadirle esa segunda responsabilidad).

