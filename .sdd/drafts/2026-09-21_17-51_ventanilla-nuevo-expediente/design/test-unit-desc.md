# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Aserciones con `org.junit.jupiter.api.Assertions` (`assertEquals`, `assertSame`, `assertThrows`, `assertTrue`…), **no** AssertJ; los colaboradores `@Inject` se inyectan por reflexión sobre la instancia, igual que en `TareaFirmaServiceImplTest` y `TareaFirmaControllerTest`. Sin base de datos real en ningún test.
- **Forma canónica del mensaje de la ventanilla.** Cada `BusinessMessage` que escribe `AsistenteNuevoExpedienteServiceImpl` lleva los **tres** argumentos que fija el Paso 3 de `design.md`: `new BusinessMessage("<campo>", I18n.get("<mensaje>"), I18n.get(Mapper.of(AsistenteNuevoExpediente.class).getProperty("<campo>").getTitle()))`. Por eso los tests de una regla `V-` propia comprueban **`getMessage()`, `getFieldName()` y `getLabel()`**, no solo el texto: el `fieldName` es lo que hace que el formulario marque el campo, y el `label` **MUST** salir del `title` del dominio y no de un literal en castellano. `Mapper.of(...)` se resuelve sin base de datos (lee la clase generada), así que **no** se mockea; el que sí se mockea es `I18n` (estático, con la identidad `I18n.get(k)` → `k`), que es lo que permite comparar el literal exacto. Títulos vigentes en `domains/AsistenteNuevoExpediente.xml`: `centro` → «Centro», `tramite` → «Trámite», `presentadoEnPapel` → «¿Cómo se presenta?», `presentadoEnRepresentacion` → «¿Para quién es el expediente?».
- **Los mensajes del motor no siguen esa forma y no deben reescribirse.** `V-AsistenteNuevoExpediente-004`, `-005` y `-006` los redacta `TramitadorService.validateCentroYPerfil` / `validateRepresentacion` con el constructor de **un solo argumento** (`fieldName` y `label` a `null`), y `validateAlta` los devuelve **tal cual**. Sus tests comprueban el texto y que la ventanilla no añade ni cambia nada.

### Fixtures comunes de `AsistenteNuevoExpedienteServiceImpl` y del controlador

Los comparten todos los tests de las dos clases; se preparan en el `@BeforeEach` y cada test ajusta lo que necesita.

| Fixture | Contenido |
|---|---|
| `USUARIO` | `new User()` con `id = 7`; sus `centroUsuarios` los pone cada test con el helper `centroUsuario(...)`. Lo devuelve `SecurityUtil.getUser()` (estático mockeado). |
| `CENTRO_A` / `CENTRO_B` | `new Centro()` con `id = 1` / `id = 2` y `name` distinto. El `id` es **obligatorio** en el fixture: `User.getCentroUsuario(centro)` devuelve `null` si el centro no lo lleva. |
| `TIPO_TRAMITE_ALUMNO` | `new TipoTramite()` con `name = "Matrícula"` y `tipoUsuario` = `new TipoUsuario()` con `codigo = "ALUMNO"`. |
| `TIPO_TRAMITE_SIN_TIPO_USUARIO` | `new TipoTramite()` con `tipoUsuario = null` (el m2o no es `required`). |
| `TRAMITE` | `new Tramite()` con `id = 10`, `name = "Anulación de matrícula"`, `help = "Texto de ayuda"`, `permitidoPresentarEnRepresentacion = true`, `tipoTramite = TIPO_TRAMITE_ALUMNO` y `defaultTipoExpediente` no nulo. |
| `OTRO_TRAMITE` | `new Tramite()` con `id = 11` y el mismo tipo de trámite, para los casos de dos filas en el catálogo. |
| `TRAMITE_NO_EVALUABLE` | `new Tramite()` con `id = 99`, `tipoTramite = null` y `defaultTipoExpediente = null`: la fila que `findTramitesEvaluables()` **no** devuelve. |
| `centroUsuario(centro, codigos…)` | helper que arma un `CentroUsuario` con ese `centro`, su `usuario = USUARIO` y un `CentroUsuarioTipoUsuario` por cada código de tipo de usuario; se añade a `USUARIO.centroUsuarios` para que `USUARIO.getCentroUsuario(centro)` lo encuentre. |
| `mensajes(texto)` | helper que devuelve `Optional.of(BusinessMessages.single(texto))`, la forma en que `TramitadorService` rechaza (un `BusinessMessage` con `fieldName` y `label` a `null`). |

**Stub del oráculo del motor.** `tramitadorService.validateTriggerInitialEvent(any(ContextoTramitacion.class))` se programa con `thenAnswer` que **inspecciona el contexto recibido** (`profile()`, `presentadoEnPapel()`, `presentadoEnRepresentacion()`) y devuelve `Optional.empty()` o el mensaje que ese test quiera para esa pareja. Es la única forma de describir las cuatro celdas de la matriz de `recalcular` con un solo stub. Recordatorio de la correspondencia real del enum `Profile`, que el servicio deduce en `perfilDeInicioPara` a partir de `puedeCrearExpediente()` y `permitePresentacionEnPapel()` **sin nombrar ninguna constante**: `presentadoEnPapel = false` → `CREADOR`, `presentadoEnPapel = true` → `TRAMITADOR`.

---

## Clase: `com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente`  —  modelo de dominio (campos derivados)

**Responsabilidad:** el modelo de pantalla del asistente (`persistable="false"`). No tiene ciclo de vida ni lógica propia **salvo los dos campos derivados de solo lectura** `nombreTramite` y `ayudaTramite`, cuyo cuerpo vive en `design/domains/AsistenteNuevoExpediente.xml` y que el generador de AOP emite como `computeNombreTramite()` / `computeAyudaTramite()`, invocados por el getter en cada lectura.
**Colaboradores a mockear:** `I18n` (estático, `mockStatic`, solo en los tests de `nombreTramite`, cuyo cuerpo es el **único** sitio del lado Java que compone la clave `value:` — el diseño **no** define ninguna clase auxiliar para eso). No hay repositorios ni servicios: la entidad se instancia con `new` y sus campos se rellenan con setters.
**Origen diseño:** `design.md` Paso 1 y tabla «Campos calculados» — `CC-AsistenteNuevoExpediente-001` (nombre del trámite) y `CC-AsistenteNuevoExpediente-002` (ayuda del trámite), los dos con `momento: lectura` y `sobreescribible: nunca`.

> **Cómo se describe el «sobreescribible: nunca»:** el getter generado guarda el resultado del `compute*()` en el campo de respaldo antes de devolverlo, así que sembrar ese campo con un valor distinto (`setNombreTramite("obsoleto")`) y comprobar que la lectura devuelve el calculado es lo que demuestra que el cálculo se ejecutó y que nada de lo que llegue del cliente sobrevive. Es la misma técnica del test hermano `com.educaflow.subsystem.sistemaeducativo.db.GradoTest`.

### Método: `String getNombreTramite()`

- **`getNombreTramite_sinTramite_devuelveNull`** — Tipo: borde. Verifica: `CC-AsistenteNuevoExpediente-001`.
  - **Arrange:** `AsistenteNuevoExpediente` nuevo, sin `tramite`, con el campo de respaldo sembrado (`setNombreTramite("valor que llega del cliente")`). Sin `mockStatic(I18n)`: el cálculo no llega a `I18n`.
  - **Act:** `getNombreTramite()`.
  - **Assert:** devuelve `null` (el valor sembrado no sobrevive).
- **`getNombreTramite_conTramite_devuelveElNombreTraducidoConElPrefijoValue`** — Tipo: happy. Verifica: `CC-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean con `tramite = TRAMITE` (`name = "Anulación de matrícula"`); `mockStatic(I18n)` programado para que `I18n.get("value:Anulación de matrícula")` devuelva `"Anul·lació de matrícula"`.
  - **Act:** `getNombreTramite()`.
  - **Assert:** devuelve `"Anul·lació de matrícula"`, y `verify` de que la clave consultada es **exactamente** `"value:Anulación de matrícula"` (con el prefijo `value:`, la clave real con la que el build registra el `<name>` de un `TramiteInstance.xml`) y de que **nunca** se consulta `I18n.get("Anulación de matrícula")` sin prefijo.
- **`getNombreTramite_conValorPrevioEnElCampo_loRecalculaYLoDescarta`** — Tipo: borde. Verifica: `CC-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean con `tramite = TRAMITE` y el campo de respaldo sembrado con `"otro nombre"`; `mockStatic(I18n)` con la identidad (`I18n.get(k)` → `k`).
  - **Act:** `getNombreTramite()`.
  - **Assert:** devuelve `"value:Anulación de matrícula"`, no `"otro nombre"`: el campo es de solo lectura y no conserva nada de lo que le asignen.

### Método: `String getAyudaTramite()`

- **`getAyudaTramite_sinTramite_devuelveNull`** — Tipo: borde. Verifica: `CC-AsistenteNuevoExpediente-002`.
  - **Arrange:** bean sin `tramite`, con el campo de respaldo sembrado (`setAyudaTramite("ayuda que llega del cliente")`).
  - **Act:** `getAyudaTramite()`.
  - **Assert:** devuelve `null`.
- **`getAyudaTramite_conTramite_devuelveLaAyudaDelTramite`** — Tipo: happy. Verifica: `CC-AsistenteNuevoExpediente-002`.
  - **Arrange:** bean con `tramite = TRAMITE` (`help = "Texto de ayuda"`).
  - **Act:** `getAyudaTramite()`.
  - **Assert:** devuelve `"Texto de ayuda"` (sin traducir: el cuerpo devuelve `tramite.getHelp()` tal cual).
- **`getAyudaTramite_tramiteSinAyuda_devuelveNull`** — Tipo: borde. Verifica: `CC-AsistenteNuevoExpediente-002`.
  - **Arrange:** bean con un `Tramite` cuyo `help` es `null`, y el campo de respaldo sembrado con `"ayuda vieja"`.
  - **Act:** `getAyudaTramite()`.
  - **Assert:** devuelve `null` (el `showIf="ayudaTramite"` de la vista depende de este `null`).

---

## Clase: `com.educaflow.system.ventanilla.service.impl.AsistenteNuevoExpedienteServiceImpl`  —  servicio

**Responsabilidad:** las tres acciones del asistente (`prepararCentros`, `prepararTramites`, `recalcular`), sus validadores, el validador completo de la creación (`validateTriggerInitialEvent`) y la whitelist de cada acción. Concentra las nueve reglas `V-`, las tres reglas `R-` de asignación y el oráculo privado `validateAlta` / `admiteAlta`.
**Colaboradores a mockear:** `TramitadorService`, `PerfilesUsuarioService` y `VentanillaRepository` (los tres campos `@Inject`, inyectados por reflexión sobre la instancia); `Repository<AsistenteNuevoExpediente>` (argumento del constructor `(Class, Repository)` que exige `ModelServiceFactory`, nunca se usa porque el modelo no se persiste); `SecurityUtil` (estático, `mockStatic`, `getUser()` → `USUARIO`); `I18n` (estático, `mockStatic`, con la identidad para poder comparar el literal exacto de cada mensaje). **Nada de base de datos real.**
**Origen diseño:** `design.md` Pasos 2, 3 y 4 (firmas, acciones, métodos de validación, `AllowProperties`, **Action Rules** —donde viven `fireActionRule_AsignarArranqueDelAsistente`, `fireActionRule_AsignarTramitesDisponibles`, `fireActionRule_AsignarPresentacion` y sus dos mitades `fireActionRule_AsignarFormaDePresentar` y `fireActionRule_AsignarDestinatario`— y «Otras funciones»), `design/rules/R-AsistenteNuevoExpediente-005.md` (único dueño del detalle de la regla de `recalcular`) y las tablas de trazabilidad de `V-AsistenteNuevoExpediente-001…009`, `R-AsistenteNuevoExpediente-003/004/005` y la frontera de confianza por acción.

> **Los métodos privados no llevan sección propia.** Los dos dueños de regla `validateCentroIndicado` (V-001) y `validateTramiteEvaluable` (V-009), el oráculo `validateAlta` / `admiteAlta`, `validateAfinadoDestinatario`, los cinco `fireActionRule_*` (`AsignarArranqueDelAsistente`, `AsignarTramitesDisponibles`, `AsignarPresentacion` y sus dos mitades `AsignarFormaDePresentar` y `AsignarDestinatario`), `getTramitesCandidatos`, `getCentrosCandidatos`, `perfilDeInicioPara`, `esDestinatarioDelTramite`, `esFamiliar`, `tieneTipoUsuario` y el `record MatrizPresentacion` se ejercen **a través** del método público que los invoca, que es el único contrato del servicio. Cada test dice, en el `Assert`, qué comportamiento del privado está fijando.

> **Los dos dueños privados se ejercen desde cada llamante, no una sola vez.** V-001 tiene tres llamantes (`validatePrepararTramites`, `validateRecalcular` puerta 1 y `validateTriggerInitialEvent` puerta 1) y V-009 dos (`validateRecalcular` puerta 2 y `validateTriggerInitialEvent` puerta 2); cada uno tiene su test de fallo con el **mismo mensaje, el mismo `fieldName` y el mismo `label`**, que es lo que demuestra que la condición y el literal tienen un único dueño. Y cada validador público se prueba también con lo que **no** comprueba (`validatePrepararTramites` no mira el trámite; `validateRecalcular` no mira las dos respuestas del usuario), que es lo que demuestra que ninguno ejecuta el validador de otra acción.

### Método: `AsistenteNuevoExpediente prepararCentros(AsistenteNuevoExpediente asistente)`

Aplica una sola regla, `R-AsistenteNuevoExpediente-003`, vía `fireActionRule_AsignarArranqueDelAsistente`: asigna incondicionalmente `centrosDisponibles`, `hayQueElegirCentro` y `centro`.

- **`prepararCentros_variosCentrosConTramitesIniciables_losOfreceTodosYExigeElegir`** — Tipo: happy. Verifica: `R-AsistenteNuevoExpediente-003`.
  - **Arrange:** `USUARIO` con `centroUsuario(CENTRO_A)` y `centroUsuario(CENTRO_B)`; `ventanillaRepository.findTramitesEvaluables()` → `List.of(TRAMITE)`; `perfilesUsuarioService.getPerfilesDeInicioSobreTramite(TRAMITE, USUARIO, CENTRO_A)` → `Set.of(Profile.CREADOR)` y lo mismo para `CENTRO_B`; bean nuevo y vacío.
  - **Act:** `prepararCentros(asistente)`.
  - **Assert:** `centrosDisponibles` contiene exactamente `CENTRO_A` y `CENTRO_B`; `hayQueElegirCentro` es `true`; `centro` es `null`; el retorno es **el mismo bean** (`assertSame`).
- **`prepararCentros_unSoloCentroConTramitesIniciables_loFijaYNoExigeElegir`** — Tipo: happy. Verifica: `R-AsistenteNuevoExpediente-003`.
  - **Arrange:** igual, pero `getPerfilesDeInicioSobreTramite(..., CENTRO_B)` → `Set.of()`.
  - **Act:** `prepararCentros(asistente)`.
  - **Assert:** `centrosDisponibles` contiene solo `CENTRO_A`; `hayQueElegirCentro` es `false`; `centro` es `CENTRO_A` (el asistente se salta el paso 1).
- **`prepararCentros_ningunCentroConTramitesIniciables_dejaLaListaVaciaYElCentroNulo`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-003`.
  - **Arrange:** `USUARIO` con los dos centros; `findTramitesEvaluables()` → `List.of(TRAMITE)`; `getPerfilesDeInicioSobreTramite` → `Set.of()` en los dos centros.
  - **Act:** `prepararCentros(asistente)`.
  - **Assert:** `centrosDisponibles` vacío; `hayQueElegirCentro` es `false`; `centro` es `null` (los tres valores con los que la vista dispara el aviso «No puede crear expedientes en ninguno de sus centros»).
- **`prepararCentros_usuarioSinCentros_dejaLaListaVaciaYNoClasificaNada`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-003`.
  - **Arrange:** `USUARIO` con `centroUsuarios` vacío.
  - **Act:** `prepararCentros(asistente)`.
  - **Assert:** `centrosDisponibles` vacío, `hayQueElegirCentro` `false`, `centro` `null`; `verifyNoInteractions(perfilesUsuarioService)` (sin centros no hay nada que clasificar). **No** se comprueba `verifyNoInteractions(ventanillaRepository)`: la lectura del catálogo es **una sola, antes** del bucle de centros, así que se hace aunque el bucle no dé ninguna vuelta.
- **`prepararCentros_catalogoSinTramitesEvaluables_noOfreceNingunCentro`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-003`.
  - **Arrange:** `USUARIO` con los dos centros; `findTramitesEvaluables()` → `List.of()` (todas las filas del catálogo sin tipo de trámite o sin tipo de expediente por defecto).
  - **Act:** `prepararCentros(asistente)`.
  - **Assert:** `centrosDisponibles` vacío y `centro` `null`; `verifyNoInteractions(perfilesUsuarioService)` (no se pregunta por un trámite que el sistema no sabe juzgar).
- **`prepararCentros_conVariosCentros_leeElCatalogoUnaSolaVez`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-003`.
  - **Arrange:** `USUARIO` con **tres** centros (`CENTRO_A`, `CENTRO_B` y un tercero); `findTramitesEvaluables()` → `List.of(TRAMITE, OTRO_TRAMITE)`; `getPerfilesDeInicioSobreTramite` → `Set.of(Profile.CREADOR)` para cualquier pareja.
  - **Act:** `prepararCentros(asistente)`.
  - **Assert:** `verify(ventanillaRepository, times(1)).findTramitesEvaluables()`: la lectura va **antes** del bucle y el catálogo leído se le pasa al cálculo de candidatos, así que no hay una consulta por centro; y `verify(perfilesUsuarioService, times(6)).getPerfilesDeInicioSobreTramite(...)` — tres centros × dos trámites —, que es el único trabajo que sí escala con los centros.
- **`prepararCentros_conCentrosYCentroEnviadosPorElCliente_losSobrescribeIncondicionalmente`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-003`.
  - **Arrange:** bean que llega con `centrosDisponibles = Set.of(CENTRO_B)`, `centro = CENTRO_B` y `hayQueElegirCentro = true`; `USUARIO` con **solo** `centroUsuario(CENTRO_A)`; `ventanillaRepository.findTramitesEvaluables()` → `List.of(TRAMITE)`; `perfilesUsuarioService.getPerfilesDeInicioSobreTramite(TRAMITE, USUARIO, CENTRO_A)` → `Set.of(Profile.CREADOR)` (es el único centro en el que puede iniciar; sin estos dos stubs el catálogo llegaría vacío por defecto y no habría ningún centro candidato).
  - **Act:** `prepararCentros(asistente)`.
  - **Assert:** `centrosDisponibles` contiene solo `CENTRO_A`, `centro` es `CENTRO_A` y `hayQueElegirCentro` es `false`: los tres campos son del servidor y se asignan sin ninguna guarda de nulidad, así que nada de lo que llegue del cliente sobrevive.

### Método: `AsistenteNuevoExpediente prepararTramites(AsistenteNuevoExpediente asistente)`

Aplica una sola regla, `R-AsistenteNuevoExpediente-004`, vía `fireActionRule_AsignarTramitesDisponibles`, que hace la **única** lectura del catálogo de la acción y se la pasa al cálculo de candidatos.

- **`prepararTramites_centroConTramitesIniciables_soloOfreceEsos`** — Tipo: happy. Verifica: `R-AsistenteNuevoExpediente-004`.
  - **Arrange:** bean con `centro = CENTRO_A`; `findTramitesEvaluables()` → `List.of(TRAMITE, OTRO_TRAMITE)`; `getPerfilesDeInicioSobreTramite(TRAMITE, USUARIO, CENTRO_A)` → `Set.of(Profile.CREADOR)`; `getPerfilesDeInicioSobreTramite(OTRO_TRAMITE, USUARIO, CENTRO_A)` → `Set.of()`.
  - **Act:** `prepararTramites(asistente)`.
  - **Assert:** `tramitesDisponibles` contiene exactamente `TRAMITE`; el retorno es el mismo bean (`assertSame`); `verify(ventanillaRepository, times(1)).findTramitesEvaluables()` — una sola lectura, la de la regla.
- **`prepararTramites_centroSinNingunTramiteIniciable_dejaLaListaVacia`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-004`.
  - **Arrange:** bean con `centro = CENTRO_A`; `findTramitesEvaluables()` → `List.of(TRAMITE, OTRO_TRAMITE)` (el catálogo sí trae filas juzgables: es el filtro por perfiles, y no un catálogo vacío, lo que deja la lista sin nada); `getPerfilesDeInicioSobreTramite` → `Set.of()` para todas ellas.
  - **Act:** `prepararTramites(asistente)`.
  - **Assert:** `tramitesDisponibles` vacío.
- **`prepararTramites_centroAjenoAlUsuario_dejaLaListaVacia`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-004`.
  - **Arrange:** bean con `centro = CENTRO_B`, al que `USUARIO` **no** pertenece (`centroUsuarios` solo tiene `CENTRO_A`); `findTramitesEvaluables()` → `List.of(TRAMITE)` (hay catálogo que juzgar); `getPerfilesDeInicioSobreTramite(TRAMITE, USUARIO, CENTRO_B)` → `Set.of()` (lo que devuelve el servicio real para un centro ajeno).
  - **Act:** `prepararTramites(asistente)`.
  - **Assert:** `tramitesDisponibles` vacío: manipular el centro por el endpoint no ofrece ni un trámite, y por eso el diseño no necesita una comprobación de pertenencia aparte.
- **`prepararTramites_conTramitesEnviadosPorElCliente_losSobrescribeIncondicionalmente`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-004`.
  - **Arrange:** bean con `centro = CENTRO_A` y `tramitesDisponibles = Set.of(OTRO_TRAMITE)` ya puesto; `findTramitesEvaluables()` → `List.of(TRAMITE, OTRO_TRAMITE)`; `getPerfilesDeInicioSobreTramite(TRAMITE, USUARIO, CENTRO_A)` → `Set.of(Profile.CREADOR)` y `getPerfilesDeInicioSobreTramite(OTRO_TRAMITE, USUARIO, CENTRO_A)` → `Set.of()`: el usuario solo puede iniciar `TRAMITE` (sin estos stubs la lista calculada quedaría vacía por el catálogo vacío por defecto, no por la sobrescritura).
  - **Act:** `prepararTramites(asistente)`.
  - **Assert:** `tramitesDisponibles` contiene solo `TRAMITE`.
- **`prepararTramites_noRecalculaHayQueElegirCentroNiTocaElCentro`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-004`.
  - **Arrange:** bean con `centro = CENTRO_A` y `hayQueElegirCentro = true` (viene del `<context>` del paso 1); `USUARIO` con `centroUsuario(CENTRO_A)` y `centroUsuario(CENTRO_B)`; `findTramitesEvaluables()` → `List.of(TRAMITE)`; `getPerfilesDeInicioSobreTramite(TRAMITE, USUARIO, CENTRO_A)` → `Set.of(Profile.CREADOR)` (con el catálogo vacío por defecto no se preguntaría por ningún centro y el `verify` de abajo se cumpliría sin ejercer nada).
  - **Act:** `prepararTramites(asistente)`.
  - **Assert:** `hayQueElegirCentro` sigue siendo `true`, `centro` sigue siendo `CENTRO_A` y `centrosDisponibles` sigue vacío; `getPerfilesDeInicioSobreTramite` **nunca** se invoca con `CENTRO_B` (este paso no recorre los demás centros del usuario).
- **`prepararTramites_sinCentro_lanzaValidationExceptionConDebeIndicarElCentro`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean sin `centro`; `I18n` con la identidad.
  - **Act:** `prepararTramites(asistente)`.
  - **Assert:** lanza `jakarta.validation.ValidationException` cuyo mensaje contiene el literal exacto «Debe indicar el centro» (la acción arranca por `validatePrepararTramites(...).ifPresent(BusinessMessages::throwIfInvalid)`); `verifyNoInteractions(ventanillaRepository)`.

### Método: `AsistenteNuevoExpediente recalcular(AsistenteNuevoExpediente asistente)`

Aplica una sola regla, `R-AsistenteNuevoExpediente-005`, vía `fireActionRule_AsignarPresentacion`, que calcula la matriz forma × destinatario una sola vez y delega las dos asignaciones en sus dos mitades, `fireActionRule_AsignarFormaDePresentar` y `fireActionRule_AsignarDestinatario`. El detalle (secuencia, ramas, errores y `MUST NOT`) vive **solo** en `design/rules/R-AsistenteNuevoExpediente-005.md`. La vista la dispara en el `onNew` y en el `onChange` de «¿Cómo se presenta?» (`sysVentanilla.Main@AsistenteNuevoExpediente-onChange-presentadoEnPapel-action`), así que corre **antes** de cualquier `validateTriggerInitialEvent`.

Todos los tests de este método parten de un bean con `centro = CENTRO_A` y `tramite = TRAMITE`, de `ventanillaRepository.findTramitesEvaluables()` → `List.of(TRAMITE)` (la puerta `V-009` de `validateRecalcular` pasa) y de `USUARIO` con `centroUsuario(CENTRO_A, "ALUMNO")` — es del tipo de usuario al que va dirigido `TRAMITE`, así que cuando el oráculo acepta una celda de la fila «yo mismo» el afinado del destinatario (`validateAfinadoDestinatario` → `tieneTipoUsuario`) la juzga en vez de lanzar `IllegalStateException` por un usuario que no pertenece al centro. La matriz se dibuja con el `thenAnswer` del oráculo.

- **`recalcular_lasDosFormasPosiblesYSinContestar_preguntaComoSePresentaYNoPreguntaParaQuienEs`** — Tipo: happy. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** el oráculo acepta las cuatro celdas (`Optional.empty()` siempre); `tipoTramite = TIPO_TRAMITE_SIN_TIPO_USUARIO` para que el afinado no intervenga; bean con `presentadoEnPapel = null`.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `hayQuePreguntarPresentacion` es `true`; `presentadoEnPapel` sigue `null` (se respeta la respuesta —aún ausente— del usuario); `hayQuePreguntarParaQuien` es `false` y `presentadoEnRepresentacion` es `null` (sin saber cómo se presenta no se puede decir para quién puede ser); el retorno es el mismo bean.
- **`recalcular_lasDosFormasPosiblesYFormaYaContestada_preguntaParaQuienEs`** — Tipo: happy. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** igual, pero bean con `presentadoEnPapel = false`.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `hayQuePreguntarPresentacion` `true` y `presentadoEnPapel` sigue `false` (la respuesta del usuario se respeta); `hayQuePreguntarParaQuien` `true` y `presentadoEnRepresentacion` `null` (la pregunta llega sin marcar).
- **`recalcular_soloSePuedePresentarEnPapel_fijaLaFormaYNoPregunta`** — Tipo: happy. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** el oráculo rechaza toda celda cuyo contexto lleve `profile = CREADOR` (mensaje «No puede presentar el expediente de esa forma en el centro indicado») y acepta las de `TRAMITADOR`; bean con `presentadoEnPapel = null`.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `hayQuePreguntarPresentacion` es `false` y `presentadoEnPapel` es `true` (la única forma que le sirve, asignada incondicionalmente por `fireActionRule_AsignarFormaDePresentar`).
- **`recalcular_soloSePuedePresentarEnPapelYElClienteEnviaLaOtraForma_laDescarta`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** misma matriz que el test anterior, pero el bean llega con `presentadoEnPapel = false` (respuesta manipulada: `presentadoEnPapel` está en `allowPropertiesRecalcular`).
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `presentadoEnPapel` queda en `true`: cuando la pregunta no se hace, el campo es del servidor y `fireActionRule_AsignarFormaDePresentar` lo sobrescribe sin mirar lo que llegó. La condición que separa las dos ramas es «¿se hace la pregunta?», no una guarda de nulidad sobre el campo.
- **`recalcular_ningunaFormaPosible_dejaLosDosCamposInformadosYNoPreguntaNada`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** el oráculo rechaza las cuatro celdas; bean con `presentadoEnPapel = null`.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `hayQuePreguntarPresentacion` `false` y `presentadoEnPapel` `false`; `hayQuePreguntarParaQuien` `false` y `presentadoEnRepresentacion` `false`. Los dos campos quedan **informados** a propósito, para que el rechazo lo dé la puerta 5 de `validateTriggerInitialEvent` con el literal que corresponda y no las puertas 3/4 pidiendo rellenar una pregunta invisible.
- **`recalcular_unSoloDestinatarioPosibleParaLaFormaFijada_loFijaYNoPregunta`** — Tipo: happy. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** el oráculo acepta solo las celdas con `presentadoEnRepresentacion = false` (el trámite no admite representación: mensaje «Este trámite no permite presentar la solicitud en representación de otra persona» en las otras dos); bean con `presentadoEnPapel = false`.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `hayQuePreguntarParaQuien` es `false` y `presentadoEnRepresentacion` es `false` (el único destinatario posible de esa fila, fijado por `fireActionRule_AsignarDestinatario`).
- **`recalcular_alCambiarLaFormaDePresentar_noConservaElDestinatarioAnterior`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** bean que llega con `presentadoEnPapel = true` y `presentadoEnRepresentacion = true` (la respuesta anterior del usuario); matriz en la que la fila «en papel» solo admite «para mí».
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `presentadoEnRepresentacion` queda en `false`: la asignación de `fireActionRule_AsignarDestinatario` es incondicional y la respuesta anterior no se conserva al cambiar la forma de presentar. Es la mitad servidor del `onChange` de «¿Cómo se presenta?».
- **`recalcular_consultaElOraculoExactamenteUnaVezPorCelda`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** el oráculo acepta siempre; `ArgumentCaptor<ContextoTramitacion>`.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `verify(tramitadorService, times(4)).validateTriggerInitialEvent(...)` y los cuatro contextos capturados son las cuatro parejas distintas (`CREADOR`/`TRAMITADOR` × `presentadoEnRepresentacion` `false`/`true`): `fireActionRule_AsignarPresentacion` calcula la matriz **una sola vez** y sus dos mitades leen de ella, no vuelven a preguntar (cuatro llamadas, no seis).
- **`recalcular_noLeeElCatalogoParaCalcularLaMatriz`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** el oráculo acepta siempre; bean completo con `presentadoEnPapel = false`.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** `verify(ventanillaRepository, times(1)).findTramitesEvaluables()`: la única lectura del catálogo de toda la acción es la de la puerta `V-009` de `validateRecalcular`; la regla del paso 3 no consulta el catálogo.
- **`recalcular_nuncaConsultaLosPerfilesDelUsuarioParaLaFormaEnviada`** — Tipo: borde. Verifica: `R-AsistenteNuevoExpediente-005`.
  - **Arrange:** bean con `presentadoEnPapel = true` cuando el usuario solo tiene `CREADOR` (el oráculo rechaza las celdas de `TRAMITADOR`).
  - **Act:** `recalcular(asistente)`.
  - **Assert:** no lanza ninguna excepción y `verify(perfilesUsuarioService, never()).getPerfil(any(), any(), any(), anyBoolean())`: el perfil lo deduce `perfilDeInicioPara` de los predicados del enum `Profile`, no se le pide al servicio que lanza, así que una petición manipulada nunca provoca un 500 en el `onNew`/`onChange`.
- **`recalcular_noAsignaNombreNiAyudaDelTramite`** — Tipo: borde. Verifica: `CC-AsistenteNuevoExpediente-001`, `CC-AsistenteNuevoExpediente-002`.
  - **Arrange:** bean con `tramite = TRAMITE`, `nombreTramite` y `ayudaTramite` sembrados con valores falsos; `I18n` con la identidad; el oráculo acepta siempre.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** al leer `getNombreTramite()` / `getAyudaTramite()` se obtienen los valores **derivados del trámite** (`"value:Anulación de matrícula"` y `"Texto de ayuda"`), no los sembrados: el servicio no toca esos dos campos, el getter los recalcula.
- **`recalcular_sinCentro_lanzaValidationExceptionConDebeIndicarElCentro`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean con `tramite = TRAMITE` y sin `centro`; `I18n` con la identidad.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** lanza `ValidationException` con el literal «Debe indicar el centro»; `verifyNoInteractions(tramitadorService)`.
- **`recalcular_tramiteNoEvaluable_lanzaValidationExceptionConDebeIndicarElTramite`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean con `centro = CENTRO_A` y `tramite = TRAMITE_NO_EVALUABLE`; `findTramitesEvaluables()` → `List.of(TRAMITE)`.
  - **Act:** `recalcular(asistente)`.
  - **Assert:** lanza `ValidationException` con el literal «Debe indicar el trámite»; `verifyNoInteractions(tramitadorService)` y `verify(perfilesUsuarioService, never()).getPerfilesDeInicioSobreTramite(any(), any(), any())` (la fila que reventaría por dentro no llega a ningún colaborador).

### Método: `Optional<BusinessMessages> validatePrepararCentros(AsistenteNuevoExpediente asistente)`

- **`validatePrepararCentros_cualquierBean_devuelveVacio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** bean vacío (la acción es deny-all y no lee nada del cliente).
  - **Act:** `validatePrepararCentros(asistente)`.
  - **Assert:** devuelve `Optional.empty()`; `verifyNoInteractions` de los tres colaboradores (el validador no tiene precondiciones y no debe consultar nada). Existe porque el par acción + validador es el contrato público mínimo de toda acción propia.

### Método: `Optional<BusinessMessages> validatePrepararTramites(AsistenteNuevoExpediente asistente)`

Una sola puerta: `validateCentroIndicado`, el dueño privado de `V-AsistenteNuevoExpediente-001`.

- **`validatePrepararTramites_conCentro_devuelveVacio`** — Tipo: happy. Verifica: `V-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean con `centro = CENTRO_A`.
  - **Act:** `validatePrepararTramites(asistente)`.
  - **Assert:** `Optional.empty()`.
- **`validatePrepararTramites_sinCentro_devuelveDebeIndicarElCentro`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean sin `centro`; `I18n` con la identidad.
  - **Act:** `validatePrepararTramites(asistente)`.
  - **Assert:** devuelve un `Optional` presente con **un solo** `BusinessMessage` en la forma canónica: `getMessage()` es exactamente «Debe indicar el centro», `getFieldName()` es `"centro"` y `getLabel()` es el `title` del campo en el dominio («Centro»). Es el mismo mensaje, campo y label que devuelven la puerta 1 de `validateRecalcular` y la puerta 1 de `validateTriggerInitialEvent`, porque los tres invocan al mismo dueño privado `validateCentroIndicado`.
- **`validatePrepararTramites_sinTramite_devuelveVacioYNoConsultaElCatalogo`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean con `centro = CENTRO_A` y `tramite = null`.
  - **Act:** `validatePrepararTramites(asistente)`.
  - **Assert:** `Optional.empty()` y `verifyNoInteractions(ventanillaRepository)`: esta acción tiene **una sola** precondición y **no** ejecuta el validador de ninguna otra (el paso 2 es justo el que todavía no tiene trámite, así que `V-009` no es puerta suya).

### Método: `Optional<BusinessMessages> validateRecalcular(AsistenteNuevoExpediente asistente)`

Dos puertas en este orden, y se para en la primera que falla: `validateCentroIndicado` (V-001) y `validateTramiteEvaluable` (V-009).

- **`validateRecalcular_centroYTramiteEvaluable_devuelveVacio`** — Tipo: happy. Verifica: `V-AsistenteNuevoExpediente-001`, `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean con `centro = CENTRO_A` y `tramite = TRAMITE`; `findTramitesEvaluables()` → `List.of(TRAMITE)`.
  - **Act:** `validateRecalcular(asistente)`.
  - **Assert:** `Optional.empty()`.
- **`validateRecalcular_sinCentro_devuelveDebeIndicarElCentroYNoConsultaElCatalogo`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean sin `centro` y con `tramite = TRAMITE`; `I18n` con la identidad.
  - **Act:** `validateRecalcular(asistente)`.
  - **Assert:** un solo mensaje, «Debe indicar el centro», con `fieldName` `"centro"` (idéntico al de `validatePrepararTramites`); `verifyNoInteractions(ventanillaRepository)` (la primera puerta corta antes de la segunda).
- **`validateRecalcular_sinTramite_devuelveDebeIndicarElTramite`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean con `centro = CENTRO_A` y `tramite = null`; `findTramitesEvaluables()` → `new ArrayList<>(List.of(TRAMITE))` — una lista que **admite** `contains(null)`, como la que devuelve la consulta JPA real; con un `List.of(…)` inmutable la condición del dueño privado lanzaría `NullPointerException` en vez de rechazar.
  - **Act:** `validateRecalcular(asistente)`.
  - **Assert:** un solo mensaje, «Debe indicar el trámite», con `fieldName` `"tramite"` y el `label` del `title` del dominio («Trámite»): la condición del dueño privado es «la lista de evaluables lo contiene», y una lista no contiene `null`, así que el caso ausente lo cubre la misma comprobación.
- **`validateRecalcular_tramiteFueraDeLosEvaluables_devuelveDebeIndicarElTramite`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean con `centro = CENTRO_A` y `tramite = TRAMITE_NO_EVALUABLE` (una fila real del catálogo sin tipo de trámite ni tipo de expediente por defecto); `findTramitesEvaluables()` → `List.of(TRAMITE)`.
  - **Act:** `validateRecalcular(asistente)`.
  - **Assert:** un solo mensaje, «Debe indicar el trámite», y **ninguna** comprobación propia sobre `getTipoTramite()` / `getDefaultTipoExpediente()`: el predicado tiene un único dueño, la consulta del repositorio, así que basta con que el trámite no esté en la lista para que se rechace.
- **`validateRecalcular_sinCentroNiTramite_devuelveUnSoloMensaje`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-001`, `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean vacío; `I18n` con la identidad.
  - **Act:** `validateRecalcular(asistente)`.
  - **Assert:** el `BusinessMessages` devuelto tiene **tamaño 1** y su mensaje es «Debe indicar el centro»: las puertas cortan en la primera que falla y los mensajes se devuelven tal cual.
- **`validateRecalcular_sinLasDosRespuestasDelUsuario_devuelveVacio`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-001`, `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean con `centro = CENTRO_A`, `tramite = TRAMITE`, `presentadoEnPapel = null` y `presentadoEnRepresentacion = null`; `findTramitesEvaluables()` → `List.of(TRAMITE)`.
  - **Act:** `validateRecalcular(asistente)`.
  - **Assert:** `Optional.empty()` y `verifyNoInteractions(tramitadorService)`: este validador tiene **solo** esas dos puertas y no ejecuta el de otra acción — las respuestas del usuario (puertas 3 y 4) son de `validateTriggerInitialEvent`, y aquí precisamente todavía no están contestadas.

### Método: `Optional<BusinessMessages> validateTriggerInitialEvent(AsistenteNuevoExpediente asistente)`

Cinco puertas en orden, cortando en la primera que falla: 1) `validateCentroIndicado` (V-001), 2) `validateTramiteEvaluable` (V-009), 3) V-002, 4) V-003 y 5) `validateAlta` (V-004/005/006 del motor, más V-007/V-008 del afinado propio). Invoca a los **dueños privados**, no al validador de otra acción. `validateAlta` construye el `ContextoTramitacion` con el `Profile` que deduce `perfilDeInicioPara(presentadoEnPapel)`; **nunca** consulta `PerfilesUsuarioService.getPerfil`.

Salvo que el test diga otra cosa: bean con `centro = CENTRO_A`, `tramite = TRAMITE`, `presentadoEnPapel = false`, `presentadoEnRepresentacion = false`; `findTramitesEvaluables()` → `new ArrayList<>(List.of(TRAMITE))` — una lista que **admite** `contains(null)`, como la que devuelve la consulta JPA real: la puerta 2 pregunta por el trámite tal cual llega, y con un `List.of(…)` inmutable el caso del trámite ausente lanzaría `NullPointerException` en vez de rechazar; `I18n` con la identidad.

- **`validateTriggerInitialEvent_datosCompletosYMotorYAfinadoConformes_devuelveVacio`** — Tipo: happy. Verifica: `V-AsistenteNuevoExpediente-001`…`V-AsistenteNuevoExpediente-009`.
  - **Arrange:** el oráculo del motor devuelve `Optional.empty()`; `USUARIO` con `centroUsuario(CENTRO_A, "ALUMNO")` (es del tipo al que va dirigido el trámite y no es Familiar).
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** `Optional.empty()`.
- **`validateTriggerInitialEvent_sinCentro_devuelveDebeIndicarElCentroYNoPreguntaAlMotor`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-001`.
  - **Arrange:** bean sin `centro`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, «Debe indicar el centro», con `fieldName` `"centro"` (el mismo mensaje, campo y label que devuelve `validatePrepararTramites`: mismo dueño privado); `verifyNoInteractions(tramitadorService)`. Es la única puerta de centro que escribe la ventanilla, porque la del motor es inalcanzable (el constructor compacto de `ContextoTramitacion` prohíbe el centro nulo).
- **`validateTriggerInitialEvent_sinTramite_devuelveDebeIndicarElTramiteYNoConstruyeElContexto`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean con `tramite = null`; el catálogo es el del preámbulo, `new ArrayList<>(List.of(TRAMITE))`, la lista que admite `contains(null)`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, «Debe indicar el trámite»; `verifyNoInteractions(tramitadorService)`: el `Objects.requireNonNull(tramite)` del constructor compacto nunca llega a ejecutarse, que es lo que convertiría el rechazo en un 500.
- **`validateTriggerInitialEvent_tramiteNoEvaluable_devuelveDebeIndicarElTramite`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean con `tramite = TRAMITE_NO_EVALUABLE`; `findTramitesEvaluables()` → `List.of(TRAMITE)`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, «Debe indicar el trámite», idéntico (mensaje, `fieldName` y `label`) al de `validateRecalcular` para el mismo bean; `verifyNoInteractions(perfilesUsuarioService)` y `verifyNoInteractions(tramitadorService)`.
- **`validateTriggerInitialEvent_sinFormaDePresentar_devuelveDebeIndicarComoSePresenta`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-002`.
  - **Arrange:** bean con `presentadoEnPapel = null` y `presentadoEnRepresentacion = false`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, «Debe indicar cómo se presenta el expediente», con `fieldName` `"presentadoEnPapel"` y `label` el `title` del dominio («¿Cómo se presenta?»); `verifyNoInteractions(tramitadorService)` (se exige siempre, se pregunte o no, porque es el campo que elige el perfil con el que se construye el contexto).
- **`validateTriggerInitialEvent_sinDestinatario_devuelveDebeIndicarParaQuienEs`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-003`.
  - **Arrange:** bean con `presentadoEnPapel = false` y `presentadoEnRepresentacion = null`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, «Debe indicar para quién es el expediente», con `fieldName` `"presentadoEnRepresentacion"` y `label` «¿Para quién es el expediente?»; `verifyNoInteractions(tramitadorService)`.
- **`validateTriggerInitialEvent_usuarioSinPerfilDeInicioEnElCentro_devuelveElLiteralDelMotor`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-004`.
  - **Arrange:** el oráculo devuelve `mensajes("No puede crear expedientes de este trámite en el centro indicado")`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** devuelve **exactamente** esos mensajes, sin reescribir el literal ni añadir ninguno: tamaño 1, ese texto y `fieldName`/`label` a `null` (la forma con la que los redacta el motor). La ventanilla no redacta mensajes de centro/perfil ni les añade campo.
- **`validateTriggerInitialEvent_formaDePresentarNoPermitida_devuelveElLiteralDelMotor`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-005`.
  - **Arrange:** el oráculo devuelve `mensajes("No puede presentar el expediente de esa forma en el centro indicado")`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, ese literal, tal cual lo devolvió el motor.
- **`validateTriggerInitialEvent_tramiteQueNoAdmiteRepresentacion_devuelveElLiteralDelMotor`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-006`.
  - **Arrange:** bean con `presentadoEnRepresentacion = true`; el oráculo devuelve `mensajes("Este trámite no permite presentar la solicitud en representación de otra persona")`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, ese literal.
- **`validateTriggerInitialEvent_motorRechaza_noEvaluaElAfinadoDelDestinatario`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-004`, `V-AsistenteNuevoExpediente-007`.
  - **Arrange:** el oráculo rechaza con «No puede crear expedientes de este trámite en el centro indicado»; además `USUARIO` **no** pertenece a `CENTRO_A` (sin `CentroUsuario`), que es la entrada con la que el afinado lanzaría `IllegalStateException`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** devuelve el mensaje del motor y **no** lanza: las dos puertas están en cadena, no sumadas, y el afinado solo ve entradas que el motor ya dio por buenas.
- **`validateTriggerInitialEvent_paraUstedMismoSiendoFamiliarQueNoEsDelTipoDelTramite_devuelveNoPuedeCrearParaUstedMismo`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-007`.
  - **Arrange:** el oráculo acepta; bean con `presentadoEnPapel = false` y `presentadoEnRepresentacion = false`; `TRAMITE` con `permitidoPresentarEnRepresentacion = true` y `tipoTramite = TIPO_TRAMITE_ALUMNO`; `USUARIO` con `centroUsuario(CENTRO_A, "FAMILIAR")` (es Familiar y **no** es Alumno).
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, «No puede crear este expediente para usted mismo en el centro indicado», en la forma canónica (`fieldName` `"presentadoEnRepresentacion"`, que es el campo del que habla la regla, y `label` tomado del `title` del dominio).
- **`validateTriggerInitialEvent_paraUstedMismoSiendoDelTipoDelTramite_devuelveVacio`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-007`.
  - **Arrange:** igual, pero `centroUsuario(CENTRO_A, "ALUMNO", "FAMILIAR")`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** `Optional.empty()`: basta con ser del tipo al que va dirigido el trámite aunque además se sea Familiar.
- **`validateTriggerInitialEvent_paraUstedMismoSinSerFamiliar_devuelveVacio`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-007`.
  - **Arrange:** igual, pero `centroUsuario(CENTRO_A, "PROFESOR")` (ni Alumno ni Familiar).
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** `Optional.empty()`.
- **`validateTriggerInitialEvent_paraUstedMismoEnTramiteQueNoAdmiteRepresentacion_devuelveVacio`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-007`.
  - **Arrange:** el oráculo acepta; `TRAMITE` con `permitidoPresentarEnRepresentacion = false`; `USUARIO` con `centroUsuario(CENTRO_A, "FAMILIAR")`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** `Optional.empty()`: la regla solo afina los trámites que admiten representación.
- **`validateTriggerInitialEvent_enRepresentacionSinSerFamiliarYSiendoDelTipoDelTramite_devuelveNoPuedeCrearEnRepresentacion`** — Tipo: error. Verifica: `V-AsistenteNuevoExpediente-008`.
  - **Arrange:** el oráculo acepta; bean con `presentadoEnPapel = false` y `presentadoEnRepresentacion = true`; `USUARIO` con `centroUsuario(CENTRO_A, "ALUMNO")`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** un solo mensaje, «No puede crear este expediente en representación de otra persona en el centro indicado», en la forma canónica (`fieldName` `"presentadoEnRepresentacion"`).
- **`validateTriggerInitialEvent_enRepresentacionSiendoFamiliar_devuelveVacio`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-008`.
  - **Arrange:** igual, pero `centroUsuario(CENTRO_A, "ALUMNO", "FAMILIAR")`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** `Optional.empty()`.
- **`validateTriggerInitialEvent_enRepresentacionSinSerDelTipoDelTramite_devuelveVacio`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-008`.
  - **Arrange:** igual, pero `centroUsuario(CENTRO_A, "PROFESOR")`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** `Optional.empty()`.
- **`validateTriggerInitialEvent_tipoTramiteSinTipoUsuario_noAplicaElAfinadoYDevuelveVacio`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-007`, `V-AsistenteNuevoExpediente-008`.
  - **Arrange:** el oráculo acepta; `TRAMITE` con `tipoTramite = TIPO_TRAMITE_SIN_TIPO_USUARIO`; `USUARIO` con `centroUsuario(CENTRO_A, "FAMILIAR")`, que con tipo de usuario declarado habría fallado en `V-007`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** `Optional.empty()`: sin tipo de usuario al que ir dirigido no hay nada que afinar y se respeta el veredicto del motor; la rama vive en `validateAfinadoDestinatario`, antes de las dos reglas, y no dentro del predicado que las dos comparten.
- **`validateTriggerInitialEvent_registradoEnPapel_noAplicaElAfinado`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-007`, `V-AsistenteNuevoExpediente-008`.
  - **Arrange:** el oráculo acepta; bean con `presentadoEnPapel = true` y `presentadoEnRepresentacion = false`; `USUARIO` con `centroUsuario(CENTRO_A, "FAMILIAR")` (la combinación que fallaría si lo presentara él mismo).
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** `Optional.empty()`: las dos reglas del afinado solo hablan de quien presenta él mismo.
- **`validateTriggerInitialEvent_construyeElContextoConElPerfilQueExigeLaFormaDePresentar`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-004`, `V-AsistenteNuevoExpediente-005`.
  - **Arrange:** el oráculo acepta; `USUARIO` con `centroUsuario(CENTRO_A, "ALUMNO")` (es del tipo al que va dirigido el trámite, así que el afinado acepta la invocación que lo atraviesa en vez de lanzar); `ArgumentCaptor<ContextoTramitacion>`; dos invocaciones, una con `presentadoEnPapel = false` y otra con `true`.
  - **Act:** `validateTriggerInitialEvent(asistente)` en las dos configuraciones.
  - **Assert:** el contexto capturado lleva `profile = Profile.CREADOR` en la primera y `Profile.TRAMITADOR` en la segunda, y en ambos casos el `tramite`, el `centro` y los dos booleanos del bean; `verify(perfilesUsuarioService, never()).getPerfil(any(), any(), any(), anyBoolean())`: el perfil lo deduce `perfilDeInicioPara` de los predicados del enum, nunca se pide al servicio que lanza.
- **`validateTriggerInitialEvent_conVariasPuertasEnFallo_devuelveUnSoloMensaje`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-001`, `V-AsistenteNuevoExpediente-002`, `V-AsistenteNuevoExpediente-009`.
  - **Arrange:** bean vacío (sin centro, sin trámite, sin forma y sin destinatario).
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** el `BusinessMessages` tiene **tamaño 1** y su mensaje es «Debe indicar el centro»: el orden de las puertas es la regla y un rechazo no arrastra los mensajes de las siguientes.
- **`validateTriggerInitialEvent_usuarioQueNoPerteneceAlCentroPeroElMotorAcepta_lanzaIllegalStateException`** — Tipo: borde. Verifica: `V-AsistenteNuevoExpediente-007`.
  - **Arrange:** el oráculo acepta (escenario imposible en producción: sin pertenecer al centro no hay perfiles de inicio); `TRAMITE` con `tipoTramite = TIPO_TRAMITE_ALUMNO`; `USUARIO` sin ningún `CentroUsuario` para `CENTRO_A`; bean con `presentadoEnPapel = false`, `presentadoEnRepresentacion = false`.
  - **Act:** `validateTriggerInitialEvent(asistente)`.
  - **Assert:** lanza `IllegalStateException`: `tieneTipoUsuario` no convierte un error de programación en un veredicto de negocio devolviendo `false`. Es la **única** excepción técnica que el servicio puede producir, y solo desde una combinación que el motor ya declaró imposible.

### Método: `AllowProperties allowPropertiesPrepararCentros()`

- **`allowPropertiesPrepararCentros_noAceptaNingunCampo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** servicio construido.
  - **Act:** `allowPropertiesPrepararCentros()`.
  - **Assert:** `allowProperty(...)` es `false` para `centro`, `tramite`, `presentadoEnPapel`, `presentadoEnRepresentacion`, `centrosDisponibles`, `hayQueElegirCentro`, `hayQuePreguntarPresentacion` y `hayQuePreguntarParaQuien`: la acción es deny-all porque todo sale del usuario autenticado, incluido el `centro` que esta acción decide.

### Método: `AllowProperties allowPropertiesPrepararTramites()`

- **`allowPropertiesPrepararTramites_soloAceptaElCentro`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** servicio construido.
  - **Act:** `allowPropertiesPrepararTramites()`.
  - **Assert:** `allowProperty("centro")` es `true`; `false` para `tramite`, `presentadoEnPapel`, `presentadoEnRepresentacion`, `tramitesDisponibles` y `hayQueElegirCentro`.

### Método: `AllowProperties allowPropertiesRecalcular()`

- **`allowPropertiesRecalcular_aceptaTramiteCentroYFormaDePresentarPeroNoElDestinatario`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** servicio construido.
  - **Act:** `allowPropertiesRecalcular()`.
  - **Assert:** `true` para `tramite`, `centro` y `presentadoEnPapel`; `false` para `presentadoEnRepresentacion` (esta acción lo recalcula siempre, nunca lo lee) y para los campos del servidor.

### Método: `AllowProperties allowPropertiesTriggerInitialEvent()`

- **`allowPropertiesTriggerInitialEvent_aceptaExactamenteLosCuatroCamposDeLaAccionCrearExpediente`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** servicio construido.
  - **Act:** `allowPropertiesTriggerInitialEvent()`.
  - **Assert:** `true` para `tramite`, `centro`, `presentadoEnPapel` y `presentadoEnRepresentacion` (los cuatro que el controlador del tramitador lee del formulario); `false` para `centrosDisponibles`, `tramitesDisponibles`, `hayQueElegirCentro`, `hayQuePreguntarPresentacion` y `hayQuePreguntarParaQuien`.

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_noAceptaNingunCampo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** servicio construido.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty(...)` es `false` para los once campos del modelo: la ficha no se guarda nunca, así que un alta por el endpoint REST genérico no acepta ni un campo. Es la única defensa del guardado que llega a ejecutarse (`modelService.validate`).

### Método: `AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_noAceptaNingunCampo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** servicio construido.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty(...)` es `false` para los once campos del modelo.
- **`allowProperties_ningunaAccionAceptaLosCamposDerivadosNiLosDelServidor`** — Tipo: borde. Verifica: `CC-AsistenteNuevoExpediente-001`, `CC-AsistenteNuevoExpediente-002`.
  - **Arrange:** las seis `AllowProperties` del servicio (`allowPropertiesPrepararCentros`, `allowPropertiesPrepararTramites`, `allowPropertiesRecalcular`, `allowPropertiesTriggerInitialEvent`, `allowPropertiesInsert` y `allowPropertiesUpdate`). Test transversal, colocado aquí por ser el último de la familia.
  - **Act:** `allowProperty(...)` sobre cada una para `nombreTramite`, `ayudaTramite`, `centrosDisponibles`, `tramitesDisponibles`, `hayQueElegirCentro`, `hayQuePreguntarPresentacion` y `hayQuePreguntarParaQuien`.
  - **Assert:** siempre `false`. Es la garantía de los dos campos derivados («sobreescribible: nunca») y de que los oráculos que publica el servidor no se pueden dictar desde el cliente.

---

## Clase: `com.educaflow.system.ventanilla.controller.AsistenteNuevoExpedienteController`  —  controlador

**Responsabilidad:** los cuatro `@CallMethod` que usan las vistas. Los cuatro siguen el mismo tramo — construir el bean con la `AllowProperties` de la acción, consultar el validador y, si devuelve mensajes, contestarlos con `doResponseBusinessMessagesAsError(...)` y `return` sin llamar a la acción — y los tres primeros devuelven a la vista los valores que esa acción acaba de calcular, con los dos mapeadores privados `toReferenciaCentro` / `toReferenciaTramite`, que componen la referencia mínima `{id, name}` y **nada más**.
**Colaboradores a mockear:** `ModelServiceFactory` (campo `@Inject`, inyectado por reflexión) cuyo `resolve(AsistenteNuevoExpediente.class)` devuelve un mock de `AsistenteNuevoExpedienteService`; `ActionRequest` (su `getData()` devuelve un mapa con la clave `context`, que a su vez lleva `_model` = FQN del modelo y lo que «mande el cliente» en cada test) y `ActionResponse` (se verifican sus `setValue` / `setError`); `JpaRepository` (estático, `mockStatic`, para que `JpaRepository.of(AsistenteNuevoExpediente.class)` devuelva un repositorio mock cuyo `create(null)` entregue un bean nuevo — es lo que usa por dentro `ActionRequestHelper.getModel(...)`, igual que en `TareaFirmaControllerTest`); `I18n` (estático, con la identidad).
**Origen diseño:** `design.md` Paso 5 (los cuatro `@CallMethod`, lo que devuelve cada uno y los dos mapeadores privados) y la tabla de frontera de confianza por acción.

> Los tests del controlador declaran `Verifica: —`: el diseño ubica las reglas `V-`/`R-`/`CC-` en el servicio y en el modelo, y el controlador solo transporta. Lo que estos tests fijan es el **tramo común** (whitelist → validador → error o acción) y la **forma exacta** de la respuesta que las vistas leen.

### Método: `void prepararCentros(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`prepararCentros_sinMensajes_invocaLaAccionYDevuelveLosTresValoresDelArranque`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `service.allowPropertiesPrepararCentros()` → `AllowProperties.createDenyAllProperties()`; `service.validatePrepararCentros(any())` → `Optional.empty()`; `service.prepararCentros(any())` → un bean con `centrosDisponibles = Set.of(CENTRO_A, CENTRO_B)`, `hayQueElegirCentro = true` y `centro = null`.
  - **Act:** `prepararCentros(actionRequest, actionResponse)`.
  - **Assert:** `verify` de `response.setValue("centrosDisponibles", <lista con una referencia {id, name} por centro>)`, `setValue("hayQueElegirCentro", true)` y `setValue("centro", null)`; `verify(actionResponse, never()).setError(anyString())`. Con esos tres valores el `action-group` del `onNew` decide el arranque.
- **`prepararCentros_conUnSoloCentroCandidato_devuelveLaReferenciaDelCentro`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `service.prepararCentros(any())` → bean con `centrosDisponibles = Set.of(CENTRO_A)`, `hayQueElegirCentro = false` y `centro = CENTRO_A`.
  - **Act:** `prepararCentros(actionRequest, actionResponse)`.
  - **Assert:** `setValue("centro", <mapa con id = 1 y name = el de CENTRO_A>)` y `setValue("hayQueElegirCentro", false)`: con esos valores el `action-group` del `onNew` salta al paso 2 sin enseñar el listado.
- **`prepararCentros_sinCentrosCandidatos_devuelveLaListaVacia`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `service.prepararCentros(any())` → bean con `centrosDisponibles` vacío, `hayQueElegirCentro = false`, `centro = null`.
  - **Act:** `prepararCentros(actionRequest, actionResponse)`.
  - **Assert:** `setValue("centrosDisponibles", <lista vacía>)`, `setValue("centro", null)` y `setValue("hayQueElegirCentro", false)`; sin `setError` (el aviso lo da el `<action-validate>` de la vista, no el servidor).
- **`prepararCentros_conMensajes_contestaErrorYNoInvocaLaAccion`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `service.validatePrepararCentros(any())` → `Optional.of(BusinessMessages.single("mensaje de precondición"))`.
  - **Act:** `prepararCentros(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setError(<html que contiene el mensaje>)` con la sobrecarga **sin título**; `verify(service, never()).prepararCentros(any())`; el método retorna sin lanzar (una precondición se contesta como mensaje de negocio, nunca como `ValidationException` cruda ni como 500).
- **`prepararCentros_conCamposEnviadosPorElCliente_construyeElBeanConLaWhitelistDeLaAccion`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** el `context` del `ActionRequest` trae `centro`, `centrosDisponibles` y `hayQueElegirCentro`; `allowPropertiesPrepararCentros()` → deny-all; `service.prepararCentros(any())` → un bean con `centrosDisponibles = Set.of(CENTRO_A)`, `hayQueElegirCentro = false` y `centro = CENTRO_A` (el controlador compone la respuesta leyendo el bean que devuelve la acción, así que sin programarlo el mock devolvería `null`); `ArgumentCaptor<AsistenteNuevoExpediente>` sobre `service.prepararCentros(...)`.
  - **Act:** `prepararCentros(actionRequest, actionResponse)`.
  - **Assert:** el bean capturado llega con `centro`, `centrosDisponibles` y `hayQueElegirCentro` **vacíos**: el controlador construye el bean con la `AllowProperties` de la acción y no con la del modelo.

### Método: `void prepararTramites(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`prepararTramites_sinMensajes_devuelveLaReferenciaMinimaDeCadaTramite`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `service.allowPropertiesPrepararTramites()` → whitelist de `centro`; `service.validatePrepararTramites(any())` → `Optional.empty()`; `service.prepararTramites(any())` → bean con `tramitesDisponibles = Set.of(TRAMITE)`.
  - **Act:** `prepararTramites(actionRequest, actionResponse)`.
  - **Assert:** `setValue("tramitesDisponibles", <lista con un mapa {id, name} por trámite>)`, con `name = "Anulación de matrícula"` **tal cual**.
- **`prepararTramites_noComponeNiLaTraduccionNiLaReferenciaDelTipoDeTramite`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `TRAMITE` con `name = "Anulación de matrícula"` y `tipoTramite = TIPO_TRAMITE_ALUMNO` (`name = "Matrícula"`); `service.prepararTramites(any())` → un bean con `tramitesDisponibles = Set.of(TRAMITE)`; `mockStatic(I18n)` sin programar ninguna respuesta.
  - **Act:** `prepararTramites(actionRequest, actionResponse)`.
  - **Assert:** el mapa del trámite tiene **exactamente** las claves `id` y `name` —ni `tipoTramite` ni ninguna otra—, su `name` es `"Anulación de matrícula"` sin traducir, y `verify` de que el controlador **no** consulta `I18n` en ningún momento: el `panel-related` vuelve a pedir estas filas al servidor y descarta lo que el controlador componga, y la traducción del listado la pone Axelor con `$t:name` porque `Tramite.name` es `translatable="true"`. La clave `value:` solo la compone el cuerpo del campo derivado `nombreTramite` del dominio, no el controlador.
- **`prepararTramites_noDevuelveHayQueElegirCentro`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `service.prepararTramites(any())` → un bean con `tramitesDisponibles = Set.of(TRAMITE)` y `hayQueElegirCentro = true` (este último viaja en el `<context>` del paso 1).
  - **Act:** `prepararTramites(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse, never()).setValue(eq("hayQueElegirCentro"), any())`: en este paso lo transporta la vista y el servidor ni lo calcula ni lo devuelve.
- **`prepararTramites_conMensajes_contestaErrorYNoInvocaLaAccion`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `service.validatePrepararTramites(any())` → `Optional.of(BusinessMessages.single("Debe indicar el centro"))`.
  - **Act:** `prepararTramites(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setError(<html con «Debe indicar el centro»>)` sin título; `verify(service, never()).prepararTramites(any())`.

### Método: `void recalcular(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`recalcular_sinMensajes_devuelveLosSeisValoresDelPaso3`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `service.allowPropertiesRecalcular()` → whitelist de `tramite`/`centro`/`presentadoEnPapel`; `service.validateRecalcular(any())` → `Optional.empty()`; `service.recalcular(any())` → bean con `tramite = TRAMITE`, `presentadoEnPapel = false`, `presentadoEnRepresentacion = null`, `hayQuePreguntarPresentacion = true`, `hayQuePreguntarParaQuien = true`; `I18n` con la identidad.
  - **Act:** `recalcular(actionRequest, actionResponse)`.
  - **Assert:** `setValue` de los seis: `nombreTramite`, `ayudaTramite`, `presentadoEnPapel`, `presentadoEnRepresentacion`, `hayQuePreguntarPresentacion` y `hayQuePreguntarParaQuien`, con esos valores. Es lo que leen el `onNew` y el `onChange` de «¿Cómo se presenta?» del paso 3.
- **`recalcular_leeNombreYAyudaDelGetterDelBean`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** el bean que devuelve el servicio tiene `tramite = TRAMITE` y **ninguna asignación previa** de `nombreTramite` / `ayudaTramite`; `I18n` con la identidad.
  - **Act:** `recalcular(actionRequest, actionResponse)`.
  - **Assert:** `setValue("nombreTramite", "value:Anulación de matrícula")` y `setValue("ayudaTramite", "Texto de ayuda")`: el controlador los toma del getter, que los recalcula desde el trámite, no de una asignación del servicio.
- **`recalcular_sinTramiteEnElBean_devuelveNombreYAyudaNulos`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** el bean devuelto por el servicio no tiene `tramite`.
  - **Act:** `recalcular(actionRequest, actionResponse)`.
  - **Assert:** `setValue("nombreTramite", null)` y `setValue("ayudaTramite", null)` (el `showIf="ayudaTramite"` de la vista depende de ese `null`).
- **`recalcular_conMensajes_contestaErrorYNoInvocaLaAccion`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `service.validateRecalcular(any())` → `Optional.of(BusinessMessages.single("Debe indicar el trámite"))`.
  - **Act:** `recalcular(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setError(<html con «Debe indicar el trámite»>)` sin título; `verify(service, never()).recalcular(any())`.

### Método: `void validateTriggerInitialEvent(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`validateTriggerInitialEvent_sinMensajes_noContestaNingunError`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `service.allowPropertiesTriggerInitialEvent()` → whitelist de los cuatro campos; `service.validateTriggerInitialEvent(any())` → `Optional.empty()`.
  - **Act:** `validateTriggerInitialEvent(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse, never()).setError(anyString())` y `never().setError(anyString(), anyString())`: sin error, el `action-group` del botón sigue hasta la acción de alta `…-Remote-triggerInitialEvent-action` (`TramitadorController.triggerInitialEvent`), que es la que crea el expediente.
- **`validateTriggerInitialEvent_conMensajes_contestaErrorConElTituloNoEsPosibleCrearElExpediente`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `service.validateTriggerInitialEvent(any())` → `Optional.of(BusinessMessages.single("No puede crear expedientes de este trámite en el centro indicado"))`; `I18n` con la identidad.
  - **Act:** `validateTriggerInitialEvent(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setError(<html que contiene el mensaje>, "No es posible crear el expediente")` — la sobrecarga **con título**, el mismo que usa `TramitadorController`, para que el encabezado no cambie según quién rechace.
- **`validateTriggerInitialEvent_conCamposDelServidorEnviadosPorElCliente_construyeElBeanConSuWhitelist`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `service.allowPropertiesTriggerInitialEvent()` → la whitelist real de los cuatro campos (`tramite`, `centro`, `presentadoEnPapel` y `presentadoEnRepresentacion`), igual que en el test happy de esta misma sección (sin programarla el mock devuelve `null` y el bean llegaría vacío del todo); el `context` trae los cuatro campos legítimos **y además** `hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien`, `hayQueElegirCentro` y `tramitesDisponibles`; `ArgumentCaptor<AsistenteNuevoExpediente>` sobre `service.validateTriggerInitialEvent(...)`.
  - **Act:** `validateTriggerInitialEvent(actionRequest, actionResponse)`.
  - **Assert:** el bean capturado trae `centro`, `tramite`, `presentadoEnPapel` y `presentadoEnRepresentacion`, y los cuatro campos del servidor llegan vacíos: la validación remota juzga solo lo que el usuario puede dictar.
- **`validateTriggerInitialEvent_noInvocaNingunaAccionDelServicio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `service.validateTriggerInitialEvent(any())` → `Optional.empty()`.
  - **Act:** `validateTriggerInitialEvent(actionRequest, actionResponse)`.
  - **Assert:** `verify(service, never()).prepararCentros(any())`, `never().prepararTramites(any())` y `never().recalcular(any())`: este `@CallMethod` solo valida; crear es del controlador del tramitador (`TramitadorController.triggerInitialEvent`).

---

## Clase: `com.educaflow.system.ventanilla.service.AsistenteNuevoExpedienteService` — sin lógica testable
**Motivo:** interfaz sin comportamiento (solo declara las firmas de las tres acciones, sus validadores y sus `AllowProperties`). El contrato se ejerce en los tests de su implementación.

---

## Clase: `com.educaflow.system.ventanilla.db.repo.VentanillaRepository` — sin lógica testable
**Motivo:** su único método, `findTramitesEvaluables()`, **es** una consulta JPQL sobre `Tramite` sin ninguna lógica alrededor (descarta las filas con `tipoTramite` o `defaultTipoExpediente` a null). Verificarlo exige base de datos real, que §3 del contrato prohíbe en los tests unitarios; en los tests del servicio se mockea, y su comportamiento real lo cubren los E2E de `test-e2e-desc.md`.

---

## Cobertura

- **Clases con lógica descritas: 3** (88 tests en total).
  - `com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente` (solo los dos campos derivados) — 6 tests sobre sus 2 getters con cálculo.
  - `com.educaflow.system.ventanilla.service.impl.AsistenteNuevoExpedienteServiceImpl` — 65 tests sobre sus 13 métodos públicos (3 acciones, 4 validadores y 6 `AllowProperties`).
  - `com.educaflow.system.ventanilla.controller.AsistenteNuevoExpedienteController` — 17 tests sobre sus 4 `@CallMethod`.
- **Clases omitidas (sin lógica):**
  - `com.educaflow.system.ventanilla.service.AsistenteNuevoExpedienteService` — interfaz sin comportamiento.
  - `com.educaflow.system.ventanilla.db.repo.VentanillaRepository` — una única consulta JPQL; requiere base de datos.
  - Los nueve campos no derivados de `AsistenteNuevoExpediente` (`centro`, `tramite`, `presentadoEnPapel`, `presentadoEnRepresentacion`, `centrosDisponibles`, `tramitesDisponibles`, `hayQueElegirCentro`, `hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien`) — accesores generados por Axelor sin cuerpo propio.
- **Reglas server-side cubiertas (`V`/`R`/`CC`):**
  - `V-AsistenteNuevoExpediente-001` (dueño privado `validateCentroIndicado`) — rama OK y rama de fallo desde **sus tres llamantes**: `validatePrepararTramites`, `validateRecalcular` (puerta 1) y `validateTriggerInitialEvent` (puerta 1), con el mismo mensaje, `fieldName` y `label` en los tres.
  - `V-AsistenteNuevoExpediente-002` — puerta 3 de `validateTriggerInitialEvent` (fallo, con la forma canónica del `BusinessMessage`, y OK en el camino feliz).
  - `V-AsistenteNuevoExpediente-003` — puerta 4 de `validateTriggerInitialEvent` (fallo, con la forma canónica del `BusinessMessage`, y OK en el camino feliz).
  - `V-AsistenteNuevoExpediente-004` — literal del motor propagado tal cual (sin `fieldName` ni `label`), y perfil `CREADOR`/`TRAMITADOR` con el que `perfilDeInicioPara` construye el contexto.
  - `V-AsistenteNuevoExpediente-005` — literal del motor propagado tal cual.
  - `V-AsistenteNuevoExpediente-006` — literal del motor propagado tal cual.
  - `V-AsistenteNuevoExpediente-007` — rama de fallo, las tres ramas de aceptación (es del tipo del trámite / no es Familiar / el trámite no admite representación), el tipo de trámite sin tipo de usuario, el caso «registrado en papel» y la rama de error de programación (`IllegalStateException`).
  - `V-AsistenteNuevoExpediente-008` — rama de fallo y las dos ramas de aceptación (es Familiar / no es del tipo del trámite).
  - `V-AsistenteNuevoExpediente-009` (dueño privado `validateTramiteEvaluable`) — rama OK y rama de fallo desde **sus dos llamantes**, `validateRecalcular` (puerta 2) y `validateTriggerInitialEvent` (puerta 2), con los tres casos: trámite ausente, trámite fuera de los evaluables y trámite correcto.
  - `R-AsistenteNuevoExpediente-003` (`fireActionRule_AsignarArranqueDelAsistente`, desde `prepararCentros`) — varios centros, uno solo, ninguno, usuario sin centros, catálogo vacío, una sola lectura del catálogo para todos los centros y sobrescritura incondicional de los tres campos.
  - `R-AsistenteNuevoExpediente-004` (`fireActionRule_AsignarTramitesDisponibles`, desde `prepararTramites`) — con candidatos, sin candidatos, centro ajeno, una sola lectura del catálogo, sobrescritura incondicional y no tocar el centro ni `hayQueElegirCentro`.
  - `R-AsistenteNuevoExpediente-005` (`fireActionRule_AsignarPresentacion` y sus dos mitades `fireActionRule_AsignarFormaDePresentar` y `fireActionRule_AsignarDestinatario`, desde `recalcular`; detalle en `design/rules/R-AsistenteNuevoExpediente-005.md`) — las cuatro configuraciones de la matriz, el descarte de la forma enviada por el cliente, la no conservación del destinatario anterior, las cuatro consultas al oráculo (una por celda), que la regla no lee el catálogo y que nunca se llama a `getPerfil`.
  - `CC-AsistenteNuevoExpediente-001` y `CC-AsistenteNuevoExpediente-002` — getters derivados (con y sin trámite, con valor previo sembrado, con la clave `value:` compuesta en el cuerpo del propio campo), que `recalcular` no los asigna y su ausencia en las seis `AllowProperties`.
- **Regla server-side NO cubierta aquí:** `R-AsistenteNuevoExpediente-001` (crear el expediente) — la ejecuta `TramitadorController.triggerInitialEvent` del subsistema tramitador, al que apunta la acción `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`; es un controlador que este diseño **reutiliza sin modificar** y que no es código de `system/ventanilla`. Se cubre en `test-e2e-desc.md`.
- **Reglas solo-cliente excluidas (E2E en `test-e2e-desc.md`):** `U-nuevo-expediente-001`, `-002`, `-003`, `-004`, `-005`, `-006`, `-007`, `-008`, `-009`, `-010`, `-011`, `-012`, `-013`, `-014`, `-015`.

---

## Notas y supuestos

Decisiones tomadas ante puntos que el contrato no resuelve por sí solo; quedan aquí documentadas para el implementador.

1. **Los campos derivados del modelo llevan tests propios.** `tests-unitarios.md` §1 excluye las entidades generadas por Axelor «sin lógica propia» y §3 manda testear un `CC-` **en el servicio** cuando el getter delega en él. Aquí no delega: el cálculo de `CC-001` y `CC-002` vive en el cuerpo del campo `transient` del `domains.xml` y el generador lo emite dentro de la propia entidad, así que el único sitio donde se puede ejercer es la entidad. Hay precedente exacto en el proyecto: `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/GradoTest.java`. El diseño **no** define ninguna clase auxiliar para componer la clave `value:` (no existe ningún `NombreTramiteUtil`), así que no hay ninguna sección de tests para ella.
2. **Los métodos privados no tienen sección.** La plantilla pide una sección «por cada método público con lógica». Los privados del servicio —los dos dueños de regla `validateCentroIndicado` (V-001) y `validateTramiteEvaluable` (V-009), el oráculo `validateAlta`/`admiteAlta`, el afinado `validateAfinadoDestinatario`, los cinco `fireActionRule_*`, `getTramitesCandidatos`/`getCentrosCandidatos`, `perfilDeInicioPara`, los predicados de tipo de usuario y el `record MatrizPresentacion`— se ejercen desde el público que los invoca, y cada test dice en su `Assert` qué comportamiento del privado fija. Que cada dueño privado tenga **un** literal y **una** condición se comprueba repitiendo el test de fallo desde cada llamante y comparando el mensaje.
3. **`getTramitesCandidatos` se prueba por su efecto observable.** Al ser privado y recibir el catálogo ya leído, lo que se fija desde fuera es que la lectura la hacen sus llamantes **una sola vez** (`prepararCentros_conVariosCentros_leeElCatalogoUnaSolaVez`, `prepararTramites_centroConTramitesIniciables_soloOfreceEsos` y `recalcular_noLeeElCatalogoParaCalcularLaMatriz`) y que el filtro aplicado es «tiene algún perfil de inicio en ese centro».
4. **`perfilDeInicioPara` no tiene test de su rama de error.** Su `IllegalStateException` solo dispara si el enum `Profile` deja de cumplir la correspondencia 1:1 con la forma de presentar (hoy `CREADOR` ↔ «lo presento yo», `TRAMITADOR` ↔ «en papel»), y el enum es una constante de compilación: la rama es inalcanzable desde un test unitario sin reescribir el enum. Lo que sí se fija es la correspondencia vigente, en `validateTriggerInitialEvent_construyeElContextoConElPerfilQueExigeLaFormaDePresentar`, y que el perfil **no** se pide a `PerfilesUsuarioService.getPerfil` (dos `verify(..., never())`, en `recalcular` y en `validateTriggerInitialEvent`).
5. **`validate*` del ciclo de vida (`validateInsert`/`validateUpdate`/`validateRemove`) no se testean:** el diseño **no** los sobrescribe (serían código inalcanzable sobre un `@MappedSuperclass`). Lo que sí se testea es la única defensa que se ejecuta, el deny-all de `allowPropertiesInsert`/`allowPropertiesUpdate`.
6. **Las `U-` con mitad en el servidor no se declaran cubiertas.** `U-006` a `U-012` y `U-015` se apoyan en booleanos que calculan `fireActionRule_AsignarFormaDePresentar` / `fireActionRule_AsignarDestinatario` o en el título de error del controlador; esa mitad está cubierta por los tests de `recalcular` y del controlador, pero la regla es de UI y su verificación es E2E, así que aparece en la lista de excluidas y **no** en ningún campo `Verifica` (el contrato marca como incoherencia una `U-` testeada como unitaria). Lo mismo vale para el `onChange` `sysVentanilla.Main@AsistenteNuevoExpediente-onChange-presentadoEnPapel-action`: aquí solo se prueba el efecto servidor que dispara (que `recalcular` no conserva el destinatario anterior), no el cableado de la vista.
7. **El oráculo del motor se programa con `thenAnswer`, no con stubs por argumento.** `recalcular` llama cuatro veces a `validateTriggerInitialEvent` con contextos que solo se distinguen por `profile` y `presentadoEnRepresentacion`; un `thenAnswer` que inspeccione el `ContextoTramitacion` recibido es la única forma de describir las cuatro celdas con un solo stub, y de paso deja verificable cuántas veces se pregunta.
8. **`centrosDisponibles` y `tramitesDisponibles` son `Set`** (AOP genera `java.util.Set<T>` para un `many-to-many`), así que las aserciones sobre ellos comparan **pertenencia y tamaño**, nunca orden: el orden lo fija el `orderBy` del grid que los pinta.
9. **Los fixtures de `Centro` y `Tramite` llevan `id`.** `User.getCentroUsuario(centro)` devuelve `null` si el centro no tiene `id`, y el `Set` de un m2m compara por identidad de entidad; sin `id` varios tests pasarían o fallarían por el motivo equivocado.
10. **El `fieldName` de `V-007` y `V-008` es `presentadoEnRepresentacion`.** El diseño fija la **forma** canónica del `BusinessMessage` (`campo`, `I18n.get(mensaje)`, `I18n.get(title del campo)`) pero no nombra el campo de estas dos reglas del afinado; las dos hablan de para quién es el expediente, así que se asume ese campo —el mismo que la puerta 4— para que el formulario marque el control correcto. Si el implementador eligiera otro, lo que **MUST** mantenerse es que los dos mensajes salen con los tres argumentos y con el `label` tomado del `title` del dominio, nunca de un literal en castellano.
