# Tests unitarios — Anulación de matrícula en ciclo formativo (`AnulacionMatriculaCicloFormativoV1`)

## No aplican tests unitarios de clases

Para este artefacto **no se describe ningún test unitario** de las clases del tipo de expediente, y los únicos tests nuevos son los de las clases auxiliares descritas en «Clases auxiliares con lógica propia». No es una omisión: es una decisión de contrato, por dos motivos.

**1. La conformidad ya la cubren tests existentes, escritos a mano.** Los tests genéricos de `src/test/java/com/educaflow/tiposexpedientes/` recorren automáticamente todos los tipos de expediente del árbol, así que cubren este tipo por construcción, sin tocar nada. Para este diseño comprueban:

- que cada fase declarada (`SOLICITUD`, `REVISION`, `RESOLUCION`) tiene su `PhaseEventManagerImpl` y su `StateEventValidatorImpl` en la carpeta de la fase en minúsculas (`solicitud/`, `revision/`, `resolucion/`);
- que hay exactamente un `InitialEventManagerImpl` en la raíz de la versión, con un único `triggerInitialEvent`, parametrizado con `AnulacionMatriculaCicloFormativoV1`, la primera `<entity>` del `domains.xml`;
- que en cada fase hay un `trigger<Evento>` por cada evento de la fase y un `onEnter<Estado>` por cada estado, ninguno de más;
- que en cada fase hay un `getForState<Estado>InEvent<Evento>` por cada pareja (estado, evento) salvo las de `DELETE`, ninguno de más;
- que el `TipoExpedienteInstance.xml` tiene exactamente un estado inicial (`DATOS_SOLICITUD`), el `events` escrito en todos los estados —incluido `events=""` en `ACEPTADA` y `RECHAZADA`— y perfiles válidos del enum `Profile`;
- que el `estados.puml` dibuja los seis estados con el alias `<FASE>_<ESTADO>` y sin alias fantasma;
- que los `views.xml` de cada fase tienen el form genérico de cada estado, el form con perfil donde procede, sin duplicados, y que todo evento tiene su botón y todo botón su evento;
- que no se referencia la clase `States` de otro tipo ni de otra versión, y que el `<defaultTipoExpediente>` (`v1`) apunta a una carpeta de versión que existe.

Esos tests **se escriben a mano** y los `.java` son su fuente de verdad: este diseño **no propone crearlos, modificarlos, ampliarlos ni regenerarlos**.

**2. Las clases del tipo no son unitariamente testeables con sentido.** `InitialEventManagerImpl`, `PhaseEventManagerImpl` y `StateEventValidatorImpl` no tienen lógica propia aislable: dependen del `Tramitador`, del `EventContext` (`updateState`, `createRegistroEntrada`, `createRegistroSalida`), de la persistencia, de la generación y firma de PDF (`getDocumentoPdf`, `AlmacenClaveResolver`) y de la clase `States` generada por el build. Un test unitario tendría que mockear todo eso y acabaría verificando el mock, no el trámite.

**Dónde se verifica el comportamiento real.** End-to-end, en `test-e2e-desc.md`: sus 56 tests cubren las diez transiciones de la máquina de estados, cada perfil que las dispara, cada pareja (estado, evento) con reglas que debe impedir avanzar, la vista genérica de solo lectura de todos los estados y los cinco tests de aislamiento. Y en el paso final del diseño (Paso 17), con `./run.sh` y el recorrido en runtime de todos los estados.

## Clases del tipo — excluidas y por qué

Además de las tres clases del tipo, la tabla lista las demás piezas que el diseño crea o modifica y que **no** cumplen las cuatro condiciones para describirse (definida por el diseño · no es uno de los tres managers · tiene lógica propia · es aislable sin `Tramitador`/`EventContext`/BD/PDF/`States`).

| Clase | Motivo de la exclusión |
|---|---|
| `…anulacion_matricula_ciclo_formativo.v1.InitialEventManagerImpl` | Sin lógica aislable: inicializa la entidad (centro, usuario registrador, curso académico, nombre y localidad del centro) y su efecto lo ejerce el `Tramitador`. Cubierta por los tests existentes (forma: I1–I2, M1) y por `test-e2e-desc.md` (comportamiento). |
| `…v1.solicitud.PhaseEventManagerImpl` | Sin lógica aislable: sus `trigger*` pasan por el `EventContext` (`updateState`, `createRegistroEntrada`), por la generación y firma del PDF de la solicitud y por `States`. Forma: E0–E5; comportamiento: E2E. |
| `…v1.revision.PhaseEventManagerImpl` | Igual: `updateState`, generación del PDF de la resolución y persistencia. |
| `…v1.resolucion.PhaseEventManagerImpl` | Igual: `updateState`, firma en servidor por cargo (`AlmacenClaveResolver`) y `createRegistroSalida`. |
| `…v1.solicitud.StateEventValidatorImpl` | Declarativa: no ejecuta nada; sus reglas y su `AllowProperties` los interpreta el `Tramitador`. Forma: V0–V2. |
| `…v1.revision.StateEventValidatorImpl` | Declarativa, igual. |
| `…v1.resolucion.StateEventValidatorImpl` | Declarativa, igual. |
| `…v1.ControlDeAcceso.exigeOstentarElPerfilDelEstado` | **Solo este método** de la clase. No es aislable: pregunta el perfil del estado a la clase **`States` generada** (`States.INSTANCE.getState(codePhase, codeState)`) y la pertenencia a `PerfilesUsuarioService`, resuelto con `Beans.get(...)`. Con las dos cosas mockeadas el aserto verificaría el mock. Su comportamiento se verifica en la **comprobación en runtime del Paso 17**: el administrador, con centro activo en el del expediente, pide la vista con el `_profile` a mano (`_profile=SECRETARIO` en `PENDIENTE_REVISION` y `_profile=DIRECTOR` en `PENDIENTE_FIRMA_DIRECTOR`) y los cuatro botones fallan con «Solo la secretaría del centro puede revisar esta solicitud», «Solo el director del centro puede firmar la resolución» y «Solo el director del centro puede devolver la resolución a la secretaría»; ningún test de build lo ve. T-056 solo comprueba una cosa distinta y complementaria: que las dos bandejas nuevas **no listan** el expediente a quien no ostenta ese perfil. Los otros dos métodos de la clase **sí** se describen abajo. |
| `…v1.SinOtraSolicitudEnCursoParaElMismoCiclo` (en `ReglasAnulacionMatricula.kt`) | No es aislable: consulta la base de datos (`JpaRepository.of(AnulacionMatriculaCicloFormativoV1::class.java).all().filter(...)`) para buscar otra solicitud abierta del mismo alumno, ciclo y curso académico. Su cobertura real es E2E: el duplicado rechazado en la validación de `CONTINUAR` (T-018, «Ya tiene una solicitud de anulación en curso para este ciclo»). La otra rama —centro sin `cursoAcademico` configurado, que falla con «Su centro no tiene configurado el curso académico; avise a la secretaría del centro»— **no es reproducible con la demo cargada**, porque los dos centros tienen `curso="2024"` (§14 nota 15): no la verifica ningún test E2E. |
| `com.educaflow.tramites.util.bandeja.BandejaPorPerfilController` | No es aislable: `JPA.all(Expediente.class).filter(...)` sobre la base de datos, más `SecurityUtil.getUser()` y `PerfilesUsuarioService`. Mockear el repositorio dejaría el aserto sobre el mock. Es **interfaz, no defensa** (§16.0), y se verifica abriendo las dos bandejas en runtime (Paso 16.0, «comprobación en runtime») y en `test-e2e-desc.md` (T-056). |
| `com.educaflow.subsystem.sistemaeducativo.service.impl.NivelServiceImpl` | El diseño solo le añade `"nombreCorto"` a la whitelist de `allowPropertiesEditables()`: es una **declaración constante**, no lógica propia (no calcula, no transforma, no ramifica), así que no cumple la tercera condición. El diseño no declara para esta clase ningún cambio bajo `src/test/`. El agujero que el diseño describe (descarte silencioso al guardar, sin error ni aviso) lo destapa la **verificación en runtime del Paso 4**: dar de alta un Nivel con nombre corto desde «Sistema educativo → Niveles», editárselo y comprobar que el valor sigue ahí tras recargar la ficha. |
| `com.educaflow.base.infrastructure.validation.rules.FirmaPdf` | No es aislable en la rama que el diseño toca: para decidir si la firma es válida lee los dos `MetaFile` del disco (`MetaFileHelper.getDocumentoPdf`) y llama a `DocumentoPdfUtil.validateFirmaPdf` con PDFs reales y el DNI del usuario autenticado. La sustitución del mensaje de firma inválida se verifica E2E en **T-020** (firma con un certificado que no es el del alumno: «La firma no es válida o no corresponde a su documento de identidad»), y la ausencia de firma en **T-019**; ambos son manuales, con la aplicación de firma del ciudadano. |

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

## Clases auxiliares con lógica propia

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ControlDeAcceso`

**Responsabilidad:** concentrar las guardas de identidad del usuario autenticado que los ocho `trigger*` de las tres fases llaman en su primera línea (§9.0.1). El mensaje llega **por parámetro** porque la especificación fija uno distinto en cada evento; la decisión vive en un solo sitio.
**Por qué sí se testea:** la define el diseño (tabla §6 y §9.0.1), no es ninguno de los tres managers, decide con ramas (compara identidades y lanza o deja pasar) y los dos métodos descritos aquí son aislables: no tocan base de datos, ni PDF, ni `EventContext`, ni la clase `States`.
**Colaboradores a mockear:** `SecurityUtil` (estático, `Mockito.mockStatic`, para el usuario autenticado) e `I18n` (estático, programado para devolver su propio argumento, porque sin el contexto de Axelor arrancado no hay traducción y lo que se comprueba es **qué texto elige** la guarda). Las entidades (`AnulacionMatriculaCicloFormativoV1`, `User`, `Centro`) se construyen a mano con sus setters, sin mocks.
**Método excluido:** `exigeOstentarElPerfilDelEstado`, por el motivo de la tabla de exclusiones.

#### Método: `public static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException`

- **`exigeSerElCreador_usuarioAutenticadoEsElRegistrador_noLanza`** — Tipo: happy.
  - **Arrange:** expediente con `usuarioRegistrador` = un `User` de `id` 7; `SecurityUtil.getUser()` devuelve un `User` de `id` 7.
  - **Act:** invocar la guarda con un mensaje cualquiera.
  - **Assert:** no lanza (el test pasa por no lanzar nada).
- **`exigeSerElCreador_usuarioAutenticadoDistintoDelRegistrador_lanzaConElMensajeRecibido`** — Tipo: error.
  - **Arrange:** expediente con `usuarioRegistrador` de `id` 7; `SecurityUtil.getUser()` devuelve un `User` de `id` 8; mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` cuyo `getMessage()` es exactamente `"Solo puede modificar sus propias solicitudes"`.
- **`exigeSerElCreador_cadaLlamanteRecibeSuPropioMensaje`** — Tipo: borde.
  - **Arrange:** el mismo escenario de fallo, invocado dos veces con dos mensajes distintos: `"Solo puede modificar sus propias solicitudes"` (el de `CONTINUAR`) y `"Solo puede borrar sus propias solicitudes"` (el de `DELETE`).
  - **Act:** invocar la guarda dos veces.
  - **Assert:** cada excepción lleva **su** mensaje, literal; la guarda no compone ni sustituye ningún texto propio.
- **`exigeSerElCreador_expedienteSinUsuarioRegistrador_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `usuarioRegistrador` a `null`; `SecurityUtil.getUser()` devuelve un `User` de `id` 7; mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con ese mismo mensaje — la guarda es **total** y falla cerrado cuando le falta el dato con el que decidir; **MUST NOT** dejar pasar.
- **`exigeSerElCreador_sinUsuarioAutenticado_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `usuarioRegistrador` de `id` 7; `SecurityUtil.getUser()` devuelve `null`; mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con ese mensaje, sin `NullPointerException`.

#### Método: `public static void exigeMismoCentroQueElExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException`

- **`exigeMismoCentro_centroActivoIgualAlDelExpediente_noLanza`** — Tipo: happy.
  - **Arrange:** expediente con `centro` de `id` 3; `SecurityUtil.getUser()` devuelve un `User` cuyo `centroActivo` tiene `id` 3.
  - **Act:** invocar la guarda.
  - **Assert:** no lanza.
- **`exigeMismoCentro_centroActivoDistinto_lanzaConElMensajeRecibido`** — Tipo: error.
  - **Arrange:** expediente con `centro` de `id` 3; usuario con `centroActivo` de `id` 4; mensaje `"Solo puede revisar solicitudes de su propio centro"`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` cuyo `getMessage()` es exactamente `"Solo puede revisar solicitudes de su propio centro"`.
- **`exigeMismoCentro_expedienteSinCentro_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` a `null`; usuario con `centroActivo` de `id` 3.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con el mensaje recibido (falla cerrado).
- **`exigeMismoCentro_usuarioSinCentroActivo_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` de `id` 3; usuario con `centroActivo` a `null`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con el mensaje recibido.
- **`exigeMismoCentro_sinUsuarioAutenticado_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` de `id` 3; `SecurityUtil.getUser()` devuelve `null`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con el mensaje recibido, sin `NullPointerException`.

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.DevolucionDelDirector`

**Responsabilidad:** ser el dueño único de qué campos forman «la devolución del director» —`motivoDevolucion`, `fechaDevolucion` y `devueltoPor`— y dejarlos limpios. La llaman `triggerPresentar`, `triggerEnviarAFirma`, `triggerSubsanar` y `triggerFirmar` (§9.0.2).
**Por qué sí se testea:** la define el diseño (tabla §6 y §9.0.2), no es ninguno de los tres managers, transforma el estado de la entidad (es el motivo de que exista: que la lista de los tres campos esté en un solo sitio) y es aislable por completo — solo setters sobre la entidad, sin `EventContext`, sin repositorio y sin PDF.
**Colaboradores a mockear:** ninguno.

#### Método: `public static void borrar(AnulacionMatriculaCicloFormativoV1 expediente)`

- **`borrar_expedienteQueVieneDeUnaDevolucion_dejaLosTresCamposANull`** — Tipo: happy.
  - **Arrange:** expediente con `motivoDevolucion` = `"Falta el NIA"`, `fechaDevolucion` = una fecha cualquiera y `devueltoPor` = un `User`.
  - **Act:** `DevolucionDelDirector.borrar(expediente)`.
  - **Assert:** los **tres** campos quedan a `null`. Es el test que destapa que alguien añada un cuarto dato a la devolución y se olvide de limpiarlo.
- **`borrar_expedienteSinDevolucionPrevia_esIdempotenteYNoLanza`** — Tipo: borde.
  - **Arrange:** expediente recién construido, con los tres campos ya a `null`.
  - **Act:** invocar `borrar` dos veces seguidas.
  - **Assert:** no lanza y los tres campos siguen a `null`; ningún llamante necesita preguntarse si le toca.
- **`borrar_noTocaNingunOtroCampoDelExpediente`** — Tipo: borde.
  - **Arrange:** expediente con la devolución rellena **y además** `sentidoRevision`, `motivoRechazo`, `fechaRevision` y `revisadoPor` con valor.
  - **Act:** `DevolucionDelDirector.borrar(expediente)`.
  - **Assert:** esos cuatro campos conservan su valor — la clase es dueña de la devolución del director y **solo** de ella; limpiar la decisión de secretaría no es cosa suya.

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula`

**Responsabilidad:** ser el dueño único **en servidor** de la clasificación «la revisión rechaza la anulación si y solo si `sentidoRevision == RECHAZAR`», de la que se siguen sus dos consecuencias (la resolución desestima y el motivo del rechazo aplica). Tiene cuatro consumidores: `triggerEnviarAFirma`, la rama del validador de `ENVIAR_A_FIRMA` y las dos expresiones Groovy de `resolucion.xml` (§9.0.3).
**Por qué sí se testea:** la define el diseño (tabla §6, §9.0.3 y §10.0), no es ninguno de los tres managers, es una decisión con ramas y es aislable del todo — un predicado puro sobre la entidad, sin colaboradores.
**Colaboradores a mockear:** ninguno.

#### Método: `@JvmStatic fun esRechazo(expediente: AnulacionMatriculaCicloFormativoV1): Boolean`

- **`esRechazo_sentidoRechazar_devuelveTrue`** — Tipo: happy.
  - **Arrange:** expediente con `sentidoRevision` = `SentidoRevisionAnulacionMatriculaCicloFormativoV1.RECHAZAR`.
  - **Act:** `ReglasAnulacionMatricula.esRechazo(expediente)` — invocado como estático desde Java, que es lo que el `@JvmStatic` promete y lo que hacen el `trigger*` y (por FQCN) las dos expresiones del PDF.
  - **Assert:** devuelve `true`.
- **`esRechazo_sentidoAceptar_devuelveFalse`** — Tipo: happy.
  - **Arrange:** expediente con `sentidoRevision` = `ACEPTAR`.
  - **Act:** invocar el predicado.
  - **Assert:** devuelve `false`.
- **`esRechazo_sinSentidoElegido_devuelveFalse`** — Tipo: borde.
  - **Arrange:** expediente con `sentidoRevision` a `null` (la secretaría aún no ha decidido).
  - **Act:** invocar el predicado.
  - **Assert:** devuelve `false` y **no lanza**: es total, y `false` es lo que sus cuatro consumidores esperan.

### Clase: `com.educaflow.subsystem.sistemaeducativo.db.Ciclo` — campo derivado `gradoNivel`

**Responsabilidad:** dar el texto del grado o del nivel que imprimen los dos documentos («en el Ciclo Formativo de Grado ____») y que la pantalla muestra junto al ciclo (CC-014). La entidad la genera el build a partir de `subsystem/sistemaeducativo/domains/Ciclo.xml`, que el diseño modifica añadiendo ese campo `transient` con cuerpo Java (§4, «Cambios en el catálogo»).
**Por qué sí se testea:** el diseño lo define (tabla §6, fila de `domains/Ciclo.xml`, y §4), no es ninguno de los tres managers, es una decisión con ramas (nivel con nombre corto / nivel sin nombre corto / sin nivel) y es aislable: se construye un `Ciclo` con su `Grado` y su `Nivel` en memoria, sin base de datos y sin `States`.
**Colaboradores a mockear:** ninguno.

#### Método: `public String getGradoNivel()`

- **`getGradoNivel_conNivelConNombreCorto_devuelveElNombreCortoDelNivel`** — Tipo: happy.
  - **Arrange:** `Ciclo` con `grado` de `name` `"Grado Superior"` y `nivel` con `name` `"Grado Superior"` y `nombreCorto` `"Superior"`.
  - **Act:** `ciclo.getGradoNivel()`.
  - **Assert:** devuelve `"Superior"`.
- **`getGradoNivel_conNivelSinNombreCorto_devuelveElNameDelNivel`** — Tipo: borde.
  - **Arrange:** `Ciclo` con `nivel` cuyo `nombreCorto` está en blanco (cadena vacía) y cuyo `name` es `"Grado Medio"`.
  - **Act:** invocar el getter.
  - **Assert:** devuelve `"Grado Medio"` — el campo es nuevo sobre una tabla con filas, así que el nivel sin nombre corto es el caso normal mientras no se rellene.
- **`getGradoNivel_conNivelConNombreCortoNulo_devuelveElNameDelNivel`** — Tipo: borde.
  - **Arrange:** el mismo caso con `nombreCorto` a `null`.
  - **Act:** invocar el getter.
  - **Assert:** devuelve el `name` del nivel, sin `NullPointerException`.
- **`getGradoNivel_sinNivel_devuelveElNameDelGrado`** — Tipo: happy.
  - **Arrange:** `Ciclo` con `nivel` a `null` y `grado` de `name` `"Curso de especialización"` (es el caso de `IABD`, el ciclo que da de alta el Paso 4).
  - **Act:** invocar el getter.
  - **Assert:** devuelve `"Curso de especialización"`.
- **`getGradoNivel_trasCambiarElNivel_recalculaEnCadaLectura`** — Tipo: borde.
  - **Arrange:** `Ciclo` con un nivel; se lee `gradoNivel`, se le cambia el nivel por otro con distinto nombre corto y se vuelve a leer.
  - **Act:** dos lecturas del getter con un cambio de nivel en medio.
  - **Assert:** la segunda devuelve el valor del nivel nuevo — es un campo `transient` calculado en cada lectura, no un valor cacheado.

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

## Cobertura

- Clases auxiliares descritas: **9** (`ControlDeAcceso` —dos de sus tres métodos—, `DevolucionDelDirector`, `ReglasAnulacionMatricula`, `Ciclo`, `Required`, `MinLength`, `MaxLength`, `Pattern`, `NoAdmitido`), con **42** tests.
- Clases del tipo excluidas: `InitialEventManagerImpl`; los tres `PhaseEventManagerImpl` (`solicitud`, `revision`, `resolucion`); los tres `StateEventValidatorImpl` (`solicitud`, `revision`, `resolucion`). Excluidas además, por no cumplir las cuatro condiciones: `ControlDeAcceso.exigeOstentarElPerfilDelEstado`, `SinOtraSolicitudEnCursoParaElMismoCiclo`, `BandejaPorPerfilController`, `NivelServiceImpl` y `FirmaPdf`.
- Tests nuevos a crear: **7** ficheros bajo `src/test/java/…`, ninguno bajo `com/educaflow/tiposexpedientes/` ni bajo `com/educaflow/views`.
