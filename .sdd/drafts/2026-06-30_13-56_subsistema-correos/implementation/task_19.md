---
type: implementation-task
---

# Tarea 19 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImplTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita (p.ej. la descripción dice
      `insert(X)` y el código tiene `guardarX(X, Long)`), o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`). Estáticos del stack con `Mockito.mockStatic` (patrón ya usado en el proyecto para `I18n`: campo `MockedStatic<...>` + `@BeforeEach`/`@AfterEach`, con `Strictness.LENIENT` cuando no todos los tests de la clase recorren la rama que lo consume; para mocks estáticos puntuales de un solo test, try-with-resources local).
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- **Aislar una violación de regla:** cuando un método `validateInsert`/`validateUpdate`/`validateRemove`/`validateReenviar` aplica varias reglas en secuencia, cada test de rama de error construye una entidad **válida en todos los campos salvo el que se quiere probar**, de forma que el resultado es el mismo tanto si el servicio acumula todos los mensajes como si corta en el primero — no hace falta asumir cuál de los dos hace la implementación real.
- **Mensaje de una validación fallida:** se comprueba con `resultado.get().get(0).getMessage()` (`Optional<BusinessMessages>`, y `BusinessMessages extends ArrayList<BusinessMessage>`); cuando la validación se ejerce a través de `insert`/`update`/`remove`/`reenviar` (que llaman a `BusinessMessages::throwIfInvalid`), se comprueba con `assertThrows(jakarta.validation.ValidationException.class, ...).getMessage()` (el mensaje de la excepción es exactamente el texto de la regla, ya que `BusinessMessage.toString()` sin `fieldName`/`label` devuelve solo el mensaje).
- **Campos `@Inject` sin setter ni constructor** (`CorreoServiceImpl.mailSender`, `CorreoServiceImpl.correoAsyncExecutor`): se rellenan por reflexión (`Field.setAccessible(true)`) tras instanciar la clase con `new`, ya que estas clases no tienen constructor ni setter para esas dependencias (no hay contenedor Guice en los tests). **Nota:** esta es una técnica nueva para este diseño, no una convención ya establecida en el proyecto — no existe en todo `src/test` ningún uso previo de `setAccessible(true)`.
- **`SecurityUtil`** (`com.educaflow.base.util.SecurityUtil`, clase real del proyecto): `isAdmin(com.axelor.auth.db.User)` y `getUser()` (delegan en `com.axelor.auth.AuthUtils`) se mockean con `Mockito.mockStatic(SecurityUtil.class)`. Para "el centro pertenece al usuario", se instancia un `com.axelor.auth.db.User` real con `setCentroUsuarios(List.of(centroUsuario))`, cada `com.educaflow.subsystem.common.db.CentroUsuario` con `setCentro(...)`; para "el usuario es Supervisor de ese centro" se añade además `centroUsuario.setCentroUsuarioTipoUsuario(List.of(cut))` con `cut.setTipoUsuario(tipoUsuario)` y `tipoUsuario.setCodigo("SUPERVISOR")` — todo son entidades de dominio, se instancian directamente con `new` y setters, no se mockean.
- **`DniUtil.isValid(String)`** y **`EMailUtil.isValid(String)`** (`com.educaflow.base.util`, utilidades puras y deterministas): **no se mockean**; se usan valores reales conocidos (un DNI/NIE y una dirección de correo concretos, válidos o inválidos) para ejercer la rama deseada.
- **`I18n.get(...)`**: se mockea con `Mockito.mockStatic(I18n.class, withSettings().strictness(Strictness.LENIENT))` devolviendo el propio argumento (`invocation -> invocation.getArgument(0)`), igual que el resto de tests del proyecto.
- **Repositorios**: se mockean con `Mockito.mock(...)`; el mock de `com.educaflow.subsystem.correos.db.repo.CorreoRepository` (subtipo autogenerado de `Repository<Correo>`) se pasa directamente al constructor de `CorreoServiceImpl` (acepta `Repository<Correo>`), y se referencia como `CorreoRepository` en los tests que necesitan `findByEstado(...)`.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl`  —  servicio

**Responsabilidad:** alta/consulta/reenvío de `Correo`, validaciones de negocio, asignación de campos servidor y orquestación del envío síncrono/asíncrono.
**Colaboradores a mockear:** `com.educaflow.subsystem.correos.db.repo.CorreoRepository` (repositorio, vía el parámetro `Repository<Correo>` del constructor); `com.educaflow.base.infrastructure.mail.MailSender` (campo `@Inject`); `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor` (campo `@Inject`); estáticos `com.educaflow.base.util.SecurityUtil`, `com.axelor.db.JpaRepository` (para `HistorialEstado`), `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner`, `com.axelor.db.JPA` (`runInTransaction`), `com.educaflow.base.util.MetaFileUtil`, `com.axelor.i18n.I18n`.
**Origen diseño:** `design.md` Paso 3 (servicios), `design/rules/R-Correo-001.md`, `design/rules/R-Correo-002.md`.

### Método: `Correo insert(Correo correo)`

- **`insert_correoValido_asignaValoresInicialesYPersiste`** — Tipo: happy. Verifica: R-Correo-003.
  - **Arrange:** `Correo` con todos los campos obligatorios rellenos válidos (`dniDestinatario` un DNI real válido, `nombre`, `apellidos`, `para="a@x.com"`, `asunto`, `cuerpo`, `centro` no nulo, `historialEstado = null` para no requerir `JpaRepository`), `estado`/`fechaCreacion`/`numeroReintentos` sin fijar (simulan lo que llegaría del cliente); mock `SecurityUtil.isAdmin(...)` → `true` (evita necesitar `centroUsuarios`); mock `repository.save(any())` → devuelve el mismo objeto recibido; mock estático `PostCommitRunner.runAfterCommit(any())` (no ejecuta el runnable en este test, solo se comprueba que se invoca — ver test siguiente).
  - **Act:** `service.insert(correo)`.
  - **Assert:** el objeto devuelto tiene `estado == PENDIENTE`, `fechaCreacion != null`, `numeroReintentos == 0`, `fechaPrimerIntentoEnvio == null`, `fechaUltimoIntentoEnvio == null`, `fechaEnvio == null`, `descripcionUltimoFallo == null`; se invoca `repository.save(correo)`; se invoca `PostCommitRunner.runAfterCommit(any())` (mock estático, `verify`).
- **`insert_clienteEnviaEstadoOFechasFalsas_seSobrescribenIncondicionalmente`** — Tipo: borde. Verifica: R-Correo-003 (defensa de mass-assignment, k-secure-coding §3.3).
  - **Arrange:** igual que el anterior pero el `Correo` de entrada ya trae `estado = SUCCESS`, `fechaEnvio = <una fecha pasada>`, `numeroReintentos = 99` (simulando lo que un cliente malicioso podría colar vía `AllowProperties`/Vía B si estos campos estuvieran mal filtrados).
  - **Act:** `service.insert(correo)`.
  - **Assert:** el resultado tiene `estado == PENDIENTE` (no `SUCCESS`), `numeroReintentos == 0` (no 99), `fechaEnvio == null` — la asignación de `fireActionRule_AsignarValoresIniciales` no depende de ninguna guarda `if`, siempre sobrescribe.
- **`insert_correoInvalido_lanzaValidationExceptionYNoPersiste`** — Tipo: error. Verifica: V-Correo-001 (representativo; cualquier `V-Correo-0xx` de `validateInsert` produce el mismo efecto).
  - **Arrange:** `Correo` con `dniDestinatario = null` (resto de campos válidos).
  - **Act:** `assertThrows(ValidationException.class, () -> service.insert(correo))`.
  - **Assert:** el mensaje de la excepción es `"El DNI del destinatario es obligatorio"`; `verify(repository, never()).save(any())`; `verify(correoAsyncExecutor, never()).submit(any())` (el mock estático de `PostCommitRunner` nunca se invoca — no hace falta asignar el mock si no se llega a usar, o se deja `lenient()`).

### Método: `Correo update(Correo nuevo, Correo original)`

- **`update_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: V-Correo-015 (patrón gemelo: `update` aplica la misma regla de inmutabilidad de forma incondicional, sin pasar por `validateUpdate`).
  - **Arrange:** dos `Correo` cualesquiera (`nuevo`/`original`), sin necesidad de que difieran.
  - **Act:** `assertThrows(UnsupportedOperationException.class, () -> service.update(nuevo, original))`.
  - **Assert:** el mensaje es `"El correo es inmutable tras su creación."`.

### Método: `void remove(Correo correo)`

- **`remove_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: V-Correo-016 / RES-Correo-003 (patrón gemelo).
  - **Arrange:** un `Correo` cualquiera.
  - **Act:** `assertThrows(UnsupportedOperationException.class, () -> service.remove(correo))`.
  - **Assert:** el mensaje es `"Los correos no se pueden borrar."`; `verify(repository, never()).remove(any())`.

### Método: `Correo reenviar(Correo entidad, Correo entidadOriginal)`

- **`reenviar_correoEnFail_programaEnvioAsincronoSinPersistirCambiosPropios`** — Tipo: happy. Verifica: R-Correo-002.
  - **Arrange:** `entidadOriginal` con `estado = FAIL`, `centro` = el centro del usuario actual; mock `SecurityUtil.isAdmin(...)` → `true`; mock estático `PostCommitRunner.runAfterCommit(any())`.
  - **Act:** `Correo resultado = service.reenviar(entidad, entidadOriginal)`.
  - **Assert:** `resultado == entidadOriginal` (mismo objeto, sin mutar); `verify(repository, never()).save(any())` (nota de diseño: `reenviar` no persiste cambios propios); se invoca `PostCommitRunner.runAfterCommit(any())`.
- **`reenviar_correoNoEnFail_lanzaValidationExceptionYNoProgramaEnvio`** — Tipo: error. Verifica: V-Correo-017.
  - **Arrange:** `entidadOriginal` con `estado = PENDIENTE` (o `SUCCESS`).
  - **Act:** `assertThrows(ValidationException.class, () -> service.reenviar(entidad, entidadOriginal))`.
  - **Assert:** mensaje `"Solo se pueden reenviar correos que han fallado"`; `PostCommitRunner.runAfterCommit` nunca invocado.
- **`reenviar_usuarioNoAdminDeOtroCentro_lanzaValidationException`** — Tipo: error. Verifica: V-Correo-018.
  - **Arrange:** `entidadOriginal` con `estado = FAIL` y `centro = centroB`; mock `SecurityUtil.isAdmin(...)` → `false`; mock `SecurityUtil.getUser()` → un `User` cuyo único `CentroUsuario` referencia `centroA` (≠ `centroB`).
  - **Act:** `assertThrows(ValidationException.class, () -> service.reenviar(entidad, entidadOriginal))`.
  - **Assert:** mensaje `"No puede reenviar correos de un centro que no es suyo"`.

### Método: `void enviarCorreo(Long correoId)`

- **`enviarCorreo_envioExitoso_marcaSuccessYRegistraFechaEnvio`** — Tipo: happy. Verifica: R-Correo-001/R-Correo-004, RES-Correo-002.
  - **Arrange:** mock estático `JPA.runInTransaction(Runnable)` para que ejecute inmediatamente el runnable recibido (`invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; }`); mock `repository.find(correoId)` → un `Correo` en `PENDIENTE`, con `para="a@x.com"`, `enCopia=null`, `enCopiaOculta=null`, `adjuntos=List.of()`, `fechaPrimerIntentoEnvio=null`, `numeroReintentos=0`; mock `mailSender.send(any())` sin lanzar excepción.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** tras la llamada, el `Correo` mockeado/capturado tiene `estado == SUCCESS`, `fechaEnvio != null`, `descripcionUltimoFallo == null`; `verify(repository).save(correo)`.
- **`enviarCorreo_envioFalla_marcaFailYGuardaTrazaCompleta`** — Tipo: error. Verifica: R-Correo-001/R-Correo-004, RES-Correo-002.
  - **Arrange:** igual que el anterior, pero `mailSender.send(any())` lanza `new RuntimeException("SMTP caído")`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** el correo queda `estado == FAIL`, `fechaEnvio == null` (RES-Correo-002), `descripcionUltimoFallo` contiene el texto `"SMTP caído"` (la traza de la excepción); `verify(repository).save(correo)`; el método **no** propaga la excepción (no hay `assertThrows`).
- **`enviarCorreo_envioExitoso_sobrescribeIncondicionalmenteDescripcionDeFalloPrevia`** — Tipo: borde. Verifica: R-Correo-004 (CC-Correo-004/CC-Correo-006; defensa de asignación incondicional de campos servidor, k-secure-coding §3.3 — `fireActionRule_MarcarEnvioCorrecto` MUST NOT llevar guarda condicional).
  - **Arrange:** `Correo` procedente de un intento anterior fallido: `estado = FAIL`, `descripcionUltimoFallo = "fallo anterior"` ya fijado; en este intento `mailSender.send(any())` no lanza.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** el correo queda `estado == SUCCESS`, `descripcionUltimoFallo == null` (se sobrescribe sin comprobar su valor anterior) y `fechaEnvio != null`.
- **`enviarCorreo_envioFalla_sobrescribeIncondicionalmenteFechaEnvioPrevia`** — Tipo: borde. Verifica: R-Correo-004 (RES-Correo-002; defensa de asignación incondicional de campos servidor, k-secure-coding §3.3 — `fireActionRule_MarcarEnvioFallido` MUST NOT llevar guarda condicional).
  - **Arrange:** `Correo` con `fechaEnvio` ya fijada a una fecha anterior (dato anómalo/heredado, para comprobar que la asignación no depende de ninguna comprobación previa); `mailSender.send(any())` lanza `new RuntimeException("SMTP caído")`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** el correo queda `estado == FAIL` y `fechaEnvio == null` (se sobrescribe incondicionalmente aunque ya tuviera valor — RES-Correo-002 garantizado por construcción, no por un `if`).
- **`enviarCorreo_correoYaEnSuccess_noHaceNadaIdempotente`** — Tipo: borde. Verifica: R-Correo-001 (idempotencia, "Notas de esta regla").
  - **Arrange:** mock `repository.find(correoId)` → un `Correo` con `estado == SUCCESS` ya fijado.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** `verify(mailSender, never()).send(any())`; `verify(repository, never()).save(any())`.
- **`enviarCorreo_correoIdInexistente_noHaceNada`** — Tipo: borde. Verifica: R-Correo-001 (defensivo).
  - **Arrange:** mock `repository.find(correoId)` → `null`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** `verify(mailSender, never()).send(any())`; no se lanza ninguna excepción.
- **`enviarCorreo_primerIntento_fijaFechaPrimerIntentoEnvio`** — Tipo: borde. Verifica: R-Correo-004 (CC-Correo-002).
  - **Arrange:** `Correo` con `fechaPrimerIntentoEnvio == null`, `numeroReintentos == 0`; `mailSender.send` no lanza.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** tras la llamada, `fechaPrimerIntentoEnvio != null`; `numeroReintentos == 1`; `fechaUltimoIntentoEnvio != null`.
- **`enviarCorreo_reintento_noSobrescribeFechaPrimerIntentoEnvio`** — Tipo: borde. Verifica: R-Correo-004 (CC-Correo-002, "se fija una sola vez").
  - **Arrange:** `Correo` con `fechaPrimerIntentoEnvio` ya fijado a una fecha `T0` conocida, `estado == FAIL`, `numeroReintentos == 1`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** `fechaPrimerIntentoEnvio` sigue siendo exactamente `T0` (no cambia); `numeroReintentos == 2` (se incrementa); `fechaUltimoIntentoEnvio` sí se actualiza a un instante posterior a `T0`.
- **`enviarCorreo_separaDireccionesDeParaCcYBccPorComas`** — Tipo: happy. Verifica: `—` (soporte de `construirMail`/`separarDirecciones`, base de V-Correo-005/006/007/008).
  - **Arrange:** `Correo` con `para = "a@x.com, b@x.com"`, `enCopia = "c@x.com"`, `enCopiaOculta = " d@x.com , e@x.com "` (espacios extra), `adjuntos = List.of()`; `mailSender.send` no lanza; capturar el argumento con `ArgumentCaptor<Mail>`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** el `Mail` capturado tiene `to = ["a@x.com", "b@x.com"]`, `cc = ["c@x.com"]`, `bcc = ["d@x.com", "e@x.com"]` (recortado, sin vacíos), `from` igual a la propiedad `correos.envio.from`, `subject` == `correo.getAsunto()`, `htmlBody`/`textBody` == `correo.getCuerpo()`.
- **`enviarCorreo_correoConAdjuntos_construyeAttachsDesdeMetaFile`** — Tipo: happy. Verifica: `—` (soporte de `construirMail`).
  - **Arrange:** `Correo` con un `Adjunto` (`nombreFichero="doc.pdf"`, `contenido` un `MetaFile` mock con `getFileType()` → `"application/pdf"`); mock estático `MetaFileUtil.downloadContent(metaFile)` → un `byte[]` conocido; `mailSender.send` no lanza.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** el `Mail` capturado tiene `attachs()` con un único `Attach` cuyo `fileName() == "doc.pdf"`, `data()` == el `byte[]` devuelto por el mock, `mimeType() == "application/pdf"`.

### Método: `List<Correo> listarCorreosEnFail()`

- **`listarCorreosEnFail_delegaEnFinderDelRepositorio`** — Tipo: happy. Verifica: `—` (design-guidelines: reenvío en bloque futuro).
  - **Arrange:** mock `((CorreoRepository) repository).findByEstado(EstadoCorreo.FAIL)` → una lista de dos `Correo`.
  - **Act:** `service.listarCorreosEnFail()`.
  - **Assert:** el resultado es exactamente esa lista (`assertEquals`/`assertSame` según corresponda); `verify(repository).findByEstado(EstadoCorreo.FAIL)`.

### Método: `Optional<BusinessMessages> validateInsert(Correo correo)`

Cada test siguiente construye un `Correo` **válido salvo en el campo que se prueba** (ver Convenciones).

- **`validateInsert_todoValido_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Correo-001 a V-Correo-014 (todas superadas).
  - **Arrange:** `Correo` con todos los campos válidos (DNI real válido, nombre/apellidos, `para` con una dirección válida, `asunto` de 10 caracteres, `cuerpo` no vacío, `centro` no nulo, `historialEstado = null`); `SecurityUtil.isAdmin(...)` → `true`.
  - **Act:** `service.validateInsert(correo)`.
  - **Assert:** `Optional.isEmpty()`.
- **`validateInsert_dniDestinatarioNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Correo-001.
  - **Arrange:** `dniDestinatario = null` (resto válido).
  - **Act:** `service.validateInsert(correo)`.
  - **Assert:** mensaje `"El DNI del destinatario es obligatorio"`.
- **`validateInsert_dniDestinatarioConLetraDeControlIncorrecta_devuelveMensajeNoValido`** — Tipo: error. Verifica: V-Correo-002.
  - **Arrange:** `dniDestinatario` con un DNI real pero con la letra de control cambiada (p.ej. `"12345678A"` si la letra correcta fuera otra — usar un valor conocido inválido de `DniUtil`).
  - **Act:** `service.validateInsert(correo)`.
  - **Assert:** mensaje `"El DNI del destinatario no es válido; compruebe la letra"`.
- **`validateInsert_nombreNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Correo-003.
  - **Arrange:** `nombre = null`.
  - **Act/Assert:** mensaje `"El nombre es obligatorio"`.
- **`validateInsert_apellidosNulos_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Correo-004.
  - **Arrange:** `apellidos = null`.
  - **Act/Assert:** mensaje `"Los apellidos son obligatorios"`.
- **`validateInsert_paraVacioTrasSepararPorComas_devuelveMensajeAlMenosUnDestinatario`** — Tipo: error. Verifica: V-Correo-005.
  - **Arrange:** `para = ""` y, como variante borde, `para = " , , "` (solo comas/espacios, ninguna dirección real tras `separarDirecciones`).
  - **Act/Assert:** mensaje `"Debe indicar al menos un destinatario en el «para»"` en ambas variantes.
- **`validateInsert_paraConDireccionInvalida_devuelveMensajeFormatoInvalido`** — Tipo: error. Verifica: V-Correo-006.
  - **Arrange:** `para = "valida@x.com, no-es-un-email"`.
  - **Act/Assert:** mensaje `"El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)"`.
- **`validateInsert_enCopiaConDireccionInvalida_devuelveMensajeFormatoInvalido`** — Tipo: error. Verifica: V-Correo-007.
  - **Arrange:** `enCopia = "no-es-un-email"` (`para` válido).
  - **Act/Assert:** mensaje `"El «en copia» debe contener direcciones de correo válidas"`.
- **`validateInsert_enCopiaVacia_noSeValida`** — Tipo: borde. Verifica: V-Correo-007 (condición "cuando tiene valor").
  - **Arrange:** `enCopia = null` (resto válido).
  - **Act/Assert:** `Optional.isEmpty()` (no se aplica V-Correo-007 si no hay valor).
- **`validateInsert_enCopiaOcultaConDireccionInvalida_devuelveMensajeFormatoInvalido`** — Tipo: error. Verifica: V-Correo-008.
  - **Arrange:** `enCopiaOculta = "no-es-un-email"`.
  - **Act/Assert:** mensaje `"El «en copia oculta» debe contener direcciones de correo válidas"`.
- **`validateInsert_asuntoNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Correo-009.
  - **Arrange:** `asunto = null`.
  - **Act/Assert:** mensaje `"El asunto es obligatorio"`.
- **`validateInsert_asuntoSuperaDoscientosCincuentaYCincoCaracteres_devuelveMensajeLongitud`** — Tipo: error. Verifica: V-Correo-010.
  - **Arrange:** `asunto` de 256 caracteres.
  - **Act/Assert:** mensaje `"El asunto no puede superar 255 caracteres"`.
- **`validateInsert_asuntoDeDoscientosCincuentaYCincoCaracteres_esValido`** — Tipo: borde. Verifica: V-Correo-010 (límite exacto permitido).
  - **Arrange:** `asunto` de exactamente 255 caracteres (resto válido).
  - **Act/Assert:** `Optional.isEmpty()`.
- **`validateInsert_cuerpoNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Correo-011.
  - **Arrange:** `cuerpo = null`.
  - **Act/Assert:** mensaje `"El cuerpo es obligatorio"`.
- **`validateInsert_centroNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Correo-012.
  - **Arrange:** `centro = null`; `SecurityUtil.isAdmin(...)` → `true` (para no interferir con V-Correo-013).
  - **Act/Assert:** mensaje `"El centro es obligatorio"`.
- **`validateInsert_usuarioNoAdminConCentroDeOtroCentro_devuelveMensajeCentroNoSuyo`** — Tipo: error. Verifica: V-Correo-013.
  - **Arrange:** `centro = centroB`; `SecurityUtil.isAdmin(...)` → `false`; `SecurityUtil.getUser()` → `User` con `centroUsuarios = List.of(cu(centroA))` (`centroA != centroB`).
  - **Act/Assert:** mensaje `"No puede crear correos para un centro que no es suyo"`.
- **`validateInsert_usuarioNoAdminConCentroPropio_esValido`** — Tipo: happy/borde. Verifica: V-Correo-013 (rama OK).
  - **Arrange:** `centro = centroA`; `SecurityUtil.isAdmin(...)` → `false`; `SecurityUtil.getUser()` → `User` con `centroUsuarios` incluyendo `cu(centroA)`.
  - **Act/Assert:** `Optional.isEmpty()`.
- **`validateInsert_administradorConCualquierCentro_esValido`** — Tipo: borde. Verifica: V-Correo-013 (rama Administrador, sin restricción).
  - **Arrange:** `centro = centroB` (no del usuario); `SecurityUtil.isAdmin(...)` → `true`; `SecurityUtil.getUser()` no se llega a invocar para esta comprobación (o se deja `lenient()`).
  - **Act/Assert:** `Optional.isEmpty()`.
- **`validateInsert_historialEstadoIndicadoNoExiste_devuelveMensajeNoExiste`** — Tipo: error. Verifica: V-Correo-014.
  - **Arrange:** `historialEstado` con un `id` cualquiera; mock estático `JpaRepository.of(HistorialEstado.class)` → repo mock; `repoMock.find(id)` → `null`.
  - **Act/Assert:** mensaje `"El historial de estado indicado no existe"`.
- **`validateInsert_historialEstadoIndicadoExiste_esValido`** — Tipo: borde. Verifica: V-Correo-014 (rama OK).
  - **Arrange:** igual que el anterior pero `repoMock.find(id)` → una `HistorialEstado` no nula.
  - **Act/Assert:** `Optional.isEmpty()`.
- **`validateInsert_historialEstadoNoIndicado_noSeValida`** — Tipo: borde. Verifica: V-Correo-014 (condición "cuando tiene valor").
  - **Arrange:** `historialEstado = null`.
  - **Act/Assert:** `Optional.isEmpty()`; `JpaRepository.of(...)` nunca se invoca para `HistorialEstado` (`verify` sobre el mock estático, o simplemente no se stubea y se deja en modo estricto para que falle si se invocara).

### Método: `Optional<BusinessMessages> validateUpdate(Correo nuevo, Correo original)`

- **`validateUpdate_siempre_devuelveMensajeInmutabilidad`** — Tipo: error. Verifica: V-Correo-015.
  - **Arrange:** dos `Correo` cualesquiera.
  - **Act:** `service.validateUpdate(nuevo, original)`.
  - **Assert:** `Optional` presente con mensaje `"El correo es inmutable tras su creación."`.

### Método: `Optional<BusinessMessages> validateRemove(Correo correo)`

- **`validateRemove_siempre_devuelveMensajeNoSePuedeBorrar`** — Tipo: error. Verifica: V-Correo-016.
  - **Arrange:** un `Correo` cualquiera.
  - **Act:** `service.validateRemove(correo)`.
  - **Assert:** `Optional` presente con mensaje `"Los correos no se pueden borrar."`.

### Método: `Optional<BusinessMessages> validateReenviar(Correo entidad, Correo entidadOriginal)`

- **`validateReenviar_estadoFail_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Correo-017, V-Correo-018.
  - **Arrange:** `entidadOriginal.estado = FAIL`, `centro = centroA`; `SecurityUtil.isAdmin(...)` → `true`.
  - **Act/Assert:** `Optional.isEmpty()`.
- **`validateReenviar_estadoDistintoDeFail_devuelveMensajeSoloFallidos`** — Tipo: error. Verifica: V-Correo-017.
  - **Arrange:** `entidadOriginal.estado = PENDIENTE` (o `SUCCESS`).
  - **Act/Assert:** mensaje `"Solo se pueden reenviar correos que han fallado"`.
- **`validateReenviar_usuarioNoAdminDeOtroCentro_devuelveMensajeCentroNoSuyo`** — Tipo: error. Verifica: V-Correo-018.
  - **Arrange:** `entidadOriginal.estado = FAIL`, `centro = centroB`; `SecurityUtil.isAdmin(...)` → `false`; `SecurityUtil.getUser()` → `User` con `centroUsuarios` solo de `centroA`.
  - **Act/Assert:** mensaje `"No puede reenviar correos de un centro que no es suyo"`.

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteCamposDeClienteYDeniegaCamposServidor`** — Tipo: happy. Verifica: `—` (frontera de confianza, tabla del `design.md`).
  - **Arrange:** ninguno (método sin dependencias).
  - **Act:** `AllowProperties result = service.allowPropertiesInsert()`.
  - **Assert:** `result.allowProperty(...)` es `true` para `dniDestinatario`, `nombre`, `apellidos`, `para`, `enCopia`, `enCopiaOculta`, `asunto`, `cuerpo`, `centro`, `historialEstado`, `adjuntos`; es `false` para `estado`, `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo`, `nombreExpediente`.

### Método: `AllowProperties allowPropertiesReenviar()`

- **`allowPropertiesReenviar_devuelveWhitelistVacia`** — Tipo: happy. Verifica: `—` (frontera de confianza — acción `Reenviar` sin `Input AllowProperties`).
  - **Arrange:** ninguno.
  - **Act:** `AllowProperties result = service.allowPropertiesReenviar()`.
  - **Assert:** `result.allowProperty("estado")` y `result.allowProperty("centro")` (o cualquier otro nombre de campo) son `false`.

**Ninguna otra clase se testea en esta tarea.** Los tests de `AdjuntoServiceImpl` van en la Tarea 20.
