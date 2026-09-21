# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Aserciones con `org.junit.jupiter.api.Assertions` (no AssertJ); para excepciones envueltas se usa `JUnitHelper.assertThrowsCause` (`com.educaflow.base.infrastructure.junit`).
- Una clase de test `<Clase>Test` por clase bajo test, en el mismo paquete que la clase en `src/test/java`, con el estilo de los tests existentes (`CicloServiceImplTest`, `TareaFirmaControllerTest`): los campos `@Inject` se rellenan por reflexión con un helper `setField`, los `ModelService` se construyen con `new <X>ServiceImpl(<Entidad>.class, repositorioMock)`, y los estáticos se abren en `@BeforeEach` y se cierran en `@AfterEach`.
- `I18n.get(String)` se mockea con `mockStatic(I18n.class, withSettings().strictness(LENIENT))` devolviendo su argumento (identidad), salvo donde el test diga otra cosa; así los mensajes esperados son los literales del spec.
- `SecurityUtil.getUser()` se mockea con `mockStatic(SecurityUtil.class)` y devuelve un `User` instanciado con `new User()` (id 10, `centroUsuarios` según el test). **Nunca** `AuthUtils`.
- Entidades (`Tramite`, `Centro`, `CentroUsuario`, `User`, `TipoExpediente`, `NuevoExpediente`, `PruebaV1`…) se instancian con `new` y setters; los `record` del diseño (`ContextoTramitacion`, `VistaExpediente`) se construyen con su constructor canónico y, al ser `record`, se comparan con `equals` (sirven directamente como argumento esperado en `verify`/`when`).
- Mensajes exactos del spec usados en las aserciones:
  - VAL-ContextoTramitacion-001: «Debe indicar el centro».
  - VAL-ContextoTramitacion-002: «No puede crear expedientes de este trámite en el centro indicado».
  - VAL-ContextoTramitacion-003: «No puede presentar el expediente de esa forma en el centro indicado».
  - VAL-ContextoTramitacion-004: «Debe indicar para quién es el expediente».
  - VAL-ContextoTramitacion-005: «Este trámite no permite presentar la solicitud en representación de otra persona».
  - VAL-NuevoExpediente-001: «No puede crear expedientes de este trámite en ninguno de sus centros».
  - Título del aviso de RUI-nuevo-expediente-formulario-019: «No es posible crear el expediente».
- Decisiones tomadas ante ambigüedades (sin preguntar):
  - **V-TipoExpediente-001 no tiene literal en el spec** (RES-TipoExpediente-001 no fija mensaje y el diseño solo dice qué debe transmitir): los tests exigen exactamente un `BusinessMessage` con texto no vacío; no comparan el literal.
  - **Rama `IllegalStateException` de `FormaPresentacion.fromPresentadoEnPapel`** (invariante rota): no se puede provocar sin cambiar el enum (Java no permite crear constantes en un test). Se cubre la invariante positiva (test obligatorio del diseño) y la rama de fallo queda sin test unitario, declarado aquí.
  - **Métodos privados** de `NuevoExpedienteServiceImpl` (`fireActionRule_*`, `toContextoTramitacion`, `getCentrosDisponibles`, `admiteElAlta`, `getFormaPresentarVigente`, `getFormaDeducida`, `getFormasPosibles`, `getTramiteObligatorio`) no se invocan directamente: se ejercitan a través de las acciones públicas (`preparar`, `recalcular`, `crear`, `validate*`), que son las que el diseño expone.
  - **Parte de servidor de U-nuevo-expediente-019** (el controlador compone el aviso con título y lista): se ejercita en `NuevoExpedienteController.validateCrear`, cuyo `Verifica` declara `V-NuevoExpediente-002` y no la `U-`, porque las `U-` se tratan como excluidas; el aspecto visual del aviso queda en E2E.
  - **`Tramitador.triggerInitialEvent`**: solo se describe el delta (lectura del contexto nuevo, precondiciones y R-ContextoTramitacion-001). El resto del alta (personas, número, historial, `onEnter`) no cambia y no se re-testea.
  - Los casos del test que se borra (`ContextoTramitacionServiceTest`) quedan absorbidos así: centros ordenados/filtrados → `NuevoExpedienteServiceImpl.preparar`/`validatePreparar`; papel deducido y opciones de «para quién» → `NuevoExpedienteServiceImpl.recalcular`; equivalencia perfil ↔ papel → `FormaPresentacion` y `ContextoTramitacion`; autorización del alta → `ExpedienteService.validateTriggerInitialEvent`.

---

## Clase: `com.educaflow.subsystem.expedientes.tramitacion.eventmanager.FormaPresentacion`  —  helper

**Responsabilidad:** dueño único de qué perfiles pueden iniciar un expediente y de la equivalencia perfil ↔ forma de presentar (`TELEMATICA` ↔ `CREADOR`/no papel, `EN_PAPEL` ↔ `TRAMITADOR`/papel), con la invariante «exactamente una constante por cada valor de `presentadoEnPapel`».
**Colaboradores a mockear:** ninguno (enum puro).
**Origen diseño:** Paso 2 (`getProfile`, `isPresentadoEnPapel`, `fromPresentadoEnPapel`, `fromProfile`, `esPerfilDeInicio`); test unitario obligatorio de la invariante; equivalencia usada por R-ContextoTramitacion-001 y R-NuevoExpediente-001.

### Método: `public Profile getProfile()` / `public boolean isPresentadoEnPapel()`

- **`getProfile_telematica_devuelveCreador`** — Tipo: happy. Verifica: `R-NuevoExpediente-001`.
  - **Arrange:** la constante `TELEMATICA`.
  - **Act:** `TELEMATICA.getProfile()` y `TELEMATICA.isPresentadoEnPapel()`.
  - **Assert:** `Profile.CREADOR` y `false`.
- **`getProfile_enPapel_devuelveTramitador`** — Tipo: happy. Verifica: `R-NuevoExpediente-001`.
  - **Arrange:** la constante `EN_PAPEL`.
  - **Act:** `EN_PAPEL.getProfile()` y `EN_PAPEL.isPresentadoEnPapel()`.
  - **Assert:** `Profile.TRAMITADOR` y `true`.

### Método: `public static FormaPresentacion fromPresentadoEnPapel(boolean presentadoEnPapel)`

- **`fromPresentadoEnPapel_invariante_unaConstantePorCadaValorDePresentadoEnPapel`** — Tipo: borde. Verifica: `R-NuevoExpediente-001` (test obligatorio del diseño, Paso 2).
  - **Arrange:** `FormaPresentacion.values()`.
  - **Act:** para `true` y para `false`: filtrar `values()` por `isPresentadoEnPapel() == valor` e invocar `fromPresentadoEnPapel(valor)`.
  - **Assert:** para cada valor, el filtro da exactamente una constante, y `fromPresentadoEnPapel(valor)` es esa misma constante (`assertSame`); además `values().length == 2`.
- **`fromPresentadoEnPapel_true_devuelveEnPapel`** — Tipo: happy. Verifica: `R-NuevoExpediente-001`.
  - **Arrange:** —.
  - **Act:** `fromPresentadoEnPapel(true)`.
  - **Assert:** `EN_PAPEL`.
- **`fromPresentadoEnPapel_false_devuelveTelematica`** — Tipo: happy. Verifica: `R-NuevoExpediente-001`.
  - **Arrange:** —.
  - **Act:** `fromPresentadoEnPapel(false)`.
  - **Assert:** `TELEMATICA`.

### Método: `public static Optional<FormaPresentacion> fromProfile(Profile profile)`

- **`fromProfile_creador_devuelveTelematica`** — Tipo: happy. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** `Profile.CREADOR`.
  - **Act:** `fromProfile(CREADOR)`.
  - **Assert:** `Optional.of(TELEMATICA)`.
- **`fromProfile_tramitador_devuelveEnPapel`** — Tipo: happy. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** `Profile.TRAMITADOR`.
  - **Act:** `fromProfile(TRAMITADOR)`.
  - **Assert:** `Optional.of(EN_PAPEL)`.
- **`fromProfile_perfilQueNoInicia_devuelveVacio`** — Tipo: error. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** cada uno de `COLABORADOR`, `AFECTADO`, `SECRETARIO`, `DIRECTOR`, `AUDITOR` (test parametrizado sobre los valores de `Profile` distintos de `CREADOR`/`TRAMITADOR`).
  - **Act:** `fromProfile(perfil)`.
  - **Assert:** `Optional.empty()`.
- **`fromProfile_null_devuelveVacio`** — Tipo: borde. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** `null`.
  - **Act:** `fromProfile(null)`.
  - **Assert:** `Optional.empty()`, sin excepción.

### Método: `public static boolean esPerfilDeInicio(Profile profile)`

- **`esPerfilDeInicio_creadorOTramitador_devuelveTrue`** — Tipo: happy. Verifica: `V-ContextoTramitacion-003`.
  - **Arrange:** `CREADOR` y `TRAMITADOR`.
  - **Act:** `esPerfilDeInicio(perfil)`.
  - **Assert:** `true` para los dos.
- **`esPerfilDeInicio_otroPerfil_devuelveFalse`** — Tipo: error. Verifica: `V-ContextoTramitacion-003`.
  - **Arrange:** cada perfil de `Profile` distinto de `CREADOR`/`TRAMITADOR` (parametrizado).
  - **Act:** `esPerfilDeInicio(perfil)`.
  - **Assert:** `false`.
- **`esPerfilDeInicio_null_devuelveFalse`** — Tipo: borde. Verifica: `V-ContextoTramitacion-003`.
  - **Arrange:** `null`.
  - **Act:** `esPerfilDeInicio(null)`.
  - **Assert:** `false`, sin excepción.

---

## Clase: `com.educaflow.subsystem.expedientes.tramitacion.eventmanager.ContextoTramitacion`  —  helper

**Responsabilidad:** `record` de entrada del motor (`tramite`, `centro`, `profile`, `presentadoEnRepresentacion`); exige trámite en el constructor y deduce `presentadoEnPapel` del perfil.
**Colaboradores a mockear:** ninguno.
**Origen diseño:** Paso 2 (constructor canónico compacto, `isPresentadoEnPapel`); R-ContextoTramitacion-001.

### Método: `public ContextoTramitacion(Tramite tramite, Centro centro, Profile profile, Boolean presentadoEnRepresentacion)`

- **`constructor_tramiteNulo_lanzaNullPointerException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** un `Centro`, `Profile.CREADOR`, `false`; trámite `null`.
  - **Act:** `new ContextoTramitacion(null, centro, CREADOR, false)`.
  - **Assert:** lanza `NullPointerException` con mensaje no nulo (el que dice que un contexto sin trámite es un error de programación).
- **`constructor_centroPerfilYRepresentacionNulos_seConstruyeSinExcepcion`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** un `Tramite`; `centro`, `profile` y `presentadoEnRepresentacion` `null`.
  - **Act:** `new ContextoTramitacion(tramite, null, null, null)`.
  - **Assert:** no lanza; los accesores devuelven el trámite y `null` en los otros tres (son justo lo que valida la puerta del motor).

### Método: `public boolean isPresentadoEnPapel()`

- **`isPresentadoEnPapel_tramitador_devuelveTrue`** — Tipo: happy. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** contexto con `Profile.TRAMITADOR`.
  - **Act:** `isPresentadoEnPapel()`.
  - **Assert:** `true`.
- **`isPresentadoEnPapel_creador_devuelveFalse`** — Tipo: happy. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** contexto con `Profile.CREADOR`.
  - **Act:** `isPresentadoEnPapel()`.
  - **Assert:** `false`.
- **`isPresentadoEnPapel_perfilQueNoInicia_lanzaIllegalStateException`** — Tipo: error. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** contexto con `Profile.COLABORADOR`.
  - **Act:** `isPresentadoEnPapel()`.
  - **Assert:** lanza `IllegalStateException` (nunca un valor por defecto).
- **`isPresentadoEnPapel_perfilNulo_lanzaIllegalStateException`** — Tipo: borde. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** contexto con `profile` `null`.
  - **Act:** `isPresentadoEnPapel()`.
  - **Assert:** lanza `IllegalStateException`.

---

## Clase: `com.educaflow.subsystem.expedientes.tramitacion.core.Tramitador`  —  servicio (modificado; solo el delta)

**Responsabilidad:** motor que da de alta el expediente; ahora lee el `ContextoTramitacion` nuevo, deduce `presentadoEnPapel` del perfil y exige centro y representación no nulos.
**Colaboradores a mockear:** `ExpedienteLocator` (`getInitialEventManager` → mock `InitialEventManager`; `getModelClass` → `PruebaV1.class`, subclase concreta real de `Expediente` con constructor sin argumentos; `getPhaseEventManager(tipo, cualquier fase)` → mock `PhaseEventManager`), `NumeradorRepository` (`getSiguienteNumeroExpediente` → `1L`), `ModelServiceFactory` (mock, solo para construir el `EventContext`), `SecurityUtil` (estático: `getUser()` → usuario con nombre, apellidos, DNI y email), `JPA` (estático: `JPA.save` no hace nada). Campos inyectados por reflexión.
**Origen diseño:** Paso 2, `triggerInitialEvent(ContextoTramitacion)`; R-ContextoTramitacion-001.

### Método: `public Expediente triggerInitialEvent(ContextoTramitacion contextoTramitacion) throws BusinessException`

- **`triggerInitialEvent_perfilTramitador_expedientePresentadoEnPapel`** — Tipo: happy. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** `Tramite` cuyo `defaultTipoExpediente` es un `TipoExpediente` con nombre; `Centro` con código; contexto `(tramite, centro, TRAMITADOR, false)`; mocks del bloque de colaboradores.
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** devuelve un `PruebaV1` con `presentadoEnPapel == true`, `presentadoEnRepresentacion == false`, `centro` = el del contexto y `tipoExpediente` = el `defaultTipoExpediente` del trámite; `verify(JPA.save(expediente))` una vez.
- **`triggerInitialEvent_perfilCreador_expedienteTelematico`** — Tipo: happy. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** igual, con contexto `(tramite, centro, CREADOR, true)`.
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** `presentadoEnPapel == false` y `presentadoEnRepresentacion == true` en el expediente devuelto.
- **`triggerInitialEvent_contexto_eventContextConElPerfilDelContexto`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** contexto `(tramite, centro, TRAMITADOR, false)`; `ArgumentCaptor<EventContext>` sobre `phaseEventManager.onEnterState(any(), captor)`.
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** el `EventContext` capturado tiene `getProfile() == TRAMITADOR`; `initialEventManager.triggerInitialEvent(...)` invocado una vez.
- **`triggerInitialEvent_centroNulo_lanzaRuntimeExceptionConCausaNullPointerException`** — Tipo: error. Verifica: `—` (precondición: contexto sin validar por la puerta).
  - **Arrange:** contexto `(tramite, null, CREADOR, false)`.
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** `JUnitHelper.assertThrowsCause(NullPointerException.class, …)` (el `catch (Exception)` del motor la envuelve en `RuntimeException`); el `NullPointerException` lleva mensaje no nulo; `JPA.save` nunca invocado.
- **`triggerInitialEvent_representacionNula_lanzaRuntimeExceptionConCausaNullPointerException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** contexto `(tramite, centro, CREADOR, null)`.
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** `assertThrowsCause(NullPointerException.class, …)`; `JPA.save` nunca invocado.
- **`triggerInitialEvent_perfilQueNoInicia_lanzaRuntimeExceptionConCausaIllegalStateException`** — Tipo: error. Verifica: `R-ContextoTramitacion-001`.
  - **Arrange:** contexto `(tramite, centro, COLABORADOR, false)`.
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** `assertThrowsCause(IllegalStateException.class, …)`; `JPA.save` nunca invocado.

---

## Clase: `com.educaflow.subsystem.expedientes.services.VistaExpediente`  —  helper

**Responsabilidad:** `record` con lo necesario para abrir el formulario de un expediente; calcula el título de la pestaña.
**Colaboradores a mockear:** `I18n` (estático).
**Origen diseño:** Paso 3, `title()`.

### Método: `public String title()`

- **`title_expediente_devuelveNumeroGuionNombreTraducidoDelTipo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `PruebaV1` con `numeroExpediente = "00012/2026"` y `tipoExpediente` con `name = "prueba.v1"`; `I18n.get("prueba.v1")` → `"Prueba"`; `VistaExpediente("vista", PruebaV1.class, expediente, CREADOR)`.
  - **Act:** `title()`.
  - **Assert:** `"00012/2026-Prueba"`; `I18n.get("prueba.v1")` invocado.

---

## Clase: `com.educaflow.subsystem.expedientes.services.ExpedienteService`  —  servicio (modificado; solo el delta)

**Responsabilidad:** puerta del motor: valida siempre el alta (`validateTriggerInitialEvent`) antes de disparar el evento inicial, es el único dueño de los perfiles de inicio del usuario y resuelve la vista del expediente recién creado.
**Colaboradores a mockear:** `Tramitador`, `PerfilesUsuarioService`, `ExpedienteLocator`, `ModelServiceFactory`, `PhaseEventManager` (devuelto por el locator), `SecurityUtil` (estático: `getUser()` → `usuario`), `I18n` (estático, identidad). Campos `@Inject` por reflexión. Para el caso de `Set` inmutable se usa `Mockito.spy` del servicio.
**Origen diseño:** Paso 3 — `triggerInitialEvent`, `validateTriggerInitialEvent` (V-ContextoTramitacion-001…005), `getPerfilesDeInicio`, `getVistaExpediente`; test unitario obligatorio del caso «perfil null».

Datos comunes: `tramite` (con `permitidoPresentarEnRepresentacion` según test), `centro`, `usuario`. «Perfiles del usuario» = lo que devuelve `perfilesUsuarioService.getPerfilesSobreTramite(tramite, usuario, centro)`.

### Método: `public Expediente triggerInitialEvent(ContextoTramitacion contextoTramitacion) throws BusinessException`

- **`triggerInitialEvent_contextoValido_delegaEnTramitadorYDevuelveSuExpediente`** — Tipo: happy. Verifica: `V-ContextoTramitacion-001`, `V-ContextoTramitacion-003`, `V-ContextoTramitacion-004`.
  - **Arrange:** perfiles del usuario `{CREADOR}`; contexto `(tramite, centro, CREADOR, false)`; `tramitador.triggerInitialEvent(contexto)` → `expediente`.
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** devuelve `expediente` (`assertSame`); `verify(tramitador).triggerInitialEvent(contexto)` una vez.
- **`triggerInitialEvent_contextoInvalido_lanzaValidationExceptionSinLlamarAlTramitador`** — Tipo: error. Verifica: `V-ContextoTramitacion-001`.
  - **Arrange:** contexto `(tramite, null, CREADOR, false)` (la validación falla sin necesidad de ventana: alta programática).
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** lanza `jakarta.validation.ValidationException`; `verifyNoInteractions(tramitador)`.
- **`triggerInitialEvent_perfilQueElUsuarioNoTiene_lanzaValidationExceptionSinLlamarAlTramitador`** — Tipo: error. Verifica: `V-ContextoTramitacion-003`.
  - **Arrange:** perfiles del usuario `{CREADOR}`; contexto `(tramite, centro, TRAMITADOR, false)`.
  - **Act:** `triggerInitialEvent(contexto)`.
  - **Assert:** lanza `ValidationException`; `verifyNoInteractions(tramitador)`.

### Método: `public Optional<BusinessMessages> validateTriggerInitialEvent(ContextoTramitacion contextoTramitacion)`

- **`validateTriggerInitialEvent_contextoValido_devuelveVacio`** — Tipo: happy. Verifica: `V-ContextoTramitacion-001`, `V-ContextoTramitacion-002`, `V-ContextoTramitacion-003`, `V-ContextoTramitacion-004`, `V-ContextoTramitacion-005`.
  - **Arrange:** perfiles del usuario `{CREADOR, TRAMITADOR}`; trámite sin representación; contexto `(tramite, centro, TRAMITADOR, false)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** `Optional.empty()`.
- **`validateTriggerInitialEvent_usuarioDelServidor_consultaPerfilesDelUsuarioAutenticado`** — Tipo: happy. Verifica: `V-ContextoTramitacion-002`.
  - **Arrange:** `SecurityUtil.getUser()` → `usuario`; perfiles del usuario `{CREADOR}`; contexto `(tramite, centro, CREADOR, false)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** `verify(perfilesUsuarioService).getPerfilesSobreTramite(tramite, usuario, centro)` (el usuario sale siempre de `SecurityUtil`, nunca del contexto).
- **`validateTriggerInitialEvent_centroNulo_devuelveMensajeDebeIndicarElCentro`** — Tipo: error. Verifica: `V-ContextoTramitacion-001`.
  - **Arrange:** contexto `(tramite, null, CREADOR, false)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** un único `BusinessMessage` sin nombre de campo con mensaje «Debe indicar el centro»; `verifyNoInteractions(perfilesUsuarioService)` (sin centro no se evalúan 002/003).
- **`validateTriggerInitialEvent_sinPerfilesDeInicioEnElCentro_devuelveMensajeNoPuedeCrear`** — Tipo: error. Verifica: `V-ContextoTramitacion-002`.
  - **Arrange:** perfiles del usuario `{COLABORADOR}` (ninguno de inicio); contexto `(tramite, centro, CREADOR, false)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** un único mensaje «No puede crear expedientes de este trámite en el centro indicado» (no aparece el de 003).
- **`validateTriggerInitialEvent_perfilDeInicioQueElUsuarioNoTiene_devuelveMensajeNoPuedePresentar`** — Tipo: error. Verifica: `V-ContextoTramitacion-003`.
  - **Arrange:** perfiles del usuario `{CREADOR}`; contexto `(tramite, centro, TRAMITADOR, false)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** un único mensaje «No puede presentar el expediente de esa forma en el centro indicado».
- **`validateTriggerInitialEvent_perfilQueNoIniciaAunqueElUsuarioLoTenga_devuelveMensajeNoPuedePresentar`** — Tipo: error. Verifica: `V-ContextoTramitacion-003`.
  - **Arrange:** perfiles del usuario `{CREADOR, COLABORADOR}`; contexto `(tramite, centro, COLABORADOR, false)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** un único mensaje «No puede presentar el expediente de esa forma en el centro indicado».
- **`validateTriggerInitialEvent_perfilNulo_devuelveMensajeNoPuedePresentarSinExcepcion`** — Tipo: borde. Verifica: `V-ContextoTramitacion-003` (test obligatorio del diseño).
  - **Arrange:** perfiles del usuario `Set.of(CREADOR)`; contexto `(tramite, centro, null, false)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** no lanza ninguna excepción; un único mensaje «No puede presentar el expediente de esa forma en el centro indicado».
- **`validateTriggerInitialEvent_perfilNuloConPerfilesDeInicioInmutables_noLanzaNullPointerException`** — Tipo: borde. Verifica: `V-ContextoTramitacion-003` (test obligatorio del diseño).
  - **Arrange:** `spy` del servicio con `doReturn(Set.of(CREADOR)).when(spy).getPerfilesDeInicio(tramite, centro)` (un `Set` inmutable cuyo `contains(null)` lanzaría `NullPointerException`); contexto `(tramite, centro, null, false)`.
  - **Act:** `spy.validateTriggerInitialEvent(contexto)`.
  - **Assert:** no lanza; un único mensaje «No puede presentar el expediente de esa forma en el centro indicado» (la comprobación `esPerfilDeInicio` va antes del `contains`).
- **`validateTriggerInitialEvent_representacionNula_devuelveMensajeDebeIndicarParaQuien`** — Tipo: error. Verifica: `V-ContextoTramitacion-004`.
  - **Arrange:** perfiles del usuario `{CREADOR}`; contexto `(tramite, centro, CREADOR, null)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** un único mensaje «Debe indicar para quién es el expediente» (no aparece el de 005).
- **`validateTriggerInitialEvent_representacionEnTramiteQueNoLaAdmite_devuelveMensajeNoPermiteRepresentacion`** — Tipo: error. Verifica: `V-ContextoTramitacion-005`.
  - **Arrange:** trámite con `permitidoPresentarEnRepresentacion = false`; perfiles del usuario `{CREADOR}`; contexto `(tramite, centro, CREADOR, true)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** un único mensaje «Este trámite no permite presentar la solicitud en representación de otra persona».
- **`validateTriggerInitialEvent_representacionEnTramiteQueLaAdmite_devuelveVacio`** — Tipo: happy. Verifica: `V-ContextoTramitacion-005`.
  - **Arrange:** trámite con `permitidoPresentarEnRepresentacion = true`; perfiles del usuario `{CREADOR}`; contexto `(tramite, centro, CREADOR, true)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** `Optional.empty()`.
- **`validateTriggerInitialEvent_paraMiEnTramiteQueNoAdmiteRepresentacion_devuelveVacio`** — Tipo: borde. Verifica: `V-ContextoTramitacion-005`.
  - **Arrange:** trámite con `permitidoPresentarEnRepresentacion = false`; perfiles `{CREADOR}`; contexto `(tramite, centro, CREADOR, false)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** `Optional.empty()`.
- **`validateTriggerInitialEvent_centroNuloYRepresentacionNula_acumulaLosDosMensajes`** — Tipo: borde. Verifica: `V-ContextoTramitacion-001`, `V-ContextoTramitacion-004`.
  - **Arrange:** contexto `(tramite, null, CREADOR, null)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** exactamente dos mensajes, «Debe indicar el centro» y «Debe indicar para quién es el expediente», ambos sin nombre de campo.
- **`validateTriggerInitialEvent_perfilIncorrectoYRepresentacionNoAdmitida_acumulaLosDosMensajes`** — Tipo: borde. Verifica: `V-ContextoTramitacion-003`, `V-ContextoTramitacion-005`.
  - **Arrange:** trámite sin representación; perfiles `{CREADOR}`; contexto `(tramite, centro, TRAMITADOR, true)`.
  - **Act:** `validateTriggerInitialEvent(contexto)`.
  - **Assert:** exactamente dos mensajes: el de VAL-ContextoTramitacion-003 y el de VAL-ContextoTramitacion-005.

### Método: `public Set<Profile> getPerfilesDeInicio(Tramite tramite, Centro centro)`

- **`getPerfilesDeInicio_centroConPerfiles_devuelveSoloCreadorYTramitador`** — Tipo: happy. Verifica: `—` (contrato de `getPerfilesDeInicio`: filtra con `FormaPresentacion::esPerfilDeInicio`).
  - **Arrange:** perfiles del usuario `{CREADOR, TRAMITADOR, COLABORADOR, AUDITOR}`.
  - **Act:** `getPerfilesDeInicio(tramite, centro)`.
  - **Assert:** `{CREADOR, TRAMITADOR}`; `verify(perfilesUsuarioService).getPerfilesSobreTramite(tramite, usuario, centro)` con el usuario de `SecurityUtil.getUser()`.
- **`getPerfilesDeInicio_sinPerfilesDeInicio_devuelveVacio`** — Tipo: error. Verifica: `—` (contrato de `getPerfilesDeInicio`: filtra con `FormaPresentacion::esPerfilDeInicio`).
  - **Arrange:** perfiles del usuario `{AFECTADO}`.
  - **Act:** `getPerfilesDeInicio(tramite, centro)`.
  - **Assert:** conjunto vacío.
- **`getPerfilesDeInicio_centroNulo_devuelveVacioSinConsultarPerfiles`** — Tipo: borde. Verifica: `—` (contrato de `getPerfilesDeInicio`: centro `null` → vacío sin consultar `PerfilesUsuarioService`).
  - **Arrange:** centro `null`.
  - **Act:** `getPerfilesDeInicio(tramite, null)`.
  - **Assert:** conjunto vacío; `verifyNoInteractions(perfilesUsuarioService)`.
- **`getPerfilesDeInicio_conjuntoDeOrigenConNull_noDevuelveNull`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `getPerfilesSobreTramite` → un `HashSet` con `CREADOR` y `null`.
  - **Act:** `getPerfilesDeInicio(tramite, centro)`.
  - **Assert:** `{CREADOR}`; el resultado no contiene `null` (contrato del `Set` devuelto).

### Método: `public VistaExpediente getVistaExpediente(Expediente expediente, Profile profile)`

- **`getVistaExpediente_expedienteYPerfil_devuelveVistaDeSuFaseActual`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `PruebaV1` con `tipoExpediente` y `codePhase = "RECEPCION"`; `expedienteLocator.getPhaseEventManager(tipo, "RECEPCION")` → `phaseEventManager`; `phaseEventManager.getViewName(eq(expediente), any(EventContext.class))` → `"prueba-v1-recepcion-form"`; `phaseEventManager.getModelClass()` → `PruebaV1.class`; `ArgumentCaptor<EventContext>`.
  - **Act:** `getVistaExpediente(expediente, TRAMITADOR)`.
  - **Assert:** devuelve `new VistaExpediente("prueba-v1-recepcion-form", PruebaV1.class, expediente, TRAMITADOR)` (igualdad de `record`); el `EventContext` capturado tiene `getProfile() == TRAMITADOR`.

---

## Clase: `com.educaflow.subsystem.expedientes.service.impl.TipoExpedienteServiceImpl`  —  servicio

**Responsabilidad:** `ModelService` de `TipoExpediente` que rechaza toda escritura (tipos de expediente de solo consulta, también para el Administrador) y cierra el endpoint REST automático.
**Colaboradores a mockear:** `Repository<TipoExpediente>` (mock, pasado al constructor), `I18n` (estático, identidad).
**Origen diseño:** Paso 4 — `insert`/`update`/`remove` (`UnsupportedOperationException`), `validateInsert`/`validateUpdate`/`validateRemove` (V-TipoExpediente-001), `allowPropertiesInsert`/`allowPropertiesUpdate` (deny-all).

Datos comunes: `tipoExpediente` = `new TipoExpediente()` con `code`, `name`, `tramite` y `basePackageName` rellenos; `original` = otro `TipoExpediente` igual.

### Método: `public Optional<BusinessMessages> validateInsert(TipoExpediente tipoExpediente)`

- **`validateInsert_cualquierTipo_devuelveMensajeDeSoloConsulta`** — Tipo: error. Verifica: `V-TipoExpediente-001`.
  - **Arrange:** `tipoExpediente`.
  - **Act:** `validateInsert(tipoExpediente)`.
  - **Assert:** `Optional` presente con exactamente un `BusinessMessage` con texto no vacío.
- **`validateInsert_tipoVacio_devuelveMensajeDeSoloConsulta`** — Tipo: borde. Verifica: `V-TipoExpediente-001`.
  - **Arrange:** `new TipoExpediente()` sin ningún campo.
  - **Act:** `validateInsert(tipoVacio)`.
  - **Assert:** igualmente un único mensaje (no depende de ningún dato: sin `if`).

### Método: `public Optional<BusinessMessages> validateUpdate(TipoExpediente tipoExpediente, TipoExpediente original)`

- **`validateUpdate_cualquierCambio_devuelveMensajeDeSoloConsulta`** — Tipo: error. Verifica: `V-TipoExpediente-001`.
  - **Arrange:** `tipoExpediente` con `name` distinto del de `original`.
  - **Act:** `validateUpdate(tipoExpediente, original)`.
  - **Assert:** exactamente un `BusinessMessage` con texto no vacío.
- **`validateUpdate_sinCambios_devuelveMensajeDeSoloConsulta`** — Tipo: borde. Verifica: `V-TipoExpediente-001`.
  - **Arrange:** `tipoExpediente` idéntico a `original`.
  - **Act:** `validateUpdate(tipoExpediente, original)`.
  - **Assert:** exactamente un mensaje (se rechaza aunque no cambie nada).

### Método: `public Optional<BusinessMessages> validateRemove(TipoExpediente tipoExpediente)`

- **`validateRemove_cualquierTipo_devuelveMensajeDeSoloConsulta`** — Tipo: error. Verifica: `V-TipoExpediente-001`.
  - **Arrange:** `tipoExpediente`.
  - **Act:** `validateRemove(tipoExpediente)`.
  - **Assert:** exactamente un `BusinessMessage` con texto no vacío.

### Método: `public TipoExpediente insert(TipoExpediente tipoExpediente)`

- **`insert_cualquierTipo_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: `V-TipoExpediente-001`.
  - **Arrange:** `tipoExpediente`.
  - **Act:** `insert(tipoExpediente)`.
  - **Assert:** lanza `UnsupportedOperationException`; `verifyNoInteractions(repository)`.

### Método: `public TipoExpediente update(TipoExpediente tipoExpediente, TipoExpediente original)`

- **`update_cualquierTipo_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: `V-TipoExpediente-001`.
  - **Arrange:** `tipoExpediente`, `original`.
  - **Act:** `update(tipoExpediente, original)`.
  - **Assert:** lanza `UnsupportedOperationException`; `verifyNoInteractions(repository)`.

### Método: `public void remove(TipoExpediente tipoExpediente)`

- **`remove_cualquierTipo_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: `V-TipoExpediente-001`.
  - **Arrange:** `tipoExpediente`.
  - **Act:** `remove(tipoExpediente)`.
  - **Assert:** lanza `UnsupportedOperationException`; `verifyNoInteractions(repository)`.

### Método: `public AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_ningunCampo_denegados`** — Tipo: happy. Verifica: `—` (frontera de confianza, Input AllowProperties «ninguna» de Crear).
  - **Arrange:** —.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty(p)` es `false` para `code`, `name`, `tramite` y `basePackageName`.

### Método: `public AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_ningunCampo_denegados`** — Tipo: happy. Verifica: `—` (frontera de confianza, Input AllowProperties «ninguna» de Modificar).
  - **Arrange:** —.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty(p)` es `false` para `code`, `name`, `tramite` y `basePackageName`.

---

## Clase: `com.educaflow.system.expedientes.service.impl.NuevoExpedienteServiceImpl`  —  servicio

**Responsabilidad:** servicio de la ventana «Nuevo expediente»: calcula sus campos `servidor` (CC-001…007 y los tres añadidos por el diseño), convierte la ventana en un `ContextoTramitacion` con el perfil decidido por el sistema y delega validación y alta en la puerta del motor. Nunca persiste `NuevoExpediente`.
**Colaboradores a mockear:** `ExpedienteService` (mock de clase, inyectado por reflexión en el campo `expedienteService`: `getPerfilesDeInicio`, `validateTriggerInitialEvent`, `triggerInitialEvent`, `getVistaExpediente`), `Repository<NuevoExpediente>` (mock, pasado al constructor; solo para verificar que nunca se usa), `SecurityUtil` (estático: `getUser()` → `usuario` con los `CentroUsuario` del test), `I18n` (estático, identidad).
**Origen diseño:** Paso 5 — acciones `preparar`, `recalcular`, `crear`; validadores `validatePreparar` (V-NuevoExpediente-001), `validateRecalcular`, `validateCrear` (V-NuevoExpediente-002); `allowProperties*`; y, a través de ellas, `fireActionRule_AsignarDatosTramite` (R-NuevoExpediente-002), `fireActionRule_AsignarCentrosDisponibles` (R-NuevoExpediente-003), `fireActionRule_AsignarFormaDePresentar` (R-NuevoExpediente-004), `fireActionRule_AsignarOpcionesParaQuien` (R-NuevoExpediente-005), `fireActionRule_IniciarExpediente` (R-NuevoExpediente-001) y los helpers privados.

Datos comunes: `tramite` (`name = "tramite.mi_tramite"`, `help = "<p>Ayuda</p>"`, `permitidoPresentarEnRepresentacion` según test); centros `batoi` (id 2, nombre «CIPFP Batoi») y `mislata` (id 1, nombre «CIPFP Mislata»); `usuario` dado de alta con `CentroUsuario` en los centros que diga el test. «Perfiles de inicio en X» = `expedienteService.getPerfilesDeInicio(tramite, X)` programado. «La puerta admite C» = `expedienteService.validateTriggerInitialEvent(C)` → `Optional.empty()`; «rechaza C» → `Optional.of(BusinessMessages.single("..."))`. `nuevoExpediente` = `new NuevoExpediente()` con los campos de entrada del test.

### Método: `public NuevoExpediente preparar(NuevoExpediente nuevoExpediente)`

- **`preparar_variosCentrosDisponibles_asignaDatosDelTramiteYCentrosOrdenadosPorNombre`** — Tipo: happy. Verifica: `R-NuevoExpediente-002`, `R-NuevoExpediente-003`, `CC-NuevoExpediente-001`, `CC-NuevoExpediente-002`, `CC-NuevoExpediente-003`.
  - **Arrange:** usuario en `mislata` y `batoi` (en ese orden); perfiles de inicio en los dos `{CREADOR}`; `nuevoExpediente` con `tramite`.
  - **Act:** `preparar(nuevoExpediente)`.
  - **Assert:** devuelve el mismo objeto (`assertSame`); `nombreTramite == "tramite.mi_tramite"` (vía `I18n.get`), `ayudaTramite == "<p>Ayuda</p>"`; `centrosDisponibles` itera `[batoi, mislata]` (orden por nombre); `hayUnSoloCentroDisponible == false`; `verifyNoInteractions(repository)`.
- **`preparar_unSoloCentroDisponible_marcaHayUnSoloCentro`** — Tipo: happy. Verifica: `R-NuevoExpediente-003`, `CC-NuevoExpediente-003`.
  - **Arrange:** usuario solo en `mislata`; perfiles de inicio en `mislata` `{TRAMITADOR}`.
  - **Act:** `preparar(nuevoExpediente)`.
  - **Assert:** `centrosDisponibles == {mislata}`; `hayUnSoloCentroDisponible == true`.
- **`preparar_centroSinPerfilesDeInicio_noSeOfrece`** — Tipo: error. Verifica: `R-NuevoExpediente-003`, `CC-NuevoExpediente-003`.
  - **Arrange:** usuario en `mislata` y `batoi`; perfiles de inicio en `mislata` vacío, en `batoi` `{CREADOR}`.
  - **Act:** `preparar(nuevoExpediente)`.
  - **Assert:** `centrosDisponibles == {batoi}`; `hayUnSoloCentroDisponible == true`.
- **`preparar_camposServidorEnviadosPorElCliente_seSobrescribenSiempre`** — Tipo: borde. Verifica: `R-NuevoExpediente-002`, `R-NuevoExpediente-003`.
  - **Arrange:** usuario en `mislata` y `batoi` con perfiles de inicio `{CREADOR}`; `nuevoExpediente` llega con `nombreTramite = "manipulado"`, `ayudaTramite = "<script>"`, `centrosDisponibles = {otroCentro}` y `hayUnSoloCentroDisponible = true`.
  - **Act:** `preparar(nuevoExpediente)`.
  - **Assert:** `nombreTramite`, `ayudaTramite`, `centrosDisponibles` (`[batoi, mislata]`) y `hayUnSoloCentroDisponible` (`false`) son los calculados; nada de lo enviado sobrevive (asignación incondicional).
- **`preparar_centroUsuarioSinCentroYCentroSinNombre_ignoraNullsYOrdenaSinNombreAlFinal`** — Tipo: borde. Verifica: `CC-NuevoExpediente-003`.
  - **Arrange:** usuario con un `CentroUsuario` sin centro, un centro `sinNombre` (nombre `null`) y `mislata`; perfiles de inicio en `sinNombre` y `mislata` `{CREADOR}`.
  - **Act:** `preparar(nuevoExpediente)`.
  - **Assert:** `centrosDisponibles` itera `[mislata, sinNombre]`; no se consulta `getPerfilesDeInicio` con centro `null`.
- **`preparar_sinCentrosDisponibles_lanzaValidationException`** — Tipo: error. Verifica: `V-NuevoExpediente-001`.
  - **Arrange:** usuario en `mislata` con perfiles de inicio vacíos.
  - **Act:** `preparar(nuevoExpediente)`.
  - **Assert:** lanza `ValidationException` (vía `throwIfInvalid`); `nombreTramite` y `centrosDisponibles` siguen sin asignar.
- **`preparar_sinTramite_lanzaIllegalStateException`** — Tipo: error. Verifica: `—` (`getTramiteObligatorio`).
  - **Arrange:** `nuevoExpediente` sin trámite.
  - **Act:** `preparar(nuevoExpediente)`.
  - **Assert:** lanza `IllegalStateException`; `verifyNoInteractions(expedienteService)`.

### Método: `public Optional<BusinessMessages> validatePreparar(NuevoExpediente nuevoExpediente)`

- **`validatePreparar_conCentroDisponible_devuelveVacio`** — Tipo: happy. Verifica: `V-NuevoExpediente-001`.
  - **Arrange:** usuario en `mislata` con perfiles de inicio `{CREADOR}`.
  - **Act:** `validatePreparar(nuevoExpediente)`.
  - **Assert:** `Optional.empty()`.
- **`validatePreparar_ningunCentroConPerfilDeInicio_devuelveMensajeNingunCentro`** — Tipo: error. Verifica: `V-NuevoExpediente-001`.
  - **Arrange:** usuario en `mislata` y `batoi`, perfiles de inicio vacíos en los dos.
  - **Act:** `validatePreparar(nuevoExpediente)`.
  - **Assert:** un único mensaje «No puede crear expedientes de este trámite en ninguno de sus centros».
- **`validatePreparar_usuarioSinCentros_devuelveMensajeNingunCentro`** — Tipo: borde. Verifica: `V-NuevoExpediente-001`.
  - **Arrange:** usuario con `centroUsuarios` vacío (y, en una segunda variante, `null`).
  - **Act:** `validatePreparar(nuevoExpediente)`.
  - **Assert:** el mismo mensaje único, sin excepción; `getPerfilesDeInicio` nunca invocado.
- **`validatePreparar_sinTramite_lanzaIllegalStateException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `nuevoExpediente` sin trámite.
  - **Act:** `validatePreparar(nuevoExpediente)`.
  - **Assert:** lanza `IllegalStateException`.

### Método: `public NuevoExpediente recalcular(NuevoExpediente nuevoExpediente)`

- **`recalcular_dosPerfilesYEnPapel_preguntaFormaYUsaTramitador`** — Tipo: happy. Verifica: `R-NuevoExpediente-004`, `R-NuevoExpediente-005`, `CC-NuevoExpediente-004`, `CC-NuevoExpediente-007`.
  - **Arrange:** perfiles de inicio en `mislata` `{CREADOR, TRAMITADOR}`; `nuevoExpediente` con `tramite`, `centro = mislata`, `presentadoEnPapel = true`; la puerta admite `(tramite, mislata, TRAMITADOR, false)` y `(tramite, mislata, TRAMITADOR, true)`.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** devuelve el mismo objeto; `hayQuePreguntarPresentacion == true`, `presentadoEnPapelDeducido == null`, `presentadoEnPapelVigente == true`; `verify(expedienteService).validateTriggerInitialEvent(new ContextoTramitacion(tramite, mislata, TRAMITADOR, false))` y con `true`; nunca con `CREADOR`.
- **`recalcular_dosPerfilesYTelematico_preguntaFormaYUsaCreador`** — Tipo: happy. Verifica: `R-NuevoExpediente-004`, `R-NuevoExpediente-005`, `CC-NuevoExpediente-004`.
  - **Arrange:** como el anterior con `presentadoEnPapel = false`; la puerta admite los contextos con `CREADOR`.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** `hayQuePreguntarPresentacion == true`, `presentadoEnPapelDeducido == null`, `presentadoEnPapelVigente == false`; la puerta se consulta con `CREADOR` (false y true).
- **`recalcular_soloTramitadorYVentanaTelematica_deduceEnPapelYUsaTramitador`** — Tipo: happy. Verifica: `R-NuevoExpediente-004`, `R-NuevoExpediente-005`, `CC-NuevoExpediente-007`, `CC-NuevoExpediente-005`.
  - **Arrange:** perfiles de inicio `{TRAMITADOR}`; ventana con `presentadoEnPapel = false`; la puerta admite `(tramite, mislata, TRAMITADOR, false)` y rechaza `(…, TRAMITADOR, true)`.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** `hayQuePreguntarPresentacion == false`, `presentadoEnPapelDeducido == true`, `presentadoEnPapelVigente == true` (la deducida manda sobre la ventana); `sePuedeCrearParaMi == true`, `sePuedeCrearEnRepresentacion == false` y `hayQuePreguntarParaQuien == false`; la puerta se consulta con `TRAMITADOR` y nunca con `CREADOR` (CC-005/006 con la forma vigente, D4).
- **`recalcular_soloCreadorYVentanaEnPapel_deduceTelematicoYUsaCreador`** — Tipo: happy. Verifica: `R-NuevoExpediente-004`, `R-NuevoExpediente-005`, `CC-NuevoExpediente-007`.
  - **Arrange:** perfiles de inicio `{CREADOR}`; ventana con `presentadoEnPapel = true`; la puerta admite los dos contextos con `CREADOR`.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** `presentadoEnPapelDeducido == false`, `presentadoEnPapelVigente == false`, `hayQuePreguntarPresentacion == false`; la puerta se consulta con `CREADOR`.
- **`recalcular_sinCentro_noPreguntaNiDeduceYOpcionesFalse`** — Tipo: borde. Verifica: `R-NuevoExpediente-004`, `R-NuevoExpediente-005`, `CC-NuevoExpediente-004`, `CC-NuevoExpediente-005`, `CC-NuevoExpediente-006`, `CC-NuevoExpediente-007`.
  - **Arrange:** `centro = null`, `presentadoEnPapel = false`; `getPerfilesDeInicio(tramite, null)` → vacío; la puerta rechaza cualquier contexto (mensaje de VAL-ContextoTramitacion-001).
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** `hayQuePreguntarPresentacion == false`, `presentadoEnPapelDeducido == null`, `presentadoEnPapelVigente == false` (la de la ventana), `sePuedeCrearParaMi`, `sePuedeCrearEnRepresentacion` y `hayQuePreguntarParaQuien` `false`.
- **`recalcular_puertaAdmiteLasDosOpciones_hayQuePreguntarParaQuien`** — Tipo: happy. Verifica: `R-NuevoExpediente-005`, `CC-NuevoExpediente-005`, `CC-NuevoExpediente-006`.
  - **Arrange:** perfiles de inicio `{CREADOR}`; trámite con representación; la puerta admite `(…, CREADOR, false)` y `(…, CREADOR, true)`.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** `sePuedeCrearParaMi == true`, `sePuedeCrearEnRepresentacion == true`, `hayQuePreguntarParaQuien == true`.
- **`recalcular_puertaRechazaRepresentacion_soloParaMiSinPreguntar`** — Tipo: error. Verifica: `R-NuevoExpediente-005`, `CC-NuevoExpediente-006`.
  - **Arrange:** perfiles de inicio `{CREADOR}`; la puerta admite `(…, CREADOR, false)` y rechaza `(…, CREADOR, true)` (mensaje de VAL-ContextoTramitacion-005).
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** `sePuedeCrearParaMi == true`, `sePuedeCrearEnRepresentacion == false`, `hayQuePreguntarParaQuien == false`.
- **`recalcular_puertaSoloAdmiteRepresentacion_soloEnRepresentacionSinPreguntar`** — Tipo: borde. Verifica: `R-NuevoExpediente-005`, `CC-NuevoExpediente-005`.
  - **Arrange:** perfiles de inicio `{CREADOR}`; la puerta rechaza `(…, CREADOR, false)` y admite `(…, CREADOR, true)`.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** `sePuedeCrearParaMi == false`, `sePuedeCrearEnRepresentacion == true`, `hayQuePreguntarParaQuien == false`.
- **`recalcular_puertaRechazaAmbas_todasLasOpcionesFalse`** — Tipo: error. Verifica: `R-NuevoExpediente-005`, `CC-NuevoExpediente-005`, `CC-NuevoExpediente-006`.
  - **Arrange:** perfiles de inicio `{CREADOR}`; la puerta rechaza los dos contextos.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** los tres campos de «para quién» `false`.
- **`recalcular_camposServidorEnviadosPorElCliente_seSobrescribenSiempre`** — Tipo: borde. Verifica: `R-NuevoExpediente-004`, `R-NuevoExpediente-005`.
  - **Arrange:** perfiles de inicio `{CREADOR}`; la puerta rechaza los dos contextos; `nuevoExpediente` llega con `hayQuePreguntarPresentacion = true`, `presentadoEnPapelDeducido = true`, `presentadoEnPapelVigente = true`, `sePuedeCrearParaMi = true`, `sePuedeCrearEnRepresentacion = true`, `hayQuePreguntarParaQuien = true` y `presentadoEnPapel = false`.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** quedan `false`, `false`, `false`, `false`, `false`, `false` respectivamente (`presentadoEnPapelDeducido == false` por la forma deducida telemática); `verifyNoInteractions(repository)`.
- **`recalcular_sinTramite_lanzaIllegalStateException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `nuevoExpediente` sin trámite.
  - **Act:** `recalcular(nuevoExpediente)`.
  - **Assert:** lanza `IllegalStateException`.

### Método: `public Optional<BusinessMessages> validateRecalcular(NuevoExpediente nuevoExpediente)`

- **`validateRecalcular_cualquierEstado_devuelveVacio`** — Tipo: happy. Verifica: `—` (el spec no declara validaciones para Recalcular).
  - **Arrange:** `nuevoExpediente` con trámite, centro y papel; y una segunda variante con todos los campos `null`.
  - **Act:** `validateRecalcular(nuevoExpediente)`.
  - **Assert:** `Optional.empty()` en los dos casos; `verifyNoInteractions(expedienteService)`.

### Método: `public VistaExpediente crear(NuevoExpediente nuevoExpediente) throws BusinessException`

- **`crear_telematicoParaMi_iniciaConCreadorYDevuelveLaVista`** — Tipo: happy. Verifica: `R-NuevoExpediente-001`, `V-NuevoExpediente-002`.
  - **Arrange:** ventana con `tramite`, `centro = mislata`, `presentadoEnPapel = false`, `presentadoEnRepresentacion = false`; `contexto = new ContextoTramitacion(tramite, mislata, CREADOR, false)`; la puerta admite `contexto`; `expedienteService.triggerInitialEvent(contexto)` → `expediente`; `expedienteService.getVistaExpediente(expediente, CREADOR)` → `vista`.
  - **Act:** `crear(nuevoExpediente)`.
  - **Assert:** devuelve `vista` (`assertSame`); `verify` de `validateTriggerInitialEvent(contexto)`, `triggerInitialEvent(contexto)` y `getVistaExpediente(expediente, CREADOR)` (se valida exactamente el contexto que luego se inicia); `verifyNoInteractions(repository)`.
- **`crear_enPapelEnRepresentacion_iniciaConTramitador`** — Tipo: happy. Verifica: `R-NuevoExpediente-001`.
  - **Arrange:** ventana con `presentadoEnPapel = true`, `presentadoEnRepresentacion = true`; `contexto = (tramite, mislata, TRAMITADOR, true)`; la puerta lo admite; `triggerInitialEvent` → `expediente`; `getVistaExpediente(expediente, TRAMITADOR)` → `vista`.
  - **Act:** `crear(nuevoExpediente)`.
  - **Assert:** devuelve `vista`; `triggerInitialEvent` recibe exactamente `(tramite, mislata, TRAMITADOR, true)` (el perfil sale de la forma de presentar, nunca de la ventana).
- **`crear_puertaRechazaElContexto_lanzaValidationExceptionSinIniciar`** — Tipo: error. Verifica: `V-NuevoExpediente-002`.
  - **Arrange:** ventana con centro y papel; la puerta rechaza el contexto con el mensaje de VAL-ContextoTramitacion-003.
  - **Act:** `crear(nuevoExpediente)`.
  - **Assert:** lanza `ValidationException`; `verify(expedienteService, never()).triggerInitialEvent(any())` y `never()` `getVistaExpediente`.
- **`crear_representacionSinContestar_llegaNulaALaPuerta`** — Tipo: borde. Verifica: `V-NuevoExpediente-002`.
  - **Arrange:** ventana con `presentadoEnRepresentacion = null`; la puerta rechaza `(tramite, mislata, CREADOR, null)` con el mensaje de VAL-ContextoTramitacion-004.
  - **Act:** `crear(nuevoExpediente)`.
  - **Assert:** lanza `ValidationException`; la puerta se consultó con `presentadoEnRepresentacion == null` (no convertido a `false`).
- **`crear_elMotorLanzaBusinessException_seRelanza`** — Tipo: error. Verifica: `R-NuevoExpediente-001`.
  - **Arrange:** la puerta admite el contexto; `triggerInitialEvent(contexto)` lanza `new BusinessException("fallo")`.
  - **Act:** `crear(nuevoExpediente)`.
  - **Assert:** lanza esa misma `BusinessException`; `getVistaExpediente` nunca invocado.
- **`crear_sinTramite_lanzaIllegalStateException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `nuevoExpediente` sin trámite.
  - **Act:** `crear(nuevoExpediente)`.
  - **Assert:** lanza `IllegalStateException`; `verifyNoInteractions(expedienteService)`.

### Método: `public Optional<BusinessMessages> validateCrear(NuevoExpediente nuevoExpediente)`

- **`validateCrear_puertaSinMensajes_devuelveVacio`** — Tipo: happy. Verifica: `V-NuevoExpediente-002`.
  - **Arrange:** ventana `(tramite, mislata, presentadoEnPapel = false, presentadoEnRepresentacion = false)`; la puerta admite `(tramite, mislata, CREADOR, false)`.
  - **Act:** `validateCrear(nuevoExpediente)`.
  - **Assert:** `Optional.empty()`; `verify(expedienteService).validateTriggerInitialEvent(new ContextoTramitacion(tramite, mislata, CREADOR, false))`.
- **`validateCrear_puertaConMensajes_devuelveLosMismosMensajes`** — Tipo: error. Verifica: `V-NuevoExpediente-002`.
  - **Arrange:** ventana `(tramite, null, presentadoEnPapel = true, presentadoEnRepresentacion = null)`; la puerta devuelve para `(tramite, null, TRAMITADOR, null)` un `BusinessMessages` con los mensajes de VAL-ContextoTramitacion-001 y 004.
  - **Act:** `validateCrear(nuevoExpediente)`.
  - **Assert:** devuelve ese mismo `BusinessMessages` (`assertSame` sobre el contenido del `Optional`), con los dos mensajes; la ventana no añade ni repite ninguna condición.
- **`validateCrear_noIniciaNada_soloValida`** — Tipo: borde. Verifica: `V-NuevoExpediente-002`.
  - **Arrange:** la puerta admite el contexto.
  - **Act:** `validateCrear(nuevoExpediente)`.
  - **Assert:** `never()` `triggerInitialEvent` ni `getVistaExpediente`.

### Método: `public AllowProperties allowPropertiesPreparar()`

- **`allowPropertiesPreparar_soloTramite`** — Tipo: happy. Verifica: `—` (frontera de confianza, Input AllowProperties de Preparar).
  - **Arrange:** —.
  - **Act:** `allowPropertiesPreparar()`.
  - **Assert:** `allowProperty("tramite") == true`; `false` para `centro`, `presentadoEnPapel`, `presentadoEnRepresentacion` y los diez campos `servidor` (`nombreTramite`, `ayudaTramite`, `centrosDisponibles`, `hayUnSoloCentroDisponible`, `hayQuePreguntarPresentacion`, `presentadoEnPapelDeducido`, `presentadoEnPapelVigente`, `sePuedeCrearParaMi`, `sePuedeCrearEnRepresentacion`, `hayQuePreguntarParaQuien`); `innerAllowProperties("tramite").allowProperty("name") == false` (de la relación solo viaja el id).

### Método: `public AllowProperties allowPropertiesRecalcular()`

- **`allowPropertiesRecalcular_tramiteCentroYPresentadoEnPapel`** — Tipo: happy. Verifica: `—` (frontera de confianza, Input AllowProperties de Recalcular).
  - **Arrange:** —.
  - **Act:** `allowPropertiesRecalcular()`.
  - **Assert:** `true` para `tramite`, `centro`, `presentadoEnPapel`; `false` para `presentadoEnRepresentacion` y los diez campos `servidor`; `innerAllowProperties("centro").allowProperty("name") == false`.

### Método: `public AllowProperties allowPropertiesCrear()`

- **`allowPropertiesCrear_losCuatroCamposDeEntrada`** — Tipo: happy. Verifica: `—` (frontera de confianza, Input AllowProperties de Crear).
  - **Arrange:** —.
  - **Act:** `allowPropertiesCrear()`.
  - **Assert:** `true` para `tramite`, `centro`, `presentadoEnPapel`, `presentadoEnRepresentacion`; `false` para los diez campos `servidor` y para `profile` (no existe en el modelo: nada por donde colarlo).

### Método: `public AllowProperties allowPropertiesInsert()` / `public AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesInsertYUpdate_ningunCampo_denegados`** — Tipo: happy. Verifica: `—` (frontera de confianza, Input AllowProperties «ninguna» de Modificar).
  - **Arrange:** —.
  - **Act:** `allowPropertiesInsert()` y `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty(p) == false` para los catorce campos del modelo (cuatro de entrada y diez `servidor`) en los dos.

---

## Clase: `com.educaflow.system.expedientes.controller.NuevoExpedienteController`  —  controlador

**Responsabilidad:** controlador de la ventana: obtiene el modelo con la whitelist de cada acción, delega en `NuevoExpedienteService` y solo devuelve valores (nunca atributos de vista), errores de negocio con `ActionResponseHelper` o la vista del expediente creado.
**Colaboradores a mockear:** `ModelServiceFactory` (inyectado por reflexión; `resolve(NuevoExpediente.class)` → mock `NuevoExpedienteService`), `NuevoExpedienteService` (cada `allowProperties<Acción>()` → `AllowProperties.createDenyAllProperties()`, para que el modelo llegue vacío y no se toque BD al mapear), `JpaRepository` (estático: `JpaRepository.of(NuevoExpediente.class)` → mock cuyo `create(null)` devuelve `new NuevoExpediente()`), `ActionRequest` (`getData()` → mapa con `context` = `{_model: NuevoExpediente}`), `ActionResponse` (verificaciones), `I18n` (estático, identidad). Estilo de `TareaFirmaControllerTest`.
**Origen diseño:** Paso 6 — `validatePreparar`, `preparar`, `recalcular`, `validateCrear`, `crear`; Notas §11 (manejo de errores de `crear`); D7.

### Método: `public void validatePreparar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`validatePreparar_conMensajes_respondeErrorSinTitulo`** — Tipo: error. Verifica: `V-NuevoExpediente-001`.
  - **Arrange:** `servicio.validatePreparar(any())` → `Optional.of(BusinessMessages.single("No puede crear expedientes de este trámite en ninguno de sus centros"))`.
  - **Act:** `validatePreparar(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setError("<ul><li>No puede crear expedientes de este trámite en ninguno de sus centros</li></ul>")` (sobrecarga sin título, D7); `verify(servicio).allowPropertiesPreparar()`.
- **`validatePreparar_sinMensajes_noRespondeError`** — Tipo: happy. Verifica: `V-NuevoExpediente-001`.
  - **Arrange:** `servicio.validatePreparar(any())` → `Optional.empty()`.
  - **Act:** `validatePreparar(actionRequest, actionResponse)`.
  - **Assert:** `setError` nunca invocado (ninguna sobrecarga).

### Método: `public void preparar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`preparar_resultadoDelServicio_devuelveSusValoresYCentrosEnOrden`** — Tipo: happy. Verifica: `—` (delegación en `NuevoExpedienteServiceImpl`; la regla se prueba en el servicio).
  - **Arrange:** `servicio.preparar(any())` → `NuevoExpediente` con `nombreTramite = "Mi trámite"`, `ayudaTramite = "<p>Ayuda</p>"`, `centrosDisponibles` = `LinkedHashSet[batoi(2, «CIPFP Batoi»), mislata(1, «CIPFP Mislata»)]`, `hayUnSoloCentroDisponible = false`.
  - **Act:** `preparar(actionRequest, actionResponse)`.
  - **Assert:** `setValue("nombreTramite", "Mi trámite")`, `setValue("ayudaTramite", "<p>Ayuda</p>")`, `setValue("centrosDisponibles", [ {id: 2, name: "CIPFP Batoi"}, {id: 1, name: "CIPFP Mislata"} ])` (en ese orden), `setValue("hayUnSoloCentroDisponible", false)`; `verify(servicio).allowPropertiesPreparar()`; `setAttr` nunca invocado (el controlador no fija atributos de vista).
- **`preparar_servicioLanzaValidationException_seTransmiteSinEnvolver`** — Tipo: error. Verifica: `—` (delegación en `NuevoExpedienteServiceImpl`; la regla se prueba en el servicio).
  - **Arrange:** `servicio.preparar(any())` lanza `ValidationException`.
  - **Act:** `preparar(actionRequest, actionResponse)`.
  - **Assert:** lanza la misma `ValidationException` (`assertSame`); `setValue` nunca invocado.

### Método: `public void recalcular(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`recalcular_resultadoDelServicio_devuelveLosSeisValores`** — Tipo: happy. Verifica: `—` (delegación en `NuevoExpedienteServiceImpl`; la regla se prueba en el servicio).
  - **Arrange:** `servicio.recalcular(any())` → `NuevoExpediente` con `hayQuePreguntarPresentacion = false`, `presentadoEnPapelDeducido = true`, `presentadoEnPapelVigente = true`, `sePuedeCrearParaMi = true`, `sePuedeCrearEnRepresentacion = false`, `hayQuePreguntarParaQuien = false`.
  - **Act:** `recalcular(actionRequest, actionResponse)`.
  - **Assert:** un `setValue` por cada uno de los seis campos con esos valores; `verify(servicio).allowPropertiesRecalcular()`; `setAttr` nunca invocado.
- **`recalcular_formaDeducidaSinValor_devuelveNull`** — Tipo: borde. Verifica: `—` (delegación en `NuevoExpedienteServiceImpl`; la regla se prueba en el servicio).
  - **Arrange:** el modelo devuelto tiene `presentadoEnPapelDeducido = null`.
  - **Act:** `recalcular(actionRequest, actionResponse)`.
  - **Assert:** `setValue("presentadoEnPapelDeducido", null)` (no se convierte a `false`).

### Método: `public void validateCrear(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`validateCrear_conMensajes_respondeErrorConTituloYUnaLineaPorMensaje`** — Tipo: error. Verifica: `V-NuevoExpediente-002` (ejercita además la parte de servidor de U-nuevo-expediente-019; ver Convenciones).
  - **Arrange:** `servicio.validateCrear(any())` → `BusinessMessages` con «Debe indicar el centro» y «Debe indicar para quién es el expediente» (sin nombre de campo).
  - **Act:** `validateCrear(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setError("<ul><li>Debe indicar el centro</li><li>Debe indicar para quién es el expediente</li></ul>", "No es posible crear el expediente")`; `verify(servicio).allowPropertiesCrear()`.
- **`validateCrear_sinMensajes_noRespondeError`** — Tipo: happy. Verifica: `V-NuevoExpediente-002`.
  - **Arrange:** `servicio.validateCrear(any())` → `Optional.empty()`.
  - **Act:** `validateCrear(actionRequest, actionResponse)`.
  - **Assert:** `setError` nunca invocado.

### Método: `public void crear(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`crear_altaCorrecta_abreElFormularioDelExpedienteYCierraLaVentana`** — Tipo: happy. Verifica: `—` (delegación en `NuevoExpedienteServiceImpl`; la regla se prueba en el servicio).
  - **Arrange:** `PruebaV1` con id 5, `numeroExpediente = "00001/2026"` y tipo con `name = "Prueba"`; `servicio.crear(any())` → `new VistaExpediente("prueba-v1-form", PruebaV1.class, expediente, CREADOR)`; `ArgumentCaptor<Map>` sobre `setView`.
  - **Act:** `crear(actionRequest, actionResponse)`.
  - **Assert:** `verify(servicio).allowPropertiesCrear()`; `setView` invocado una vez con un mapa cuyo título es `"00001/2026-Prueba"` y cuyo contexto lleva `_profile = "CREADOR"` y `_showRecord = 5`; `verify(actionResponse).setCanClose(true)`; `setError` nunca invocado.
- **`crear_servicioLanzaBusinessException_seRelanzaEnvueltaEnRuntimeException`** — Tipo: error. Verifica: `—` (Notas §11).
  - **Arrange:** `servicio.crear(any())` lanza `businessException = new BusinessException("fallo")`.
  - **Act:** `crear(actionRequest, actionResponse)`.
  - **Assert:** lanza `RuntimeException` cuya causa es `businessException` (`assertSame`); `setView`, `setCanClose` y `setError` nunca invocados (no se capturan sus mensajes).
- **`crear_servicioLanzaValidationException_seTransmiteSinEnvolver`** — Tipo: error. Verifica: `—` (delegación en `NuevoExpedienteServiceImpl`; la regla se prueba en el servicio).
  - **Arrange:** `servicio.crear(any())` lanza `validationException` (`jakarta.validation.ValidationException`).
  - **Act:** `crear(actionRequest, actionResponse)`.
  - **Assert:** lanza exactamente `validationException` (`assertSame`, no envuelta); `setView` nunca invocado.
- **`crear_servicioLanzaUnauthorizedException_seTransmiteSinEnvolver`** — Tipo: error. Verifica: `—` (Notas §11).
  - **Arrange:** `servicio.crear(any())` lanza `unauthorizedException` (`org.apache.shiro.authz.UnauthorizedException`).
  - **Act:** `crear(actionRequest, actionResponse)`.
  - **Assert:** lanza exactamente `unauthorizedException` (`assertSame`); `setView` nunca invocado.

---

## Clase: `com.educaflow.system.expedientes.db.NuevoExpediente` — sin lógica testable
**Motivo:** POJO de dominio generado por Axelor desde `domains/NuevoExpediente.xml` (`persistable="false"`); sus campos `servidor` los calcula `NuevoExpedienteServiceImpl`, que es donde se testean.

## Clase: `com.educaflow.system.expedientes.service.NuevoExpedienteService` — sin lógica testable
**Motivo:** interfaz sin comportamiento; su contrato se testea en `NuevoExpedienteServiceImpl`.

## Clase: `com.educaflow.subsystem.expedientes.service.TipoExpedienteService` — sin lógica testable
**Motivo:** interfaz vacía que solo existe para que `ModelServiceFactory` encuentre `TipoExpedienteServiceImpl`.

## Clase: `com.educaflow.subsystem.expedientes.tramitacion.eventmanager.InitialEventContext` — sin lógica testable
**Motivo:** el diseño solo le cambia un import y el Javadoc; no hay comportamiento nuevo ni cambiado.

## Clase: `com.educaflow.subsystem.expedientes.controllers.ExpedienteController` — sin lógica testable
**Motivo:** el diseño solo borra `triggerInitialEvent` y un import sin uso; `viewExpediente`, `triggerEvent` y `getTabName` se conservan tal cual, así que no hay comportamiento nuevo que testear.

---

## Cobertura
- Clases con lógica descritas: 8 (`FormaPresentacion`, `ContextoTramitacion`, `Tramitador` —delta—, `VistaExpediente`, `ExpedienteService` —delta—, `TipoExpedienteServiceImpl`, `NuevoExpedienteServiceImpl`, `NuevoExpedienteController`).
- Clases omitidas (sin lógica): `NuevoExpediente` (POJO generado), `NuevoExpedienteService` (interfaz), `TipoExpedienteService` (interfaz), `InitialEventContext` (solo import/Javadoc), `ExpedienteController` (solo borrado de un método).
- Reglas server-side cubiertas (`V`/`R`/`CC`): `V-ContextoTramitacion-001`, `V-ContextoTramitacion-002`, `V-ContextoTramitacion-003`, `V-ContextoTramitacion-004`, `V-ContextoTramitacion-005`, `V-NuevoExpediente-001`, `V-NuevoExpediente-002`, `V-TipoExpediente-001`, `R-ContextoTramitacion-001`, `R-NuevoExpediente-001`, `R-NuevoExpediente-002`, `R-NuevoExpediente-003`, `R-NuevoExpediente-004`, `R-NuevoExpediente-005`, `CC-NuevoExpediente-001`, `CC-NuevoExpediente-002`, `CC-NuevoExpediente-003`, `CC-NuevoExpediente-004`, `CC-NuevoExpediente-005`, `CC-NuevoExpediente-006`, `CC-NuevoExpediente-007`.
- Reglas solo-cliente excluidas (E2E en test-e2e-desc.md): `U-nuevo-expediente-001`, `U-nuevo-expediente-002`, `U-nuevo-expediente-003`, `U-nuevo-expediente-004`, `U-nuevo-expediente-005`, `U-nuevo-expediente-006`, `U-nuevo-expediente-007`, `U-nuevo-expediente-008`, `U-nuevo-expediente-009`, `U-nuevo-expediente-010`, `U-nuevo-expediente-011`, `U-nuevo-expediente-012`, `U-nuevo-expediente-013`, `U-nuevo-expediente-014`, `U-nuevo-expediente-015`, `U-nuevo-expediente-016`, `U-nuevo-expediente-017`, `U-nuevo-expediente-018`, `U-nuevo-expediente-019` (su presentación visual; la composición del aviso en el servidor se ejercita en `NuevoExpedienteController.validateCrear` sin declararla en `Verifica`).
