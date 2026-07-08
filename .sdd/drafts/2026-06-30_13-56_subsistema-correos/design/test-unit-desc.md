# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño del subsistema `correos`. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`). Estáticos del stack con `Mockito.mockStatic` (patrón ya usado en el proyecto para `I18n`: campo `MockedStatic<...>` + `@BeforeEach`/`@AfterEach`, con `Strictness.LENIENT` cuando no todos los tests de la clase recorren la rama que lo consume; para mocks estáticos puntuales de un solo test, try-with-resources local).
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- **Aislar una violación de regla:** cuando un método `validateInsert`/`validateUpdate`/`validateRemove`/`validateReenviar` aplica varias reglas en secuencia, cada test de rama de error construye una entidad **válida en todos los campos salvo el que se quiere probar**, de forma que el resultado es el mismo tanto si el servicio acumula todos los mensajes como si corta en el primero — no hace falta asumir cuál de los dos hace la implementación real.
- **Mensaje de una validación fallida:** se comprueba con `resultado.get().get(0).getMessage()` (`Optional<BusinessMessages>`, y `BusinessMessages extends ArrayList<BusinessMessage>`); cuando la validación se ejerce a través de `insert`/`update`/`remove`/`reenviar` (que llaman a `BusinessMessages::throwIfInvalid`), se comprueba con `assertThrows(jakarta.validation.ValidationException.class, ...).getMessage()` (el mensaje de la excepción es exactamente el texto de la regla, ya que `BusinessMessage.toString()` sin `fieldName`/`label` devuelve solo el mensaje).
- **Campos `@Inject` sin setter ni constructor** (`CorreoServiceImpl.mailSender`, `CorreoServiceImpl.correoAsyncExecutor`, `CorreoEventObserver.correoAsyncExecutor`): se rellenan por reflexión (`Field.setAccessible(true)`) tras instanciar la clase con `new`, ya que estas clases no tienen constructor ni setter para esas dependencias (no hay contenedor Guice en los tests). **Nota:** esta es una técnica nueva para este diseño, no una convención ya establecida en el proyecto — no existe en todo `src/test` ningún uso previo de `setAccessible(true)`. Se introduce aquí porque, a diferencia de dependencias que sí tienen un parámetro de constructor (p. ej. el repositorio), `mailSender`/`correoAsyncExecutor` solo se declaran como campos `@Inject` sin constructor ni setter.
- **`SecurityUtil`** (`com.educaflow.base.util.SecurityUtil`, clase real del proyecto): `isAdmin(com.axelor.auth.db.User)` y `getUser()` (delegan en `com.axelor.auth.AuthUtils`) se mockean con `Mockito.mockStatic(SecurityUtil.class)`. Para "el centro pertenece al usuario", se instancia un `com.axelor.auth.db.User` real con `setCentroUsuarios(List.of(centroUsuario))`, cada `com.educaflow.subsystem.common.db.CentroUsuario` con `setCentro(...)`; para "el usuario es Supervisor de ese centro" se añade además `centroUsuario.setCentroUsuarioTipoUsuario(List.of(cut))` con `cut.setTipoUsuario(tipoUsuario)` y `tipoUsuario.setCodigo("SUPERVISOR")` — todo son entidades de dominio, se instancian directamente con `new` y setters, no se mockean.
- **`DniUtil.isValid(String)`** y **`EMailUtil.isValid(String)`** (`com.educaflow.base.util`, utilidades puras y deterministas): **no se mockean**; se usan valores reales conocidos (un DNI/NIE y una dirección de correo concretos, válidos o inválidos) para ejercer la rama deseada.
- **`I18n.get(...)`**: se mockea con `Mockito.mockStatic(I18n.class, withSettings().strictness(Strictness.LENIENT))` devolviendo el propio argumento (`invocation -> invocation.getArgument(0)`), igual que el resto de tests del proyecto.
- **Repositorios**: se mockean con `Mockito.mock(...)`; el mock de `com.educaflow.subsystem.correos.db.repo.CorreoRepository` (subtipo autogenerado de `Repository<Correo>`) se pasa directamente al constructor de `CorreoServiceImpl` (acepta `Repository<Correo>`), y se referencia como `CorreoRepository` en los tests que necesitan `findByEstado(...)`.
- **`AppSettings`** (`com.axelor.app.AppSettings`, estático): `construirMail` lee `mail.address.from` mediante `AppSettings.get().get("mail.address.from")`. En los tests de `enviarCorreo` que llegan a `construirMail` (todos salvo `enviarCorreo_correoYaEnSuccess_noHaceNadaIdempotente` y `enviarCorreo_correoIdInexistente_noHaceNada`, que retornan antes), se mockea con `Mockito.mockStatic(AppSettings.class)` + `AppSettings settingsMock = Mockito.mock(AppSettings.class)`; `AppSettings.get()` → `settingsMock`; `settingsMock.get("mail.address.from")` → `"noreply@educaflow.test"` (mismo valor en todos, salvo que un test indique otro explícitamente).

---

## Clase: `com.educaflow.base.infrastructure.mail.Mail`  —  helper (record de datos)

**Responsabilidad:** transportar los datos de un correo a enviar (destinatarios `to`/`cc`/`bcc`, remitente, asunto, cuerpo y adjuntos) hacia `MailSender`.
**Colaboradores a mockear:** ninguno (record puro, sin dependencias).
**Origen diseño:** `design.md` Paso 1 — constructor de compatibilidad de 6 argumentos que delega en el canónico de 8.

### Método: `Mail(List<String> to, String from, String subject, String htmlBody, String textBody, List<Attach> attachs)` (constructor de compatibilidad)

- **`constructorCompatibilidad_seisArgumentos_delegaConCcYBccVacios`** — Tipo: happy. Verifica: `—` (soporte técnico de R-Correo-001, sin regla propia).
  - **Arrange:** una lista `to` con una dirección, `from`/`subject`/`htmlBody`/`textBody` con texto, una lista `attachs` con un `Attach`.
  - **Act:** invocar el constructor de 6 argumentos.
  - **Assert:** `mail.cc()` es una lista vacía; `mail.bcc()` es una lista vacía; `mail.to()`/`mail.from()`/`mail.subject()`/`mail.htmlBody()`/`mail.textBody()`/`mail.attachs()` son exactamente los valores pasados (delegación sin pérdida al constructor canónico de 8 argumentos).

---

## Clase: `com.educaflow.base.infrastructure.mail.impl.JavaMailHelper`  —  helper

**Responsabilidad:** construir el `jakarta.mail.Message` a partir de un `Mail`, incluyendo (tras esta ampliación) las cabeceras `CC`/`BCC` cuando `Mail.cc()`/`Mail.bcc()` no vienen vacías.
**Colaboradores a mockear:** ninguno — se usa una `jakarta.mail.Session` real (`Session.getDefaultInstance(new Properties())`, sin conexión SMTP: `getMessage(...)` solo ensambla el objeto en memoria).
**Origen diseño:** `design.md` Paso 1 y `design/rules/R-Correo-001.md` "Notas de esta regla".

### Método: `static Message getMessage(Mail mail, Session session)`

- **`getMessage_conCcNoVacio_estableceCabeceraCc`** — Tipo: happy. Verifica: `—` (soporte técnico de R-Correo-001).
  - **Arrange:** `Mail` con `cc = List.of("copia@x.com")`, `to`/`from`/`subject` rellenos, `bcc = List.of()`, `attachs = List.of()`; sesión real sin autenticador.
  - **Act:** `JavaMailHelper.getMessage(mail, session)`.
  - **Assert:** `message.getRecipients(Message.RecipientType.CC)` contiene exactamente `copia@x.com` (una `InternetAddress`).
- **`getMessage_conBccNoVacio_estableceCabeceraBcc`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `Mail` con `bcc = List.of("oculto@x.com")`, `cc = List.of()`, resto relleno.
  - **Act:** `JavaMailHelper.getMessage(mail, session)`.
  - **Assert:** `message.getRecipients(Message.RecipientType.BCC)` contiene exactamente `oculto@x.com`.
- **`getMessage_conCcYBccVacios_noEstableceEsasCabeceras`** — Tipo: borde. Verifica: `—` (regresión: no romper el envío existente sin CC/BCC, único caso usado hoy por `RegistroSalidaServiceImpl`).
  - **Arrange:** `Mail` con `cc = List.of()` y `bcc = List.of()` (o `null` en ambos, dos variantes del mismo test).
  - **Act:** `JavaMailHelper.getMessage(mail, session)`.
  - **Assert:** `message.getRecipients(Message.RecipientType.CC)` es `null`; `message.getRecipients(Message.RecipientType.BCC)` es `null` (comportamiento nativo de `jakarta.mail` cuando no se han fijado destinatarios de ese tipo); `message.getRecipients(Message.RecipientType.TO)` sigue conteniendo los de `to` (no afectado por el cambio).

---

## Clase: `com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl`  —  servicio

**Responsabilidad:** alta/consulta/reenvío de `Correo`, validaciones de negocio, asignación de campos servidor y orquestación del envío síncrono/asíncrono.
**Colaboradores a mockear:** `com.educaflow.subsystem.correos.db.repo.CorreoRepository` (repositorio, vía el parámetro `Repository<Correo>` del constructor); `com.educaflow.base.infrastructure.mail.MailSender` (campo `@Inject`); `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor` (campo `@Inject`); estáticos `com.educaflow.base.util.SecurityUtil`, `com.axelor.db.JpaRepository` (para `HistorialEstado`), `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner`, `com.axelor.db.JPA` (`runInTransaction`), `com.educaflow.base.util.MetaFileUtil`, `com.axelor.i18n.I18n`, `com.axelor.app.AppSettings` (para `mail.address.from` en `construirMail`, ver Convenciones).
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
  - **Arrange:** mock estático `JPA.runInTransaction(Runnable)` para que ejecute inmediatamente el runnable recibido (`invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; }`); mock `repository.find(correoId)` → un `Correo` en `PENDIENTE`, con `para="a@x.com"`, `enCopia=null`, `enCopiaOculta=null`, `adjuntos=List.of()`, `fechaPrimerIntentoEnvio=null`, `numeroReintentos=0`; mock `mailSender.send(any())` sin lanzar excepción; mock estático `AppSettings` (ver Convenciones) → `mail.address.from` = `"noreply@educaflow.test"`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** tras la llamada, el `Correo` mockeado/capturado tiene `estado == SUCCESS`, `fechaEnvio != null`, `descripcionUltimoFallo == null`; `verify(repository).save(correo)`.
- **`enviarCorreo_envioFalla_marcaFailYGuardaTrazaCompleta`** — Tipo: error. Verifica: R-Correo-001/R-Correo-004, RES-Correo-002.
  - **Arrange:** igual que el anterior, pero `mailSender.send(any())` lanza `new RuntimeException("SMTP caído")`; mock estático `AppSettings` (ver Convenciones) → `mail.address.from` = `"noreply@educaflow.test"`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** el correo queda `estado == FAIL`, `fechaEnvio == null` (RES-Correo-002), `descripcionUltimoFallo` contiene el texto `"SMTP caído"` (la traza de la excepción); `verify(repository).save(correo)`; el método **no** propaga la excepción (no hay `assertThrows`).
- **`enviarCorreo_envioExitoso_sobrescribeIncondicionalmenteDescripcionDeFalloPrevia`** — Tipo: borde. Verifica: R-Correo-004 (CC-Correo-004/CC-Correo-006; defensa de asignación incondicional de campos servidor, k-secure-coding §3.3 — `fireActionRule_MarcarEnvioCorrecto` MUST NOT llevar guarda condicional).
  - **Arrange:** `Correo` procedente de un intento anterior fallido: `estado = FAIL`, `descripcionUltimoFallo = "fallo anterior"` ya fijado; en este intento `mailSender.send(any())` no lanza; mock estático `AppSettings` (ver Convenciones) → `mail.address.from` = `"noreply@educaflow.test"`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** el correo queda `estado == SUCCESS`, `descripcionUltimoFallo == null` (se sobrescribe sin comprobar su valor anterior) y `fechaEnvio != null`.
- **`enviarCorreo_envioFalla_sobrescribeIncondicionalmenteFechaEnvioPrevia`** — Tipo: borde. Verifica: R-Correo-004 (RES-Correo-002; defensa de asignación incondicional de campos servidor, k-secure-coding §3.3 — `fireActionRule_MarcarEnvioFallido` MUST NOT llevar guarda condicional).
  - **Arrange:** `Correo` con `fechaEnvio` ya fijada a una fecha anterior (dato anómalo/heredado, para comprobar que la asignación no depende de ninguna comprobación previa); `mailSender.send(any())` lanza `new RuntimeException("SMTP caído")`; mock estático `AppSettings` (ver Convenciones) → `mail.address.from` = `"noreply@educaflow.test"`.
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
  - **Arrange:** `Correo` con `fechaPrimerIntentoEnvio == null`, `numeroReintentos == 0`; `mailSender.send` no lanza; mock estático `AppSettings` (ver Convenciones) → `mail.address.from` = `"noreply@educaflow.test"`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** tras la llamada, `fechaPrimerIntentoEnvio != null`; `numeroReintentos == 1`; `fechaUltimoIntentoEnvio != null`.
- **`enviarCorreo_reintento_noSobrescribeFechaPrimerIntentoEnvio`** — Tipo: borde. Verifica: R-Correo-004 (CC-Correo-002, "se fija una sola vez").
  - **Arrange:** `Correo` con `fechaPrimerIntentoEnvio` ya fijado a una fecha `T0` conocida, `estado == FAIL`, `numeroReintentos == 1`; `mailSender.send` no lanza; mock estático `AppSettings` (ver Convenciones) → `mail.address.from` = `"noreply@educaflow.test"`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** `fechaPrimerIntentoEnvio` sigue siendo exactamente `T0` (no cambia); `numeroReintentos == 2` (se incrementa); `fechaUltimoIntentoEnvio` sí se actualiza a un instante posterior a `T0`.
- **`enviarCorreo_separaDireccionesDeParaCcYBccPorComas`** — Tipo: happy. Verifica: `—` (soporte de `construirMail`/`separarDirecciones`, base de V-Correo-005/006/007/008).
  - **Arrange:** `Correo` con `para = "a@x.com, b@x.com"`, `enCopia = "c@x.com"`, `enCopiaOculta = " d@x.com , e@x.com "` (espacios extra), `adjuntos = List.of()`; `mailSender.send` no lanza; capturar el argumento con `ArgumentCaptor<Mail>`; mock estático `AppSettings` (ver Convenciones) → `mail.address.from` = `"noreply@educaflow.test"`.
  - **Act:** `service.enviarCorreo(correoId)`.
  - **Assert:** el `Mail` capturado tiene `to = ["a@x.com", "b@x.com"]`, `cc = ["c@x.com"]`, `bcc = ["d@x.com", "e@x.com"]` (recortado, sin vacíos), `from == "noreply@educaflow.test"` (el valor stubado de `mail.address.from`), `subject` == `correo.getAsunto()`, `htmlBody`/`textBody` == `correo.getCuerpo()`.
- **`enviarCorreo_correoConAdjuntos_construyeAttachsDesdeMetaFile`** — Tipo: happy. Verifica: `—` (soporte de `construirMail`).
  - **Arrange:** `Correo` con un `Adjunto` (`nombreFichero="doc.pdf"`, `contenido` un `MetaFile` mock con `getFileType()` → `"application/pdf"`); mock estático `MetaFileUtil.downloadContent(metaFile)` → un `byte[]` conocido; `mailSender.send` no lanza; mock estático `AppSettings` (ver Convenciones) → `mail.address.from` = `"noreply@educaflow.test"`.
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
  - **Act/Assert:** mensaje `"El historial de estado indicado no existe"` (mensaje inferido por el `test-unitarios`: `design.md` no fija un texto literal para esta validación — ver Notas de este fichero al final).
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

---

## Clase: `com.educaflow.subsystem.correos.service.impl.AdjuntoServiceImpl`  —  servicio

**Responsabilidad:** validar el alta de `Adjunto` (pertenencia a un correo en creación, unicidad de nombre, permiso de centro) e impedir su modificación/borrado.
**Colaboradores a mockear:** `SecurityUtil` (estático), `I18n` (estático).
**Origen diseño:** `design.md` Paso 3 (servicios).

### Método: `Adjunto update(Adjunto nuevo, Adjunto original)`

- **`update_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: V-Adjunto-007 (patrón gemelo).
  - **Arrange:** dos `Adjunto` cualesquiera.
  - **Act:** `assertThrows(UnsupportedOperationException.class, () -> service.update(nuevo, original))`.
  - **Assert:** mensaje `"Los adjuntos son inmutables tras su creación."` (mensaje inferido por el `test-unitarios`: la spec/diseño no fijan un texto literal para esta acción — ver Notas de este fichero al final).

### Método: `void remove(Adjunto adjunto)`

- **`remove_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: V-Adjunto-008 (patrón gemelo, análogo a RES-Correo-003).
  - **Arrange:** un `Adjunto` cualquiera.
  - **Act:** `assertThrows(UnsupportedOperationException.class, () -> service.remove(adjunto))`.
  - **Assert:** mensaje `"Los adjuntos no se pueden borrar."` (mensaje inferido, ver Notas).

### Método: `Optional<BusinessMessages> validateInsert(Adjunto adjunto)`

Cada test construye un `Adjunto` **válido salvo en el campo probado** (ver Convenciones).

- **`validateInsert_todoValido_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Adjunto-001 a V-Adjunto-006 (todas superadas).
  - **Arrange:** `Adjunto` con `correo` no nulo (con `fechaCreacion = null`, `centro = centroA`, `adjuntos = List.of(esteAdjunto)`), `nombreFichero = "doc.pdf"`, `contenido` un `MetaFile` no nulo; `SecurityUtil.isAdmin(...)` → `true`.
  - **Act/Assert:** `Optional.isEmpty()`.
- **`validateInsert_correoNulo_devuelveMensajeDebePertenecerAUnCorreo`** — Tipo: error. Verifica: V-Adjunto-001.
  - **Arrange:** `correo = null`.
  - **Act/Assert:** mensaje `"El adjunto debe pertenecer a un correo"`.
- **`validateInsert_usuarioNoAdminDeOtroCentro_devuelveMensajeCentroNoSuyo`** — Tipo: error. Verifica: V-Adjunto-002.
  - **Arrange:** `correo.centro = centroB`; `SecurityUtil.isAdmin(...)` → `false`; `SecurityUtil.getUser()` → `User` con `centroUsuarios` solo de `centroA`.
  - **Act/Assert:** mensaje `"No puede añadir adjuntos a correos de un centro que no es suyo"`.
- **`validateInsert_correoYaExistente_devuelveMensajeNoSePuedenAnadirAUnoExistente`** — Tipo: error. Verifica: V-Adjunto-003.
  - **Arrange:** `correo.fechaCreacion = <una fecha ya fijada>` (simula un correo ya persistido de antes).
  - **Act/Assert:** mensaje `"No se pueden añadir adjuntos a un correo ya existente"`.
- **`validateInsert_correoEnCreacion_esValido`** — Tipo: borde. Verifica: V-Adjunto-003 (rama OK).
  - **Arrange:** `correo.fechaCreacion = null`.
  - **Act/Assert:** no se añade el mensaje de V-Adjunto-003 (puede seguir habiendo `Optional` vacío si el resto es válido).
- **`validateInsert_nombreFicheroNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Adjunto-004.
  - **Arrange:** `nombreFichero = null`.
  - **Act/Assert:** mensaje `"El nombre del fichero es obligatorio"`.
- **`validateInsert_contenidoNulo_devuelveMensajeDebeAdjuntarFichero`** — Tipo: error. Verifica: V-Adjunto-005.
  - **Arrange:** `contenido = null`.
  - **Act/Assert:** mensaje `"Debe adjuntar el fichero"`.
- **`validateInsert_nombreFicheroDuplicadoEntreHermanos_devuelveMensajeYaExiste`** — Tipo: error. Verifica: V-Adjunto-006.
  - **Arrange:** `correo.adjuntos = List.of(hermanoConMismoNombreFichero("doc.pdf"), esteAdjunto("doc.pdf"))`.
  - **Act/Assert:** mensaje `"ya existe un adjunto con ese nombre en el correo"`.
- **`validateInsert_nombreFicheroUnicoEntreHermanos_esValido`** — Tipo: borde. Verifica: V-Adjunto-006 (rama OK).
  - **Arrange:** `correo.adjuntos = List.of(hermano("otro.pdf"), esteAdjunto("doc.pdf"))`.
  - **Act/Assert:** no se añade el mensaje de V-Adjunto-006.

### Método: `Optional<BusinessMessages> validateUpdate(Adjunto nuevo, Adjunto original)`

- **`validateUpdate_siempre_devuelveMensajeInmutabilidad`** — Tipo: error. Verifica: V-Adjunto-007.
  - **Arrange:** dos `Adjunto` cualesquiera.
  - **Act/Assert:** `Optional` presente con mensaje `"Los adjuntos son inmutables tras su creación."` (mismo mensaje inferido que `update()`).

### Método: `Optional<BusinessMessages> validateRemove(Adjunto adjunto)`

- **`validateRemove_siempre_devuelveMensajeNoSePuedenBorrar`** — Tipo: error. Verifica: V-Adjunto-008.
  - **Arrange:** un `Adjunto` cualquiera.
  - **Act/Assert:** `Optional` presente con mensaje `"Los adjuntos no se pueden borrar."` (mismo mensaje inferido que `remove()`).

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteNombreFicheroContenidoYCorreo`** — Tipo: happy. Verifica: `—` (frontera de confianza).
  - **Arrange:** ninguno.
  - **Act:** `AllowProperties result = service.allowPropertiesInsert()`.
  - **Assert:** `result.allowProperty("nombreFichero")`, `result.allowProperty("contenido")` y `result.allowProperty("correo")` son `true` (Adjunto no tiene ningún campo servidor que deba estar en `false`).

---

## Clase: `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor`  —  helper (infraestructura)

**Responsabilidad:** envoltorio de un `ExecutorService` de tamaño fijo con hilos daemon nombrados, con ejecución de tareas aislada de fallos y parada ordenada.
**Colaboradores a mockear:** ninguno — se usa un `ExecutorService` real (tamaño de pool pequeño, p.ej. 1) para observar el comportamiento real de los hilos.
**Origen diseño:** `design.md` Paso 6, `design/rules/R-Correo-001.md` "Diseño detallado".

### Método: `CorreoAsyncExecutor(int tamanoPool)` (constructor)

Sin test propio: el constructor no tiene lógica condicional que verificar de forma aislada (solo crea el `ExecutorService` interno con el tamaño de pool indicado); se ejercita como parte del `Arrange` (`new CorreoAsyncExecutor(1)`) de los tests de `submit(Runnable tarea)` y `detener()` siguientes.

### Método: `void submit(Runnable tarea)`

- **`submit_tareaValida_seEjecutaEnUnHiloDaemonConNombreCorreoEnvio`** — Tipo: happy. Verifica: `—` (design-guidelines: evitar leaks de hilos).
  - **Arrange:** `new CorreoAsyncExecutor(1)`; una tarea que captura `Thread.currentThread()` en un `AtomicReference` y libera un `CountDownLatch`.
  - **Act:** `executor.submit(tarea)`; esperar el `latch.await(...)`.
  - **Assert:** el hilo capturado tiene `isDaemon() == true` y `getName()` empieza por `"correo-envio-"`.
- **`submit_tareaLanzaRuntimeException_noPropagaYElPoolSigueUtilizable`** — Tipo: borde. Verifica: `—` (design-guidelines: aislamiento de fallos, "MUST NOT dejar que una excepción no capturada mate el hilo del pool").
  - **Arrange:** `new CorreoAsyncExecutor(1)`; primera tarea que lanza `new RuntimeException("boom")`; segunda tarea (enviada justo después) que libera un `CountDownLatch`.
  - **Act:** `executor.submit(tareaQueFalla); executor.submit(tareaQueMarcaLatch); latch.await(...)`.
  - **Assert:** el `latch` llega a 0 dentro del timeout (el hilo del pool sigue vivo y procesa la segunda tarea pese al fallo de la primera); no se lanza ninguna excepción fuera de `submit`.

### Método: `void detener()`

- **`detener_sinTareasPendientes_terminaYRechazaNuevosEnvios`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `new CorreoAsyncExecutor(1)`.
  - **Act:** `executor.detener()`.
  - **Assert:** una llamada posterior a `executor.submit(...)` lanza `java.util.concurrent.RejectedExecutionException` (el `ExecutorService` interno ya está cerrado).
- **`detener_conTareaEnCurso_esperaSuFinalizacionAntesDeCerrar`** — Tipo: borde. Verifica: `—` ("awaitTermination" antes de forzar el cierre).
  - **Arrange:** `new CorreoAsyncExecutor(1)`; enviar una tarea que tarda unos milisegundos y al terminar marca un `AtomicBoolean completada = true`.
  - **Act:** `executor.detener()` (se invoca justo tras el `submit`, sin esperar aparte).
  - **Assert:** `completada` es `true` tras `detener()` (la tarea ya en curso se deja terminar dentro del margen de `awaitTermination`, no se aborta con `shutdownNow` de inmediato).

---

## Clase: `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner`  —  helper (utilidad estática)

**Responsabilidad:** ejecutar una tarea solo si la transacción JPA actual termina en commit, nunca en rollback.
**Colaboradores a mockear:** estáticos `com.axelor.db.JPA` (`em()`); mocks de `jakarta.persistence.EntityManager`, `org.hibernate.Session` (resultado de `unwrap(Session.class)`) y `org.hibernate.Transaction`.
**Origen diseño:** `design/rules/R-Correo-001.md` "Diseño detallado".

### Método: `static void runAfterCommit(Runnable tarea)`

- **`runAfterCommit_transaccionHaceCommit_ejecutaLaTarea`** — Tipo: happy. Verifica: `—` (soporte de R-Correo-001/R-Correo-002).
  - **Arrange:** `Mockito.mockStatic(JPA.class)`; `JPA.em()` → `EntityManager` mock; `entityManagerMock.unwrap(Session.class)` → `Session` mock; `sessionMock.getTransaction()` → `Transaction` mock; capturar con `ArgumentCaptor<Synchronization>` el argumento de `transactionMock.registerSynchronization(...)`; una `Runnable tarea` mock (o un `AtomicBoolean` que la lambda marca a `true`).
  - **Act:** `PostCommitRunner.runAfterCommit(tarea)`; después, invocar manualmente `synchronizationCapturado.afterCompletion(jakarta.transaction.Status.STATUS_COMMITTED)`.
  - **Assert:** `verify(tarea).run()` (o el `AtomicBoolean` es `true`).
- **`runAfterCommit_transaccionHaceRollback_noEjecutaLaTarea`** — Tipo: error/borde. Verifica: `—` ("si la transacción hace rollback, la tarea no se ejecuta").
  - **Arrange:** igual que el anterior.
  - **Act:** `PostCommitRunner.runAfterCommit(tarea)`; invocar `synchronizationCapturado.afterCompletion(jakarta.transaction.Status.STATUS_ROLLEDBACK)`.
  - **Assert:** `verify(tarea, never()).run()`.

---

## Clase: `com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver`  —  helper

**Responsabilidad:** parar ordenadamente el `CorreoAsyncExecutor` al detener la aplicación.
**Colaboradores a mockear:** `CorreoAsyncExecutor` (campo `@Inject`, se asigna por reflexión — ver Convenciones).
**Origen diseño:** `design.md` Paso 6.

### Método: `void onAppShutdown(ShutdownEvent event)`

- **`onAppShutdown_evento_delegaEnDetenerDelExecutor`** — Tipo: happy. Verifica: `—` (design-guidelines: cierre ordenado sin leaks de hilos).
  - **Arrange:** `new CorreoEventObserver()` con el campo `correoAsyncExecutor` sustituido por reflexión por un mock; un `ShutdownEvent` cualquiera (mock o instancia real si el constructor es accesible).
  - **Act:** `observer.onAppShutdown(event)`.
  - **Assert:** `verify(correoAsyncExecutorMock).detener()`.

### Método: `void onAppStart(StartupEvent event)`

**Sin lógica testable** — el método solo escribe una línea de log informativa (`log.info(...)`), sin ninguna rama ni efecto observable sobre ningún colaborador.

---

## Clase: `com.educaflow.subsystem.correos.module.MailSenderProvider`  —  helper (Guice `Provider`)

**Responsabilidad:** construir el `MailSender` real leyendo las credenciales SMTP de `AppSettings`.
**Colaboradores a mockear:** estático `com.axelor.app.AppSettings` (`get()` devuelve una instancia mockeada de `AppSettings` sobre la que se stubean `get(String)`).
**Origen diseño:** `design.md` Paso 6.

### Método: `MailSender get()`

- **`get_leeCredencialesDeAppSettingsYDevuelveMailSenderImpl`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `Mockito.mockStatic(AppSettings.class)`; `AppSettings settingsMock = Mockito.mock(AppSettings.class)`; `AppSettings.get()` → `settingsMock`; `settingsMock.get("mail.smtp.host")` → `"smtp.test.com"`; `settingsMock.get("mail.smtp.user")` → `"user@test.com"`; `settingsMock.get("mail.smtp.password")` → `"secret"`.
  - **Act:** `MailSender result = provider.get()`.
  - **Assert:** `result` es instancia de `com.educaflow.base.infrastructure.mail.impl.MailSenderImplSmtp`; `verify(settingsMock).get("mail.smtp.host")`, `verify(settingsMock).get("mail.smtp.user")`, `verify(settingsMock).get("mail.smtp.password")` (las tres claves exactas se leen).

---

## Clase: `com.educaflow.subsystem.correos.module.CorreoAsyncExecutorProvider`  —  helper (Guice `Provider`)

**Responsabilidad:** construir el `CorreoAsyncExecutor` leyendo el tamaño de pool configurado (o su valor por defecto).
**Colaboradores a mockear:** estático `com.axelor.app.AppSettings`.
**Origen diseño:** `design.md` Paso 6.

### Método: `CorreoAsyncExecutor get()`

- **`get_conPropiedadConfigurada_usaElTamanoIndicado`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `Mockito.mockStatic(AppSettings.class)`; `AppSettings settingsMock = Mockito.mock(AppSettings.class)`; `AppSettings.get()` → `settingsMock`; `settingsMock.getInt("mail.send.pool-size", 2)` → `4`.
  - **Act:** `CorreoAsyncExecutor result = provider.get()`.
  - **Assert:** `result` no es `null` (instancia de `CorreoAsyncExecutor`); `verify(settingsMock).getInt("mail.send.pool-size", 2)` (se pasa el valor por defecto correcto junto con la clave).
- **`get_sinPropiedadConfigurada_usaDosComoValorPorDefecto`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** igual, pero `settingsMock.getInt("mail.send.pool-size", 2)` → `2` (simula que `AppSettings` real aplicaría el valor por defecto pasado si la propiedad no existe).
  - **Act:** `provider.get()`.
  - **Assert:** `result` no es `null`; se confirma que el `Provider` invoca `getInt` con el literal `2` como valor por defecto (no otro número), que es lo único verificable desde fuera sin un getter de tamaño de pool en `CorreoAsyncExecutor`.

---

## Clase: `com.educaflow.subsystem.correos.controller.CorreoController` — sin lógica testable
**Motivo:** ambos métodos (`validateReenviar`, `reenviar`) son delegación pura — resuelven `CorreoService` vía `ModelServiceFactory`, construyen un `ActionRequestHelper` con `new` (no inyectado) y delegan toda la lógica de negocio en `CorreoService.validateReenviar`/`reenviar` (ya cubiertos arriba). `ActionRequestHelper.getOriginalModel()`/`getModel(...)` invocan internamente `JpaRepository.of(...)` y `BeanMapperModel` reales, lo que exige una infraestructura JPA viva para ejercer el controlador de extremo a extremo; **ningún controlador del proyecto que use `ActionRequestHelper` tiene hoy test unitario** (`CertificadoDigitalController`, `TareaFirmaController`, `ExpedienteController`, etc. — todos sin test JUnit), por lo que se sigue la misma convención ya establecida. El comportamiento de `CorreoController` se verifica mediante los tests E2E de `test-e2e-desc.md` (flujo real de "Reenviar" desde la pantalla de administración/centro).

---

## Clase: `com.educaflow.subsystem.correos.module.CorreosModule` — sin lógica testable
**Motivo:** `configure()` solo registra bindings estáticos (`bind(...).toProvider(...)`, `bind(CorreoEventObserver.class)`), sin ninguna condición ni cálculo — es cableado Guice puro. Ningún módulo Guice del proyecto (`CriptografiaModule`, `RegistroModule`, `SecurityModule`) tiene test unitario; se sigue la misma convención. El cableado se verifica en tiempo de arranque real (`./run.sh`, ver "Verificar" del Paso 6 de `design.md`).

---

## Clase: `com.educaflow.base.infrastructure.mail.impl.MailSenderImplSmtp` — sin lógica testable para este cambio
**Motivo:** el único cambio de este diseño es una línea (`transport.sendMessage(message, message.getRecipients(RecipientType.TO))` → `transport.sendMessage(message, message.getAllRecipients())`), dentro de un método que construye una `jakarta.mail.Session` real y abre un `Transport` SMTP (`session.getTransport("smtp")`), lo que en tiempo de ejecución instancia una clase concreta del proveedor de correo por reflexión (fuera del control directo de Mockito sin introducir infraestructura de test adicional no prevista en este diseño, p. ej. un servidor SMTP de pruebas). La clase no tiene ningún test unitario hoy pese a llevar tiempo en producción. La corrección del cambio se verifica con el `grep` ya indicado en `design.md` Paso 1 ("Verificar") y con el envío real de correos con CC/BCC en los tests E2E que ejercen el alta de un `Correo` con "en copia"/"en copia oculta" rellenos.

---

## Clase: `com.educaflow.subsystem.correos.db.Correo` / `com.educaflow.subsystem.correos.db.Adjunto` / `com.educaflow.subsystem.correos.db.EstadoCorreo` — sin lógica testable
**Motivo:** entidades de dominio autogeneradas por Axelor a partir de `domains/Correo.xml`/`domains/Adjunto.xml` (POJOs con getters/setters), sin ningún cuerpo Java propio. El campo calculado `nombreExpediente` (CC-Correo-007) es una columna `formula="true"` (subselect SQL declarado en el propio XML), no un getter Java — no hay código que testear con JUnit para él; su corrección se verifica por inspección del SQL y, si aplica, en un test E2E que abra un correo con `historialEstado` relleno.

---

## Cobertura

- **Clases con lógica descritas (9):** `Mail`, `JavaMailHelper`, `CorreoServiceImpl`, `AdjuntoServiceImpl`, `CorreoAsyncExecutor`, `PostCommitRunner`, `CorreoEventObserver`, `MailSenderProvider`, `CorreoAsyncExecutorProvider`.
- **Clases omitidas (sin lógica testable, con motivo):** `CorreoController` (delegación pura + convención del proyecto de no testear unitariamente controladores con `ActionRequestHelper`), `CorreosModule` (solo bindings Guice), `MailSenderImpl` (cambio de una línea sobre infraestructura SMTP real, no mockeable de forma realista; verificado por `grep`+E2E), `Correo`/`Adjunto`/`EstadoCorreo` (entidades autogeneradas sin cuerpo Java propio).
- **Reglas server-side cubiertas (`V`/`R`/`CC`):**
  - `V-Correo-001` a `V-Correo-018` (todas, en `CorreoServiceImpl`).
  - `V-Adjunto-001` a `V-Adjunto-008` (todas, en `AdjuntoServiceImpl`).
  - `R-Correo-001`, `R-Correo-002`, `R-Correo-003`, `R-Correo-004` (en `CorreoServiceImpl.insert`/`reenviar`/`enviarCorreo`).
  - `CC-Correo-001` a `CC-Correo-006` (cubiertos como parte de `R-Correo-003`/`R-Correo-004`, mismos tests de `insert`/`enviarCorreo`).
  - `RES-Correo-002`, `RES-Correo-003` (cubiertas junto a `R-Correo-004` y `V-Correo-016`/`remove()`).
  - `RES-Adjunto-001` — su mitad Java (`V-Adjunto-006`) está cubierta; la mitad declarativa (`<unique-constraint>` de `Adjunto.xml`) no es código Java, se verifica por validación XML (`validate.sh`) y por E2E.
- **Reglas server-side sin cuerpo Java (excluidas de JUnit por ser puramente declarativas, no por ser `U-`):**
  - `V-Correo-019` (RES-Correo-001, integridad referencial de `centro` — clave foránea JPA declarada en `domains/Correo.xml`, sin código Java que la implemente).
  - `CC-Correo-007` (nombreExpediente — `formula` SQL declarativa en `domains/Correo.xml`, sin getter Java).
- **Reglas solo-cliente excluidas (E2E en `test-e2e-desc.md`):** `U-correos-administracion-formulario-001` a `008` (8), `U-correos-administracion-listado-adjuntos-001` (1), `U-correos-administracion-formulario-adjunto-001` a `007` (7), `U-correos-centro-formulario-001` a `004` (4) — 20 reglas `U-` en total, todas de `views/Correo.xml`/`views/Correo-Centro.xml`.

## Notas de este fichero

1. **Mensajes de `AdjuntoServiceImpl.update()`/`validateUpdate()` y `remove()`/`validateRemove()`.** `entity-Adjunto.md` y `design.md` fijan que estas cuatro operaciones "SIEMPRE rechazan" pero, a diferencia de las equivalentes de `Correo`, no citan un texto literal de mensaje. Se ha decidido un texto coherente con el estilo del resto de mensajes del subsistema y con la intro de `entity-Adjunto.md` ("Como un correo nunca se borra, sus adjuntos tampoco"): `"Los adjuntos son inmutables tras su creación."` y `"Los adjuntos no se pueden borrar."`. Si al implementar se elige otro texto, basta ajustar el literal esperado en estos cuatro tests (`update_siempre_lanzaUnsupportedOperationException`, `validateUpdate_siempre_devuelveMensajeInmutabilidad`, `remove_siempre_lanzaUnsupportedOperationException`, `validateRemove_siempre_devuelveMensajeNoSePuedenBorrar`), sin cambiar nada más de este fichero.
2. **Convención de testeo de controladores.** Se ha comprobado explícitamente que ningún controlador existente del proyecto que use `ActionRequestHelper` (patrón que también usa `CorreoController`) tiene test unitario, así que excluir `CorreoController` de este fichero mantiene la coherencia con el resto del código base en vez de introducir un patrón de test nuevo y aislado.
3. **Mensaje de `CorreoServiceImpl.validateInsert` para V-Correo-014.** `design.md` fija que "el historial de estado indicado" (si se informa) debe existir, pero no cita un texto literal de mensaje para esta validación. Se ha decidido el texto `"El historial de estado indicado no existe"`, coherente con el estilo del resto de mensajes `V-Correo-0xx` de obligatoriedad/existencia. Si al implementar se elige otro texto, basta ajustar el literal esperado en `validateInsert_historialEstadoIndicadoNoExiste_devuelveMensajeNoExiste`, sin cambiar nada más de este fichero.
