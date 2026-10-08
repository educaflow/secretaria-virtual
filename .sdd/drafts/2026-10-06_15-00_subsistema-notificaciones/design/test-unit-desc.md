# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Aserciones con `org.junit.jupiter.api.Assertions` (no AssertJ); `JUnitHelper.assertThrowsCause` cuando la excepción pueda llegar envuelta.
- Mismo estilo que los tests que se sustituyen (`subsystem/correos/service/impl/CorreoServiceImplTest`, `AdjuntoServiceImplTest`, `subsystem/sms/service/impl/SmsServiceImplTest`, `subsystem/sms/controller/SmsControllerTest`, `subsystem/sms/module/SmsSenderProviderTest`): clase bajo test construida con `new <Clase>(<Entidad>.class, repositorioMock)`, los `@Inject` por campo rellenados por reflexión (`setField`; los de la base, como `ejecutorAsincrono`, se buscan en la clase que los declara, `NotificacionCanalServiceImpl`), `I18n.get` mockeado en el `@BeforeEach` como identidad (`I18n.get(x)` → `x`) y `SecurityUtil` mockeado con `Strictness.LENIENT`, cerrados en el `@AfterEach`.
- Mensajes: los literales de la spec tal cual (con `I18n` como identidad, el mensaje de la excepción/`BusinessMessage` es el literal). Cuando un método acumula varios mensajes, se comprueba que el esperado está entre ellos (los demás campos del `Arrange` son válidos, así que es el único).
- Paquetes de test: los de las clases bajo test (`com.educaflow.subsystem.notificaciones.{util,service.impl,controller,module,db}`), lo que da acceso a los métodos `protected` (ganchos de los canales) y al package-private `enviar(Long)`.
- Datos comunes: `DNI_VALIDO = "12345678Z"`, `DNI_LETRA_INCORRECTA = "12345678A"`; `centroA` (id 1) y `centroB` (id 2) instanciados con `new Centro()`; un `correoValido()` (motivo «Aviso de prueba», DNI válido, nombre «Juan», apellidos «Pérez», para `a@x.com`, asunto «Asunto de prueba», cuerpo «Cuerpo del correo», `centroA`, sin `historialEstado` ni adjuntos) y un `smsValido()` (los mismos datos comunes, teléfono `600111222`, mensaje «Hola»). Usuarios construidos como en los tests actuales (`new User()` con `CentroUsuario`/`CentroUsuarioTipoUsuario`/`CentroUsuarioCargo` reales).
- **Ninguno toca BD real**: repositorios y servicios mockeados; `JPA` (`em()`, `runInTransaction`), `AppSettings`, `MetaFiles`, `MetaFileUtil`, `SecurityUtil`, `GestorNotificacionesUtil` e `I18n` con `mockStatic` donde se indica. Las utilidades puras del proyecto (`DniUtil`, `EMailUtil`, `NumeroTelefono`, `MensajeSmsUtil`, `EntityHelper`) se usan **reales**.

---

## Clase: `com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil`  —  helper

**Responsabilidad:** único dueño de la clasificación «gestor del centro» de notificaciones (D4): si un usuario es gestor en algún centro y los ids de los centros que gestiona (con el centinela `-1L`).
**Colaboradores a mockear:** ninguno (estático puro). `User`, `CentroUsuario`, `CentroUsuarioTipoUsuario`, `TipoUsuario`, `CentroUsuarioCargo`, `Cargo` instanciados con `new` y setters.
**Origen diseño:** Paso 2 (`esGestorEnAlgunCentro`, `idsCentrosGestionados`, privado `gestiona`); `idsCentrosGestionados` es soporte de V-Notificacion-015 (`validateReenviar`) y `esGestorEnAlgunCentro` solo decide la visibilidad del menú «Del centro» (Paso 12), sin regla V/R/CC.

### Método: `public static boolean esGestorEnAlgunCentro(User user)`

- **`esGestorEnAlgunCentro_supervisorDeUnCentro_devuelveTrue`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** usuario con un `CentroUsuario` de `centroA` con un `CentroUsuarioTipoUsuario` de tipo `SUPERVISOR`.
  - **Act:** `GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario)`.
  - **Assert:** devuelve `true`.
- **`esGestorEnAlgunCentro_administrativoDeUnCentro_devuelveTrue`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ídem con tipo `ADMINISTRATIVO`.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `true`.
- **`esGestorEnAlgunCentro_directorDeUnCentro_devuelveTrue`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `CentroUsuario` de `centroA` sin tipos de usuario y con un `CentroUsuarioCargo` cuyo `cargo.code` es `DIRECTOR`.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `true`.
- **`esGestorEnAlgunCentro_jefeDeEstudios_devuelveTrue`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ídem con cargo `JEFE_ESTUDIOS`.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `true`.
- **`esGestorEnAlgunCentro_secretario_devuelveTrue`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ídem con cargo `SECRETARIO`.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `true`.
- **`esGestorEnAlgunCentro_vicesecretario_devuelveFalse`** — Tipo: error. Verifica: `—`.
  - **Arrange:** ídem con cargo `VICESECRETARIO` y sin tipos de usuario.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `false`.
- **`esGestorEnAlgunCentro_profesorSinCargo_devuelveFalse`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `CentroUsuario` de `centroA` con tipo `PROFESOR` y sin cargos.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `false`.
- **`esGestorEnAlgunCentro_gestorSoloEnElSegundoCentro_devuelveTrue`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** dos `CentroUsuario`: `centroA` con tipo `PROFESOR`, `centroB` con tipo `SUPERVISOR`.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `true`.
- **`esGestorEnAlgunCentro_centroUsuariosNull_devuelveFalse`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `new User()` sin `centroUsuarios` (null).
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `false`, sin excepción.
- **`esGestorEnAlgunCentro_centroUsuariosVacio_devuelveFalse`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** usuario con `centroUsuarios = List.of()`.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `false`.
- **`esGestorEnAlgunCentro_coleccionesDeTiposYCargosNullOConElementosNull_seIgnoranSinExcepcion`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** un `CentroUsuario` con `centroUsuarioTipoUsuario = null` y `centroUsuarioCargo = null`; otro `CentroUsuario` con un `CentroUsuarioTipoUsuario` cuyo `tipoUsuario` es null y un `CentroUsuarioCargo` cuyo `cargo` es null.
  - **Act:** `esGestorEnAlgunCentro(usuario)`.
  - **Assert:** `false`, sin `NullPointerException`.

### Método: `public static List<Long> idsCentrosGestionados(User user)`

- **`idsCentrosGestionados_gestorDeDosCentros_devuelveAmbosIds`** — Tipo: happy. Verifica: `V-Notificacion-015`.
  - **Arrange:** usuario `SUPERVISOR` en `centroA` y con cargo `DIRECTOR` en `centroB`.
  - **Act:** `GestorNotificacionesUtil.idsCentrosGestionados(usuario)`.
  - **Assert:** la lista contiene exactamente `1L` y `2L`.
- **`idsCentrosGestionados_gestorEnUnoYProfesorEnOtro_devuelveSoloElGestionado`** — Tipo: happy. Verifica: `V-Notificacion-015`.
  - **Arrange:** `ADMINISTRATIVO` en `centroA`, `PROFESOR` en `centroB`.
  - **Act:** `idsCentrosGestionados(usuario)`.
  - **Assert:** `List.of(1L)`.
- **`idsCentrosGestionados_mismoCentroEnDosCentroUsuario_noRepiteElId`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** dos `CentroUsuario` del mismo `centroA`, uno `SUPERVISOR` y otro con cargo `SECRETARIO`.
  - **Act:** `idsCentrosGestionados(usuario)`.
  - **Assert:** `List.of(1L)` (sin repetidos).
- **`idsCentrosGestionados_noGestionaNinguno_devuelveCentinelaMenosUno`** — Tipo: error. Verifica: `V-Notificacion-015`.
  - **Arrange:** usuario `PROFESOR` con cargo `VICESECRETARIO` en `centroA`.
  - **Act:** `idsCentrosGestionados(usuario)`.
  - **Assert:** `List.of(-1L)`.
- **`idsCentrosGestionados_usuarioNull_devuelveCentinelaMenosUno`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `user = null`.
  - **Act:** `idsCentrosGestionados(null)`.
  - **Assert:** `List.of(-1L)`, sin excepción.
- **`idsCentrosGestionados_centroUsuariosNull_devuelveCentinelaMenosUno`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `new User()` sin `centroUsuarios`.
  - **Act:** `idsCentrosGestionados(usuario)`.
  - **Assert:** `List.of(-1L)`.

---

## Clase: `com.educaflow.subsystem.notificaciones.service.impl.NotificacionCanalServiceImpl`  —  servicio

**Responsabilidad:** base abstracta del ciclo de vida de un canal (D1): alta (`insert` + `validateInsert` comunes), inmutabilidad y no borrado, reenvío, envío asíncrono con reintentos (`enviar`) y `AllowProperties` comunes; delega lo propio del canal en cinco ganchos.
**Colaboradores a mockear:** `CorreoRepository` (mock; `save(x)` → devuelve `x`), `EjecutorAsincrono` (mock, inyectado por reflexión en el campo de la base), `Provider<MailSender>` (mock → `MailSender` mock) del canal concreto, `EntityManager` (mock), `JPA` (`mockStatic`: `em()` → `em`; `runInTransaction(Runnable)` ejecuta el `Runnable`), `SecurityUtil` (`mockStatic`), `GestorNotificacionesUtil` (`mockStatic`, en los tests de reenvío), `I18n` (`mockStatic`, identidad), `MetaFileUtil` (`mockStatic`, solo si el correo lleva adjuntos), `AppSettings` (`mockStatic`, `mail.address.from` → `noreply@educaflow.test`, solo en `enviar`).
**Instancia bajo test:** la base es abstracta y no tiene entidad propia; se ejerce a través del canal real `new CorreoServiceImpl(Correo.class, correoRepository)` (sin subclase de test). Los textos del canal que aparecen en los asserts son por tanto los del correo; los del SMS se comprueban en `SmsServiceImpl.textosDelCanal`. Los dos tests marcados «(canal SMS)» usan en su lugar `new SmsServiceImpl(Sms.class, smsRepository)` (con `SmsRepository` mock, `Provider<SmsSender>` mock → `SmsSender` mock y el mismo `EjecutorAsincrono` mock) para comprobar que lo que la base hace depende del canal real y no de una clase fija.
**Origen diseño:** Paso 3 — `insert`, `update`, `remove`, `reenviar`, `validateInsert` (privados `validarMotivo`, `validarDniDestinatario`, `validarNombreYApellidos`, `validarCentro`, `validarHistorialEstado`), `validateUpdate`, `validateRemove`, `validateReenviar`, `allowPropertiesInsert/Update/Remove/Reenviar`, `fireActionRule_AsignarValoresIniciales` (R-Notificacion-001), `fireActionRule_ProgramarEnvioAsincrono` (R-Notificacion-002/004), `enviar` + `fireActionRule_RegistrarIntentoEnvio/MarcarEnvioCorrecto/MarcarEnvioFallido` (R-Notificacion-003, `rules/R-Notificacion-003.md`); V-Notificacion-001..016; R-Adjunto-001.

### Método: `public T insert(T notificacion)`

- **`insert_correoValido_asignaValoresInicialesYPersiste`** — Tipo: happy. Verifica: `R-Notificacion-001`.
  - **Arrange:** `correoValido()`; `SecurityUtil.isAdmin(any)` → `true`; `correoRepository.save(any)` → su argumento.
  - **Act:** `service.insert(correo)`.
  - **Assert:** devuelve el correo guardado con `tipoNotificacion = CORREO`, `estado = PENDIENTE`, `numeroReintentos = 0`, `fechaCreacion` no nula y `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio` y `descripcionUltimoFallo` nulos; `verify(correoRepository).save(correo)` una vez.
- **`insert_clienteDictaTipoNotificacionSms_seGuardaComoCorreo`** — Tipo: error. Verifica: `R-Notificacion-001`.
  - **Arrange:** `correoValido()` con `tipoNotificacion = SMS`; admin; `save` → su argumento.
  - **Act:** `service.insert(correo)`.
  - **Assert:** el correo capturado en `save` tiene `tipoNotificacion = CORREO` (el cliente no puede dictarlo; requisito explícito de las guías).
- **`insert_clienteDictaCamposDelEnvio_seSobrescribenIncondicionalmente`** — Tipo: error. Verifica: `R-Notificacion-001`, `V-Notificacion-016`.
  - **Arrange:** `correoValido()` con `estado = ENVIADO`, `numeroReintentos = 7`, `fechaCreacion` = 2000-01-01, `fechaPrimerIntentoEnvio`/`fechaUltimoIntentoEnvio`/`fechaEnvio` rellenas y `descripcionUltimoFallo = "falso"`; admin; `save` → su argumento.
  - **Act:** `service.insert(correo)`.
  - **Assert:** `estado = PENDIENTE`, `numeroReintentos = 0`, `fechaCreacion` posterior a 2000-01-01 (la actual), las tres fechas de envío y la descripción nulas.
- **`insert_correoValido_programaElEnvioTrasCommitConSoloElId`** — Tipo: happy. Verifica: `R-Notificacion-002`.
  - **Arrange:** `correoValido()`; admin; `save(any)` → el mismo correo con `id = 100L`; `JPA` mockeado (`em()` → `em`, `runInTransaction` ejecuta); `em.find(Correo.class, 100L, PESSIMISTIC_WRITE)` → un correo `ENVIADO`.
  - **Act:** `service.insert(correo)`; después se captura el `Runnable` pasado a `ejecutorAsincrono.ejecutarTrasCommit(...)` y se ejecuta.
  - **Assert:** `verify(ejecutorAsincrono).ejecutarTrasCommit(any())` una vez y **después** de `save` (`InOrder`); al ejecutar el `Runnable`, `verify(em).find(Correo.class, 100L, LockModeType.PESSIMISTIC_WRITE)` (relee por id en su propia transacción).
- **`insert_correoInvalido_lanzaValidationExceptionYNoPersisteNiPrograma`** — Tipo: error. Verifica: `V-Notificacion-001`.
  - **Arrange:** `correoValido()` con `name = null`; admin.
  - **Act:** `service.insert(correo)`.
  - **Assert:** lanza `ValidationException` con mensaje «El motivo es obligatorio»; `verify(correoRepository, never()).save(any())`; `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())`.
- **`insert_correoConAdjuntos_seGuardanEnCascadaConUnUnicoSave`** — Tipo: happy. Verifica: `R-Adjunto-001`.
  - **Arrange:** `correoValido()` con dos `Adjunto` («a.pdf», «b.pdf») con `contenido` (`MetaFile`) y `correo` apuntando al correo; admin; `MetaFileUtil.fileSizeOnDisk(any)` → 1024; `MetaFileUtil.cloneMetaFile(any)` → un `MetaFile` nuevo por llamada; `save` → su argumento.
  - **Act:** `service.insert(correo)`.
  - **Assert:** `verify(correoRepository, times(1)).save(correo)` y el correo guardado conserva sus dos adjuntos, ya con su `contenido` sustituido por la copia (el gancho del canal corre **antes** de `save`: `InOrder` sobre `MetaFileUtil.cloneMetaFile` y `save`).
- **`insert_smsConTipoCorreoDictado_seGuardaComoSmsConElTelefonoNormalizado`** — Tipo: error. Verifica: `R-Notificacion-001`, `R-Sms-001`. (canal SMS)
  - **Arrange:** `smsValido()` con `tipoNotificacion = CORREO` y `telefono = "600111222"`; admin; `smsRepository.save(any)` → su argumento.
  - **Act:** `smsService.insert(sms)`.
  - **Assert:** el SMS capturado en `save` tiene `tipoNotificacion = SMS` (sale de `TipoNotificacion.deClase` de la clase real, no del valor dictado), `estado = PENDIENTE` y `telefono` «+34600111222» (el gancho del canal SMS se ejecutó antes de `save`); `verify(ejecutorAsincrono).ejecutarTrasCommit(any())`.

### Método: `public T update(T nueva, T original)`

- **`update_siempre_lanzaUnsupportedOperationExceptionConElTextoDelCanal`** — Tipo: error. Verifica: `V-Notificacion-012`.
  - **Arrange:** dos `correoValido()` (nueva y original con `id = 100L`).
  - **Act:** `service.update(nueva, original)`.
  - **Assert:** lanza `UnsupportedOperationException` con mensaje «El correo es inmutable tras su creación.»; `verify(correoRepository, never()).save(any())`.

### Método: `public void remove(T notificacion)`

- **`remove_siempre_lanzaUnsupportedOperationExceptionConElTextoDelCanal`** — Tipo: error. Verifica: `V-Notificacion-013`.
  - **Arrange:** `correoValido()` con `id = 100L`.
  - **Act:** `service.remove(correo)`.
  - **Assert:** lanza `UnsupportedOperationException` con mensaje «Los correos no se pueden borrar.»; `verify(correoRepository, never()).remove(any())`.

### Método: `public T reenviar(T entidad, T entidadOriginal)`

- **`reenviar_fallidoPorAdministrador_programaElEnvioSinGuardarYDevuelveElOriginal`** — Tipo: happy. Verifica: `R-Notificacion-004`, `V-Notificacion-014`, `V-Notificacion-015`.
  - **Arrange:** `entidadOriginal` = `correoValido()` con `id = 100L`, `estado = FALLIDO`, `centroA`; `entidad = new Correo()` con `id = 100L`; `SecurityUtil.isAdmin(any)` → `true`.
  - **Act:** `service.reenviar(entidad, entidadOriginal)`.
  - **Assert:** devuelve la misma instancia `entidadOriginal` (`assertSame`); `verify(correoRepository, never()).save(any())`; `verify(ejecutorAsincrono).ejecutarTrasCommit(any())`; el estado del original sigue siendo `FALLIDO` (el cambio lo hace `enviar`).
- **`reenviar_noFallido_lanzaValidationExceptionYNoProgramaEnvio`** — Tipo: error. Verifica: `V-Notificacion-014`.
  - **Arrange:** original `PENDIENTE` de `centroA`; admin.
  - **Act:** `service.reenviar(entidad, entidadOriginal)`.
  - **Assert:** lanza `ValidationException` con mensaje «Solo se pueden reenviar correos que han fallado»; `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())`.
- **`reenviar_usuarioNoGestorDelCentro_lanzaValidationExceptionYNoProgramaEnvio`** — Tipo: error. Verifica: `V-Notificacion-015`.
  - **Arrange:** original `FALLIDO` de `centroB`; `SecurityUtil.getUser()` → usuario; `isAdmin` → `false`; `GestorNotificacionesUtil.idsCentrosGestionados(usuario)` → `List.of(1L)`.
  - **Act:** `service.reenviar(entidad, entidadOriginal)`.
  - **Assert:** lanza `ValidationException` con mensaje «No puede reenviar correos de un centro que no es suyo»; `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())`.

### Método: `void enviar(Long notificacionId)`

Arrange común: `JPA` mockeado (`em()` → `em`, `runInTransaction` ejecuta el `Runnable`); `AppSettings` mockeado (`mail.address.from`); `mailSenderProvider.get()` → `mailSender` (mock); `em.find(Correo.class, 100L, LockModeType.PESSIMISTIC_WRITE)` → el correo preparado.

- **`enviar_primerIntentoConExito_marcaEnviadoYRegistraElIntento`** — Tipo: happy. Verifica: `R-Notificacion-003`.
  - **Arrange:** correo `PENDIENTE`, `numeroReintentos = 0`, sin fechas de intento, `descripcionUltimoFallo = null`, `fechaCreacion` = ahora menos 1 minuto.
  - **Act:** `service.enviar(100L)`.
  - **Assert:** `verify(mailSender).send(any(Mail.class))`; `estado = ENVIADO`; `fechaEnvio` no nula; `descripcionUltimoFallo` nula; `numeroReintentos = 1`; `fechaPrimerIntentoEnvio` y `fechaUltimoIntentoEnvio` no nulas, `fechaUltimoIntentoEnvio` no anterior a `fechaPrimerIntentoEnvio` y ninguna anterior a `fechaCreacion` (sin exigir igualdad); `verify(correoRepository).save(correo)`; `verify(em).find(Correo.class, 100L, LockModeType.PESSIMISTIC_WRITE)`.
- **`enviar_canalFalla_marcaFallidoConLaTrazaYSinFechaDeEnvio`** — Tipo: error. Verifica: `R-Notificacion-003`.
  - **Arrange:** correo `PENDIENTE`; `mailSender.send(any)` lanza `new RuntimeException("SMTP caído")`.
  - **Act:** `service.enviar(100L)`.
  - **Assert:** no propaga la excepción; `estado = FALLIDO`; `descripcionUltimoFallo` contiene «SMTP caído» (traza completa de `ExceptionUtil.getTraceAsString`); `fechaEnvio` nula; `numeroReintentos = 1`; `verify(correoRepository).save(correo)`.
- **`enviar_providerSinConfiguracion_marcaFallido`** — Tipo: error. Verifica: `R-Notificacion-003`.
  - **Arrange:** correo `PENDIENTE`; `mailSenderProvider.get()` lanza `new IllegalArgumentException("clientId no puede ser null ni blank")` (RN-Correo-003: el `Provider` se resuelve dentro del envío).
  - **Act:** `service.enviar(100L)`.
  - **Assert:** no propaga; `estado = FALLIDO`; `descripcionUltimoFallo` contiene «clientId no puede ser null ni blank»; `verify(correoRepository).save(correo)`.
- **`enviar_reintentoDeUnFallido_conservaFechaPrimerIntentoEIncrementaReintentos`** — Tipo: happy. Verifica: `R-Notificacion-003`.
  - **Arrange:** correo `FALLIDO`, `numeroReintentos = 2`, `fechaPrimerIntentoEnvio` = T0 (hace 1 hora), `fechaUltimoIntentoEnvio` = T0, `descripcionUltimoFallo = "fallo anterior"`; `send` sin excepción.
  - **Act:** `service.enviar(100L)`.
  - **Assert:** `fechaPrimerIntentoEnvio` sigue siendo T0; `fechaUltimoIntentoEnvio` posterior a T0; `numeroReintentos = 3`; `estado = ENVIADO`; `descripcionUltimoFallo` nula (sobrescrita incondicionalmente).
- **`enviar_reintentoQueVuelveAFallar_sobrescribeFechaEnvioYDescripcion`** — Tipo: error. Verifica: `R-Notificacion-003`.
  - **Arrange:** correo `FALLIDO` con `fechaEnvio` rellena (dato incoherente) y `descripcionUltimoFallo = "viejo"`; `send` lanza `RuntimeException("nuevo")`.
  - **Act:** `service.enviar(100L)`.
  - **Assert:** `estado = FALLIDO`, `fechaEnvio` nula, `descripcionUltimoFallo` contiene «nuevo» y no «viejo».
- **`enviar_yaEnviado_noHaceNada`** — Tipo: borde. Verifica: `R-Notificacion-003`.
  - **Arrange:** correo `ENVIADO` con `numeroReintentos = 1`.
  - **Act:** `service.enviar(100L)`.
  - **Assert:** `verify(mailSenderProvider, never()).get()`; `verify(correoRepository, never()).save(any())`; `numeroReintentos` sigue en 1.
- **`enviar_idInexistente_lanzaIllegalStateException`** — Tipo: borde. Verifica: `R-Notificacion-003`.
  - **Arrange:** `em.find(Correo.class, 999L, PESSIMISTIC_WRITE)` → `null`.
  - **Act:** `service.enviar(999L)`.
  - **Assert:** lanza `IllegalStateException` (con `JUnitHelper.assertThrowsCause`, por si `runInTransaction` la envuelve); `verify(correoRepository, never()).save(any())`.
- **`enviar_canalSms_releeConLaClaseDelCanalYEnviaPorElProveedor`** — Tipo: happy. Verifica: `R-Notificacion-003`. (canal SMS)
  - **Arrange:** `em.find(Sms.class, 200L, LockModeType.PESSIMISTIC_WRITE)` → SMS `PENDIENTE`, `numeroReintentos = 0`, teléfono «+34600111222», mensaje «Hola».
  - **Act:** `smsService.enviar(200L)`.
  - **Assert:** `verify(em).find(Sms.class, 200L, LockModeType.PESSIMISTIC_WRITE)` (el `model` del servicio, no `Notificacion` ni `Correo`); `verify(smsSender).send(any())`; `estado = ENVIADO`, `numeroReintentos = 1`; `verify(smsRepository).save(sms)`.

### Método: `public Optional<BusinessMessages> validateInsert(T notificacion)`

Arrange común: `SecurityUtil.getUser()` → usuario de `centroA` (`perteneceAlCentro(centroA)` real); `isAdmin` → `false` salvo que se diga.

- **`validateInsert_todoValido_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Notificacion-001`…`V-Notificacion-011`.
  - **Arrange:** `correoValido()` (centro `centroA`, del usuario).
  - **Act:** `service.validateInsert(correo)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_motivoNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Notificacion-001`.
  - **Arrange:** `name = null`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El motivo es obligatorio».
- **`validateInsert_motivoSoloEspacios_devuelveMensajeObligatorio`** — Tipo: borde. Verifica: `V-Notificacion-001`.
  - **Arrange:** `name = "   "`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El motivo es obligatorio» y **no** el de longitud.
- **`validateInsert_motivoDe256Caracteres_devuelveMensajeLongitud`** — Tipo: error. Verifica: `V-Notificacion-002`.
  - **Arrange:** `name` = 256 × «a».
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El motivo no puede superar 255 caracteres».
- **`validateInsert_motivoDe255Caracteres_esValido`** — Tipo: borde. Verifica: `V-Notificacion-002`.
  - **Arrange:** `name` = 255 × «a».
  - **Act:** `validateInsert(correo)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_dniNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Notificacion-003`.
  - **Arrange:** `dniDestinatario = null`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El DNI del destinatario es obligatorio» y no el de DNI no válido.
- **`validateInsert_dniConLetraIncorrecta_devuelveMensajeNoValido`** — Tipo: error. Verifica: `V-Notificacion-004`.
  - **Arrange:** `dniDestinatario = DNI_LETRA_INCORRECTA`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El DNI del destinatario no es válido; compruebe la letra».
- **`validateInsert_nombreNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Notificacion-005`.
  - **Arrange:** `nombre = null`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El nombre es obligatorio».
- **`validateInsert_apellidosVacios_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Notificacion-006`.
  - **Arrange:** `apellidos = ""`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «Los apellidos son obligatorios».
- **`validateInsert_nombreDe256Caracteres_devuelveMensajeLongitud`** — Tipo: error. Verifica: `V-Notificacion-007`.
  - **Arrange:** `nombre` = 256 × «a».
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El nombre no puede superar 255 caracteres».
- **`validateInsert_apellidosDe256Caracteres_devuelveMensajeLongitud`** — Tipo: error. Verifica: `V-Notificacion-008`.
  - **Arrange:** `apellidos` = 256 × «a».
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «Los apellidos no pueden superar 255 caracteres».
- **`validateInsert_nombreYApellidosDe255Caracteres_esValido`** — Tipo: borde. Verifica: `V-Notificacion-007`, `V-Notificacion-008`.
  - **Arrange:** `nombre` y `apellidos` de 255 caracteres.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_centroNulo_devuelveMensajeObligatorioYNoComprobaPertenencia`** — Tipo: error. Verifica: `V-Notificacion-009`.
  - **Arrange:** `centro = null`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El centro es obligatorio» y no contiene «No puede crear correos para un centro que no es suyo».
- **`validateInsert_noAdminConCentroAjeno_devuelveMensajeCentroDelCanal`** — Tipo: error. Verifica: `V-Notificacion-010`.
  - **Arrange:** `centro = centroB`; usuario solo de `centroA`; `isAdmin` → `false`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «No puede crear correos para un centro que no es suyo».
- **`validateInsert_administradorConCualquierCentro_esValido`** — Tipo: happy. Verifica: `V-Notificacion-010`.
  - **Arrange:** `centro = centroB`; `isAdmin` → `true`; usuario sin centros.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_historialEstadoDelMismoCentro_esValido`** — Tipo: happy. Verifica: `V-Notificacion-011`.
  - **Arrange:** `historialEstado` con `id = 5L` y `expediente` (`new PruebaV1()` u otra subclase concreta de `Expediente` ya usada en tests) de `centroA`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_historialEstadoSinId_devuelveMensajeNoExisteDelCanal`** — Tipo: error. Verifica: `V-Notificacion-011`.
  - **Arrange:** `historialEstado = new HistorialEstado()` (sin id, cáscara del cliente).
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El historial de estado indicado no existe».
- **`validateInsert_historialEstadoSinExpediente_devuelveMensajeNoExiste`** — Tipo: error. Verifica: `V-Notificacion-011`.
  - **Arrange:** `historialEstado` con `id = 5L` y `expediente = null`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El historial de estado indicado no existe».
- **`validateInsert_historialEstadoDeExpedienteDeOtroCentro_devuelveMensajeNoExiste`** — Tipo: error. Verifica: `V-Notificacion-011`.
  - **Arrange:** `historialEstado` con `id = 5L` y expediente de `centroB`; correo de `centroA`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El historial de estado indicado no existe».
- **`validateInsert_historialEstadoConCentroNulo_noSeValidaElHistorial`** — Tipo: borde. Verifica: `V-Notificacion-011`.
  - **Arrange:** `centro = null`; `historialEstado = new HistorialEstado()` (sin id).
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El centro es obligatorio» y no contiene «El historial de estado indicado no existe».
- **`validateInsert_variosErrores_losAcumulaTodos`** — Tipo: borde. Verifica: `V-Notificacion-001`, `V-Notificacion-003`, `V-Notificacion-009`.
  - **Arrange:** `name = null`, `dniDestinatario = null`, `centro = null`.
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene los tres mensajes («El motivo es obligatorio», «El DNI del destinatario es obligatorio», «El centro es obligatorio»), en ese orden.
- **`validateInsert_datosDelCanalInvalidos_incluyeLosMensajesDelGancho`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `correoValido()` con `cuerpo = null` (comprueba que la base llama al gancho `validateDatosDelCanal`; la regla del canal se prueba en `CorreoServiceImpl`).
  - **Act:** `validateInsert(correo)`.
  - **Assert:** contiene «El cuerpo es obligatorio».

### Método: `public Optional<BusinessMessages> validateUpdate(T nueva, T original)`

- **`validateUpdate_siempre_devuelveMensajeInmutableDelCanal`** — Tipo: error. Verifica: `V-Notificacion-012`.
  - **Arrange:** dos `correoValido()`.
  - **Act:** `service.validateUpdate(nueva, original)`.
  - **Assert:** presente con un único mensaje «El correo es inmutable tras su creación.».

### Método: `public Optional<BusinessMessages> validateRemove(T notificacion)`

- **`validateRemove_siempre_devuelveMensajeNoBorrableDelCanal`** — Tipo: error. Verifica: `V-Notificacion-013`.
  - **Arrange:** `correoValido()`.
  - **Act:** `service.validateRemove(correo)`.
  - **Assert:** presente con un único mensaje «Los correos no se pueden borrar.».

### Método: `public Optional<BusinessMessages> validateReenviar(T entidad, T entidadOriginal)`

Arrange común: `entidad = new Correo()`; `entidadOriginal` = `correoValido()` con `id = 100L`; `SecurityUtil.getUser()` → usuario.

- **`validateReenviar_fallidoYAdministrador_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Notificacion-014`, `V-Notificacion-015`.
  - **Arrange:** original `FALLIDO` de `centroB`; `isAdmin` → `true`.
  - **Act:** `service.validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** `Optional.empty()`.
- **`validateReenviar_fallidoYGestorDelCentro_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Notificacion-015`.
  - **Arrange:** original `FALLIDO` de `centroA`; `isAdmin` → `false`; `GestorNotificacionesUtil.idsCentrosGestionados(usuario)` → `List.of(1L)`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** `Optional.empty()`.
- **`validateReenviar_pendiente_devuelveMensajeSoloFallidas`** — Tipo: error. Verifica: `V-Notificacion-014`.
  - **Arrange:** original `PENDIENTE`; admin.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** contiene «Solo se pueden reenviar correos que han fallado».
- **`validateReenviar_enviado_devuelveMensajeSoloFallidas`** — Tipo: borde. Verifica: `V-Notificacion-014`.
  - **Arrange:** original `ENVIADO`; admin.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** contiene «Solo se pueden reenviar correos que han fallado».
- **`validateReenviar_noGestorDelCentroDeLaNotificacion_devuelveMensajeCentroAjeno`** — Tipo: error. Verifica: `V-Notificacion-015`.
  - **Arrange:** original `FALLIDO` de `centroB`; `isAdmin` → `false`; `idsCentrosGestionados` → `List.of(1L)`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** contiene «No puede reenviar correos de un centro que no es suyo».
- **`validateReenviar_usuarioSinCentrosGestionados_devuelveMensajeCentroAjeno`** — Tipo: borde. Verifica: `V-Notificacion-015`.
  - **Arrange:** original `FALLIDO` de `centroA`; `isAdmin` → `false`; `idsCentrosGestionados` → `List.of(-1L)` (centinela).
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** contiene «No puede reenviar correos de un centro que no es suyo».
- **`validateReenviar_seEvaluaSobreElOriginalNoSobreLaEntidad`** — Tipo: borde. Verifica: `V-Notificacion-014`.
  - **Arrange:** `entidad` con `estado = FALLIDO` (lo que dictaría el cliente) y original `PENDIENTE`; admin.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** contiene «Solo se pueden reenviar correos que han fallado».
- **`validateReenviar_originalNull_lanzaNullPointerException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `entidadOriginal = null`.
  - **Act:** `validateReenviar(entidad, null)`.
  - **Assert:** lanza `NullPointerException` con mensaje «entidadOriginal no puede ser null» (error de programación, sin `I18n`).

### Método: `public AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteComunesYDelCanalYDeniegaLosDelServidor`** — Tipo: happy. Verifica: `V-Notificacion-016`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesInsert()`.
  - **Assert:** `allowProperty` es `true` para `name`, `dniDestinatario`, `nombre`, `apellidos`, `centro`, `historialEstado` (comunes) y `para`, `enCopia`, `enCopiaOculta`, `asunto`, `cuerpo`, `adjuntos` (del canal); `false` para `tipoNotificacion`, `destino`, `nombreExpediente`, `estado`, `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo`.

### Método: `public AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_denegaTodo`** — Tipo: happy. Verifica: `V-Notificacion-012`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesUpdate()`.
  - **Assert:** `allowProperty` es `false` para `name`, `para`, `estado` y `centro`.

### Método: `public AllowProperties allowPropertiesRemove()`

- **`allowPropertiesRemove_denegaTodo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesRemove()`.
  - **Assert:** `allowProperty` es `false` para `name`, `estado` y `centro`.

### Método: `public AllowProperties allowPropertiesReenviar()`

- **`allowPropertiesReenviar_denegaTodo`** — Tipo: happy. Verifica: `V-Notificacion-016`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesReenviar()`.
  - **Assert:** `allowProperty` es `false` para `estado`, `centro`, `numeroReintentos` y `fechaEnvio`.

---

## Clase: `com.educaflow.subsystem.notificaciones.service.impl.CorreoServiceImpl`  —  servicio

**Responsabilidad:** canal correo: ganchos de la base (validaciones propias, campos cliente propios, copia de adjuntos antes de guardar, envío por `MailSender`, textos del canal) y `listarCorreosEnFail`.
**Colaboradores a mockear:** `CorreoRepository` (mock; `findByEstado(...)` → `Query<Correo>` mock con `fetch()`), `Provider<MailSender>` (mock → `MailSender` mock), `I18n` (identidad), `MetaFileUtil` (`mockStatic`: `fileSizeOnDisk`, `cloneMetaFile`, `downloadContent`), `AppSettings` (`mockStatic`, `mail.address.from` → `noreply@educaflow.test`).
**Origen diseño:** Paso 4 — `listarCorreosEnFail`, `validateListarCorreosEnFail`, `aplicarReglasDelCanalAntesDeGuardar` (`fireActionRule_CopiarAdjuntos`, R-Correo-001), `enviarPorCanal` (`construirMail`, `separarDirecciones`; RN-Correo-001/004 dentro de R-Notificacion-003), `textosDelCanal`, `validateDatosDelCanal` (V-Correo-001..011), `allowPropertiesDelCanal`.

### Método: `public List<Correo> listarCorreosEnFail()`

- **`listarCorreosEnFail_devuelveLosFallidosDelFinder`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `correoRepository.findByEstado(EstadoNotificacion.FALLIDO)` → query mock cuyo `fetch()` devuelve dos correos.
  - **Act:** `service.listarCorreosEnFail()`.
  - **Assert:** devuelve esa lista; `verify(correoRepository).findByEstado(EstadoNotificacion.FALLIDO)`.
- **`listarCorreosEnFail_sinFallidos_devuelveListaVacia`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `fetch()` → `List.of()`.
  - **Act:** `listarCorreosEnFail()`.
  - **Assert:** lista vacía.

### Método: `public Optional<BusinessMessages> validateListarCorreosEnFail()`

- **`validateListarCorreosEnFail_siempre_devuelveOptionalVacio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.validateListarCorreosEnFail()`.
  - **Assert:** `Optional.empty()`.

### Método: `protected void validateDatosDelCanal(Correo correo, BusinessMessages messages)`

Arrange común: `correoValido()` y un `BusinessMessages` vacío; se invoca el gancho directamente (mismo paquete). Assert «sin mensajes» = `messages.isValid()`.

- **`validateDatosDelCanal_correoValido_noAnadeMensajes`** — Tipo: happy. Verifica: `V-Correo-001`…`V-Correo-011`.
  - **Arrange:** `correoValido()` con `enCopia = "b@x.com, c@x.com"` y `enCopiaOculta = "d@x.com"`.
  - **Act:** `service.validateDatosDelCanal(correo, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_paraNulo_anadeMensajeAlMenosUnDestinatario`** — Tipo: error. Verifica: `V-Correo-001`.
  - **Arrange:** `para = null`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «Debe indicar al menos un destinatario en el «para»».
- **`validateDatosDelCanal_paraSoloComasYEspacios_anadeMensajeAlMenosUnDestinatario`** — Tipo: borde. Verifica: `V-Correo-001`.
  - **Arrange:** `para = " , ,"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «Debe indicar al menos un destinatario en el «para»» y no el de una sola dirección.
- **`validateDatosDelCanal_paraConDosDirecciones_anadeMensajeUnaSolaDireccion`** — Tipo: error. Verifica: `V-Correo-002`.
  - **Arrange:** `para = "a@x.com, b@x.com"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «El «para» debe contener una sola dirección de correo; use «en copia» para añadir más destinatarios».
- **`validateDatosDelCanal_paraConFormatoInvalido_anadeMensajeFormato`** — Tipo: error. Verifica: `V-Correo-003`.
  - **Arrange:** `para = "no-es-un-correo"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)».
- **`validateDatosDelCanal_paraConEspaciosAlrededor_esValido`** — Tipo: borde. Verifica: `V-Correo-003`.
  - **Arrange:** `para = "  a@x.com  "`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_enCopiaConUnaDireccionInvalida_anadeMensajeEnCopia`** — Tipo: error. Verifica: `V-Correo-004`.
  - **Arrange:** `enCopia = "b@x.com, malo"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «El «en copia» debe contener direcciones de correo válidas».
- **`validateDatosDelCanal_enCopiaOcultaConUnaDireccionInvalida_anadeMensajeEnCopiaOculta`** — Tipo: error. Verifica: `V-Correo-005`.
  - **Arrange:** `enCopiaOculta = "malo@"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «El «en copia oculta» debe contener direcciones de correo válidas».
- **`validateDatosDelCanal_copiasEnBlanco_noSeValidanNiSeBuscanRepetidas`** — Tipo: borde. Verifica: `V-Correo-004`, `V-Correo-005`, `V-Correo-010`.
  - **Arrange:** `enCopia = "  "`, `enCopiaOculta = null`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_direccionDelParaRepetidaEnCopiaConOtraCapitalizacion_anadeMensajeRepetida`** — Tipo: error. Verifica: `V-Correo-010`.
  - **Arrange:** `para = "a@x.com"`, `enCopia = " A@X.com "`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «Una misma dirección de correo no puede aparecer más de una vez entre el «para», el «en copia» y el «en copia oculta»».
- **`validateDatosDelCanal_direccionRepetidaDentroDeEnCopia_anadeMensajeRepetida`** — Tipo: error. Verifica: `V-Correo-010`.
  - **Arrange:** `enCopia = "b@x.com,b@x.com"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene el mensaje de dirección repetida.
- **`validateDatosDelCanal_direccionRepetidaEntreEnCopiaYEnCopiaOculta_anadeMensajeRepetida`** — Tipo: error. Verifica: `V-Correo-010`.
  - **Arrange:** `enCopia = "b@x.com"`, `enCopiaOculta = "b@x.com"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene el mensaje de dirección repetida.
- **`validateDatosDelCanal_asuntoSoloEspacios_anadeMensajeObligatorio`** — Tipo: error. Verifica: `V-Correo-006`.
  - **Arrange:** `asunto = "   "`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «El asunto es obligatorio» y ningún otro mensaje del asunto.
- **`validateDatosDelCanal_asuntoDe256Caracteres_anadeMensajeLongitud`** — Tipo: error. Verifica: `V-Correo-007`.
  - **Arrange:** `asunto` = 256 × «a».
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «El asunto no puede superar 255 caracteres».
- **`validateDatosDelCanal_asuntoDe255Caracteres_esValido`** — Tipo: borde. Verifica: `V-Correo-007`.
  - **Arrange:** `asunto` = 255 × «a».
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_asuntoConSaltoDeLinea_anadeMensajeCaracteresDeControl`** — Tipo: error. Verifica: `V-Correo-008`.
  - **Arrange:** `asunto = "Línea 1\nLínea 2"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «El asunto no puede contener saltos de línea ni caracteres de control».
- **`validateDatosDelCanal_asuntoConTabulador_anadeMensajeCaracteresDeControl`** — Tipo: borde. Verifica: `V-Correo-008`.
  - **Arrange:** `asunto = "Asunto\tcon tab"`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene el mismo mensaje.
- **`validateDatosDelCanal_cuerpoSoloEspacios_anadeMensajeObligatorio`** — Tipo: error. Verifica: `V-Correo-009`.
  - **Arrange:** `cuerpo = "  \n "`.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «El cuerpo es obligatorio».
- **`validateDatosDelCanal_adjuntosQueSuperan25MB_anadeMensajeTamanoTotal`** — Tipo: error. Verifica: `V-Correo-011`.
  - **Arrange:** dos adjuntos con `contenido`; `MetaFileUtil.fileSizeOnDisk(contenido1)` → 15 MB, `fileSizeOnDisk(contenido2)` → 15 MB (1 MB = 1024 × 1024 bytes, como el límite de 10 MB actual del adjunto).
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene «Los adjuntos del correo no pueden superar 25 MB en total».
- **`validateDatosDelCanal_adjuntosDeExactamente25MB_esValido`** — Tipo: borde. Verifica: `V-Correo-011`.
  - **Arrange:** dos adjuntos de 10 MB y 15 MB (suma = 25 × 1024 × 1024).
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_seMideEnDiscoNoConElFileSizeDelCliente`** — Tipo: borde. Verifica: `V-Correo-011`.
  - **Arrange:** un adjunto cuyo `MetaFile.fileSize` dice 1 byte y `fileSizeOnDisk` → 26 MB.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** contiene el mensaje de tamaño total.
- **`validateDatosDelCanal_adjuntoSinContenido_noSeSumaNiFalla`** — Tipo: borde. Verifica: `V-Correo-011`.
  - **Arrange:** un adjunto con `contenido = null` y otro de 1 MB.
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** sin mensajes; `fileSizeOnDisk` invocado una sola vez.
- **`validateDatosDelCanal_sinAdjuntos_noMideNada`** — Tipo: borde. Verifica: `V-Correo-011`.
  - **Arrange:** `adjuntos` null (y, en una segunda aserción, `List.of()`).
  - **Act:** `validateDatosDelCanal(correo, messages)`.
  - **Assert:** sin mensajes; `MetaFileUtil.fileSizeOnDisk` nunca invocado.

### Método: `protected void aplicarReglasDelCanalAntesDeGuardar(Correo correo)`

- **`aplicarReglasDelCanalAntesDeGuardar_sustituyeCadaContenidoPorUnaCopiaConElNombreDelAdjunto`** — Tipo: happy. Verifica: `R-Correo-001`.
  - **Arrange:** correo con dos adjuntos (`nombreFichero = "  informe.pdf "` y `"foto.png"`) con `MetaFile` originales; `MetaFileUtil.cloneMetaFile(original_i)` → `copia_i` (`new MetaFile()` distinto).
  - **Act:** `service.aplicarReglasDelCanalAntesDeGuardar(correo)`.
  - **Assert:** el `contenido` de cada adjunto es su `copia_i` (`assertSame`), no el original; `copia_1.getFileName()` = «informe.pdf» (recortado) y `copia_2.getFileName()` = «foto.png»; `cloneMetaFile` invocado una vez por adjunto.
- **`aplicarReglasDelCanalAntesDeGuardar_sinAdjuntos_noClonaNada`** — Tipo: borde. Verifica: `R-Correo-001`.
  - **Arrange:** correo con `adjuntos` null (y, en una segunda aserción, vacío).
  - **Act:** `aplicarReglasDelCanalAntesDeGuardar(correo)`.
  - **Assert:** sin excepción; `MetaFileUtil.cloneMetaFile` nunca invocado.

### Método: `protected void enviarPorCanal(Correo correo)`

Arrange común: `mailSenderProvider.get()` → `mailSender`; `AppSettings` mockeado; `ArgumentCaptor<Mail>` sobre `mailSender.send`.

- **`enviarPorCanal_correoCompleto_construyeElMailConDireccionesRecortadasYTextoPlano`** — Tipo: happy. Verifica: `R-Notificacion-003`.
  - **Arrange:** correo con `para = " a@x.com "`, `enCopia = "b@x.com , c@x.com"`, `enCopiaOculta = " d@x.com"`, asunto y cuerpo, sin adjuntos.
  - **Act:** `service.enviarPorCanal(correo)`.
  - **Assert:** `Mail` capturado: `to = [a@x.com]`, `cc = [b@x.com, c@x.com]`, `bcc = [d@x.com]`, remitente `noreply@educaflow.test`, asunto y cuerpo de texto plano iguales a los del correo, cuerpo HTML `null`, sin adjuntos.
- **`enviarPorCanal_copiasVaciasOConEntradasVacias_seOmiten`** — Tipo: borde. Verifica: `R-Notificacion-003`.
  - **Arrange:** `enCopia = null`, `enCopiaOculta = " , "`.
  - **Act:** `enviarPorCanal(correo)`.
  - **Assert:** `cc` y `bcc` del `Mail` vacíos.
- **`enviarPorCanal_correoConAdjuntos_losAdjuntaConElNombreDelAdjunto`** — Tipo: happy. Verifica: `R-Notificacion-003`.
  - **Arrange:** un adjunto `nombreFichero = "informe.pdf"` con `MetaFile` de `fileType = "application/pdf"`; `MetaFileUtil.downloadContent(metaFile)` → `{1,2,3}`.
  - **Act:** `enviarPorCanal(correo)`.
  - **Assert:** el `Mail` lleva un `Fichero` con nombre «informe.pdf», bytes `{1,2,3}` y tipo «application/pdf».
- **`enviarPorCanal_providerFalla_propagaLaRuntimeException`** — Tipo: error. Verifica: `R-Notificacion-003`.
  - **Arrange:** `mailSenderProvider.get()` lanza `IllegalArgumentException`.
  - **Act:** `enviarPorCanal(correo)`.
  - **Assert:** lanza `IllegalArgumentException` (la convierte en FALLIDO `enviar`, probado en la base); `mailSender` sin interacciones.

### Método: `protected TextosCanal textosDelCanal()`

- **`textosDelCanal_devuelveLosSeisMensajesDelCorreo`** — Tipo: happy. Verifica: `V-Notificacion-010`, `V-Notificacion-011`, `V-Notificacion-012`, `V-Notificacion-013`, `V-Notificacion-014`, `V-Notificacion-015`.
  - **Arrange:** `I18n` identidad.
  - **Act:** `service.textosDelCanal()`.
  - **Assert:** `centroAjeno` = «No puede crear correos para un centro que no es suyo»; `estadoExpedienteInexistente` = «El historial de estado indicado no existe»; `inmutable` = «El correo es inmutable tras su creación.»; `noBorrable` = «Los correos no se pueden borrar.»; `soloFallidas` = «Solo se pueden reenviar correos que han fallado»; `reenvioCentroAjeno` = «No puede reenviar correos de un centro que no es suyo»; los seis pasan por `I18n.get` (`i18nMock.verify` de cada literal).

### Método: `protected Map<String, Object> allowPropertiesDelCanal()`

- **`allowPropertiesDelCanal_declaraLosCamposDelCorreoYLosDelAdjuntoSinElPadre`** — Tipo: happy. Verifica: `V-Notificacion-016`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesDelCanal()`.
  - **Assert:** claves exactamente `para`, `enCopia`, `enCopiaOculta`, `asunto`, `cuerpo`, `adjuntos`; el valor de `adjuntos` declara exactamente `nombreFichero` y `contenido` (no `correo`).

---

## Clase: `com.educaflow.subsystem.notificaciones.service.impl.SmsServiceImpl`  —  servicio

**Responsabilidad:** canal SMS: validaciones propias (teléfono y mensaje), normalización del teléfono antes de guardar, envío por `SmsSender`, textos del canal y campos cliente propios.
**Colaboradores a mockear:** `SmsRepository` (mock), `Provider<SmsSender>` (mock → `SmsSender` mock), `I18n` (identidad). `NumeroTelefono` y `MensajeSmsUtil` reales.
**Origen diseño:** Paso 5 — `aplicarReglasDelCanalAntesDeGuardar` (`fireActionRule_NormalizarTelefono`, R-Sms-001), `enviarPorCanal`, `textosDelCanal`, `validateDatosDelCanal` (V-Sms-001..004), `allowPropertiesDelCanal`.

### Método: `protected void validateDatosDelCanal(Sms sms, BusinessMessages messages)`

Arrange común: `smsValido()` y un `BusinessMessages` vacío; se invoca el gancho directamente.

- **`validateDatosDelCanal_smsValido_noAnadeMensajes`** — Tipo: happy. Verifica: `V-Sms-001`…`V-Sms-004`.
  - **Arrange:** `smsValido()`.
  - **Act:** `service.validateDatosDelCanal(sms, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_telefonoNulo_anadeMensajeObligatorio`** — Tipo: error. Verifica: `V-Sms-001`.
  - **Arrange:** `telefono = null`.
  - **Act:** `validateDatosDelCanal(sms, messages)`.
  - **Assert:** contiene «El teléfono es obligatorio» y no el de móvil no válido.
- **`validateDatosDelCanal_telefonoFijo_anadeMensajeMovilNoValido`** — Tipo: error. Verifica: `V-Sms-002`.
  - **Arrange:** `telefono = "961234567"`.
  - **Act:** `validateDatosDelCanal(sms, messages)`.
  - **Assert:** contiene «El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)».
- **`validateDatosDelCanal_telefonoConPrefijoYEspacios_esValido`** — Tipo: borde. Verifica: `V-Sms-002`.
  - **Arrange:** `telefono = "+34 600 111 222"`.
  - **Act:** `validateDatosDelCanal(sms, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_mensajeSoloEspacios_anadeMensajeObligatorio`** — Tipo: error. Verifica: `V-Sms-003`.
  - **Arrange:** `mensaje = "   "`.
  - **Act:** `validateDatosDelCanal(sms, messages)`.
  - **Assert:** contiene «El mensaje es obligatorio» y no el de longitud (no se llama a `MensajeSmsUtil` con blancos).
- **`validateDatosDelCanal_mensajeBasicoDe160Caracteres_esValido`** — Tipo: borde. Verifica: `V-Sms-004`.
  - **Arrange:** `mensaje` = 160 × «a».
  - **Act:** `validateDatosDelCanal(sms, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_mensajeBasicoDe161Caracteres_anadeMensajeNoCabe`** — Tipo: error. Verifica: `V-Sms-004`.
  - **Arrange:** `mensaje` = 161 × «a».
  - **Act:** `validateDatosDelCanal(sms, messages)`.
  - **Assert:** contiene «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales».
- **`validateDatosDelCanal_mensajeConAcentoDe70Caracteres_esValido`** — Tipo: borde. Verifica: `V-Sms-004`.
  - **Arrange:** `mensaje` = «á» + 69 × «a».
  - **Act:** `validateDatosDelCanal(sms, messages)`.
  - **Assert:** sin mensajes.
- **`validateDatosDelCanal_mensajeConAcentoDe71Caracteres_anadeMensajeNoCabe`** — Tipo: error. Verifica: `V-Sms-004`.
  - **Arrange:** `mensaje` = «á» + 70 × «a».
  - **Act:** `validateDatosDelCanal(sms, messages)`.
  - **Assert:** contiene el mensaje de «no cabe en un solo SMS».

### Método: `protected void aplicarReglasDelCanalAntesDeGuardar(Sms sms)`

- **`aplicarReglasDelCanalAntesDeGuardar_telefonoNacional_loGuardaEnE164`** — Tipo: happy. Verifica: `R-Sms-001`.
  - **Arrange:** `telefono = "600111222"`.
  - **Act:** `service.aplicarReglasDelCanalAntesDeGuardar(sms)`.
  - **Assert:** `telefono` = «+34600111222».
- **`aplicarReglasDelCanalAntesDeGuardar_telefonoInternacionalConEspacios_loNormaliza`** — Tipo: borde. Verifica: `R-Sms-001`.
  - **Arrange:** `telefono = "+34 600 111 222"`.
  - **Act:** `aplicarReglasDelCanalAntesDeGuardar(sms)`.
  - **Assert:** `telefono` = «+34600111222».

### Método: `protected void enviarPorCanal(Sms sms)`

- **`enviarPorCanal_enviaTelefonoYMensajeAlProveedor`** — Tipo: happy. Verifica: `R-Notificacion-003`.
  - **Arrange:** `telefono = "+34600111222"`, `mensaje = "Hola"`; `smsSenderProvider.get()` → `smsSender`.
  - **Act:** `service.enviarPorCanal(sms)`.
  - **Assert:** `verify(smsSender).send(captor)`; el `com.educaflow.base.infrastructure.sms.Sms` capturado tiene `telefonoDestino` «+34600111222» y `mensaje` «Hola».
- **`enviarPorCanal_proveedorSinConfigurar_propagaLaRuntimeException`** — Tipo: error. Verifica: `R-Notificacion-003`.
  - **Arrange:** `smsSenderProvider.get()` lanza `IllegalArgumentException`.
  - **Act:** `enviarPorCanal(sms)`.
  - **Assert:** lanza `IllegalArgumentException`; `smsSender` sin interacciones.

### Método: `protected TextosCanal textosDelCanal()`

- **`textosDelCanal_devuelveLosSeisMensajesDelSms`** — Tipo: happy. Verifica: `V-Notificacion-010`, `V-Notificacion-011`, `V-Notificacion-012`, `V-Notificacion-013`, `V-Notificacion-014`, `V-Notificacion-015`.
  - **Arrange:** `I18n` identidad.
  - **Act:** `service.textosDelCanal()`.
  - **Assert:** `centroAjeno` = «No puede crear SMS para un centro que no es suyo»; `estadoExpedienteInexistente` = «El estado del expediente indicado no existe»; `inmutable` = «El SMS es inmutable tras su creación.»; `noBorrable` = «Los SMS no se pueden borrar.»; `soloFallidas` = «Solo se pueden reenviar SMS que han fallado»; `reenvioCentroAjeno` = «No puede reenviar SMS de un centro que no es suyo».

### Método: `protected Map<String, Object> allowPropertiesDelCanal()`

- **`allowPropertiesDelCanal_declaraTelefonoYMensaje`** — Tipo: happy. Verifica: `V-Notificacion-016`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesDelCanal()`.
  - **Assert:** claves exactamente `telefono` y `mensaje`.

---

## Clase: `com.educaflow.subsystem.notificaciones.service.impl.AdjuntoServiceImpl`  —  servicio

**Responsabilidad:** ciclo de vida de `Adjunto`: solo alta (en cascada con su correo), inmutable y no borrable; validaciones del adjunto.
**Colaboradores a mockear:** `AdjuntoRepository` (mock), `SecurityUtil` (`mockStatic`: `getUser()` → usuario de `centroA`, `isAdmin` según el test), `MetaFileUtil` (`mockStatic`: `fileSizeOnDisk(any)` → 1024 por defecto), `I18n` (identidad).
**Origen diseño:** Paso 4 — `update`, `remove`, `validateInsert` (V-Adjunto-001, 002, 003, 006..013), `validateUpdate`, `validateRemove`, `allowPropertiesInsert/Update/Remove`. Datos: `adjuntoValido()` = `nombreFichero` «informe.pdf», `contenido` = `new MetaFile()`, `correo` = un `Correo` sin id de `centroA` cuya lista `adjuntos` contiene solo este adjunto.

### Método: `public Adjunto update(Adjunto nuevo, Adjunto original)`

- **`update_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: `V-Adjunto-004`.
  - **Arrange:** dos `adjuntoValido()`.
  - **Act:** `service.update(nuevo, original)`.
  - **Assert:** lanza `UnsupportedOperationException` con mensaje «El adjunto es inmutable tras su creación.»; `verify(adjuntoRepository, never()).save(any())`.

### Método: `public void remove(Adjunto adjunto)`

- **`remove_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: `V-Adjunto-005`.
  - **Arrange:** `adjuntoValido()`.
  - **Act:** `service.remove(adjunto)`.
  - **Assert:** lanza `UnsupportedOperationException` con mensaje «Los adjuntos no se pueden borrar.»; `verify(adjuntoRepository, never()).remove(any())`.

### Método: `public Optional<BusinessMessages> validateInsert(Adjunto adjunto)`

- **`validateInsert_todoValido_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Adjunto-001`…`V-Adjunto-003`, `V-Adjunto-006`…`V-Adjunto-013`.
  - **Arrange:** `adjuntoValido()`; `isAdmin` → `false` (usuario de `centroA`).
  - **Act:** `service.validateInsert(adjunto)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_correoNulo_devuelveMensajeDebePertenecerAUnCorreo`** — Tipo: error. Verifica: `V-Adjunto-001`.
  - **Arrange:** `correo = null`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El adjunto debe pertenecer a un correo» y ningún mensaje de centro, de correo existente ni de nombre repetido.
- **`validateInsert_noAdminCorreoDeOtroCentro_devuelveMensajeCentroAjeno`** — Tipo: error. Verifica: `V-Adjunto-006`.
  - **Arrange:** correo de `centroB`; usuario solo de `centroA`; `isAdmin` → `false`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «No puede añadir adjuntos a correos de un centro que no es suyo».
- **`validateInsert_administradorCorreoDeOtroCentro_esValido`** — Tipo: happy. Verifica: `V-Adjunto-006`.
  - **Arrange:** correo de `centroB`; `isAdmin` → `true`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_correoYaPersistido_devuelveMensajeCorreoExistente`** — Tipo: error. Verifica: `V-Adjunto-007`.
  - **Arrange:** `correo.setId(100L)`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «No se pueden añadir adjuntos a un correo ya existente».
- **`validateInsert_nombreFicheroSoloEspacios_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Adjunto-002`.
  - **Arrange:** `nombreFichero = "  "`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El nombre del fichero es obligatorio» y ninguno de los mensajes de V-Adjunto-009/010/011.
- **`validateInsert_nombreFicheroConBarra_devuelveMensajeCaracteresProhibidos`** — Tipo: error. Verifica: `V-Adjunto-009`.
  - **Arrange:** `nombreFichero = "a/b.pdf"` (y, con la misma aserción, `"a\\b.pdf"` y `"a\nb.pdf"`).
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El nombre del fichero no puede contener los caracteres / \ ni caracteres de control».
- **`validateInsert_nombreFicheroConEspaciosYTildes_esValido`** — Tipo: borde. Verifica: `V-Adjunto-009`.
  - **Arrange:** `nombreFichero = "Solicitud de matrícula.pdf"`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_nombreFicheroDe256Caracteres_devuelveMensajeLongitud`** — Tipo: error. Verifica: `V-Adjunto-010`.
  - **Arrange:** `nombreFichero` = 252 × «a» + «.pdf» (256).
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El nombre del fichero no puede superar 255 caracteres».
- **`validateInsert_nombreFicheroDe255Caracteres_esValido`** — Tipo: borde. Verifica: `V-Adjunto-010`.
  - **Arrange:** `nombreFichero` de 255 caracteres.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_nombreFicheroPunto_devuelveMensajeNombreNoPermitido`** — Tipo: error. Verifica: `V-Adjunto-011`.
  - **Arrange:** `nombreFichero = "."`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El nombre del fichero no puede ser «.» ni «..»».
- **`validateInsert_nombreFicheroDosPuntosConEspacios_devuelveMensajeNombreNoPermitido`** — Tipo: borde. Verifica: `V-Adjunto-011`.
  - **Arrange:** `nombreFichero = " .. "`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El nombre del fichero no puede ser «.» ni «..»».
- **`validateInsert_contenidoNulo_devuelveMensajeObligatorioYNoMide`** — Tipo: error. Verifica: `V-Adjunto-003`.
  - **Arrange:** `contenido = null`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El contenido del adjunto es obligatorio»; `MetaFileUtil.fileSizeOnDisk` nunca invocado.
- **`validateInsert_contenidoDeMasDe10MB_devuelveMensajeTamanoMaximo`** — Tipo: error. Verifica: `V-Adjunto-008`.
  - **Arrange:** `fileSizeOnDisk(contenido)` → 10 × 1024 × 1024 + 1.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El adjunto no puede superar los 10 MB».
- **`validateInsert_contenidoDeExactamente10MB_esValido`** — Tipo: borde. Verifica: `V-Adjunto-008`.
  - **Arrange:** `fileSizeOnDisk` → 10 × 1024 × 1024.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_fileSizeDelClienteMenorQueElDisco_seUsaElDisco`** — Tipo: borde. Verifica: `V-Adjunto-008`.
  - **Arrange:** `contenido.setFileSize(1L)`; `fileSizeOnDisk` → 11 MB.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El adjunto no puede superar los 10 MB».
- **`validateInsert_ficheroVacio_devuelveMensajeVacio`** — Tipo: error. Verifica: `V-Adjunto-012`.
  - **Arrange:** `fileSizeOnDisk` → 0.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «El fichero adjunto está vacío».
- **`validateInsert_ficheroDeUnByte_esValido`** — Tipo: borde. Verifica: `V-Adjunto-012`.
  - **Arrange:** `fileSizeOnDisk` → 1.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_nombreRepetidoEntreHermanosTrasTrim_devuelveMensajeYaExiste`** — Tipo: error. Verifica: `V-Adjunto-013`.
  - **Arrange:** el correo tiene otro adjunto con `nombreFichero = " informe.pdf "` además del que se valida («informe.pdf»).
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** contiene «Ya existe un adjunto con ese nombre en el correo».
- **`validateInsert_nombreUnicoEntreHermanos_esValido`** — Tipo: borde. Verifica: `V-Adjunto-013`.
  - **Arrange:** el correo tiene otro adjunto «foto.png» y uno con `nombreFichero = null`.
  - **Act:** `validateInsert(adjunto)`.
  - **Assert:** `Optional.empty()`, sin excepción.

### Método: `public Optional<BusinessMessages> validateUpdate(Adjunto nuevo, Adjunto original)`

- **`validateUpdate_siempre_devuelveMensajeInmutable`** — Tipo: error. Verifica: `V-Adjunto-004`.
  - **Arrange:** dos `adjuntoValido()`.
  - **Act:** `service.validateUpdate(nuevo, original)`.
  - **Assert:** presente con «El adjunto es inmutable tras su creación.».

### Método: `public Optional<BusinessMessages> validateRemove(Adjunto adjunto)`

- **`validateRemove_siempre_devuelveMensajeNoSeBorran`** — Tipo: error. Verifica: `V-Adjunto-005`.
  - **Arrange:** `adjuntoValido()`.
  - **Act:** `service.validateRemove(adjunto)`.
  - **Assert:** presente con «Los adjuntos no se pueden borrar.».

### Método: `public AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteNombreFicheroContenidoYCorreo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesInsert()`.
  - **Assert:** `allowProperty` `true` para `nombreFichero`, `contenido` y `correo`; `false` para un campo no declarado (p. ej. `nombreOriginal`).

### Método: `public AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_denegaTodo`** — Tipo: happy. Verifica: `V-Adjunto-004`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesUpdate()`.
  - **Assert:** `false` para `nombreFichero`, `contenido` y `correo`.

### Método: `public AllowProperties allowPropertiesRemove()`

- **`allowPropertiesRemove_denegaTodo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesRemove()`.
  - **Assert:** `false` para `nombreFichero`, `contenido` y `correo`.

---

## Clase: `com.educaflow.subsystem.notificaciones.service.impl.NotificacionServiceImpl`  —  servicio

**Responsabilidad:** factorías `createCorreo`/`createSms` y cierre de la puerta REST de la base `Notificacion`: el alta genérica nunca se admite; modificar/borrar se delegan al servicio del canal de la instancia real.
**Colaboradores a mockear:** `NotificacionRepository` (mock), `ModelServiceFactory` (mock, inyectado por reflexión; `resolve(Correo.class)` → `CorreoService` mock, `resolve(Sms.class)` → `SmsService` mock), `I18n` (identidad). `EntityHelper` real (con `new Correo()`/`new Sms()` devuelve la propia clase).
**Origen diseño:** Paso 6 — `insert`, `update`, `remove`, `createCorreo`, `createSms`, `validateInsert`, `validateUpdate`, `validateRemove`, `validateCreateCorreo`, `validateCreateSms`, `allowPropertiesInsert/Update/Remove`, privado `servicioDelCanal`; V-Notificacion-012/013/017.

### Método: `public Notificacion insert(Notificacion notificacion)`

- **`insert_siempre_lanzaUnsupportedOperationExceptionYNoPersiste`** — Tipo: error. Verifica: `V-Notificacion-017`.
  - **Arrange:** `new Notificacion()` con datos comunes rellenos.
  - **Act:** `service.insert(notificacion)`.
  - **Assert:** lanza `UnsupportedOperationException` cuyo mensaje es el mismo que el de `validateInsert` (texto de V-Notificacion-017, ver Notas); `verify(notificacionRepository, never()).save(any())`; `modelServiceFactory` sin interacciones.

### Método: `public Notificacion update(Notificacion nueva, Notificacion original)`

- **`update_originalCorreo_delegaEnElServicioDelCorreoYPropagaSuExcepcion`** — Tipo: error. Verifica: `V-Notificacion-012`.
  - **Arrange:** `original = new Correo()` (id 100), `nueva = new Correo()`; `correoService.update(nueva, original)` lanza `UnsupportedOperationException("El correo es inmutable tras su creación.")`.
  - **Act:** `service.update(nueva, original)`.
  - **Assert:** lanza `UnsupportedOperationException` con ese mensaje; `verify(modelServiceFactory).resolve(Correo.class)`; `verify(correoService).update(nueva, original)`; `notificacionRepository` sin `save`.
- **`update_originalSms_delegaEnElServicioDelSms`** — Tipo: error. Verifica: `V-Notificacion-012`.
  - **Arrange:** `original = new Sms()`; `smsService.update(...)` lanza `UnsupportedOperationException("El SMS es inmutable tras su creación.")`.
  - **Act:** `service.update(nueva, original)`.
  - **Assert:** lanza con «El SMS es inmutable tras su creación.»; `verify(modelServiceFactory).resolve(Sms.class)`.
- **`update_claseQueNoEsUnCanal_lanzaIllegalStateException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `original = new Notificacion()`; `modelServiceFactory.resolve(Notificacion.class)` → un `ModelService` mock que **no** es `NotificacionCanalService`.
  - **Act:** `service.update(nueva, original)`.
  - **Assert:** lanza `IllegalStateException`.

### Método: `public void remove(Notificacion notificacion)`

- **`remove_correo_delegaEnElServicioDelCorreo`** — Tipo: error. Verifica: `V-Notificacion-013`.
  - **Arrange:** `new Correo()`; `correoService.remove(correo)` lanza `UnsupportedOperationException("Los correos no se pueden borrar.")`.
  - **Act:** `service.remove(correo)`.
  - **Assert:** lanza con ese mensaje; `verify(correoService).remove(correo)`; `notificacionRepository` sin `remove`.

### Método: `public Correo createCorreo()`

- **`createCorreo_devuelveUnCorreoNuevoConTipoCorreoSinInsertarlo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.createCorreo()`.
  - **Assert:** devuelve un `Correo` con `id` nulo y `tipoNotificacion = CORREO`; `notificacionRepository` sin interacciones; `modelServiceFactory` sin interacciones (no lo inserta ni resuelve servicios).
- **`createCorreo_dosLlamadas_devuelvenInstanciasDistintas`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** dos llamadas a `createCorreo()`.
  - **Assert:** `assertNotSame`.

### Método: `public Sms createSms()`

- **`createSms_devuelveUnSmsNuevoConTipoSmsSinInsertarlo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.createSms()`.
  - **Assert:** devuelve un `Sms` con `id` nulo y `tipoNotificacion = SMS`; `notificacionRepository` y `modelServiceFactory` sin interacciones.

### Método: `public Optional<BusinessMessages> validateInsert(Notificacion notificacion)`

- **`validateInsert_siempre_rechazaElAltaGenerica`** — Tipo: error. Verifica: `V-Notificacion-017`.
  - **Arrange:** `new Notificacion()` con todos los datos comunes válidos.
  - **Act:** `service.validateInsert(notificacion)`.
  - **Assert:** presente, con un único mensaje no vacío (el texto de V-Notificacion-017) que pasa por `I18n.get`.
- **`validateInsert_notificacionNula_tambienRechaza`** — Tipo: borde. Verifica: `V-Notificacion-017`.
  - **Arrange:** `notificacion = null`.
  - **Act:** `validateInsert(null)`.
  - **Assert:** presente con el mismo mensaje (la regla no depende de los datos).

### Método: `public Optional<BusinessMessages> validateUpdate(Notificacion nueva, Notificacion original)`

- **`validateUpdate_originalCorreo_devuelveLoQueDiceElServicioDelCorreo`** — Tipo: error. Verifica: `V-Notificacion-012`.
  - **Arrange:** `original = new Correo()`; `correoService.validateUpdate(nueva, original)` → `Optional.of(BusinessMessages.single("El correo es inmutable tras su creación."))`.
  - **Act:** `service.validateUpdate(nueva, original)`.
  - **Assert:** devuelve ese mismo `Optional` (`assertSame` del contenido); `verify(correoService).validateUpdate(nueva, original)`.
- **`validateUpdate_originalSms_resuelveElServicioDelSms`** — Tipo: error. Verifica: `V-Notificacion-012`.
  - **Arrange:** `original = new Sms()`; `smsService.validateUpdate(...)` → mensaje «El SMS es inmutable tras su creación.».
  - **Act:** `validateUpdate(nueva, original)`.
  - **Assert:** contiene «El SMS es inmutable tras su creación.»; `verify(modelServiceFactory).resolve(Sms.class)`.

### Método: `public Optional<BusinessMessages> validateRemove(Notificacion notificacion)`

- **`validateRemove_sms_devuelveLoQueDiceElServicioDelSms`** — Tipo: error. Verifica: `V-Notificacion-013`.
  - **Arrange:** `new Sms()`; `smsService.validateRemove(sms)` → mensaje «Los SMS no se pueden borrar.».
  - **Act:** `service.validateRemove(sms)`.
  - **Assert:** contiene «Los SMS no se pueden borrar.»; `verify(smsService).validateRemove(sms)`.

### Método: `public Optional<BusinessMessages> validateCreateCorreo()`

- **`validateCreateCorreo_siempre_devuelveOptionalVacio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.validateCreateCorreo()`.
  - **Assert:** `Optional.empty()`.

### Método: `public Optional<BusinessMessages> validateCreateSms()`

- **`validateCreateSms_siempre_devuelveOptionalVacio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.validateCreateSms()`.
  - **Assert:** `Optional.empty()`.

### Método: `public AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_denegaTodo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesInsert()`.
  - **Assert:** `false` para `name`, `dniDestinatario`, `centro`, `tipoNotificacion` y `estado`.

### Método: `public AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_denegaTodo`** — Tipo: happy. Verifica: `V-Notificacion-012`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesUpdate()`.
  - **Assert:** `false` para `name`, `centro` y `estado`.

### Método: `public AllowProperties allowPropertiesRemove()`

- **`allowPropertiesRemove_denegaTodo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `service.allowPropertiesRemove()`.
  - **Assert:** `false` para `name`, `centro` y `estado`.

---

## Clase: `com.educaflow.subsystem.notificaciones.controller.NotificacionController`  —  controlador

**Responsabilidad:** abrir en popup el form del canal de una fila de los tres listados (polimórfico, sin `if` por tipo), continuar el alta tras elegir canal, y mostrar el tipo en el `onNew` del form de alta.
**Colaboradores a mockear:** `ActionRequest` (mock; `getData()` → mapa con `context` que contiene `_model`, `id` y `tipoNotificacion` según el test, y `getContext()` → `null`, con lo que `ActionRequestHelper` usa el mapa crudo; si la implementación lee `tipoNotificacion` con `actionRequest.getContext()`, se programa en su lugar un `Context` mock con `get("tipoNotificacion")`), `ActionResponse` (mock; `ArgumentCaptor<Map>` sobre `setView`), `I18n` (identidad). Sin repositorios ni servicios: el controlador no carga filas (si lo hiciera, el test fallaría al no haber JPA).
**Origen diseño:** Paso 8 — `abrirTodas`, `abrirDelCentro`, `abrirRecibida`, `continuarAlta`, `prepararAlta`; privados `abrirEnPopup`, `tipoDelContexto`, `nombreDelForm`; usa `ActionResponseHelper.doResponseViewFormEnPopup` (real).

### Método: `public void abrirTodas(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`abrirTodas_filaCorreo_abreElFormMainDelCorreoEnPopupConSuId`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** contexto `_model` = FQN de `Notificacion`, `id` = 7, `tipoNotificacion` = `"CORREO"`.
  - **Act:** `controller.abrirTodas(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setView(captor)`; la vista tiene `model` = FQN de `Correo`, un único form `subsysNotificaciones.Main@Correo-form`, `params` con `popup` = `"true"` y `popup-save` = `"false"`, y `context._showRecord` = 7.
- **`abrirTodas_filaSms_abreElFormMainDelSms`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `tipoNotificacion` = `"SMS"`, `id` = 8.
  - **Act:** `abrirTodas(...)`.
  - **Assert:** `model` = FQN de `Sms`, form `subsysNotificaciones.Main@Sms-form`, `_showRecord` = 8.
- **`abrirTodas_contextoSinTipo_lanzaIllegalStateException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** contexto sin `tipoNotificacion`.
  - **Act:** `abrirTodas(...)`.
  - **Assert:** lanza `IllegalStateException`; `verify(actionResponse, never()).setView(any())`.
- **`abrirTodas_tipoDesconocido_lanzaIllegalArgumentException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `tipoNotificacion` = `"FAX"`.
  - **Act:** `abrirTodas(...)`.
  - **Assert:** lanza `IllegalArgumentException` (de `TipoNotificacion.valueOf`); sin `setView`.

### Método: `public void abrirDelCentro(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`abrirDelCentro_filaCorreo_abreElFormCentroDelCorreo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `tipoNotificacion` = `"CORREO"`, `id` = 7.
  - **Act:** `controller.abrirDelCentro(...)`.
  - **Assert:** form `subsysNotificaciones.Centro@Correo-form`, `model` `Correo`, `_showRecord` = 7, `popup` = `"true"`.
- **`abrirDelCentro_filaSms_abreElFormCentroDelSms`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `tipoNotificacion` = `"SMS"`.
  - **Act:** `abrirDelCentro(...)`.
  - **Assert:** form `subsysNotificaciones.Centro@Sms-form`.

### Método: `public void abrirRecibida(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`abrirRecibida_filaCorreo_abreElFormMisDelCorreo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `tipoNotificacion` = `"CORREO"`, `id` = 7.
  - **Act:** `controller.abrirRecibida(...)`.
  - **Assert:** form `subsysNotificaciones.Mis@Correo-form`, `_showRecord` = 7.
- **`abrirRecibida_filaSms_abreElFormMisDelSms`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `tipoNotificacion` = `"SMS"`.
  - **Act:** `abrirRecibida(...)`.
  - **Assert:** form `subsysNotificaciones.Mis@Sms-form`.

### Método: `public void continuarAlta(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`continuarAlta_eligeCorreo_abreElFormMainDelCorreoEnAlta`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** contexto del form de elección: `_model` = FQN de `Notificacion`, sin `id`, `tipoNotificacion` = `"CORREO"`.
  - **Act:** `controller.continuarAlta(...)`.
  - **Assert:** `setView` con `model` `Correo`, form `subsysNotificaciones.Main@Correo-form`, `popup` = `"true"`, `popup-save` = `"false"` y **sin** `_showRecord` en el contexto (alta); título «Notificación».
- **`continuarAlta_eligeSms_abreElFormMainDelSmsEnAlta`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `tipoNotificacion` = `"SMS"`.
  - **Act:** `continuarAlta(...)`.
  - **Assert:** form `subsysNotificaciones.Main@Sms-form`, sin `_showRecord`.
- **`continuarAlta_sinCanalElegido_lanzaIllegalStateException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** contexto con `tipoNotificacion` ausente.
  - **Act:** `continuarAlta(...)`.
  - **Assert:** lanza `IllegalStateException`; sin `setView`.

### Método: `public void prepararAlta(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`prepararAlta_formDelCorreo_muestraTipoCorreo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** contexto `_model` = FQN de `Correo`.
  - **Act:** `controller.prepararAlta(...)`.
  - **Assert:** `verify(actionResponse).setValue("tipoNotificacion", TipoNotificacion.CORREO)`.
- **`prepararAlta_formDelSms_muestraTipoSms`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `_model` = FQN de `Sms`.
  - **Act:** `prepararAlta(...)`.
  - **Assert:** `verify(actionResponse).setValue("tipoNotificacion", TipoNotificacion.SMS)`.
- **`prepararAlta_modeloQueNoEsUnCanal_lanzaIllegalStateException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `_model` = FQN de `Notificacion`.
  - **Act:** `prepararAlta(...)`.
  - **Assert:** lanza `IllegalStateException` (de `TipoNotificacion.deClase`); sin `setValue`.

---

## Clase: `com.educaflow.subsystem.notificaciones.controller.CorreoController`  —  controlador

**Responsabilidad:** reenvío del correo (movido de `correos/controller`): valida y reenvía vía `CorreoService` con la whitelist `allowPropertiesReenviar`, avisa al usuario y recarga el form del popup (el listado de debajo lo refresca `remote-refreshTab-action`, no el controlador).
**Colaboradores a mockear:** `ModelServiceFactory` (mock por reflexión; `resolve(Correo.class)` → `CorreoService` mock; `allowPropertiesReenviar()` → `AllowProperties.createDenyAllProperties()`), `ActionRequest` (mock; `getData()` → `context` con `_model` = FQN de `Correo` e `id` = 100 y, en algún test, campos de servidor), `ActionResponse` (mock), `JpaRepository` (`mockStatic`: `JpaRepository.of(Correo.class)` → repo mock; `find(100L)` → correo `FALLIDO` de `centroA`), `I18n` (identidad). Mismo montaje que el `SmsControllerTest` actual.
**Origen diseño:** Paso 8 — `@CallMethod @Transactional reenviar`, `@CallMethod validateReenviar` (mismo comportamiento que hoy + `setReload(true)` en `reenviar`, y sin `setSignal("refresh-tab")`, que pasa a `DefaultModelController.refreshTab`). El `@Transactional` es declarativo (lo aplica el interceptor de Guice) y no se ejerce en un test unitario.

### Método: `public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`reenviar_correoFallido_delegaAvisaYRecargaElPopupSinRefrescarLaPestana`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `correoService.reenviar(any(), any())` → el correo de BD.
  - **Act:** `controller.reenviar(actionRequest, actionResponse)`.
  - **Assert:** `verify(correoService).reenviar(any(), any())`; `verify(actionResponse).setNotify("El reenvío del correo se ha puesto en marcha.")`; `verify(actionResponse).setReload(true)`; `verify(actionResponse, never()).setSignal(anyString(), any())` (el refresco del listado es de `remote-refreshTab-action`).
- **`reenviar_pasaElOriginalDeBdYUnaEntidadFiltradaPorLaWhitelist`** — Tipo: borde. Verifica: `V-Notificacion-016`.
  - **Arrange:** contexto con `estado` = `"ENVIADO"` y `centro` dictados por el cliente.
  - **Act:** `controller.reenviar(...)`.
  - **Assert:** capturados los argumentos de `correoService.reenviar(entidad, original)`: `original` lleva los datos del correo de `find(100L)` (`id` 100, estado `FALLIDO`, `centroA`; es un clon, así que se comparan valores, no la instancia) y `entidad` no trae el `estado` ni el `centro` del cliente; `verify(correoService).allowPropertiesReenviar()`.
- **`reenviar_servicioLanzaValidationException_noAvisaNiRecarga`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `correoService.reenviar(any(), any())` lanza `ValidationException("Solo se pueden reenviar correos que han fallado")`.
  - **Act:** `controller.reenviar(...)`.
  - **Assert:** propaga `ValidationException`; `verify(actionResponse, never()).setNotify(anyString())`, `never().setReload(anyBoolean())`, `never().setSignal(anyString(), any())`.

### Método: `public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`validateReenviar_servicioSinMensajes_noDevuelveError`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `correoService.validateReenviar(any(), any())` → `Optional.empty()`.
  - **Act:** `controller.validateReenviar(...)`.
  - **Assert:** `verify(actionResponse, never()).setError(anyString())`; `verify(correoService).allowPropertiesReenviar()`.
- **`validateReenviar_servicioConMensajes_losDevuelveComoError`** — Tipo: error. Verifica: `V-Notificacion-014`.
  - **Arrange:** `validateReenviar` → `Optional.of(BusinessMessages.single("Solo se pueden reenviar correos que han fallado"))`.
  - **Act:** `controller.validateReenviar(...)`.
  - **Assert:** `verify(actionResponse).setError(captor)` y el HTML capturado contiene «Solo se pueden reenviar correos que han fallado».

---

## Clase: `com.educaflow.subsystem.notificaciones.controller.SmsController`  —  controlador

**Responsabilidad:** reenvío del SMS (movido de `sms/controller`), idéntico al del correo con su aviso y su `SmsService`.
**Colaboradores a mockear:** como en `CorreoController`, con `Sms`/`SmsService`; el SMS de BD: `id` 100, `FALLIDO`, teléfono `+34600111222`, `centroA` (montaje del `SmsControllerTest` actual, con `EstadoNotificacion`).
**Origen diseño:** Paso 8 — `reenviar`, `validateReenviar` (+ `setReload(true)`, sin `setSignal("refresh-tab")`).

### Método: `public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`reenviar_smsFallido_delegaAvisaYRecargaElPopupSinRefrescarLaPestana`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `smsService.reenviar(any(), any())` → el SMS de BD.
  - **Act:** `controller.reenviar(...)`.
  - **Assert:** `verify(smsService).reenviar(any(), any())`; `setNotify("El reenvío del SMS se ha puesto en marcha.")`; `setReload(true)`; `never().setSignal(anyString(), any())`.
- **`reenviar_contextoConCamposDictados_noLlegaNingunoAlServicio`** — Tipo: borde. Verifica: `V-Notificacion-016`.
  - **Arrange:** contexto con campos del envío dictados por el cliente: `estado` = `"ENVIADO"`, `numeroReintentos` = `7`.
  - **Act:** `controller.reenviar(...)`.
  - **Assert:** la `entidad` capturada no trae ni ese estado ni ese `numeroReintentos` (la whitelist deny-all del reenvío los descarta); `verify(smsService).allowPropertiesReenviar()`.
- **`reenviar_servicioLanzaValidationException_noAvisaNiRecarga`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `smsService.reenviar` lanza `ValidationException("Solo se pueden reenviar SMS que han fallado")`.
  - **Act:** `controller.reenviar(...)`.
  - **Assert:** propaga; ni `setNotify`, ni `setReload`, ni `setSignal`.

### Método: `public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`validateReenviar_servicioSinMensajes_noDevuelveError`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `smsService.validateReenviar(any(), any())` → `Optional.empty()`.
  - **Act:** `controller.validateReenviar(...)`.
  - **Assert:** `never().setError(anyString())`.
- **`validateReenviar_servicioConMensajes_losDevuelveComoError`** — Tipo: error. Verifica: `V-Notificacion-014`.
  - **Arrange:** `validateReenviar` → mensaje «Solo se pueden reenviar SMS que han fallado».
  - **Act:** `controller.validateReenviar(...)`.
  - **Assert:** `setError` con un HTML que contiene ese mensaje.
- **`validateReenviar_contextoSinId_pasaOriginalNuloAlServicio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** contexto sin `id`; `smsJpaRepository.create(null)` → `new Sms()`; `smsService.validateReenviar(any(), isNull())` → `Optional.empty()`.
  - **Act:** `controller.validateReenviar(...)`.
  - **Assert:** `verify(smsService).validateReenviar(any(), isNull())`.

---

## Clase: `com.educaflow.subsystem.notificaciones.module.NotificacionesModule`  —  helper

**Responsabilidad:** módulo Guice del subsistema; sus dos `@Provides` construyen el `MailSender` y el `SmsSender` desde `AppSettings` (mismo cuerpo que los antiguos `MailSenderProvider` y `SmsSenderProvider`, cuyos tests se sustituyen por estos).
**Colaboradores a mockear:** `AppSettings` (`mockStatic`: `AppSettings.get()` → `AppSettings` mock con las claves de cada test). Se invocan los métodos `@Provides` directamente sobre `new NotificacionesModule()` (sin crear inyector).
**Origen diseño:** Paso 9 — `@Provides MailSender mailSender()`, `@Provides SmsSender smsSender()`. Sin regla V/R/CC propia: la matriz ubica R-Notificacion-003 en `enviar`/`enviarPorCanal`; estos tests solo fijan que la falta de configuración llega como `RuntimeException` (la que `enviar` convierte en FALLIDO).

### Método: `MailSender mailSender()`

- **`mailSender_credencialesGmailConfiguradas_devuelveElEmisor`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `mail.credentials.gmail.api.clientId`, `.projectId`, `.clientSecret` y `.refreshToken` con valores no vacíos.
  - **Act:** `module.mailSender()`.
  - **Assert:** devuelve un `MailSender` no nulo.
- **`mailSender_clientIdEnBlanco_lanzaIllegalArgumentException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `clientId` = `""`, el resto configurado (lenient).
  - **Act:** `module.mailSender()`.
  - **Assert:** lanza `IllegalArgumentException` (es la `RuntimeException` que, dentro de `enviar`, deja el correo FALLIDO: RN-Correo-003).
- **`mailSender_refreshTokenNoConfigurado_lanzaNullPointerException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `refreshToken` → `null`, el resto configurado.
  - **Act:** `module.mailSender()`.
  - **Assert:** lanza `NullPointerException`.

### Método: `SmsSender smsSender()`

- **`smsSender_credencialesYEmisorConfigurados_devuelveElEmisor`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `sms.credentials.twilio.accountSid`, `sms.credentials.twilio.authToken` y `sms.twilio.from` configurados.
  - **Act:** `module.smsSender()`.
  - **Assert:** no nulo.
- **`smsSender_sinAccountSid_lanzaIllegalArgumentException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `accountSid` = `""`.
  - **Act:** `module.smsSender()`.
  - **Assert:** `IllegalArgumentException`.
- **`smsSender_sinAuthToken_lanzaNullPointerException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `authToken` → `null`.
  - **Act:** `module.smsSender()`.
  - **Assert:** `NullPointerException`.
- **`smsSender_sinNumeroEmisor_lanzaIllegalArgumentException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `sms.twilio.from` = `""`.
  - **Act:** `module.smsSender()`.
  - **Assert:** `IllegalArgumentException`.

---

## Clase: `com.educaflow.subsystem.notificaciones.db.TipoNotificacion`  —  helper

**Responsabilidad:** enum generado por Axelor con lógica en su `extra-code-model`: único sitio que sabe qué clase de notificación es cada canal (D2).
**Colaboradores a mockear:** ninguno.
**Origen diseño:** Paso 1 (`Notificacion.xml`) — `getClaseNotificacion()`, `static deClase(Class<?>)`; lo usan R-Notificacion-001, `createCorreo`/`createSms` y `NotificacionController`.

### Método: `public Class<? extends Notificacion> getClaseNotificacion()`

- **`getClaseNotificacion_cadaCanal_devuelveSuClase`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `CORREO.getClaseNotificacion()` y `SMS.getClaseNotificacion()`.
  - **Assert:** `Correo.class` y `Sms.class`.
- **`getClaseNotificacion_todosLosValores_idaYVueltaConDeClase`** — Tipo: borde. Verifica: `R-Notificacion-001`.
  - **Arrange:** —.
  - **Act:** para cada `tipo` de `TipoNotificacion.values()`, `deClase(tipo.getClaseNotificacion())`.
  - **Assert:** devuelve el propio `tipo` (garantiza que un canal nuevo sin su `case` no compila o no pasa).

### Método: `public static TipoNotificacion deClase(Class<?> clase)`

- **`deClase_correo_devuelveCorreo`** — Tipo: happy. Verifica: `R-Notificacion-001`.
  - **Arrange:** —.
  - **Act:** `TipoNotificacion.deClase(Correo.class)`.
  - **Assert:** `CORREO`.
- **`deClase_sms_devuelveSms`** — Tipo: happy. Verifica: `R-Notificacion-001`.
  - **Arrange:** —.
  - **Act:** `deClase(Sms.class)`.
  - **Assert:** `SMS`.
- **`deClase_notificacionGenerica_lanzaIllegalStateException`** — Tipo: error. Verifica: `R-Notificacion-001`.
  - **Arrange:** —.
  - **Act:** `deClase(Notificacion.class)`.
  - **Assert:** `IllegalStateException`.
- **`deClase_claseAjena_lanzaIllegalStateException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `deClase(String.class)`.
  - **Assert:** `IllegalStateException`.

---

## Clase: `com.educaflow.subsystem.notificaciones.db.Notificacion`  —  helper

**Responsabilidad:** entidad base generada; su única lógica propia es el campo función `destino`, que en la base rechaza el cálculo (una notificación sin canal no tiene destino).
**Colaboradores a mockear:** ninguno (`new Notificacion()`).
**Origen diseño:** Paso 1 (`Notificacion.xml`, campo función `destino`, D2); CC destino.

### Método: `public String getDestino()`

- **`getDestino_notificacionGenerica_lanzaIllegalStateException`** — Tipo: error. Verifica: `CC-Notificacion-002`.
  - **Arrange:** `new Notificacion()`.
  - **Act:** `notificacion.getDestino()`.
  - **Assert:** `JUnitHelper.assertThrowsCause(IllegalStateException.class, ...)` (no devuelve `null`).

---

## Clase: `com.educaflow.subsystem.notificaciones.db.Correo`  —  helper

**Responsabilidad:** entidad generada; sobrescribe `computeDestino()` para que el destino sea el «para».
**Colaboradores a mockear:** ninguno.
**Origen diseño:** Paso 1 (`Correo.xml`, `extra-code-model`); CC destino.

### Método: `public String getDestino()`

- **`getDestino_correo_devuelveElPara`** — Tipo: happy. Verifica: `CC-Correo-001`, `CC-Notificacion-002`.
  - **Arrange:** `new Correo()` con `para = "a@x.com"`.
  - **Act:** `correo.getDestino()`.
  - **Assert:** «a@x.com».
- **`getDestino_valorAsignadoPorElCliente_seIgnora`** — Tipo: borde. Verifica: `CC-Correo-001`.
  - **Arrange:** `para = "a@x.com"`; `setDestino("falso@x.com")`.
  - **Act:** `getDestino()`.
  - **Assert:** «a@x.com».
- **`getDestino_sinPara_devuelveNull`** — Tipo: borde. Verifica: `CC-Correo-001`.
  - **Arrange:** `new Correo()` sin `para`.
  - **Act:** `getDestino()`.
  - **Assert:** `null`, sin excepción.

---

## Clase: `com.educaflow.subsystem.notificaciones.db.Sms`  —  helper

**Responsabilidad:** entidad generada; sobrescribe `computeDestino()` para que el destino sea el teléfono.
**Colaboradores a mockear:** ninguno.
**Origen diseño:** Paso 1 (`Sms.xml`, `extra-code-model`); CC destino.

### Método: `public String getDestino()`

- **`getDestino_sms_devuelveElTelefono`** — Tipo: happy. Verifica: `CC-Sms-001`, `CC-Notificacion-002`.
  - **Arrange:** `new Sms()` con `telefono = "+34600111222"`.
  - **Act:** `sms.getDestino()`.
  - **Assert:** «+34600111222».
- **`getDestino_valorAsignadoPorElCliente_seIgnora`** — Tipo: borde. Verifica: `CC-Sms-001`.
  - **Arrange:** `telefono = "+34600111222"`; `setDestino("+34699999999")`.
  - **Act:** `getDestino()`.
  - **Assert:** «+34600111222».

---

## Clase: `com.educaflow.base.infrastructure.controller.DefaultModelController`  —  controlador

**Responsabilidad (cambio):** nuevo `refreshTab`, dueño único de refrescar el listado de la pestaña desde un form en popup (D3). Solo se describe lo nuevo; `validateSave`/`validateDelete` no cambian.
**Colaboradores a mockear:** `ActionRequest` (mock, sin programar: el método no lo lee), `ActionResponse` (mock). Clase bajo test real: `new DefaultModelController()`; sus campos `@Inject` (los que usan `validateSave`/`validateDelete`) se dejan sin rellenar porque `refreshTab` no los usa. Hoy no hay test de esta clase: es una clase de test nueva, `DefaultModelControllerTest`, en `com.educaflow.base.infrastructure.controller`.
**Origen diseño:** Paso 8 — `refreshTab` (acción global `remote-refreshTab-action`).

### Método: `public void refreshTab(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`refreshTab_pideRefrescarLaPestanaSinTocarElPopup`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** —.
  - **Act:** `controller.refreshTab(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setSignal("refresh-tab", null)`; `verify(actionResponse, never()).setView(any())`; `verify(actionResponse, never()).setReload(true)` (en el alta el popup se cierra a continuación con `close`; en el reenvío la recarga ya la pidió `reenviar`).

---

## Clase: `com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper`  —  helper

**Responsabilidad (cambio):** nuevo `doResponseViewFormEnPopup` y extracción de la parte común de `doResponseViewForm` a `vistaForm` (D3). Solo se describe lo nuevo/cambiado, que se añade al `ActionResponseHelperTest` existente; sus tests actuales de `doResponseBusinessMessagesAsError` se conservan.
**Colaboradores a mockear:** `ActionResponse` (mock; `ArgumentCaptor<Map>` sobre `setView`); `I18n` con `mockStatic` identidad solo si el builder de Axelor lo requiere. Clase bajo test real: `new ActionResponseHelper(actionResponse)`.
**Origen diseño:** Paso 8 — `doResponseViewFormEnPopup`, `doResponseViewForm` (refactor), privado `vistaForm`.

### Método: `public void doResponseViewFormEnPopup(String viewName, Class<? extends Model> modelClass, Long id, String title)`

- **`doResponseViewFormEnPopup_conId_abreElFormEnPopupMostrandoElRegistro`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `viewName = "subsysNotificaciones.Main@Correo-form"`, `Correo.class`, `id = 7L`, `title = "Notificación"`.
  - **Act:** `helper.doResponseViewFormEnPopup(...)`.
  - **Assert:** la vista capturada tiene título «Notificación», `model` FQN de `Correo`, un único form con ese nombre; `params`: `popup` = `"true"`, `popup-save` = `"false"`, `forceEdit` = `"true"`, `show-confirm` = `"false"`, `show-toolbar` = `"false"`; `context._showRecord` = 7; **sin** `forceTitle`, `_profile` ni `newEntity`.
- **`doResponseViewFormEnPopup_sinId_abreElFormEnAlta`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** ídem con `id = null`.
  - **Act:** `doResponseViewFormEnPopup(...)`.
  - **Assert:** mismos `params` de popup; el contexto no contiene `_showRecord`.

### Método: `public void doResponseViewForm(String viewName, Class<? extends Model> modelClass, Model entity, String title, String profile)`

- **`doResponseViewForm_entidadConId_conservaElComportamientoAnterior`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** entidad `Correo` con `id = 5L`, `profile = "perfil"`.
  - **Act:** `helper.doResponseViewForm(...)`.
  - **Assert:** `params` `forceEdit` = `"true"`, `forceTitle` = `"true"`, `show-confirm` = `"false"`, `show-toolbar` = `"false"`; `context._profile` = «perfil» y `_showRecord` = 5; **sin** `popup` ni `popup-save` (regresión del refactor).
- **`doResponseViewForm_entidadSinId_pasaLaEntidadNueva`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** entidad sin id.
  - **Act:** `doResponseViewForm(...)`.
  - **Assert:** `context.newEntity` es la entidad; sin `_showRecord`.

---

## Clase: `com.educaflow.base.util.MetaFileUtil`  —  helper

**Responsabilidad (cambio):** nuevo `fileSizeOnDisk(MetaFile)`: tamaño real del fichero en disco (no el `fileSize` que dicta el cliente). El resto de la clase no cambia. Hoy no hay test de esta clase: es una clase de test nueva, `MetaFileUtilTest`, en `com.educaflow.base.util`.
**Colaboradores a mockear:** `MetaFiles` (`mockStatic`: `getPath(metaFile)` → un `Path` en un `@TempDir`).
**Origen diseño:** Paso 4 — `public static long fileSizeOnDisk(MetaFile metaFile)`; utilidad de soporte (sin regla propia) que usan V-Adjunto-008/012 y V-Correo-011, cuyas reglas se verifican en `AdjuntoServiceImpl.validateInsert` y `CorreoServiceImpl.validateDatosDelCanal`.

### Método: `public static long fileSizeOnDisk(MetaFile metaFile)`

- **`fileSizeOnDisk_ficheroExistente_devuelveSuTamanoReal`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** fichero temporal con 5 bytes; `MetaFile` con `fileSize = 1L`; `MetaFiles.getPath(metaFile)` → ese `Path`.
  - **Act:** `MetaFileUtil.fileSizeOnDisk(metaFile)`.
  - **Assert:** 5.
- **`fileSizeOnDisk_ficheroVacio_devuelveCero`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** fichero temporal vacío.
  - **Act:** `fileSizeOnDisk(metaFile)`.
  - **Assert:** 0.
- **`fileSizeOnDisk_ficheroInexistente_lanzaUncheckedIOException`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `getPath` → ruta inexistente dentro del `@TempDir`.
  - **Act:** `fileSizeOnDisk(metaFile)`.
  - **Assert:** `UncheckedIOException` con causa `IOException`.

---

## Clase: `com.educaflow.subsystem.security.service.impl.MenuSecurityServiceImpl`  —  servicio

**Responsabilidad (cambio):** el `case` de `correos-delCentro-menuitem` se sustituye por `notificaciones-delCentro-menuitem`, que pregunta a `GestorNotificacionesUtil.esGestorEnAlgunCentro(user)`. Se modifica `MenuSecurityServiceImplTest`: se **quitan** `correosDelCentroEsVisibleParaSupervisor`, `correosDelCentroEsVisibleParaAdministrativo` y `correosDelCentroNoEsVisibleSinSerSupervisorNiAdministrativo`, y se añaden los de abajo; el resto se conserva.
**Colaboradores a mockear:** los del test actual (`PerfilesUsuarioService` `@Mock`, `User` `@Mock`, `SecurityUtil` `mockStatic` con `getUser()` → el `user` mock) más `GestorNotificacionesUtil` (`mockStatic`).
**Origen diseño:** Paso 12 — `isVisible(String)`.

### Método: `public boolean isVisible(String menuName)`

- **`isVisible_notificacionesDelCentroUsuarioGestor_esVisible`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `usuario(false, false)`; `GestorNotificacionesUtil.esGestorEnAlgunCentro(user)` → `true`.
  - **Act:** `service.isVisible("notificaciones-delCentro-menuitem")`.
  - **Assert:** `true`.
- **`isVisible_notificacionesDelCentroUsuarioNoGestorAunqueSeaAdminYSupervisor_noEsVisible`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `usuario(true, true)` (admin y supervisor, para comprobar que solo cuenta el dueño de la clasificación); `esGestorEnAlgunCentro(user)` → `false`.
  - **Act:** `isVisible("notificaciones-delCentro-menuitem")`.
  - **Assert:** `false`.
- **`isVisible_notificacionesDelCentroSinUsuario_noEsVisibleNiPreguntaAlGestor`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `SecurityUtil.getUser()` → `null`.
  - **Act:** `isVisible("notificaciones-delCentro-menuitem")`.
  - **Assert:** `false`; `GestorNotificacionesUtil` sin invocaciones.

---

## Clase: `com.educaflow.tramites.util.verificacion.VerificacionHelper`  —  helper

**Responsabilidad (cambio):** el aviso de subsanación parte de `NotificacionService.createCorreo()`, rellena además el motivo y liga el correo al estado actual del expediente. Se modifica `VerificacionHelperTest`: se adapta su montaje (de `CorreoService` a `NotificacionService` + `CorreoService`) y sus dos tests actuales pasan a ser los dos primeros de abajo (`avisarDeSubsanacion_elSolicitanteTieneCorreo_creaElCorreoConElTextoDeLaSubsanacion` → `…_solicitanteConCorreo_creaElCorreoDesdeLaFactoriaConMotivoYEstadoActual`; `avisarDeSubsanacion_noSePuedeEscribirAlSolicitante_noCreaNingunCorreoYNoFalla` → `…_correoNoValido_noLoCreaYDevuelveFalse`), ampliados; se añade el tercero.
**Colaboradores a mockear:** `ModelServiceFactory` (`@Mock`; `resolve(Notificacion.class)` → `NotificacionService` mock, `resolve(Correo.class)` → `CorreoService` mock), `NotificacionService` (`createCorreo()` → `new Correo()` con `tipoNotificacion = CORREO`), `CorreoService`, `I18n` (identidad). Expediente `new PruebaV1()` con solicitante, `centro`, `numeroExpediente = "00007/2026"`, `name = "Prueba V1"` y tres `HistorialEstado` con `fecha` T−2h, T−1h (el más reciente) y T−3h, en ese orden en la lista.
**Origen diseño:** Paso 7 — `avisarDeSubsanacion`, privados `crearCorreoSubsanacion`, `estadoActual`; R-Correo-002.

### Método: `public boolean avisarDeSubsanacion(Expediente expediente, String textoSubsanacion)`

- **`avisarDeSubsanacion_solicitanteConCorreo_creaElCorreoDesdeLaFactoriaConMotivoYEstadoActual`** — Tipo: happy. Verifica: `R-Correo-002`.
  - **Arrange:** `correoService.validateInsert(any(Correo.class))` → `Optional.empty()`.
  - **Act:** `verificacionHelper.avisarDeSubsanacion(expediente, TEXTO_SUBSANACION)`.
  - **Assert:** devuelve `true`; `verify(notificacionService).createCorreo()`; el `Correo` capturado en `correoService.insert` es la instancia devuelta por `createCorreo` (`assertSame`) y tiene `tipoNotificacion = CORREO`, `para` «ana@example.com», DNI «12345678Z», nombre «Ana», apellidos «García López», `centro` el del expediente, `name` «Subsanación del expediente 00007/2026», `historialEstado` el de fecha T−1h (`assertSame`), asunto que contiene «00007/2026» y cuerpo que contiene el texto de subsanación y «Prueba V1».
- **`avisarDeSubsanacion_correoNoValido_noLoCreaYDevuelveFalse`** — Tipo: error. Verifica: `R-Correo-002`.
  - **Arrange:** solicitante sin email; `validateInsert` → `Optional.of(BusinessMessages.single("Debe indicar al menos un destinatario en el «para»"))`.
  - **Act:** `avisarDeSubsanacion(expediente, TEXTO_SUBSANACION)`.
  - **Assert:** `false`; `verify(correoService, never()).insert(any())`; sin excepción.
- **`avisarDeSubsanacion_unSoloHistorial_loLiga`** — Tipo: borde. Verifica: `R-Correo-002`.
  - **Arrange:** el expediente con un único `HistorialEstado`; `validateInsert` → vacío.
  - **Act:** `avisarDeSubsanacion(...)`.
  - **Assert:** el correo insertado tiene ese `historialEstado`.

---

## Clase: `com.educaflow.subsystem.notificaciones.service.NotificacionCanalService` — sin lógica testable
**Motivo:** interfaz genérica (solo firmas).

## Clase: `com.educaflow.subsystem.notificaciones.service.NotificacionService` — sin lógica testable
**Motivo:** interfaz de servicio (solo firmas).

## Clase: `com.educaflow.subsystem.notificaciones.service.CorreoService` — sin lógica testable
**Motivo:** interfaz de servicio (solo firmas).

## Clase: `com.educaflow.subsystem.notificaciones.service.SmsService` — sin lógica testable
**Motivo:** interfaz de servicio sin métodos propios.

## Clase: `com.educaflow.subsystem.notificaciones.service.AdjuntoService` — sin lógica testable
**Motivo:** interfaz de servicio sin métodos propios.

## Clase: `com.educaflow.subsystem.notificaciones.db.Adjunto` — sin lógica testable
**Motivo:** POJO de dominio generado por Axelor, sin `extra-code-model`.

## Clase: `com.educaflow.subsystem.notificaciones.db.EstadoNotificacion` — sin lógica testable
**Motivo:** enum generado sin comportamiento.

## Clase: `com.educaflow.subsystem.notificaciones.db.repo.{Notificacion,Correo,Sms,Adjunto}Repository` — sin lógica testable
**Motivo:** repositorios generados por Axelor (el finder `findByEstado` se mockea en `CorreoServiceImpl`).

---

## Cobertura
- Clases con lógica descritas: 19 (`GestorNotificacionesUtil`, `NotificacionCanalServiceImpl` —ejercida a través de `CorreoServiceImpl`—, `CorreoServiceImpl`, `SmsServiceImpl`, `AdjuntoServiceImpl`, `NotificacionServiceImpl`, `NotificacionController`, `CorreoController`, `SmsController`, `NotificacionesModule`, `TipoNotificacion`, `Notificacion`, `Correo`, `Sms`, `DefaultModelController`, `ActionResponseHelper`, `MetaFileUtil`, `MenuSecurityServiceImpl`, `VerificacionHelper`).
- Clases omitidas (sin lógica): `NotificacionCanalService`, `NotificacionService`, `CorreoService`, `SmsService`, `AdjuntoService`, `Adjunto`, `EstadoNotificacion`, `NotificacionRepository`, `CorreoRepository`, `SmsRepository`, `AdjuntoRepository`.
- Reglas server-side cubiertas (`V`/`R`/`CC`):
  - `V-Notificacion-001`…`V-Notificacion-017` (la 016, garantía por construcción, mediante los tests de `allowPropertiesInsert/Reenviar`, `allowPropertiesDelCanal`, `insert` con campos dictados y los controladores de reenvío).
  - `V-Correo-001`…`V-Correo-011`.
  - `V-Sms-001`…`V-Sms-004`.
  - `V-Adjunto-001`…`V-Adjunto-013`.
  - `R-Notificacion-001`, `R-Notificacion-002`, `R-Notificacion-003`, `R-Notificacion-004`, `R-Correo-001`, `R-Correo-002`, `R-Sms-001`, `R-Adjunto-001`.
  - CC destino: `CC-Notificacion-002`, `CC-Correo-001`, `CC-Sms-001`; `CC-Notificacion-001` y `CC-Notificacion-004` dentro de `R-Notificacion-001`.
  - **No cubierta por unitarios**, a propósito: CC expediente `CC-Notificacion-003` — es una `formula` SQL de Axelor que solo existe en la consulta a BD; no hay nada que ejercer sin BD real (la cubre el E2E de los listados).
- Reglas solo-cliente excluidas (E2E en test-e2e-desc.md): `U-notificaciones-centro-001`…`U-notificaciones-centro-014`, `U-notificaciones-recibidas-001`, `U-notificaciones-recibidas-002`, `U-notificaciones-todas-001`…`U-notificaciones-todas-035`. Las que el diseño apoya en un método de servidor (`U-notificaciones-todas-003/022` en `NotificacionController.prepararAlta`; `U-notificaciones-centro-005/011` y `U-notificaciones-todas-011/031` en el `setNotify` de `CorreoController/SmsController.reenviar`) se ejercen en los tests de esos métodos con `Verifica: —`, pero la regla como tal se verifica en E2E.

## Notas y decisiones del test-unitarios
- **Base abstracta sin subclase de test:** `NotificacionCanalServiceImpl` se prueba a través de `CorreoServiceImpl` (canal real) para no inventar clases; los textos que difieren por canal se cubren en los `textosDelCanal` de cada canal. Dos tests del flujo común (`insert` y `enviar`) se repiten con `SmsServiceImpl` porque lo que la base resuelve por el canal real (`TipoNotificacion.deClase` de la clase de la instancia y el `model` del `find` con bloqueo) fallaría en silencio si se fijara a una clase.
- **Utilidades de soporte sin regla:** `NotificacionesModule` y `MetaFileUtil.fileSizeOnDisk` declaran `Verifica: —` porque la matriz de trazabilidad no las ubica en ninguna regla; las reglas que se apoyan en ellas (R-Notificacion-003, V-Adjunto-008/012, V-Correo-011) se verifican en los servicios que las aplican.
- **`@Transactional` de `CorreoController/SmsController.reenviar`:** declarativo; no se describe test unitario (lo ejercen los E2E de reenvío).
- **Texto de V-Notificacion-017:** la spec no le da mensaje (Origen spec: —) y el diseño solo dice que «transmite que una notificación se da de alta siempre como uno de sus canales». Los tests no fijan el literal: comprueban que el mensaje existe, pasa por `I18n.get` y es el mismo en `validateInsert` y en la excepción de `insert`.
- **Unidad «MB»:** 1 MB = 1024 × 1024 bytes, como el `TAMANO_MAXIMO_BYTES` actual del adjunto; los límites de 10 MB y 25 MB se prueban en el valor exacto y en +1 byte (o en una suma que lo supera).
- **`estadoActual` con el historial vacío:** el diseño no lo define (en la verificación siempre hay al menos un estado), así que no se describe ese caso.
- **Validación local del modal de adjunto** (`Main@Correo.Adjunto-Local-validateSave-action` como `<action-validate>`, Paso 10): es solo-cliente (`U-notificaciones-todas-017/018/020`), así que no tiene test unitario; las mismas reglas en servidor (V-Adjunto-002/003/009..012) se prueban en `AdjuntoServiceImpl.validateInsert`.
- **Refresco del listado** (`remote-refreshTab-action`, D3): el único Java es `DefaultModelController.refreshTab`; que los grupos `btnSave`/`btnReenviar` lo encadenen entre `save` y `close` es XML y lo verifican los tests de vistas (`VAR-7.2`/`VAR-7.3`, regenerados en el Paso 11) y los E2E, no estos unitarios.
- **Selector del «Estado del expediente»** (D8, `subsystem/expedientes/views/Ref-HistorialEstado.xml`): solo XML (`target-name`, `canSuggest`, `grid-view`/`form-view` y el `domain` por centro, U-notificaciones-todas-012/032), así que no añade clases ni métodos Java y no tiene test unitario. Lo que el servidor decide sobre ese campo sigue siendo V-Notificacion-011 en `NotificacionCanalServiceImpl.validateInsert`, con sus tests de arriba.
- **Tests nuevos frente a tests modificados:** son clases de test nuevas las de `subsystem/notificaciones` (sustituyen a las de `correos` y `sms`, que se borran en el Paso 1), `DefaultModelControllerTest` y `MetaFileUtilTest`. Se amplían las que ya existen: `ActionResponseHelperTest`, `MenuSecurityServiceImplTest` y `VerificacionHelperTest`.
- **`tipoNotificacion` en el contexto del controlador:** el diseño dice «String del context»; el `Arrange` lo pone en el `context` de `getData()` y, si la implementación usa `getContext()`, también en un `Context` mockeado.
