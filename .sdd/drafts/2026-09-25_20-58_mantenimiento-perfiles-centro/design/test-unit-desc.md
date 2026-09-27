# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Estilo de los tests existentes del proyecto (referencia: `system/ventanilla/service/impl/AsistenteNuevoExpedienteServiceImplTest` y `system/ventanilla/controller/AsistenteNuevoExpedienteControllerTest`):
  - aserciones con `org.junit.jupiter.api.Assertions` (no AssertJ);
  - los `MockedStatic` se abren en `@BeforeEach` y se cierran en `@AfterEach`;
  - `I18n.get(String)` se mockea como identidad (devuelve su argumento), así los mensajes se comparan con el texto literal de la spec;
  - los mensajes de un `Optional<BusinessMessages>` se comprueban por `BusinessMessage.getMessage()` (no por el `label`, que depende del título del campo), con un helper de test que extrae la lista de textos;
  - los campos `@Inject` de un controlador se inyectan por reflexión (helper `setField`, como en los tests existentes).
- Entidades (`AceProfileCentro`, `Centro`, `Tramite`, `User`, `CentroUsuario`, `TipoUsuario`, `Cargo`) se instancian con `new` y setters, nunca se mockean. El perfil es el enum `Profile` real (p. ej. `Profile.TRAMITADOR`).
- Ningún test toca la base de datos: el repositorio se mockea.
- Mensajes exactos de la spec (constantes del test):
  - V-AceProfileCentro-001: «El centro es obligatorio»
  - V-AceProfileCentro-002: «El trámite es obligatorio»
  - V-AceProfileCentro-003: «El perfil es obligatorio»
  - V-AceProfileCentro-004: «Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario»
  - V-AceProfileCentro-005: «Indica solo uno: un tipo de usuario, un cargo o un usuario»
  - V-AceProfileCentro-006: «El usuario no pertenece al centro»
  - V-AceProfileCentro-007: «Ya existe esa asignación de perfil»
  - V-AceProfileCentro-008: «Solo puedes gestionar perfiles de los centros de los que eres supervisor»

### Decisiones tomadas ante ambigüedades
- **Métodos privados de `AceProfileCentroServiceImpl`** (`validarFila`, `validarObligatorios`, `validarCentroGestionable`, `validarDestinatario`, `validarUsuarioDelCentro`, `validarAsignacionNoRepetida`): no se testean directamente; sus ramas se ejercen a través de los métodos públicos `validateInsert`/`validateUpdate`/`validateRemove`, que son los que el diseño expone.
- **`AceProfileCentroRepository`** (clase modificada: `findCentrosSupervisados`, `existeOtraIgual`): su lógica es una consulta JPQL contra la BD, que el contrato prohíbe tocar en un test unitario; se declara «sin lógica testable unitariamente» y en los tests del servicio se mockea. Su comportamiento real lo cubren los E2E (repetición de asignación, pantalla del supervisor filtrada por sus centros).
- **Fixture común del servicio** (se reutiliza en todos sus tests salvo que el test diga otra cosa):
  - `centroMislata` (id 1) y `centroBatoi` (id 2);
  - `usuarioAutenticado` (id 7) devuelto por `SecurityUtil.getUser()`;
  - `SecurityUtil.isAdmin(usuarioAutenticado)` → `false`;
  - `repository.findCentrosSupervisados(usuarioAutenticado)` → `[centroMislata]`;
  - `repository.existeOtraIgual(any())` → `false`;
  - «fila válida con cargo»: centro `centroMislata`, trámite (id 10), perfil `TRAMITADOR`, cargo «Jefe de estudios», `tipoUsuario` y `usuario` nulos.
  - Los stubs que un test no llega a usar (p. ej. los de fase 2 en un test de fase 1) se declaran `lenient` para que `MockitoExtension` no falle por `UnnecessaryStubbing`.

---

## Clase: `com.educaflow.subsystem.security.service.impl.AceProfileCentroServiceImpl`  —  servicio

**Responsabilidad:** `ModelService` de `AceProfileCentro`: valida la fila en alta, modificación y borrado (destinatario único, obligatorios, usuario del centro, no repetida y centro gestionable por el usuario autenticado), fija las whitelists `AllowProperties` por acción y ofrece los centros que supervisa el usuario autenticado.
**Colaboradores a mockear:** `AceProfileCentroRepository` (mock pasado al constructor `AceProfileCentroServiceImpl(AceProfileCentro.class, repositoryMock)`; stubs `findCentrosSupervisados(User)` y `existeOtraIgual(AceProfileCentro)`), `SecurityUtil` (estático: `getUser()`, `isAdmin(User)`), `I18n` (estático: identidad).
**Origen diseño:** Paso 2 de `design.md` — `getCentrosSupervisados`, `validateInsert`, `validateUpdate`, `validateRemove`, `validateGetCentrosSupervisados`, `allowPropertiesInsert/Update/Remove`; reglas V-AceProfileCentro-001…008.

### Método: `Optional<BusinessMessages> validateInsert(AceProfileCentro fila)`

- **`validateInsert_filaConCargoEnCentroSupervisado_devuelveVacio`** — Tipo: happy. Verifica: `V-AceProfileCentro-001`…`V-AceProfileCentro-008` (rama OK).
  - **Arrange:** fixture común; fila válida con cargo.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** devuelve `Optional.empty()`; `verify(repository).existeOtraIgual(fila)`.
- **`validateInsert_filaConTipoUsuarioEnCentroSupervisado_devuelveVacio`** — Tipo: happy. Verifica: `V-AceProfileCentro-004`, `V-AceProfileCentro-005` (rama OK con el otro destinatario).
  - **Arrange:** fixture común; fila con centro `centroMislata`, trámite, perfil `CREADOR`, `tipoUsuario` «ALUMNO», `cargo` y `usuario` nulos.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** devuelve `Optional.empty()`.
- **`validateInsert_filaConUsuarioDelCentro_devuelveVacio`** — Tipo: happy. Verifica: `V-AceProfileCentro-006` (rama OK).
  - **Arrange:** fixture común; `usuarioDestino` (id 20) con `centroUsuarios` = [un `CentroUsuario` de `centroMislata`]; fila con centro `centroMislata`, trámite, perfil `COLABORADOR`, `usuario` = `usuarioDestino`, `tipoUsuario` y `cargo` nulos.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** devuelve `Optional.empty()`.
- **`validateInsert_administradorEnCentroNoSupervisado_devuelveVacio`** — Tipo: happy. Verifica: `V-AceProfileCentro-008` (rama administrador).
  - **Arrange:** fixture común con `SecurityUtil.isAdmin(usuarioAutenticado)` → `true` y `findCentrosSupervisados` (lenient) → lista vacía; fila válida con cargo pero con centro `centroBatoi`.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** devuelve `Optional.empty()`.
- **`validateInsert_centroNulo_devuelveMensajeCentroObligatorio`** — Tipo: error. Verifica: `V-AceProfileCentro-001`.
  - **Arrange:** fixture común; fila válida con cargo y `centro` = null.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con un único mensaje «El centro es obligatorio»; `verify(repository, never()).existeOtraIgual(any())` y `verify(repository, never()).findCentrosSupervisados(any())` (la fase 2 no se ejecuta).
- **`validateInsert_tramiteNulo_devuelveMensajeTramiteObligatorio`** — Tipo: error. Verifica: `V-AceProfileCentro-002`.
  - **Arrange:** fixture común; fila válida con cargo y `tramite` = null.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con un único mensaje «El trámite es obligatorio»; `verify(repository, never()).existeOtraIgual(any())`.
- **`validateInsert_perfilNulo_devuelveMensajePerfilObligatorio`** — Tipo: error. Verifica: `V-AceProfileCentro-003`.
  - **Arrange:** fixture común; fila válida con cargo y `perfil` = null.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con un único mensaje «El perfil es obligatorio»; `verify(repository, never()).existeOtraIgual(any())`.
- **`validateInsert_sinDestinatario_devuelveMensajeIndicaAQuien`** — Tipo: error. Verifica: `V-AceProfileCentro-004`.
  - **Arrange:** fixture común; fila con centro, trámite y perfil, y `tipoUsuario`, `cargo` y `usuario` nulos.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con un único mensaje «Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario»; `verify(repository, never()).existeOtraIgual(any())`.
- **`validateInsert_tipoUsuarioYCargo_devuelveMensajeSoloUno`** — Tipo: error. Verifica: `V-AceProfileCentro-005`.
  - **Arrange:** fixture común; fila válida con cargo «Jefe de estudios» y además `tipoUsuario` «PROFESOR».
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con un único mensaje «Indica solo uno: un tipo de usuario, un cargo o un usuario»; `verify(repository, never()).existeOtraIgual(any())`.
- **`validateInsert_tresDestinatarios_devuelveUnSoloMensajeSoloUno`** — Tipo: borde. Verifica: `V-AceProfileCentro-005`.
  - **Arrange:** fixture común; fila con `tipoUsuario`, `cargo` y `usuario` (del centro) informados a la vez.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con exactamente un mensaje «Indica solo uno: un tipo de usuario, un cargo o un usuario» (no uno por destinatario sobrante).
- **`validateInsert_obligatoriosYDestinatarioFallan_acumulaMensajesDeFase1`** — Tipo: borde. Verifica: `V-AceProfileCentro-001`, `V-AceProfileCentro-002`, `V-AceProfileCentro-003`, `V-AceProfileCentro-004`.
  - **Arrange:** fixture común; `AceProfileCentro` vacío (`new AceProfileCentro()`, todos los campos nulos).
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con exactamente los cuatro mensajes «El centro es obligatorio», «El trámite es obligatorio», «El perfil es obligatorio» e «Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario»; ningún mensaje de fase 2; `verify(repository, never()).existeOtraIgual(any())`.
- **`validateInsert_centroNoSupervisado_devuelveMensajeSoloCentrosSupervisados`** — Tipo: error. Verifica: `V-AceProfileCentro-008`.
  - **Arrange:** fixture común (supervisa solo `centroMislata`); fila válida con cargo pero con centro `centroBatoi`.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente que contiene «Solo puedes gestionar perfiles de los centros de los que eres supervisor».
- **`validateInsert_usuarioSinCentrosSupervisados_devuelveMensajeSoloCentrosSupervisados`** — Tipo: borde. Verifica: `V-AceProfileCentro-008`.
  - **Arrange:** fixture común con `findCentrosSupervisados(usuarioAutenticado)` → lista vacía; fila válida con cargo (centro `centroMislata`).
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente que contiene «Solo puedes gestionar perfiles de los centros de los que eres supervisor».
- **`validateInsert_centroSupervisadoComoOtraInstanciaConMismoId_devuelveVacio`** — Tipo: borde. Verifica: `V-AceProfileCentro-008` (comparación por id).
  - **Arrange:** fixture común; fila válida con cargo cuyo centro es una instancia **distinta** de `Centro` con id 1 (no la misma referencia que devuelve `findCentrosSupervisados`).
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** devuelve `Optional.empty()`.
- **`validateInsert_usuarioDeOtroCentro_devuelveMensajeUsuarioNoPertenece`** — Tipo: error. Verifica: `V-AceProfileCentro-006`.
  - **Arrange:** fixture común; `usuarioDestino` con `centroUsuarios` = [un `CentroUsuario` de `centroBatoi`]; fila con centro `centroMislata`, trámite, perfil y `usuario` = `usuarioDestino` como único destinatario.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente que contiene «El usuario no pertenece al centro».
- **`validateInsert_usuarioSinCentros_devuelveMensajeUsuarioNoPertenece`** — Tipo: borde. Verifica: `V-AceProfileCentro-006`.
  - **Arrange:** fixture común; `usuarioDestino` con `centroUsuarios` = lista vacía; fila con centro `centroMislata` y `usuario` = `usuarioDestino` como único destinatario.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente que contiene «El usuario no pertenece al centro».
- **`validateInsert_asignacionRepetida_devuelveMensajeYaExiste`** — Tipo: error. Verifica: `V-AceProfileCentro-007`.
  - **Arrange:** fixture común con `repository.existeOtraIgual(fila)` → `true`; fila válida con cargo.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con un único mensaje «Ya existe esa asignación de perfil»; `verify(repository).existeOtraIgual(fila)`.
- **`validateInsert_variasReglasDeFase2Fallan_acumulaMensajes`** — Tipo: borde. Verifica: `V-AceProfileCentro-006`, `V-AceProfileCentro-007`, `V-AceProfileCentro-008`.
  - **Arrange:** fixture común con `existeOtraIgual` → `true`; `usuarioDestino` sin `CentroUsuario` en `centroBatoi`; fila con centro `centroBatoi` (no supervisado), trámite, perfil y `usuario` = `usuarioDestino` como único destinatario.
  - **Act:** `service.validateInsert(fila)`.
  - **Assert:** `Optional` presente con exactamente los tres mensajes «Solo puedes gestionar perfiles de los centros de los que eres supervisor», «El usuario no pertenece al centro» y «Ya existe esa asignación de perfil».

### Método: `Optional<BusinessMessages> validateUpdate(AceProfileCentro fila, AceProfileCentro original)`

- **`validateUpdate_filaValidaEnCentroSupervisado_devuelveVacio`** — Tipo: happy. Verifica: `V-AceProfileCentro-001`…`V-AceProfileCentro-008` (rama OK).
  - **Arrange:** fixture común; fila válida con cargo con id 50; `original` = otra instancia con los mismos valores.
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** devuelve `Optional.empty()`; `verify(repository).existeOtraIgual(fila)`.
- **`validateUpdate_centroNulo_devuelveMensajeCentroObligatorio`** — Tipo: error. Verifica: `V-AceProfileCentro-001`.
  - **Arrange:** fixture común; fila válida con cargo (id 50) y `centro` = null; `original` con centro `centroMislata`.
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** `Optional` presente con un único mensaje «El centro es obligatorio».
- **`validateUpdate_tramiteNulo_devuelveMensajeTramiteObligatorio`** — Tipo: error. Verifica: `V-AceProfileCentro-002`.
  - **Arrange:** fixture común; fila válida con cargo (id 50) y `tramite` = null.
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** `Optional` presente con un único mensaje «El trámite es obligatorio».
- **`validateUpdate_perfilNulo_devuelveMensajePerfilObligatorio`** — Tipo: error. Verifica: `V-AceProfileCentro-003`.
  - **Arrange:** fixture común; fila válida con cargo (id 50) y `perfil` = null.
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** `Optional` presente con un único mensaje «El perfil es obligatorio».
- **`validateUpdate_sinDestinatario_devuelveMensajeIndicaAQuien`** — Tipo: error. Verifica: `V-AceProfileCentro-004`.
  - **Arrange:** fixture común; fila (id 50) con centro, trámite y perfil y los tres destinatarios nulos.
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** `Optional` presente con un único mensaje «Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario».
- **`validateUpdate_dosDestinatarios_devuelveMensajeSoloUno`** — Tipo: error. Verifica: `V-AceProfileCentro-005`.
  - **Arrange:** fixture común; fila válida con cargo (id 50) y además `tipoUsuario` «PROFESOR».
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** `Optional` presente con un único mensaje «Indica solo uno: un tipo de usuario, un cargo o un usuario».
- **`validateUpdate_usuarioDeOtroCentro_devuelveMensajeUsuarioNoPertenece`** — Tipo: error. Verifica: `V-AceProfileCentro-006`.
  - **Arrange:** fixture común; `usuarioDestino` solo con `CentroUsuario` de `centroBatoi`; fila (id 50) con centro `centroMislata` y `usuario` = `usuarioDestino` como único destinatario.
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** `Optional` presente que contiene «El usuario no pertenece al centro».
- **`validateUpdate_asignacionRepetida_devuelveMensajeYaExiste`** — Tipo: error. Verifica: `V-AceProfileCentro-007`.
  - **Arrange:** fixture común con `repository.existeOtraIgual(fila)` → `true`; fila válida con cargo (id 50).
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** `Optional` presente con un único mensaje «Ya existe esa asignación de perfil»; `verify(repository).existeOtraIgual(fila)` (con la propia fila, que lleva su id para excluirse).
- **`validateUpdate_centroDeLaFilaNoSupervisado_devuelveMensajeSoloCentrosSupervisados`** — Tipo: error. Verifica: `V-AceProfileCentro-008`.
  - **Arrange:** fixture común; fila válida con cargo (id 50) con centro `centroBatoi`; `original` con centro `centroMislata` (supervisado).
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** `Optional` presente que contiene «Solo puedes gestionar perfiles de los centros de los que eres supervisor» (la regla se evalúa sobre `fila`, no sobre `original`).
- **`validateUpdate_administradorEnCentroNoSupervisado_devuelveVacio`** — Tipo: happy. Verifica: `V-AceProfileCentro-008` (rama administrador).
  - **Arrange:** fixture común con `SecurityUtil.isAdmin(usuarioAutenticado)` → `true`; fila válida con cargo (id 50) con centro `centroBatoi`.
  - **Act:** `service.validateUpdate(fila, original)`.
  - **Assert:** devuelve `Optional.empty()`.
- **`validateUpdate_originalNulo_noSeUsaYDevuelveVacio`** — Tipo: borde. Verifica: `—` (el diseño fija que `original` no se usa).
  - **Arrange:** fixture común; fila válida con cargo (id 50); `original` = null.
  - **Act:** `service.validateUpdate(fila, null)`.
  - **Assert:** devuelve `Optional.empty()` sin lanzar excepción.

### Método: `Optional<BusinessMessages> validateRemove(AceProfileCentro fila)`

- **`validateRemove_filaDeCentroSupervisado_devuelveVacio`** — Tipo: happy. Verifica: `V-AceProfileCentro-008` (rama OK).
  - **Arrange:** fixture común; fila válida con cargo (id 50) de `centroMislata`.
  - **Act:** `service.validateRemove(fila)`.
  - **Assert:** devuelve `Optional.empty()`.
- **`validateRemove_filaDeCentroNoSupervisado_devuelveMensajeSoloCentrosSupervisados`** — Tipo: error. Verifica: `V-AceProfileCentro-008`.
  - **Arrange:** fixture común; fila válida con cargo (id 50) de `centroBatoi`.
  - **Act:** `service.validateRemove(fila)`.
  - **Assert:** `Optional` presente con un único mensaje «Solo puedes gestionar perfiles de los centros de los que eres supervisor».
- **`validateRemove_administradorEnCentroNoSupervisado_devuelveVacio`** — Tipo: happy. Verifica: `V-AceProfileCentro-008` (rama administrador).
  - **Arrange:** fixture común con `SecurityUtil.isAdmin(usuarioAutenticado)` → `true`; fila (id 50) de `centroBatoi`.
  - **Act:** `service.validateRemove(fila)`.
  - **Assert:** devuelve `Optional.empty()`.
- **`validateRemove_usuarioSinCentrosSupervisados_devuelveMensajeSoloCentrosSupervisados`** — Tipo: borde. Verifica: `V-AceProfileCentro-008`.
  - **Arrange:** fixture común con `findCentrosSupervisados(usuarioAutenticado)` → lista vacía; fila (id 50) de `centroMislata`.
  - **Act:** `service.validateRemove(fila)`.
  - **Assert:** `Optional` presente con un único mensaje «Solo puedes gestionar perfiles de los centros de los que eres supervisor».
- **`validateRemove_filaSinDestinatarioNiTramite_soloAplicaCentroGestionable`** — Tipo: borde. Verifica: `V-AceProfileCentro-008` (y que no aplica V-001…007).
  - **Arrange:** fixture común; fila (id 50) de `centroMislata` con `tramite`, `perfil` y los tres destinatarios nulos.
  - **Act:** `service.validateRemove(fila)`.
  - **Assert:** devuelve `Optional.empty()`; `verify(repository, never()).existeOtraIgual(any())`.

### Método: `Optional<BusinessMessages> validateGetCentrosSupervisados()`

- **`validateGetCentrosSupervisados_siempre_devuelveVacio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** fixture común con `findCentrosSupervisados` (lenient) → lista vacía.
  - **Act:** `service.validateGetCentrosSupervisados()`.
  - **Assert:** devuelve `Optional.empty()` (un usuario sin centros supervisados no recibe error).

### Método: `List<Centro> getCentrosSupervisados()`

- **`getCentrosSupervisados_supervisorDeDosCentros_devuelveLaListaDelRepositorio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** fixture común con `repository.findCentrosSupervisados(usuarioAutenticado)` → `[centroMislata, centroBatoi]`.
  - **Act:** `service.getCentrosSupervisados()`.
  - **Assert:** devuelve una lista igual a `[centroMislata, centroBatoi]` en ese orden; `verify(repository).findCentrosSupervisados(usuarioAutenticado)` (el usuario es el de `SecurityUtil.getUser()`).
- **`getCentrosSupervisados_sinCentrosSupervisados_devuelveListaVacia`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** fixture común con `repository.findCentrosSupervisados(usuarioAutenticado)` → lista vacía.
  - **Act:** `service.getCentrosSupervisados()`.
  - **Assert:** devuelve una lista vacía, sin lanzar excepción.

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_aceptaLosSeisCamposDeLaFila`** — Tipo: happy. Verifica: `—` (frontera de confianza de la acción Crear).
  - **Arrange:** servicio de la fixture común.
  - **Act:** `service.allowPropertiesInsert()`.
  - **Assert:** `allowProperty(...)` es `true` para `centro`, `tramite`, `perfil`, `tipoUsuario`, `cargo` y `usuario`.

### Método: `AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_excluyeCentroYAceptaElResto`** — Tipo: happy. Verifica: `—` (inmutabilidad de `centro` tras el alta, frontera de confianza de la acción Modificar).
  - **Arrange:** servicio de la fixture común.
  - **Act:** `service.allowPropertiesUpdate()`.
  - **Assert:** `allowProperty("centro")` es `false`; `allowProperty(...)` es `true` para `tramite`, `perfil`, `tipoUsuario`, `cargo` y `usuario`.

### Método: `AllowProperties allowPropertiesRemove()`

- **`allowPropertiesRemove_noAceptaNingunCampo`** — Tipo: happy. Verifica: `—` (frontera de confianza de la acción Borrar).
  - **Arrange:** servicio de la fixture común.
  - **Act:** `service.allowPropertiesRemove()`.
  - **Assert:** `allowProperty(...)` es `false` para `centro`, `tramite`, `perfil`, `tipoUsuario`, `cargo` y `usuario`.

---

## Clase: `com.educaflow.subsystem.security.controller.AceProfileCentroController`  —  controlador

**Responsabilidad:** método de tipo 3 que da a la vista del supervisor los ids de los centros que supervisa el usuario autenticado, con el centinela `List.of(-1L)` si no supervisa ninguno.
**Colaboradores a mockear:** `ModelServiceFactory` (mock inyectado por reflexión en el campo `@Inject modelServiceFactory`; `resolve(AceProfileCentro.class)` → mock de `AceProfileCentroService`), `AceProfileCentroService` (mock; stub `getCentrosSupervisados()`). No usa `ActionRequest`/`ActionResponse` (tipo 3).
**Origen diseño:** Paso 4 de `design.md` — `idsCentrosSupervisados()`.

### Método: `List<Long> idsCentrosSupervisados()`

- **`idsCentrosSupervisados_supervisorDeDosCentros_devuelveSusIds`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `aceProfileCentroService.getCentrosSupervisados()` → `[centro id 1, centro id 2]`.
  - **Act:** `controller.idsCentrosSupervisados()`.
  - **Assert:** devuelve `[1L, 2L]` en ese orden; `verify(modelServiceFactory).resolve(AceProfileCentro.class)`; `verify(aceProfileCentroService).getCentrosSupervisados()`.
- **`idsCentrosSupervisados_supervisorDeUnCentro_devuelveSuUnicoId`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `aceProfileCentroService.getCentrosSupervisados()` → `[centro id 1]`.
  - **Act:** `controller.idsCentrosSupervisados()`.
  - **Assert:** devuelve `[1L]` (una lista de tamaño 1, que es lo que activa el prellenado de la vista).
- **`idsCentrosSupervisados_sinCentrosSupervisados_devuelveCentinelaMenosUno`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `aceProfileCentroService.getCentrosSupervisados()` → lista vacía.
  - **Act:** `controller.idsCentrosSupervisados()`.
  - **Assert:** devuelve `[-1L]` (nunca una lista vacía, para que el `domain` `self.id IN (:idsCentrosSupervisados)` no quede en `IN ()`).

---

## Clase: `com.educaflow.subsystem.security.db.repo.AceProfileCentroRepository` — sin lógica testable
**Motivo:** los dos métodos que el diseño añade (`findCentrosSupervisados(User)`, `existeOtraIgual(AceProfileCentro)`) son consultas JPQL contra la BD; un test unitario no puede ejecutarlas sin BD real, que el contrato prohíbe. En los tests del servicio se mockean; su comportamiento real lo cubren los E2E de `test-e2e-desc.md` (asignación repetida, pantalla del supervisor limitada a sus centros). `findPerfiles` no cambia.

---

## Clase: `com.educaflow.subsystem.security.service.AceProfileCentroService` — sin lógica testable
**Motivo:** interfaz del `ModelService`; sin comportamiento propio.

---

## Clase: `com.educaflow.subsystem.security.db.AceProfileCentro` — sin lógica testable
**Motivo:** POJO de dominio generado por Axelor, sin cambios en esta iniciativa y sin métodos con lógica.

---

## Cobertura
- Clases con lógica descritas: 2 (`AceProfileCentroServiceImpl`, `AceProfileCentroController`).
- Clases omitidas (sin lógica): `AceProfileCentroRepository` (consultas JPQL, sin BD en unitarios), `AceProfileCentroService` (interfaz), `AceProfileCentro` (POJO generado).
- Reglas server-side cubiertas (`V`/`R`/`CC`): V-AceProfileCentro-001, V-AceProfileCentro-002, V-AceProfileCentro-003, V-AceProfileCentro-004, V-AceProfileCentro-005, V-AceProfileCentro-006, V-AceProfileCentro-007, V-AceProfileCentro-008. No hay reglas `R-` ni `CC-` en el diseño.
- Reglas solo-cliente excluidas (E2E en test-e2e-desc.md): U-perfiles-tramites-mi-centro-001, U-perfiles-tramites-mi-centro-002, U-perfiles-tramites-mi-centro-003, U-perfiles-tramites-mi-centro-004, U-perfiles-tramites-mi-centro-005, U-perfiles-tramites-todos-centros-001, U-perfiles-tramites-todos-centros-002, U-perfiles-tramites-todos-centros-003.
