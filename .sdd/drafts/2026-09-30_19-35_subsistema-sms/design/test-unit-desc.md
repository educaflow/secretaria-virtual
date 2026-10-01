# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Aserciones de `org.junit.jupiter.api.Assertions` (`assertEquals`, `assertThrows`, `assertTrue`, `assertNull`, `assertSame`, `assertInstanceOf`, `assertDoesNotThrow`), nunca AssertJ.
- Los mensajes de negocio se comprueban **literalmente** con el texto del spec (`entity-Sms.md`); `I18n` se mockea como identidad (`I18n.get(x)` → `x`), igual que en `CorreoServiceImplTest`.
- Las dependencias `@Inject` de los servicios/controladores/observers se colocan por reflexión con un helper `setField(...)`, igual que en `CorreoServiceImplTest`, `TareaFirmaControllerTest` y `CorreoEventObserverTest`.
- Nada de base de datos real: los `Repository`/`JpaRepository` son mocks y las entidades se instancian con `new` y setters.

### Decisiones tomadas ante ambigüedades del diseño
1. **`TextUtil.trazaCompleta(null)`**: el diseño **mueve** el cuerpo del privado de `CorreoServiceImpl` tal cual y no declara contrato para `null`; su único llamador (`fireActionRule_MarcarEnvioFallido`) recibe siempre la `RuntimeException` capturada. Por eso **no** se describe test de `null` para ese método: describirlo obligaría a inventar un contrato que el diseño no fija.
2. **Detección de «ninguna transacción activa»** en `EjecutorAsincrono.ejecutarTrasCommit`: el diseño declara la entrada pero no cómo se pregunta. Se describen los dos casos que la representan sobre la cadena `JPA.em().unwrap(Session.class).getTransaction()`: transacción **no activa** (`isActive()` → `false`) y `getTransaction()` → `null`.
3. **Usuario con tipo de usuario en un centro** (V-Sms-015): se construye el grafo real `User → CentroUsuario → CentroUsuarioTipoUsuario → TipoUsuario.codigo` con `new` (§3: las entidades no se mockean), no un mock de `User`. El `Centro` **MUST** llevar `id`, porque `User.getCentroUsuario(Centro)` compara por `id`.
4. **`SmsServiceImpl.enviarSms` es privado**: se ejercita **siempre** a través de la tarea que `fireActionRule_ProgramarEnvioAsincrono` entrega al ejecutor — se captura el `Runnable` con un `ArgumentCaptor` sobre `ejecutorAsincrono.ejecutarTrasCommit(...)` y se ejecuta con `Mockito.mockStatic(JPA.class)` programando `JPA.runInTransaction(r)` para que invoque `r.run()` (el mismo helper `mockJpaRunInTransaction()` que ya usa `CorreoServiceImplTest`).
5. **`SmsSenderProvider.get()` construye el emisor de verdad**: `TwilioSmsSender` solo crea un `TwilioRestClient` y no abre ninguna conexión, así que **no** hace falta mockear `SmsSenderFactory`; basta mockear el estático `AppSettings`.
6. **Cuándo se evalúa V-Sms-011**: el diseño pone la condición («el SMS trae centro **y** estado de expediente») en `validateInsert`, no dentro del privado `validarHistorialEstado`. Como el privado no se testea directamente, la condición se observa desde `validateInsert`: sin `historialEstado` o sin `centro` no aparece nunca el mensaje de V-Sms-011 ni se lanza ninguna excepción.
7. **Mensajes literales**: `design.md` describe qué transmite cada mensaje sin escribir el literal; el texto exacto que se asierta es el de `entity-Sms.md` (y, para el aviso del reenvío, el de ESC-016/017). Los límites de V-Sms-008 se prueban con los mismos textos de ESC-009 (la letra «a» 160/161 veces) y ESC-010 (la «ú» seguida de 69/70 letras «a»).

---

## Clase: `com.educaflow.base.util.NumeroTelefono`  —  helper (clase de instancia, encapsula el parseo)

**Responsabilidad:** encapsular un teléfono escrito por una persona (país por defecto España) y responder dos preguntas: si es un móvil de España válido y cuál es su E.164. Única clase que importa `libphonenumber`, y ningún método público expone un tipo de esa librería.
**Colaboradores a mockear:** ninguno — se usa `libphonenumber` real (determinista y sin E/S). Es el único punto del diseño donde la librería se ejerce de verdad, así que mockearla no probaría nada.
**Origen diseño:** paso 2, `com.educaflow.base.util.NumeroTelefono` (constructor, `esMovilDeEspana()`, `enFormatoE164()`; los tres privados `esValido`/`esMovil`/`esDeEspana` se cubren a través de los dos públicos). Reglas: V-Sms-006 y R-Sms-001.

### Método: `NumeroTelefono(String telefono)`

- **`constructor_telefonoNoParseable_noLanza`** — Tipo: borde. Verifica: `V-Sms-006`.
  - **Arrange:** el texto `"no-es-un-telefono"`.
  - **Act:** construir el `NumeroTelefono`.
  - **Assert:** no lanza ninguna excepción (`assertDoesNotThrow`) — «mal escrito» es un caso de negocio, no una excepción.
- **`constructor_telefonoNulo_noLanza`** — Tipo: borde. Verifica: `V-Sms-006`.
  - **Arrange:** `null`.
  - **Act:** construir el `NumeroTelefono`.
  - **Assert:** no lanza (`assertDoesNotThrow`); el objeto queda construido con el número interno sin resolver.

### Método: `public boolean esMovilDeEspana()`

- **`esMovilDeEspana_movilEspanolSinPrefijo_devuelveTrue`** — Tipo: happy. Verifica: `V-Sms-006`.
  - **Arrange:** `"600111222"` (sin prefijo: el país por defecto es España).
  - **Act:** `esMovilDeEspana()`.
  - **Assert:** `true`.
- **`esMovilDeEspana_movilEspanolEnE164_devuelveTrue`** — Tipo: happy. Verifica: `V-Sms-006`.
  - **Arrange:** `"+34600111222"`.
  - **Act:** `esMovilDeEspana()`.
  - **Assert:** `true`.
- **`esMovilDeEspana_movilEspanolConEspacios_devuelveTrue`** — Tipo: borde. Verifica: `V-Sms-006`.
  - **Arrange:** `"600 11 12 22"` (tal y como lo teclea una persona).
  - **Act:** `esMovilDeEspana()`.
  - **Assert:** `true`.
- **`esMovilDeEspana_fijoEspanol_devuelveFalse`** — Tipo: error. Verifica: `V-Sms-006`.
  - **Arrange:** `"963000000"`.
  - **Act:** `esMovilDeEspana()`.
  - **Assert:** `false` — es válido y de España, pero no es móvil.
- **`esMovilDeEspana_movilFrancesValido_devuelveFalse`** — Tipo: error. Verifica: `V-Sms-006`.
  - **Arrange:** `"+33612345678"`.
  - **Act:** `esMovilDeEspana()`.
  - **Assert:** `false` — es un móvil válido, pero no de España (RES-Sms-004).
- **`esMovilDeEspana_numeroIncompleto_devuelveFalse`** — Tipo: error. Verifica: `V-Sms-006`.
  - **Arrange:** `"60011"`.
  - **Act:** `esMovilDeEspana()`.
  - **Assert:** `false`.
- **`esMovilDeEspana_textoNoParseable_devuelveFalseSinLanzar`** — Tipo: borde. Verifica: `V-Sms-006`.
  - **Arrange:** `"no-es-un-telefono"`.
  - **Act:** `esMovilDeEspana()`.
  - **Assert:** `false` y ninguna excepción.
- **`esMovilDeEspana_nuloVacioOBlanco_devuelveFalseSinLanzar`** — Tipo: borde. Verifica: `V-Sms-006`.
  - **Arrange:** tres instancias, con `null`, `""` y `"   "`.
  - **Act:** `esMovilDeEspana()` en las tres.
  - **Assert:** `false` en las tres y ninguna excepción.

### Método: `public String enFormatoE164()`

- **`enFormatoE164_movilEspanolSinPrefijo_devuelveElNumeroConPrefijo34`** — Tipo: happy. Verifica: `R-Sms-001`.
  - **Arrange:** `"600111222"`.
  - **Act:** `enFormatoE164()`.
  - **Assert:** `"+34600111222"` (el ejemplo literal de RN-Sms-001).
- **`enFormatoE164_movilYaEnE164_devuelveElMismoNumero`** — Tipo: borde. Verifica: `R-Sms-001`.
  - **Arrange:** `"+34600111222"`.
  - **Act:** `enFormatoE164()`.
  - **Assert:** `"+34600111222"` — la canonicalización es idempotente.
- **`enFormatoE164_movilConEspacios_devuelveElNumeroCanonico`** — Tipo: borde. Verifica: `R-Sms-001`.
  - **Arrange:** `"600 11 12 22"`.
  - **Act:** `enFormatoE164()`.
  - **Assert:** `"+34600111222"`.
- **`enFormatoE164_fijoEspanol_lanzaIllegalStateException`** — Tipo: error. Verifica: `R-Sms-001`.
  - **Arrange:** `"963000000"`.
  - **Act:** `enFormatoE164()`.
  - **Assert:** lanza `IllegalStateException` — pedir el canónico de algo que no es un móvil de España es un error de programación; **MUST NOT** devolver `null` ni el texto original.
- **`enFormatoE164_movilFrancesValido_lanzaIllegalStateException`** — Tipo: error. Verifica: `R-Sms-001`, `V-Sms-006`.
  - **Arrange:** `"+33612345678"`.
  - **Act:** `enFormatoE164()`.
  - **Assert:** lanza `IllegalStateException` — la guarda es `esMovilDeEspana()` y **no** `esValido()`: con `esValido()` un `+33…` se habría guardado en BD rompiendo RES-Sms-004.
- **`enFormatoE164_telefonoNoParseableONulo_lanzaIllegalStateException`** — Tipo: borde. Verifica: `R-Sms-001`.
  - **Arrange:** dos instancias, con `null` y con `"no-es-un-telefono"`.
  - **Act:** `enFormatoE164()` en las dos.
  - **Assert:** las dos lanzan `IllegalStateException`.

---

## Clase: `com.educaflow.base.util.MensajeSmsUtil`  —  helper (estático, sin estado)

**Responsabilidad:** único dueño de la decisión «este texto cabe en un solo SMS»: GSM-7 con máximo 160 unidades (los caracteres de la tabla extendida cuentan 2) o, si el texto no es codificable en GSM-7, UCS-2 con máximo 70 caracteres (unidades UTF-16).
**Colaboradores a mockear:** ninguno — función pura. `TextUtil.requireNonBlank` se usa **real** (es la precondición que se comprueba).
**Origen diseño:** paso 2, `com.educaflow.base.util.MensajeSmsUtil` (`cabeEnUnSms`; los privados `esCodificableEnGsm7` y `contarUnidadesGsm7` se cubren a través del público). Regla: V-Sms-008.

### Método: `public static boolean cabeEnUnSms(String mensaje)`

- **`cabeEnUnSms_textoCortoBasico_devuelveTrue`** — Tipo: happy. Verifica: `V-Sms-008`.
  - **Arrange:** `"Mañana no hay clase"` (todo en la tabla básica de GSM-7; «ñ» pertenece a la básica).
  - **Act:** `cabeEnUnSms(...)`.
  - **Assert:** `true`.
- **`cabeEnUnSms_cientoSesentaCaracteresBasicos_devuelveTrue`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** la letra «a» repetida 160 veces (ESC-009, paso 6).
  - **Act:** `cabeEnUnSms(...)`.
  - **Assert:** `true` — 160 es el límite inclusivo.
- **`cabeEnUnSms_cientoSesentaYUnCaracteresBasicos_devuelveFalse`** — Tipo: error. Verifica: `V-Sms-008`.
  - **Arrange:** la letra «a» repetida 161 veces (ESC-009, paso 3).
  - **Act:** `cabeEnUnSms(...)`.
  - **Assert:** `false`.
- **`cabeEnUnSms_caracterExtendidoCuentaDosUnidades_devuelveTrueEnElLimite`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** la letra «a» repetida 158 veces más `"€"` (158 + 2 = 160 unidades).
  - **Act:** `cabeEnUnSms(...)`.
  - **Assert:** `true`.
- **`cabeEnUnSms_caracterExtendidoQueRebasaElLimite_devuelveFalse`** — Tipo: error. Verifica: `V-Sms-008`.
  - **Arrange:** la letra «a» repetida 159 veces más `"€"` (159 + 2 = 161 unidades), es decir **160 caracteres** que no caben.
  - **Act:** `cabeEnUnSms(...)`.
  - **Assert:** `false` — demuestra que los extendidos cuentan 2 y no 1.
- **`cabeEnUnSms_todosLosCaracteresExtendidos_cuentanDosUnidadesCadaUno`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** un texto con los diez caracteres de la tabla extendida (`\f`, `^`, `{`, `}`, `\`, `[`, `~`, `]`, `|`, `€`) repetido hasta 80 caracteres (80 × 2 = 160 unidades) y otro de 81 (162 unidades).
  - **Act:** `cabeEnUnSms(...)` en los dos.
  - **Assert:** `true` para el de 80 y `false` para el de 81.
- **`cabeEnUnSms_textoConAcentoNoGsm7DeSetentaCaracteres_devuelveTrue`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** `"ú"` seguida de la letra «a» repetida 69 veces (70 caracteres; la «ú» está fuera de las dos tablas GSM-7 y obliga a UCS-2) — ESC-010, paso 6.
  - **Act:** `cabeEnUnSms(...)`.
  - **Assert:** `true` — límite UCS-2 inclusivo.
- **`cabeEnUnSms_textoConAcentoNoGsm7DeSetentaYUnCaracteres_devuelveFalse`** — Tipo: error. Verifica: `V-Sms-008`.
  - **Arrange:** `"ú"` seguida de la letra «a» repetida 70 veces (71 caracteres) — ESC-010, paso 3.
  - **Act:** `cabeEnUnSms(...)`.
  - **Assert:** `false`.
- **`cabeEnUnSms_unSoloCaracterFueraDeGsm7_cambiaElLimiteA70`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** dos textos de 100 caracteres: uno todo básico GSM-7 y otro idéntico salvo que un carácter es `"á"`.
  - **Act:** `cabeEnUnSms(...)` en los dos.
  - **Assert:** `true` para el básico (100 ≤ 160) y `false` para el que lleva el acento (100 > 70) — un solo carácter fuera de las tablas baja el límite.
- **`cabeEnUnSms_caracterFueraDelBmp_cuentaDosUnidadesUtf16`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** 69 caracteres básicos más un emoji (par surrogate: 2 unidades UTF-16 → 71).
  - **Act:** `cabeEnUnSms(...)`.
  - **Assert:** `false` — se cuentan unidades UTF-16 (`String.length()`), que es lo que cuenta UCS-2.
- **`cabeEnUnSms_mensajeNulo_lanzaNullPointerException`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** `null`.
  - **Act:** `cabeEnUnSms(null)`.
  - **Assert:** lanza `NullPointerException` con el mensaje exacto `"El mensaje del SMS es obligatorio"` (comportamiento de `TextUtil.requireNonBlank` ante `null`): su llamador solo pregunta cuando el mensaje está indicado, así que un mensaje ausente aquí es un error de programación.
- **`cabeEnUnSms_mensajeVacioOEnBlanco_lanzaIllegalArgumentException`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** `""` y `"   "`.
  - **Act:** `cabeEnUnSms(...)` en los dos.
  - **Assert:** los dos lanzan `IllegalArgumentException` con el mensaje exacto `"El mensaje del SMS es obligatorio"` — **nunca** devuelven `true` (`Objects.requireNonNull` sobre un `String` dejaría pasar `""` y `"   "`).

---

## Clase: `com.educaflow.base.util.TextUtil`  —  helper (Modificar: solo el método nuevo)

**Responsabilidad (delta):** `trazaCompleta(Throwable)` devuelve el stack trace completo (con causas) como `String`, tal cual lo escribe `printStackTrace`. Es el único dueño del texto que se guarda en `descripcionUltimoFallo`, compartido por correos y SMS.
**Colaboradores a mockear:** ninguno — función pura sobre la excepción que recibe.
**Origen diseño:** paso 2, `com.educaflow.base.util.TextUtil` (Modificar — delta: `public static String trazaCompleta(Throwable)`, cuerpo movido del privado `CorreoServiceImpl.trazaCompleta`). Reglas: R-Sms-006 (y el mismo uso en `CorreoServiceImpl`). Los tests existentes de `TextUtilTest` no cambian.

### Método: `public static String trazaCompleta(Throwable excepcion)`

- **`trazaCompleta_excepcionConMensaje_devuelveElNombreDeLaClaseElMensajeYLaTraza`** — Tipo: happy. Verifica: `R-Sms-006`.
  - **Arrange:** `new RuntimeException("Twilio caído")` lanzada y capturada (para que tenga stack trace real).
  - **Act:** `trazaCompleta(excepcion)`.
  - **Assert:** el resultado contiene `"java.lang.RuntimeException"`, `"Twilio caído"` y al menos una línea `"\tat "`.
- **`trazaCompleta_excepcionConCausa_incluyeLaCausaEncadenada`** — Tipo: borde. Verifica: `R-Sms-006`.
  - **Arrange:** `new RuntimeException("Fallo al enviar SMS", new IllegalStateException("socket cerrado"))`.
  - **Act:** `trazaCompleta(excepcion)`.
  - **Assert:** el resultado contiene `"Fallo al enviar SMS"`, `"Caused by"` y `"socket cerrado"` — es el «detalle completo del error» que pide CC-Sms-007.
- **`trazaCompleta_excepcionSinMensaje_devuelveAlMenosLaClaseYLaTraza`** — Tipo: borde. Verifica: `R-Sms-006`.
  - **Arrange:** `new RuntimeException()` (sin mensaje).
  - **Act:** `trazaCompleta(excepcion)`.
  - **Assert:** el resultado no es nulo ni blanco y contiene `"java.lang.RuntimeException"` — el campo nunca queda vacío tras un fallo (RES-Sms-003 / R-Sms-006).

---

## Clase: `com.educaflow.base.infrastructure.async.EjecutorAsincrono`  —  helper (infraestructura, singleton de la aplicación)

**Responsabilidad:** pool de hilos daemon de tamaño fijo que ejecuta una tarea **solo si la transacción actual hace commit**; descarta la tarea si hay rollback y falla en el acto si no hay transacción activa. Aísla los fallos de una tarea del hilo del pool y se para de forma ordenada.
**Colaboradores a mockear:** el estático `com.axelor.db.JPA` (`mockStatic`) y los mocks de la cadena `EntityManager` → `org.hibernate.Session` → `org.hibernate.Transaction`; el `Runnable` de la tarea (mock o lambda con `CountDownLatch`). El `ExecutorService` **no** se mockea: se usa un pool real de tamaño 1 y se espera con `CountDownLatch.await(timeout)`, igual que `CorreoAsyncExecutorTest`. Un teardown (`tearDown`) llama a `detener()` en todos los ejecutores creados para no dejar hilos vivos.
**Origen diseño:** paso 3, `com.educaflow.base.infrastructure.async.EjecutorAsincrono` (constructor, `ejecutarTrasCommit`, `detener`) y su tabla de las **tres entradas** del contrato. Recoge los casos de los desaparecidos `PostCommitRunnerTest` y `CorreoAsyncExecutorTest` más el caso nuevo de la tercera entrada. Sin regla `V`/`R`/`CC` propia (es la infraestructura de R-Sms-003 y del envío asíncrono de correos).

### Método: `public EjecutorAsincrono(int tamanoPool)`

- **`constructor_tareaEjecutada_correEnUnHiloDaemonLlamadoAsyncN`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ejecutor de tamaño 1; `JPA` mockeado con transacción activa que luego hace commit; una tarea que captura `Thread.currentThread()` en un `AtomicReference` y baja un `CountDownLatch`.
  - **Act:** `ejecutarTrasCommit(tarea)` y `afterCompletion(Status.STATUS_COMMITTED)` sobre el `Synchronization` capturado; esperar el latch.
  - **Assert:** la tarea se ejecutó dentro del timeout; el hilo es daemon (`isDaemon()` → `true`) y su nombre empieza por `"async-"`.

### Método: `public void ejecutarTrasCommit(Runnable tarea)`

- **`ejecutarTrasCommit_transaccionHaceCommit_ejecutaLaTarea`** — Tipo: happy. Verifica: `—` (soporte de `R-Sms-003`).
  - **Arrange:** `mockStatic(JPA)` con `JPA.em()` → `EntityManager` mock; `unwrap(Session.class)` → `Session` mock; `getTransaction()` → `Transaction` mock con `isActive()` → `true`; una tarea con `CountDownLatch`.
  - **Act:** `ejecutarTrasCommit(tarea)`; capturar el `Synchronization` con `ArgumentCaptor` sobre `transaction.registerSynchronization(...)` e invocar `afterCompletion(Status.STATUS_COMMITTED)`.
  - **Assert:** la tarea se ejecuta (latch bajado dentro del timeout / `verify(tarea).run()`).
- **`ejecutarTrasCommit_transaccionHaceRollback_noEjecutaLaTarea`** — Tipo: error. Verifica: `—`.
  - **Arrange:** igual que el anterior.
  - **Act:** `ejecutarTrasCommit(tarea)` y `afterCompletion(Status.STATUS_ROLLEDBACK)`.
  - **Assert:** `verify(tarea, never()).run()` — no se trabaja sobre una fila que no existe en BD.
- **`ejecutarTrasCommit_sinTransaccionActiva_lanzaIllegalStateExceptionSinRegistrarNada`** — Tipo: error. Verifica: `—`.
  - **Arrange:** `mockStatic(JPA)` con la misma cadena, pero `transaction.isActive()` → `false`.
  - **Act:** `ejecutarTrasCommit(tarea)`.
  - **Assert:** lanza `IllegalStateException`; `verify(transaction, never()).registerSynchronization(any())` y `verify(tarea, never()).run()` — la tercera entrada declarada: ni se degrada a «ejecutar ya» ni se descarta en silencio.
- **`ejecutarTrasCommit_sinTransaccionEnLaSesion_lanzaIllegalStateException`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** la misma cadena con `session.getTransaction()` → `null`.
  - **Act:** `ejecutarTrasCommit(tarea)`.
  - **Assert:** lanza `IllegalStateException` y la tarea no se ejecuta.
- **`ejecutarTrasCommit_tareaLanzaRuntimeException_noPropagaYElPoolSigueUtilizable`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** ejecutor de tamaño 1; `JPA` mockeado con transacción activa; una primera tarea que lanza `new RuntimeException("boom")` y una segunda que baja un `CountDownLatch`.
  - **Act:** programar las dos y disparar `afterCompletion(STATUS_COMMITTED)` de cada `Synchronization`.
  - **Assert:** no se propaga nada al llamante (`assertDoesNotThrow`) y la segunda tarea se ejecuta (el hilo del pool sobrevive al fallo de la primera).

### Método: `public void detener()`

- **`detener_conTareaEnCurso_esperaSuFinalizacionAntesDeCerrar`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ejecutor de tamaño 1; `JPA` mockeado con transacción activa; una tarea lenta (`Thread.sleep(200)`) que al terminar marca un `AtomicBoolean`.
  - **Act:** programar la tarea, disparar el commit y llamar a `detener()`.
  - **Assert:** el `AtomicBoolean` está a `true` — `awaitTermination` esperó a la tarea en curso.
- **`detener_trasHaberDetenido_elPoolRechazaNuevasTareas`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** ejecutor de tamaño 1 ya detenido; `JPA` mockeado con transacción activa; una tarea mock.
  - **Act:** `ejecutarTrasCommit(tarea)` y `afterCompletion(STATUS_COMMITTED)` sobre el `Synchronization` capturado.
  - **Assert:** el `afterCompletion` lanza `RejectedExecutionException` y `verify(tarea, never()).run()` — el pool cerrado no acepta trabajo (comprobación que venía de `CorreoAsyncExecutorTest`).
- **`detener_hiloInterrumpido_propagaLaInterrupcionYFuerzaElCierre`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** ejecutor de tamaño 1 con una tarea larga en curso; marcar el hilo del test como interrumpido (`Thread.currentThread().interrupt()`) antes de parar.
  - **Act:** `detener()`.
  - **Assert:** el hilo del test sigue marcado como interrumpido (`Thread.currentThread().isInterrupted()` → `true`, limpiando la marca al final del test) y el pool queda cerrado (una tarea posterior se rechaza) — `shutdownNow()` tras la `InterruptedException`.

---

## Clase: `com.educaflow.secretariavirtual.module.EjecutorAsincronoProvider`  —  helper (`Provider` de Guice)

**Responsabilidad:** construir el `EjecutorAsincrono` único leyendo `async.pool-size` de la configuración, con `2` por defecto. Es el único sitio del proyecto que lee un tamaño de pool.
**Colaboradores a mockear:** el estático `com.axelor.app.AppSettings` (`mockStatic`) y el mock de `AppSettings` que devuelve `AppSettings.get()`. El `EjecutorAsincrono` devuelto es real: el teardown (`tearDown`) lo detiene para no dejar hilos vivos.
**Origen diseño:** paso 3, «Cableado del ejecutor único» → `EjecutorAsincronoProvider.get()`. Recoge el caso del desaparecido `CorreoAsyncExecutorProviderTest`. Sin regla `V`/`R`/`CC`.

### Método: `public EjecutorAsincrono get()`

- **`get_conPropiedadConfigurada_usaElTamanoIndicado`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `mockStatic(AppSettings)` con `AppSettings.get()` → mock; `settings.getInt("async.pool-size", 2)` → `4`.
  - **Act:** `get()`.
  - **Assert:** devuelve un `EjecutorAsincrono` no nulo (`assertInstanceOf`) y `verify(settings).getInt("async.pool-size", 2)`.
- **`get_sinPropiedadConfigurada_usaDosComoValorPorDefecto`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** el mismo estático, con `settings.getInt("async.pool-size", 2)` → `2` (valor por defecto).
  - **Act:** `get()`.
  - **Assert:** devuelve un `EjecutorAsincrono` no nulo y `verify(settings).getInt("async.pool-size", 2)` — la clave leída es `async.pool-size` y **no** `mail.send.pool-size`.

---

## Clase: `com.educaflow.secretariavirtual.startup.AppEventObserver`  —  helper (observer del ciclo de vida; Modificar: solo el delta)

**Responsabilidad (delta):** al apagar la aplicación, además de su log, para el `EjecutorAsincrono` inyectado.
**Colaboradores a mockear:** el `EjecutorAsincrono` (mock, colocado por reflexión con `setField`) y el `ShutdownEvent` (mock).
**Origen diseño:** paso 3, `AppEventObserver.onAppShutdown(@Observes ShutdownEvent)` (Modificar — delta). Recoge el caso del desaparecido `CorreoEventObserverTest`. Sin regla `V`/`R`/`CC`. `onAppStart` **no** se testea: el diseño no lo modifica y arranca la BD y la criptografía reales.

### Método: `public void onAppShutdown(@Observes ShutdownEvent event)`

- **`onAppShutdown_evento_detieneElEjecutorAsincrono`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** observer con un mock de `EjecutorAsincrono` colocado por reflexión; un mock de `ShutdownEvent`.
  - **Act:** `onAppShutdown(event)`.
  - **Assert:** `verify(ejecutorAsincrono).detener()` — no hace falta ningún observer nuevo por subsistema.

---

## Clase: `com.educaflow.subsystem.sms.module.SmsSenderProvider`  —  helper (`Provider` de Guice)

**Responsabilidad:** construir el `SmsSender` de Twilio a partir de `sms.credentials.twilio.accountSid`, `sms.credentials.twilio.authToken` y `sms.twilio.from`. Se invoca en **cada** intento de envío (no es singleton), para que una credencial ausente deje el SMS FALLIDO en vez de romper la construcción del servicio.
**Colaboradores a mockear:** el estático `com.axelor.app.AppSettings` (`mockStatic`) y el mock de `AppSettings`. `SmsSenderFactory` **no** se mockea: construir un `TwilioSmsSender` no abre ninguna conexión.
**Origen diseño:** paso 7 y `rules/R-Sms-003.md` §«Clases nuevas» → `SmsSenderProvider.get()`. Regla asociada: **ninguna** — `get()` no aplica R-Sms-006 (esa regla vive en `SmsServiceImpl.fireActionRule_MarcarEnvioFallido`); lo que estos tests fijan es la **precondición** del caso de R-Sms-006 que cubre `enviarSms_credencialesAusentes_marcaFallidoConLaTraza`: que una credencial ausente salga de aquí como `RuntimeException`.

### Método: `public SmsSender get()`

- **`get_conCredencialesYNumeroEmisorConfigurados_devuelveElEmisor`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `mockStatic(AppSettings)` con `get()` → mock; `settings.get("sms.credentials.twilio.accountSid")` → `"AC0000000000000000000000000000000"`, `settings.get("sms.credentials.twilio.authToken")` → `"token-de-prueba"`, `settings.get("sms.twilio.from")` → `"+34600999888"`.
  - **Act:** `get()`.
  - **Assert:** devuelve un `SmsSender` no nulo y se leen exactamente esas tres claves (`verify` de las tres) — ninguna credencial sale del cliente ni del bean.
- **`get_sinAccountSidConfigurado_lanzaIllegalArgumentException`** — Tipo: error. Verifica: `—` (precondición del caso de R-Sms-006 que cubre `enviarSms_credencialesAusentes_marcaFallidoConLaTraza`).
  - **Arrange:** el mismo estático con `accountSid` → `""` (el valor que trae `axelor-config.properties`) y las otras dos con valor.
  - **Act:** `get()`.
  - **Assert:** lanza `IllegalArgumentException` (`TwilioCredential` rechaza los valores blancos). Es una `RuntimeException`, así que cae en el `catch` de `enviarSms` y el SMS queda FALLIDO con su descripción.
- **`get_sinAuthTokenConfigurado_lanzaExcepcion`** — Tipo: error. Verifica: `—` (precondición del caso de R-Sms-006 que cubre `enviarSms_credencialesAusentes_marcaFallidoConLaTraza`).
  - **Arrange:** `accountSid` con valor, `authToken` → `null`.
  - **Act:** `get()`.
  - **Assert:** lanza `NullPointerException` (comportamiento de `TextUtil.requireNonBlank` ante `null`), subclase de `RuntimeException`.
- **`get_sinNumeroEmisorConfigurado_lanzaIllegalArgumentException`** — Tipo: error. Verifica: `—` (precondición del caso de R-Sms-006 que cubre `enviarSms_credencialesAusentes_marcaFallidoConLaTraza`).
  - **Arrange:** las dos credenciales con valor y `sms.twilio.from` → `""`.
  - **Act:** `get()`.
  - **Assert:** lanza `IllegalArgumentException` (lo rechaza `SmsSenderFactory.getTwilioSmsSender`).

---

## Clase: `com.educaflow.subsystem.sms.service.impl.SmsServiceImpl`  —  servicio

**Responsabilidad:** ciclo de vida del `Sms`: validar el alta (V-Sms-001…011), canonicalizar el teléfono y asignar los campos `servidor` (R-Sms-001, R-Sms-002), persistir y programar el envío tras el commit (R-Sms-003); rechazar la modificación y el borrado (V-Sms-012, V-Sms-013); validar y programar el reenvío (V-Sms-014, V-Sms-015); y, en el hilo del pool, intentar el envío y dejar el SMS en ENVIADO o FALLIDO (R-Sms-004, R-Sms-005, R-Sms-006). Publica una whitelist por acción.
**Colaboradores a mockear:** `com.axelor.db.Repository<Sms>` (mock; `find`/`save`), `jakarta.inject.Provider<SmsSender>` y el `SmsSender` que devuelve (mocks), `com.educaflow.base.infrastructure.async.EjecutorAsincrono` (mock, para capturar el `Runnable`), los estáticos `com.axelor.i18n.I18n` (identidad, `LENIENT`), `com.educaflow.base.util.SecurityUtil` (`isAdmin`, `getUser`; `LENIENT`) y `com.axelor.db.JPA` (solo en los tests de la tarea asíncrona, programando `runInTransaction` para ejecutar el `Runnable`). El mock de `Repository<Sms>` **no** va por reflexión: se pasa por el **constructor** (`new SmsServiceImpl(Sms.class, repository)`, igual que `CorreoServiceImplTest`), porque `DefaultModelService` lo guarda en un campo `final`. Los **dos** colaboradores `@Inject` (`Provider<SmsSender>` y `EjecutorAsincrono`) se colocan por reflexión con `setField(...)`. Las entidades (`Sms`, `Centro`, `HistorialEstado`, `Expediente`, `User`, `CentroUsuario`, `CentroUsuarioTipoUsuario`, `TipoUsuario`) se instancian con `new`. `NumeroTelefono`, `MensajeSmsUtil`, `DniUtil` y `TextUtil` se usan **reales** (funciones puras y deterministas). **MUST NOT** tocar la BD.
**Origen diseño:** paso 5 (`insert`, `update`, `remove`, `reenviar`, `validateInsert` y sus cinco privados, `validateUpdate`, `validateRemove`, `validateReenviar`, los cuatro `allowProperties*`, los seis `fireActionRule_*` y el privado `enviarSms`) y `rules/R-Sms-003.md`. Reglas: V-Sms-001…015 y R-Sms-001…006.

**Fixture común:** `smsValido()` → `dniDestinatario = "12345678Z"` (DNI con letra correcta), `nombre = "Juan"`, `apellidos = "Pérez"`, `telefono = "600111222"`, `mensaje = "Mañana no hay clase"`, `centro = centroA` (id `1L`), `historialEstado = null`. Helpers: `stubIsAdmin(boolean)`, `usuarioDeCentro(Centro)` (con `CentroUsuario` de ese centro), `usuarioConTipoEnCentro(Centro, TipoUsuarioCodigo)` (grafo `CentroUsuario → CentroUsuarioTipoUsuario → TipoUsuario.codigo`), `mensaje(Optional<BusinessMessages>)` (primer mensaje), `mensajes(...)` (todos), `mockJpaRunInTransaction()` y `ejecutarTareaProgramada()` (captura el `Runnable` de `ejecutarTrasCommit` y lo ejecuta con `JPA` mockeado).

### Método: `public Optional<BusinessMessages> validateInsert(Sms sms)`

- **`validateInsert_smsValido_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Sms-001`…`V-Sms-011`.
  - **Arrange:** `smsValido()`; `SecurityUtil.isAdmin(...)` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `Optional` vacío.
- **`validateInsert_dniDestinatarioNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Sms-001`.
  - **Arrange:** `smsValido()` con `dniDestinatario = null`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** un mensaje, exactamente `"El DNI del destinatario es obligatorio"`.
- **`validateInsert_dniDestinatarioEnBlanco_devuelveMensajeObligatorio`** — Tipo: borde. Verifica: `V-Sms-001`.
  - **Arrange:** `smsValido()` con `dniDestinatario = "   "`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `"El DNI del destinatario es obligatorio"` (null **o** blanco).
- **`validateInsert_dniDestinatarioConLetraIncorrecta_devuelveMensajeNoValido`** — Tipo: error. Verifica: `V-Sms-002`.
  - **Arrange:** `smsValido()` con `dniDestinatario = "12345678A"`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** un mensaje, exactamente `"El DNI del destinatario no es válido; compruebe la letra"`; el mensaje **no** contiene el DNI completo.
- **`validateInsert_dniDestinatarioNulo_noAnadeElMensajeDeDniNoValido`** — Tipo: borde. Verifica: `V-Sms-001`, `V-Sms-002`.
  - **Arrange:** `smsValido()` con `dniDestinatario = null`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** exactamente **un** mensaje (el de obligatorio) y nunca el de «no es válido» — V-Sms-002 va en el `else if` de V-Sms-001.
- **`validateInsert_nombreNuloOEnBlanco_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Sms-003`.
  - **Arrange:** dos SMS válidos, uno con `nombre = null` y otro con `nombre = "   "`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)` en los dos.
  - **Assert:** los dos devuelven `"El nombre es obligatorio"`.
- **`validateInsert_apellidosNulosOEnBlanco_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Sms-004`.
  - **Arrange:** dos SMS válidos, uno con `apellidos = null` y otro con `apellidos = "   "`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)` en los dos.
  - **Assert:** los dos devuelven `"Los apellidos son obligatorios"`.
- **`validateInsert_nombreYApellidosAusentes_devuelveLosDosMensajes`** — Tipo: borde. Verifica: `V-Sms-003`, `V-Sms-004`.
  - **Arrange:** `smsValido()` con `nombre = null` y `apellidos = null`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** **dos** mensajes, `"El nombre es obligatorio"` y `"Los apellidos son obligatorios"` — son independientes y se acumulan (lo exige ESC-005).
- **`validateInsert_variosCamposAusentes_acumulaTodosLosMensajes`** — Tipo: borde. Verifica: `V-Sms-001`, `V-Sms-003`, `V-Sms-004`, `V-Sms-005`, `V-Sms-007`, `V-Sms-009`.
  - **Arrange:** un `Sms` recién instanciado, con todos los campos a `null`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** seis mensajes, uno por cada campo obligatorio (DNI, nombre, apellidos, teléfono, mensaje, centro) y ninguno de los condicionados (V-Sms-002, V-Sms-006, V-Sms-008, V-Sms-010, V-Sms-011).
- **`validateInsert_telefonoNuloOEnBlanco_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Sms-005`.
  - **Arrange:** dos SMS válidos, uno con `telefono = null` y otro con `telefono = "   "`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)` en los dos.
  - **Assert:** los dos devuelven `"El teléfono es obligatorio"`.
- **`validateInsert_telefonoFijoEspanol_devuelveMensajeDeMovilDeEspana`** — Tipo: error. Verifica: `V-Sms-006`.
  - **Arrange:** `smsValido()` con `telefono = "963000000"`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** un mensaje, exactamente `"El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)"`.
- **`validateInsert_telefonoIncompleto_devuelveMensajeDeMovilDeEspana`** — Tipo: error. Verifica: `V-Sms-006`.
  - **Arrange:** `smsValido()` con `telefono = "60011"`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** el mismo mensaje literal de V-Sms-006 — un fijo y un número incompleto fallan por la misma regla y con el mismo texto.
- **`validateInsert_telefonoMovilExtranjeroValido_devuelveMensajeDeMovilDeEspana`** — Tipo: error. Verifica: `V-Sms-006`.
  - **Arrange:** `smsValido()` con `telefono = "+33612345678"`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** el mensaje de V-Sms-006 — la validación es «móvil **de España**», no «móvil válido» (RES-Sms-004).
- **`validateInsert_telefonoMovilSinPrefijo_esValido`** — Tipo: happy. Verifica: `V-Sms-006`.
  - **Arrange:** `smsValido()` con `telefono = "600111222"`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `Optional` vacío — sin prefijo se entiende de España.
- **`validateInsert_telefonoNulo_noAnadeElMensajeDeMovilDeEspana`** — Tipo: borde. Verifica: `V-Sms-005`, `V-Sms-006`.
  - **Arrange:** `smsValido()` con `telefono = null`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** exactamente **un** mensaje (el de obligatorio) — V-Sms-006 va en el `else` de V-Sms-005.
- **`validateInsert_mensajeNuloOEnBlanco_devuelveMensajeObligatorioSinLanzar`** — Tipo: borde. Verifica: `V-Sms-007`, `V-Sms-008`.
  - **Arrange:** tres SMS válidos, con `mensaje = null`, `mensaje = ""` y `mensaje = "   "`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)` en los tres.
  - **Assert:** los tres devuelven exactamente **un** mensaje, `"El mensaje es obligatorio"`, y **ninguno lanza** — V-Sms-007 cubre el blanco porque es la única guarda de `MensajeSmsUtil.cabeEnUnSms`, cuya precondición lanzaría (un `validate*` nunca lanza).
- **`validateInsert_mensajeQueNoCabeEnUnSms_devuelveMensajeDeLongitud`** — Tipo: error. Verifica: `V-Sms-008`.
  - **Arrange:** `smsValido()` con `mensaje` de 161 caracteres GSM-7 (la letra «a» repetida 161 veces, ESC-009); `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** un mensaje, exactamente `"El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales"`.
- **`validateInsert_mensajeConAcentosQueSuperaSetentaCaracteres_devuelveMensajeDeLongitud`** — Tipo: error. Verifica: `V-Sms-008`.
  - **Arrange:** `smsValido()` con `mensaje` = `"ú"` seguida de 70 letras «a» (71 caracteres, ESC-010); `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** el mismo mensaje literal de V-Sms-008.
- **`validateInsert_mensajeDeCientoSesentaCaracteresBasicos_esValido`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** `smsValido()` con un `mensaje` de 160 veces la letra «a»; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `Optional` vacío — el servicio delega enteramente en `MensajeSmsUtil` y no cuenta caracteres por su cuenta.
- **`validateInsert_mensajeConAcentoDeSetentaCaracteres_esValido`** — Tipo: borde. Verifica: `V-Sms-008`.
  - **Arrange:** `smsValido()` con `mensaje` = `"ú"` seguida de 69 letras «a» (70 caracteres, ESC-010 paso 6); `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `Optional` vacío — límite UCS-2 inclusivo.
- **`validateInsert_centroNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: `V-Sms-009`.
  - **Arrange:** `smsValido()` con `centro = null`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** un mensaje, exactamente `"El centro es obligatorio"`.
- **`validateInsert_usuarioNoAdminConCentroAjeno_devuelveMensajeCentroNoSuyo`** — Tipo: error. Verifica: `V-Sms-010`.
  - **Arrange:** `smsValido()` con `centro = centroB` (id `2L`); `SecurityUtil.isAdmin(...)` → `false`; `SecurityUtil.getUser()` → `usuarioDeCentro(centroA)`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** un mensaje, exactamente `"No puede crear SMS para un centro que no es suyo"`.
- **`validateInsert_usuarioNoAdminConCentroPropio_esValido`** — Tipo: happy. Verifica: `V-Sms-010`.
  - **Arrange:** `smsValido()` con `centro = centroA`; `isAdmin` → `false`; `getUser()` → `usuarioDeCentro(centroA)`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `Optional` vacío.
- **`validateInsert_administradorConCualquierCentro_esValido`** — Tipo: happy. Verifica: `V-Sms-010`.
  - **Arrange:** `smsValido()` con `centro = centroB`; `isAdmin` → `true` (la rama del administrador es explícita, nunca se quita el filtro).
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `Optional` vacío.
- **`validateInsert_centroNuloYUsuarioNoAdmin_devuelveSoloElMensajeDeObligatorio`** — Tipo: borde. Verifica: `V-Sms-009`, `V-Sms-010`.
  - **Arrange:** `smsValido()` con `centro = null`; `isAdmin` → `false`; `getUser()` → `usuarioDeCentro(centroA)`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** exactamente **un** mensaje, `"El centro es obligatorio"`, y nunca el falso «no es suyo» sobre un centro que no existe.
- **`validateInsert_historialEstadoConExpedienteDelMismoCentro_esValidoSinReleerPorRepositorio`** — Tipo: happy. Verifica: `V-Sms-011`.
  - **Arrange:** `smsValido()` con `centro = centroA` y un `HistorialEstado` con `id = 5L` cuyo `expediente` (instanciado con `new`) tiene `centro = centroA`; `isAdmin` → `true`; `mockStatic(JpaRepository)` sin programar nada.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `Optional` vacío y ninguna interacción con el estático `JpaRepository` — la regla trabaja sobre la instancia que trae la propia entidad y **no** relee el padre de otro subsistema (regla C27; `decisiones.md` D7).
- **`validateInsert_historialEstadoSinId_devuelveMensajeNoExiste`** — Tipo: error. Verifica: `V-Sms-011`.
  - **Arrange:** `smsValido()` con un `HistorialEstado` recién instanciado (sin `id`, la cáscara `{"historialEstado": {}}` de `/ws/rest`); `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** un mensaje, exactamente `"El estado del expediente indicado no existe"`.
- **`validateInsert_historialEstadoSinExpediente_devuelveMensajeNoExiste`** — Tipo: error. Verifica: `V-Sms-011`.
  - **Arrange:** `smsValido()` con un `HistorialEstado` con `id = 5L` y `expediente = null` (el many-to-one no es `required`); `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** el **mismo** mensaje de V-Sms-011 — no se distingue cuál de las mitades falló.
- **`validateInsert_historialEstadoDeExpedienteDeOtroCentro_devuelveMensajeNoExiste`** — Tipo: error. Verifica: `V-Sms-011`.
  - **Arrange:** `smsValido()` con `centro = centroA` y un `HistorialEstado` con `id = 5L` cuyo expediente tiene `centro = centroB`; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** el mismo mensaje de V-Sms-011 — sin esta mitad, el `formula` `nombreExpediente` filtraría el nombre de un expediente ajeno a los tres grids.
- **`validateInsert_historialEstadoNoIndicado_noSeEvaluaYEsValido`** — Tipo: borde. Verifica: `V-Sms-011`.
  - **Arrange:** `smsValido()` con `historialEstado = null` (el caso normal: ningún escenario del spec crea un SMS con él); `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** `Optional` vacío y ninguna excepción (ni NPE) — la condición de `validateInsert` deja fuera V-Sms-011 cuando no hay estado de expediente, en vez de producir el falso «El estado del expediente indicado no existe».
- **`validateInsert_historialEstadoIndicadoYCentroNulo_devuelveSoloElMensajeDelCentro`** — Tipo: borde. Verifica: `V-Sms-009`, `V-Sms-011`.
  - **Arrange:** `smsValido()` con `centro = null` y un `HistorialEstado` con `id = 5L` y expediente con centro; `isAdmin` → `true`.
  - **Act:** `validateInsert(sms)`.
  - **Assert:** exactamente **un** mensaje, `"El centro es obligatorio"`, ninguna excepción y nunca el mensaje de V-Sms-011 — sin centro, la condición de `validateInsert` no evalúa V-Sms-011 (ya ha hablado V-Sms-009).

### Método: `public Sms insert(Sms sms)`

- **`insert_smsValido_asignaLosValoresInicialesCanonicalizaElTelefonoYPersiste`** — Tipo: happy. Verifica: `R-Sms-001`, `R-Sms-002`, `R-Sms-003`.
  - **Arrange:** `smsValido()` (`telefono = "600111222"`); `isAdmin` → `true`; `repository.save(any())` devuelve su argumento.
  - **Act:** `insert(sms)`.
  - **Assert:** el resultado tiene `estado = EstadoSms.PENDIENTE`, `fechaCreacion` no nula, `numeroReintentos = 0`, y `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio` y `descripcionUltimoFallo` a `null`; `telefono = "+34600111222"`; `verify(repository).save(sms)`; `verify(ejecutorAsincrono).ejecutarTrasCommit(any())`.
- **`insert_telefonoYaEnE164_loDejaIgual`** — Tipo: borde. Verifica: `R-Sms-001`.
  - **Arrange:** `smsValido()` con `telefono = "+34600111222"`; `isAdmin` → `true`; `save` devuelve su argumento.
  - **Act:** `insert(sms)`.
  - **Assert:** `telefono = "+34600111222"` — la canonicalización es idempotente y **sin guarda** (`startsWith("+34")`).
- **`insert_telefonoConEspacios_loGuardaEnE164`** — Tipo: borde. Verifica: `R-Sms-001`.
  - **Arrange:** `smsValido()` con `telefono = "600 11 12 22"`; `isAdmin` → `true`; `save` devuelve su argumento.
  - **Act:** `insert(sms)`.
  - **Assert:** `telefono = "+34600111222"`.
- **`insert_clienteEnviaCamposDeServidor_seSobrescribenIncondicionalmente`** — Tipo: borde (seguridad). Verifica: `R-Sms-002`.
  - **Arrange:** `smsValido()` con `estado = EstadoSms.ENVIADO`, `fechaEnvio = LocalDateTime.of(2000,1,1,0,0)`, `fechaPrimerIntentoEnvio` y `fechaUltimoIntentoEnvio` con valor, `numeroReintentos = 99` y `descripcionUltimoFallo = "inventado"`; `isAdmin` → `true`; `save` devuelve su argumento.
  - **Act:** `insert(sms)`.
  - **Assert:** `estado = PENDIENTE`, `numeroReintentos = 0`, y `fechaEnvio`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio` y `descripcionUltimoFallo` a `null` — ninguna asignación va envuelta en `if (campo == null)`.
- **`insert_smsInvalido_lanzaValidationExceptionYNoPersisteNiProgramaEnvio`** — Tipo: error. Verifica: `V-Sms-001`, `R-Sms-003`.
  - **Arrange:** `smsValido()` con `dniDestinatario = null`; `isAdmin` → `true`.
  - **Act:** `insert(sms)`.
  - **Assert:** lanza `ValidationException` con mensaje `"El DNI del destinatario es obligatorio"`; `verify(repository, never()).save(any())` y `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())`.
- **`insert_smsValido_programaLaTareaConElIdGuardadoYNoConLaEntidad`** — Tipo: borde. Verifica: `R-Sms-003`.
  - **Arrange:** `smsValido()`; `isAdmin` → `true`; `repository.save(any())` devuelve el mismo `Sms` con `id = 100L`; `repository.find(100L)` devuelve ese `Sms`; `smsSenderProvider.get()` → mock de `SmsSender`.
  - **Act:** `insert(sms)` y después ejecutar el `Runnable` capturado (`ejecutarTareaProgramada()`).
  - **Assert:** `verify(repository).find(100L)` — la tarea recarga el SMS por su `id` en la transacción del hilo del pool, en vez de reutilizar la entidad de la transacción anterior.

### Método: `public Sms update(Sms nuevo, Sms original)`

- **`update_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: `V-Sms-012`.
  - **Arrange:** dos `smsValido()` (nuevo y original).
  - **Act:** `update(nuevo, original)`.
  - **Assert:** lanza `UnsupportedOperationException` con mensaje exacto `"El SMS es inmutable tras su creación."`; `verify(repository, never()).save(any())`.

### Método: `public void remove(Sms sms)`

- **`remove_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: `V-Sms-013`.
  - **Arrange:** `smsValido()`.
  - **Act:** `remove(sms)`.
  - **Assert:** lanza `UnsupportedOperationException` con mensaje exacto `"Los SMS no se pueden borrar."`; `verify(repository, never()).remove(any())`.

### Método: `public Optional<BusinessMessages> validateUpdate(Sms nuevo, Sms original)`

- **`validateUpdate_siempre_devuelveElMensajeDeInmutabilidad`** — Tipo: error. Verifica: `V-Sms-012`.
  - **Arrange:** dos `smsValido()`.
  - **Act:** `validateUpdate(nuevo, original)`.
  - **Assert:** un mensaje, exactamente `"El SMS es inmutable tras su creación."` — sin condición: siempre rechaza.

### Método: `public Optional<BusinessMessages> validateRemove(Sms sms)`

- **`validateRemove_siempre_devuelveElMensajeDeNoBorrado`** — Tipo: error. Verifica: `V-Sms-013`.
  - **Arrange:** `smsValido()`.
  - **Act:** `validateRemove(sms)`.
  - **Assert:** un mensaje, exactamente `"Los SMS no se pueden borrar."`.

### Método: `public Optional<BusinessMessages> validateReenviar(Sms entidad, Sms entidadOriginal)`

- **`validateReenviar_smsFallidoYAdministrador_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Sms-014`, `V-Sms-015`.
  - **Arrange:** `entidadOriginal` = `smsValido()` con `id = 100L`, `estado = EstadoSms.FALLIDO`, `centro = centroA`; `entidad` = `new Sms()` con `id = 100L` (la whitelist de reenviar es vacía); `isAdmin` → `true`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** `Optional` vacío.
- **`validateReenviar_smsPendiente_devuelveMensajeSoloFallidos`** — Tipo: error. Verifica: `V-Sms-014`.
  - **Arrange:** igual, con `estado = EstadoSms.PENDIENTE`; `isAdmin` → `true`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** un mensaje, exactamente `"Solo se pueden reenviar SMS que han fallado"`.
- **`validateReenviar_smsYaEnviado_devuelveMensajeSoloFallidos`** — Tipo: error. Verifica: `V-Sms-014`.
  - **Arrange:** igual, con `estado = EstadoSms.ENVIADO`; `isAdmin` → `true`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** el mismo mensaje literal de V-Sms-014 — ENVIADO es terminal.
- **`validateReenviar_entidadOriginalNula_devuelveMensajeSoloFallidosSinLanzar`** — Tipo: borde. Verifica: `V-Sms-014`.
  - **Arrange:** `entidadOriginal = null` (lo que devuelve `getOriginalModel()` cuando el contexto no trae `id`); `entidad = new Sms()`.
  - **Act:** `validateReenviar(entidad, null)`.
  - **Assert:** un mensaje, exactamente `"Solo se pueden reenviar SMS que han fallado"`, y **ninguna excepción** (ni NPE): la guarda está arriba del método y ningún `validate*` lanza.
- **`validateReenviar_usuarioSupervisorDelCentro_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Sms-015`.
  - **Arrange:** `entidadOriginal` FALLIDO con `centro = centroA`; `isAdmin` → `false`; `getUser()` → `usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.SUPERVISOR)`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** `Optional` vacío.
- **`validateReenviar_usuarioAdministrativoDelCentro_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Sms-015`.
  - **Arrange:** igual, con `usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.ADMINISTRATIVO)`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** `Optional` vacío — los dos tipos de la lista de gestión valen.
- **`validateReenviar_usuarioDelCentroSinCargoDeGestion_devuelveMensajeCentroNoSuyo`** — Tipo: error (seguridad). Verifica: `V-Sms-015`.
  - **Arrange:** `entidadOriginal` FALLIDO con `centro = centroA`; `isAdmin` → `false`; `getUser()` → `usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.ALUMNO)` (pertenece al centro pero no lo gestiona).
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** un mensaje, exactamente `"No puede reenviar SMS de un centro que no es suyo"` — la autorización real es **gestionar** el centro, no la mera pertenencia; quedarse en `perteneceAlCentro` dejaría a un alumno relanzar envíos con un POST a la acción.
- **`validateReenviar_usuarioQueNoPerteneceAlCentro_devuelveMensajeCentroNoSuyo`** — Tipo: error. Verifica: `V-Sms-015`.
  - **Arrange:** `entidadOriginal` FALLIDO con `centro = centroB`; `isAdmin` → `false`; `getUser()` → `usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.SUPERVISOR)`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** el mismo mensaje de V-Sms-015 — `tieneTipoUsuario(centro, …)` devuelve `false` si no pertenece a ese centro (expresión null-safe).
- **`validateReenviar_administradorDeCentroAjeno_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Sms-015`.
  - **Arrange:** `entidadOriginal` FALLIDO con `centro = centroB`; `isAdmin` → `true`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** `Optional` vacío — rama explícita del administrador.
- **`validateReenviar_smsPendienteYUsuarioSinGestion_devuelveLosDosMensajes`** — Tipo: borde. Verifica: `V-Sms-014`, `V-Sms-015`.
  - **Arrange:** `entidadOriginal` con `estado = PENDIENTE` y `centro = centroB`; `isAdmin` → `false`; `getUser()` → `usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.SUPERVISOR)`.
  - **Act:** `validateReenviar(entidad, entidadOriginal)`.
  - **Assert:** **dos** mensajes, los de V-Sms-014 y V-Sms-015 — las dos reglas son independientes y se acumulan en el mismo `BusinessMessages`.

### Método: `public Sms reenviar(Sms entidad, Sms entidadOriginal)`

- **`reenviar_smsFallido_programaElEnvioSinPersistirCambios`** — Tipo: happy. Verifica: `R-Sms-003`.
  - **Arrange:** `entidadOriginal` FALLIDO con `id = 100L` y `centro = centroA`; `entidad` con solo el `id`; `isAdmin` → `true`.
  - **Act:** `reenviar(entidad, entidadOriginal)`.
  - **Assert:** devuelve la **misma** instancia (`assertSame(entidadOriginal, resultado)`); `verify(repository, never()).save(any())` (el reenvío no cambia nada de forma síncrona); `verify(ejecutorAsincrono).ejecutarTrasCommit(any())`.
- **`reenviar_smsFallido_programaLaTareaDelMismoIdQueElOriginal`** — Tipo: borde. Verifica: `R-Sms-003`.
  - **Arrange:** como el anterior; `repository.find(100L)` devuelve el `entidadOriginal`; `smsSenderProvider.get()` → mock de `SmsSender`.
  - **Act:** `reenviar(...)` y ejecutar el `Runnable` capturado.
  - **Assert:** `verify(repository).find(100L)` — es el **mismo** `fireActionRule_ProgramarEnvioAsincrono` que usa `insert` (la política de envío tiene un solo dueño).
- **`reenviar_smsNoFallido_lanzaValidationExceptionYNoProgramaEnvio`** — Tipo: error. Verifica: `V-Sms-014`, `R-Sms-003`.
  - **Arrange:** `entidadOriginal` con `estado = PENDIENTE`, `centro = centroA`; `isAdmin` → `true`.
  - **Act:** `reenviar(entidad, entidadOriginal)`.
  - **Assert:** lanza `ValidationException` con mensaje `"Solo se pueden reenviar SMS que han fallado"`; `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())`.
- **`reenviar_usuarioSinGestionDelCentro_lanzaValidationExceptionYNoProgramaEnvio`** — Tipo: error (seguridad). Verifica: `V-Sms-015`, `R-Sms-003`.
  - **Arrange:** `entidadOriginal` FALLIDO con `centro = centroA`; `isAdmin` → `false`; `getUser()` → `usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.ALUMNO)`.
  - **Act:** `reenviar(entidad, entidadOriginal)`.
  - **Assert:** lanza `ValidationException` con mensaje `"No puede reenviar SMS de un centro que no es suyo"`; `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())`.
- **`reenviar_entidadOriginalNula_lanzaValidationExceptionYNoProgramaEnvio`** — Tipo: borde. Verifica: `V-Sms-014`, `R-Sms-003`.
  - **Arrange:** `entidadOriginal = null`; `entidad = new Sms()`.
  - **Act:** `reenviar(entidad, null)`.
  - **Assert:** lanza `ValidationException` con mensaje `"Solo se pueden reenviar SMS que han fallado"` (no NPE); `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())` — la guarda impide llegar a programar la tarea con un `null`.

### Método: `private void enviarSms(Long smsId)` — a través del `Runnable` que entrega `private void fireActionRule_ProgramarEnvioAsincrono(Sms sms)`

Todos estos tests parten de un `insert` o un `reenviar` que deja el SMS en BD (mock), capturan el `Runnable` entregado a `ejecutorAsincrono.ejecutarTrasCommit(...)` y lo ejecutan con `mockStatic(JPA)` programando `JPA.runInTransaction(r)` para que invoque `r.run()`. `repository.find(100L)` devuelve el `Sms` preparado y `smsSenderProvider.get()` devuelve el mock de `SmsSender`.

- **`enviarSms_proveedorAceptaElEnvio_marcaEnviadoConFechaDeEnvioYSinDescripcionDeFallo`** — Tipo: happy. Verifica: `R-Sms-004`, `R-Sms-005`.
  - **Arrange:** `Sms` en BD con `id = 100L`, `estado = PENDIENTE`, `telefono = "+34600111222"`, `mensaje = "Mañana no hay clase"`, `numeroReintentos = 0`, fechas de intento a `null`; `smsSender.send(any())` no lanza.
  - **Act:** ejecutar el `Runnable` capturado.
  - **Assert:** `estado = EstadoSms.ENVIADO`, `fechaEnvio` no nula, `descripcionUltimoFallo` nulo, `fechaPrimerIntentoEnvio` y `fechaUltimoIntentoEnvio` no nulas, `numeroReintentos = 1`; `verify(repository).save(sms)` **una** sola vez.
- **`enviarSms_envio_construyeElRecordDeTransporteConElTelefonoEnE164YElMensaje`** — Tipo: happy. Verifica: `R-Sms-003`.
  - **Arrange:** el mismo `Sms`; `ArgumentCaptor<com.educaflow.base.infrastructure.sms.Sms>` sobre `smsSender.send(...)`.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** el record capturado tiene `telefonoDestino = "+34600111222"` y `mensaje = "Mañana no hay clase"` — se copian tal cual, sin traducción.
- **`enviarSms_proveedorLanzaExcepcion_marcaFallidoConLaTrazaYSinFechaDeEnvioSinPropagar`** — Tipo: error. Verifica: `R-Sms-006`.
  - **Arrange:** `Sms` en BD como arriba; `smsSender.send(...)` lanza `RuntimeException("Twilio caído")`.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** no propaga (`assertDoesNotThrow`); `estado = EstadoSms.FALLIDO`, `fechaEnvio` nulo, `descripcionUltimoFallo` contiene `"Twilio caído"` y una línea de traza (`"\tat "`); `verify(repository).save(sms)`.
- **`enviarSms_credencialesAusentes_marcaFallidoConLaTraza`** — Tipo: error. Verifica: `R-Sms-006`.
  - **Arrange:** `Sms` en BD como arriba; `smsSenderProvider.get()` lanza `new IllegalArgumentException("accountSid no puede ser null ni blank")` (lo que hace `TwilioCredential` con la configuración vacía).
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** no propaga; `estado = FALLIDO`, `descripcionUltimoFallo` contiene `"accountSid"`, `fechaEnvio` nulo; `verify(repository).save(sms)` — el `Provider` se resuelve **dentro** del `try`, así que la falta de credenciales cae por el camino normal (ESC-001, ESC-020).
- **`enviarSms_envioCorrectoTrasUnFallo_borraLaDescripcionDeFalloPrevia`** — Tipo: borde. Verifica: `R-Sms-005`.
  - **Arrange:** `Sms` en BD con `estado = FALLIDO`, `descripcionUltimoFallo = "fallo anterior"`, `numeroReintentos = 1`; `smsSender.send` no lanza.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** `estado = ENVIADO`, `descripcionUltimoFallo` nulo, `fechaEnvio` no nula — asignación incondicional.
- **`enviarSms_falloConFechaDeEnvioPrevia_ponLaFechaDeEnvioANull`** — Tipo: borde. Verifica: `R-Sms-006`.
  - **Arrange:** `Sms` en BD con `estado = FALLIDO` y, a propósito, `fechaEnvio` de ayer (dato incoherente: comprueba que la asignación es incondicional); `smsSender.send` lanza `RuntimeException`.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** `estado = FALLIDO` y `fechaEnvio` nulo — RES-Sms-003: fuera de ENVIADO nunca hay fecha de envío.
- **`enviarSms_smsYaEnviado_noLlamaAlProveedorNiPersiste`** — Tipo: borde. Verifica: `R-Sms-003`.
  - **Arrange:** `Sms` en BD con `estado = EstadoSms.ENVIADO`.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** `verify(smsSender, never()).send(any())` y `verify(repository, never()).save(any())` — idempotencia ante un doble disparo (ENVIADO es terminal).
- **`enviarSms_smsInexistente_lanzaIllegalStateExceptionConElId`** — Tipo: error. Verifica: `R-Sms-003`.
  - **Arrange:** `repository.find(100L)` → `null`.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** lanza `IllegalStateException` con mensaje `"No existe el SMS 100"`; `verify(smsSender, never()).send(any())` y `verify(repository, never()).save(any())` — RES-Sms-002 garantiza que la fila sigue ahí, así que **MUST** fallar y no callar; el envoltorio del ejecutor lo registra y el hilo del pool sobrevive. El mensaje **no** contiene identificadores de spec.
- **`enviarSms_primerIntento_fijaLaFechaDelPrimerIntento`** — Tipo: borde. Verifica: `R-Sms-004`.
  - **Arrange:** `Sms` en BD con `fechaPrimerIntentoEnvio = null` y `numeroReintentos = 0`.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** `fechaPrimerIntentoEnvio` no nula, `fechaUltimoIntentoEnvio` no nula, `numeroReintentos = 1`.
- **`enviarSms_reintento_noSobrescribeLaFechaDelPrimerIntentoYSumaUnReintento`** — Tipo: borde. Verifica: `R-Sms-004`.
  - **Arrange:** `Sms` en BD con `estado = FALLIDO`, `fechaPrimerIntentoEnvio` de hace dos días, `fechaUltimoIntentoEnvio` de ayer y `numeroReintentos = 1`.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** `fechaPrimerIntentoEnvio` **igual** que antes, `fechaUltimoIntentoEnvio` posterior a la de ayer y `numeroReintentos = 2` (CC-Sms-003: «ya no cambia en los reintentos»).
- **`enviarSms_intentoQueFalla_cuentaElIntentoAntesDeLlamarAlProveedor`** — Tipo: borde. Verifica: `R-Sms-004`.
  - **Arrange:** `Sms` en BD con `numeroReintentos = 0` y fechas de intento a `null`; `smsSender.send` lanza `RuntimeException`.
  - **Act:** ejecutar el `Runnable`.
  - **Assert:** `numeroReintentos = 1` y `fechaUltimoIntentoEnvio` no nula aunque el envío haya fallado — el intento se registra antes de llamar al proveedor.
- **`enviarSms_tarea_seEjecutaEnSuPropiaTransaccion`** — Tipo: borde. Verifica: `R-Sms-003`.
  - **Arrange:** `Sms` en BD como en el camino feliz; `mockStatic(JPA)` programado para ejecutar el `Runnable`.
  - **Act:** ejecutar el `Runnable` capturado.
  - **Assert:** `verify` de que se invocó `JPA.runInTransaction(any(Runnable.class))` — el hilo del pool abre su propia transacción.

### Método: `public AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteLosSieteCamposClienteYDeniegaLosDeServidor`** — Tipo: happy. Verifica: `—` (frontera de confianza de `insert`).
  - **Arrange:** el servicio construido (no hace falta nada más).
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty(...)` es `true` para `dniDestinatario`, `nombre`, `apellidos`, `telefono`, `mensaje`, `centro` e `historialEstado`; y `false` para `estado`, `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo` y `nombreExpediente`.

### Método: `public AllowProperties allowPropertiesReenviar()`

- **`allowPropertiesReenviar_devuelveWhitelistVacia`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** el servicio construido.
  - **Act:** `allowPropertiesReenviar()`.
  - **Assert:** `allowProperty(...)` es `false` para `estado`, `centro`, `telefono` y `mensaje` — el reenvío no acepta ningún dato del cliente (solo el `id`, que resuelve `ActionRequestHelper`).

### Método: `public AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_devuelveWhitelistVacia`** — Tipo: happy. Verifica: `—` (frontera de confianza de `update`; el rechazo de negocio V-Sms-012 es la otra puerta y lo cubren `validateUpdate_siempre_devuelveElMensajeDeInmutabilidad` y `update_siempre_lanzaUnsupportedOperationException`).
  - **Arrange:** el servicio construido.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty(...)` es `false` para los siete campos `cliente` y para los siete `servidor` — sin sobrescribirlo quedaría el `createAllowAllProperties()` de `DefaultModelService`, que es el detector de vulnerabilidad de `k-secure-coding` §3.4. Es la **segunda** puerta, además del rechazo de negocio.

### Método: `public AllowProperties allowPropertiesRemove()`

- **`allowPropertiesRemove_devuelveWhitelistVacia`** — Tipo: happy. Verifica: `—` (frontera de confianza de `remove`; el rechazo de negocio V-Sms-013 es la otra puerta y lo cubren `validateRemove_siempre_devuelveElMensajeDeNoBorrado` y `remove_siempre_lanzaUnsupportedOperationException`).
  - **Arrange:** el servicio construido.
  - **Act:** `allowPropertiesRemove()`.
  - **Assert:** `allowProperty(...)` es `false` para los siete campos `cliente` y para los siete `servidor`.

---

## Clase: `com.educaflow.subsystem.sms.controller.SmsController`  —  controlador

**Responsabilidad:** traducir las dos acciones remotas de reenvío a llamadas al servicio, sin lógica de negocio propia: resolver el servicio con `ModelServiceFactory`, leer `entidadOriginal` y `entidad` del `ActionRequest` (con la whitelist **pedida** al servicio) y responder (error con los `BusinessMessages`, o aviso + `refresh-tab`).
**Colaboradores a mockear:** `ModelServiceFactory` (colocado por reflexión) y el `SmsService` que devuelve `resolve(Sms.class)`; `ActionRequest` (con `getData()` → mapa con la clave `context`, que lleva `_model` = FQN de `Sms` y, cuando procede, `id`; `getContext()` sin programar → `null`) y `ActionResponse` (mocks); el estático `com.axelor.db.JpaRepository` (`mockStatic`) con `JpaRepository.of(Sms.class)` → mock cuyo `find(id)` devuelve el `Sms` de BD; el estático `I18n` como identidad. Mismo montaje que `TareaFirmaControllerTest`.
**Origen diseño:** paso 6, `SmsController.validateReenviar` y `SmsController.reenviar`. Reglas: no aporta ninguna (V-Sms-014 y V-Sms-015 viven en el servicio; los tests comprueban la **delegación** y que el controlador no añade guardas).

### Método: `public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`validateReenviar_servicioSinMensajes_noDevuelveNingunError`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** contexto con `_model` e `id = 100L`; `find(100L)` → `Sms` FALLIDO; `smsService.allowPropertiesReenviar()` → whitelist vacía (`AllowProperties.createAllowProperties(new HashMap<>())`); `smsService.validateReenviar(any(), any())` → `Optional.empty()`.
  - **Act:** `validateReenviar(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse, never()).setError(anyString())` (ninguna interacción con la respuesta).
- **`validateReenviar_servicioConMensajes_devuelveElMensajeComoError`** — Tipo: error. Verifica: `—` (delegación de `V-Sms-014`, que verifica `SmsServiceImpl`; aquí solo se fija que el controlador entrega los mensajes del servicio).
  - **Arrange:** igual, con `smsService.validateReenviar(any(), any())` → `Optional.of(BusinessMessages.single("Solo se pueden reenviar SMS que han fallado"))`.
  - **Act:** `validateReenviar(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setError(...)` con un texto que contiene `"Solo se pueden reenviar SMS que han fallado"`.
- **`validateReenviar_contextoSinId_pasaEntidadOriginalNulaAlServicio`** — Tipo: borde. Verifica: `—` (delegación de `V-Sms-014`, que verifica `SmsServiceImpl`; aquí solo se fija que el controlador pasa `entidadOriginal` tal cual).
  - **Arrange:** contexto con `_model` y **sin** `id`; `smsService.allowPropertiesReenviar()` → whitelist vacía; `validateReenviar(any(), isNull())` → `Optional.of(...)` con el mensaje de V-Sms-014.
  - **Act:** `validateReenviar(actionRequest, actionResponse)`.
  - **Assert:** no lanza (`assertDoesNotThrow`); `verify(smsService).validateReenviar(any(), isNull())` — el controlador pasa `entidadOriginal` **tal cual** y **MUST NOT** añadir ninguna guarda: el caso lo resuelve el servicio con un `BusinessMessage`.
- **`validateReenviar_pideLaWhitelistAlServicio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** el montaje del camino feliz.
  - **Act:** `validateReenviar(actionRequest, actionResponse)`.
  - **Assert:** `verify(smsService).allowPropertiesReenviar()` — la ruta de validación ve exactamente los mismos campos que la de ejecución; la whitelist **MUST NOT** construirse inline en el controlador.

### Método: `public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`reenviar_smsFallido_delegaEnElServicioYAvisaAlUsuario`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** contexto con `_model` e `id = 100L`; `find(100L)` → `Sms` FALLIDO con `centro`; `allowPropertiesReenviar()` → whitelist vacía; `smsService.reenviar(any(), any())` devuelve el mismo `Sms`.
  - **Act:** `reenviar(actionRequest, actionResponse)`.
  - **Assert:** `verify(smsService).reenviar(any(), any())`; `verify(actionResponse).setNotify("El reenvío del SMS se ha puesto en marcha.")`; `verify(actionResponse).setSignal("refresh-tab", null)`.
- **`reenviar_contextoConCamposDeServidor_noLlegaNingunoAlServicio`** — Tipo: borde (seguridad). Verifica: `—`.
  - **Arrange:** contexto con `_model`, `id = 100L` y además `"estado": "ENVIADO"` y `"telefono": "+34700000000"`; `find(100L)` → `Sms` FALLIDO con `telefono = "+34600111222"`; `allowPropertiesReenviar()` → whitelist vacía; `ArgumentCaptor` sobre `smsService.reenviar(...)`.
  - **Act:** `reenviar(actionRequest, actionResponse)`.
  - **Assert:** la `entidad` capturada conserva `estado = FALLIDO` y `telefono = "+34600111222"` (los valores de BD) — con la whitelist vacía nada de lo que manda el cliente se aplica.
- **`reenviar_servicioLanzaValidationException_noAvisaAlUsuario`** — Tipo: error. Verifica: `—` (es la delegación de `V-Sms-014`/`V-Sms-015`, que verifica `SmsServiceImpl`; aquí solo se fija que el rechazo del servicio se propaga).
  - **Arrange:** el mismo montaje, con `smsService.reenviar(any(), any())` lanzando `new ValidationException("Solo se pueden reenviar SMS que han fallado")`.
  - **Act:** `reenviar(actionRequest, actionResponse)`.
  - **Assert:** la excepción sale del controlador (`assertThrows`); `verify(actionResponse, never()).setNotify(anyString())` y `verify(actionResponse, never()).setSignal(anyString(), any())` — el controlador **MUST NOT** comprobar roles ni tragarse el rechazo.
- **`reenviar_pideLaWhitelistAlServicio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** el montaje del camino feliz.
  - **Act:** `reenviar(actionRequest, actionResponse)`.
  - **Assert:** `verify(smsService).allowPropertiesReenviar()` — misma whitelist que el validador, pedida al servicio.

---

## Clase: `com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl`  —  servicio (Modificar: solo el delta del paso 3)

**Responsabilidad (delta):** el campo inyectado pasa de `CorreoAsyncExecutor` a `EjecutorAsincrono` y la programación del envío pasa de `PostCommitRunner.runAfterCommit(() -> correoAsyncExecutor.submit(...))` a `ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviarCorreo(correoId))`; los dos `fireActionRule_Programar*Asincrono` (copias byte a byte) se funden en **uno** al que llaman `insert` y `reenviar`; y el privado `trazaCompleta` desaparece en favor de `TextUtil.trazaCompleta`. **El comportamiento observable no cambia.**
**Colaboradores a mockear:** los ya existentes en `CorreoServiceImplTest` (`CorreoRepository`, `MailSender`, estáticos `I18n`, `SecurityUtil`, `JPA`, `AppSettings`, `JpaRepository`), sustituyendo el mock de `CorreoAsyncExecutor` por un mock de `com.educaflow.base.infrastructure.async.EjecutorAsincrono` colocado por reflexión en el campo `ejecutorAsincrono`.
**Origen diseño:** paso 3, «Cambios en correos» (deltas 1, 2 y 3) y la fila `Modificar` de `src/test/java/.../CorreoServiceImplTest.java`. Tests **modificados, no nuevos**: se conservan **exactamente** los nombres que los tests ya tienen y solo se sustituyen los **cuatro** `Mockito.mockStatic(PostCommitRunner.class)` y el mock de `CorreoAsyncExecutor` por el mock de `EjecutorAsincrono` y su `verify(...).ejecutarTrasCommit(any())`. **MUST NOT** renombrarlos ni añadir tests nuevos en paralelo a los existentes, y **MUST NOT** perderse ninguna comprobación de las que ya hay. Cambio común a toda la clase: en `setUp`, `setField(service, "correoAsyncExecutor", correoAsyncExecutor)` pasa a `setField(service, "ejecutorAsincrono", ejecutorAsincrono)`.

### Método: `public Correo insert(Correo correo)`

- **`insert_correoValido_asignaValoresInicialesYPersiste`** — Tipo: happy. Verifica: `—` (test existente, solo el delta).
  - **Arrange:** el `correoValido()` del test actual; `isAdmin` → `true`; `repository.save` devuelve su argumento; el `try (MockedStatic<PostCommitRunner> …)` desaparece y en su lugar está el mock de `EjecutorAsincrono` inyectado por reflexión en el campo `ejecutorAsincrono`.
  - **Act:** `insert(correo)`.
  - **Assert:** las mismas aserciones de valores iniciales y el mismo `verify(repository).save(correo)` que hoy, y `verify(ejecutorAsincrono).ejecutarTrasCommit(any())` en lugar de `postCommitMock.verify(() -> PostCommitRunner.runAfterCommit(any()))`.
- **`insert_clienteEnviaEstadoOFechasFalsas_seSobrescribenIncondicionalmente`** — Tipo: borde (seguridad). Verifica: `—` (test existente, solo el delta).
  - **Arrange:** el `correoValido()` del test actual con `estado = EstadoCorreo.SUCCESS`, `fechaEnvio = LocalDateTime.of(2000, 1, 1, 0, 0)` y `numeroReintentos = 99`; `isAdmin` → `true`; `repository.save` devuelve su argumento; el `try (MockedStatic<PostCommitRunner> …)` desaparece y en su lugar está el mock de `EjecutorAsincrono` inyectado — sin este cambio el test **no compila**, porque `PostCommitRunner` se elimina.
  - **Act:** `insert(correo)`.
  - **Assert:** las mismas aserciones de sobrescritura incondicional que hoy: `estado = EstadoCorreo.PENDIENTE`, `numeroReintentos = 0` y `fechaEnvio` nulo.
- **`insert_correoInvalido_lanzaValidationExceptionYNoPersiste`** — Tipo: error. Verifica: `—` (test existente, solo el delta).
  - **Arrange:** `correoValido()` con `dniDestinatario = null`; `isAdmin` → `true`.
  - **Act:** `insert(correo)`.
  - **Assert:** lanza `ValidationException` con `"El DNI del destinatario es obligatorio"`; `verify(repository, never()).save(any())` y `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())` (sustituye al `verify(correoAsyncExecutor, never()).submit(any())`).

### Método: `public Correo reenviar(Correo entidad, Correo entidadOriginal)`

- **`reenviar_correoEnFail_programaEnvioAsincronoSinPersistirCambiosPropios`** — Tipo: happy. Verifica: `—` (test existente, solo el delta).
  - **Arrange:** el `entidadOriginal` en `FAIL` del test actual (con `id = 100L` y `centro = centroA`) y `entidad` con solo el `id`; `isAdmin` → `true`; en vez del `try (MockedStatic<PostCommitRunner> …)`, el mock de `EjecutorAsincrono`.
  - **Act:** `reenviar(entidad, entidadOriginal)`.
  - **Assert:** `assertSame(entidadOriginal, resultado)` y `verify(repository, never()).save(any())` como hoy, y `verify(ejecutorAsincrono).ejecutarTrasCommit(any())` **una** vez en lugar del `postCommitMock.verify(...)` — la política de envío queda con un solo dueño tras borrar `fireActionRule_ProgramarReenvioAsincrono`.
- **`reenviar_correoNoEnFail_lanzaValidationExceptionYNoProgramaEnvio`** — Tipo: error. Verifica: `—` (test existente, solo el delta).
  - **Arrange:** `entidadOriginal` en `PENDIENTE` (con `id = 100L` y `centro = centroA`); `isAdmin` → `true`; en vez del `try (MockedStatic<PostCommitRunner> …)`, el mock de `EjecutorAsincrono`.
  - **Act:** `reenviar(entidad, entidadOriginal)`.
  - **Assert:** lanza `ValidationException` con `"Solo se pueden reenviar correos que han fallado"`; `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())` en lugar de `postCommitMock.verify(() -> PostCommitRunner.runAfterCommit(any()), never())`.

### Método: `public void enviarCorreo(Long correoId)`

- **`enviarCorreo_envioFalla_marcaFailYGuardaTrazaCompleta`** — Tipo: error. Verifica: `—` (test existente **sin cambios**; es la red que comprueba que el delta 3 no cambia el texto guardado).
  - **Arrange:** el `correoParaEnvio()` del test actual; `mailSender.send` lanza `new RuntimeException("SMTP caído")`; `JPA.runInTransaction` y `AppSettings` mockeados como hoy.
  - **Act:** `enviarCorreo(correoId)`.
  - **Assert:** `estado = FAIL`, `fechaEnvio` nulo y `descripcionUltimoFallo` contiene `"SMTP caído"` — mismo texto que antes de mover el método a `TextUtil` (no se reimplementa ningún `trazaCompleta` privado).

Estos **seis** tests son los **únicos** de `CorreoServiceImplTest` que entran en el delta: los **cuatro** que hoy abren `Mockito.mockStatic(PostCommitRunner.class)` (`insert_correoValido_asignaValoresInicialesYPersiste`, `insert_clienteEnviaEstadoOFechasFalsas_seSobrescribenIncondicionalmente`, `reenviar_correoEnFail_programaEnvioAsincronoSinPersistirCambiosPropios` y `reenviar_correoNoEnFail_lanzaValidationExceptionYNoProgramaEnvio`), `insert_correoInvalido_lanzaValidationExceptionYNoPersiste`, cuyo `verify(correoAsyncExecutor, never()).submit(any())` pasa a ser `verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any())` al desaparecer `CorreoAsyncExecutor`, más `enviarCorreo_envioFalla_marcaFailYGuardaTrazaCompleta`, que se conserva tal cual. El **resto** de los tests de `CorreoServiceImplTest` (validaciones, `update`/`remove`, `allowProperties*`, `reenviar_usuarioNoAdminDeOtroCentro_lanzaValidationException`, `listarCorreosEnFail_*` y los demás `enviarCorreo_*`) **no** cambia: el delta no toca su comportamiento.

---

## Clase: `com.educaflow.subsystem.sms.db.Sms` (y el enum `com.educaflow.subsystem.sms.db.EstadoSms`) — sin lógica testable
**Motivo:** entidad y enum **generados** por Axelor a partir de `domains/Sms.xml`; POJO con getters/setters y sin `<extra-code-model>`. El único campo con cálculo, `nombreExpediente`, es un `formula="true"` (subselect SQL) sin código Java que ejecutar.

## Clase: `com.educaflow.subsystem.sms.service.SmsService` — sin lógica testable
**Motivo:** interfaz (`reenviar`, `validateReenviar`, `allowPropertiesReenviar`); el comportamiento está en `SmsServiceImpl`.

## Clase: `com.educaflow.subsystem.sms.module.SmsModule` — sin lógica testable
**Motivo:** módulo Guice cuyo `configure()` es un único `bind(SmsSender.class).toProvider(SmsSenderProvider.class)`. Cableado declarativo, sin ramas ni cálculo: se comprueba al arrancar la aplicación (paso 7) y en los E2E, no con JUnit.

## Clase: `com.educaflow.secretariavirtual.module.SecretariaVirtualModule` — sin lógica testable
**Motivo:** el delta es **una** línea de `bind(...).toProvider(...).in(Singleton.class)` en `configure()`. Cableado declarativo; lo que sí se testea es el `EjecutorAsincronoProvider` que esa línea referencia.

## Clase: `com.educaflow.subsystem.correos.module.CorreosModule` — sin lógica testable
**Motivo:** el delta solo **quita** dos `bind` de su `configure()` y conserva el de `MailSender`. Cableado declarativo; que la aplicación siga arrancando y los E2E de correos sigan pasando es la comprobación.

---

## Cobertura

- **Clases con lógica descritas: 9.**
  1. `com.educaflow.base.util.NumeroTelefono` (Crear)
  2. `com.educaflow.base.util.MensajeSmsUtil` (Crear)
  3. `com.educaflow.base.util.TextUtil` (Modificar — solo `trazaCompleta`)
  4. `com.educaflow.base.infrastructure.async.EjecutorAsincrono` (Crear)
  5. `com.educaflow.secretariavirtual.module.EjecutorAsincronoProvider` (Crear)
  6. `com.educaflow.secretariavirtual.startup.AppEventObserver` (Modificar — solo `onAppShutdown`)
  7. `com.educaflow.subsystem.sms.module.SmsSenderProvider` (Crear)
  8. `com.educaflow.subsystem.sms.service.impl.SmsServiceImpl` (Crear)
  9. `com.educaflow.subsystem.sms.controller.SmsController` (Crear)
  Más una **décima** clase que el diseño **modifica** y cuyos tests existentes se adaptan: `com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl` (solo el delta del paso 3).

- **Clases omitidas (sin lógica):** `com.educaflow.subsystem.sms.db.Sms` y `EstadoSms` (generadas), `com.educaflow.subsystem.sms.service.SmsService` (interfaz), `com.educaflow.subsystem.sms.module.SmsModule`, `com.educaflow.secretariavirtual.module.SecretariaVirtualModule` y `com.educaflow.subsystem.correos.module.CorreosModule` (cableado Guice declarativo).

- **Reglas server-side cubiertas (`V`/`R`/`CC`):** V-Sms-001, V-Sms-002, V-Sms-003, V-Sms-004, V-Sms-005, V-Sms-006, V-Sms-007, V-Sms-008, V-Sms-009, V-Sms-010, V-Sms-011, V-Sms-012, V-Sms-013, V-Sms-014, V-Sms-015, R-Sms-001, R-Sms-002, R-Sms-003, R-Sms-004, R-Sms-005, R-Sms-006. Las 15 `V` y las 6 `R` tienen su rama de fallo y, donde aplica, su rama OK.

- **Regla server-side NO cubierta con tests unitarios:** CC-Sms-008 (`nombreExpediente`). **Motivo:** es un `<string name="nombreExpediente" formula="true">` de `domains/Sms.xml` — un subselect SQL que evalúa la base de datos; no existe código Java que invocar, así que no es testeable con JUnit + Mockito. Se comprueba en los tests E2E de `test-e2e-desc.md` (columna del grid en las tres pantallas) y en la verificación del paso 4.

- **Reglas solo-cliente excluidas (E2E en `test-e2e-desc.md`):** U-sms-todos-001, U-sms-todos-002, U-sms-todos-003, U-sms-todos-004, U-sms-todos-005, U-sms-todos-006, U-sms-todos-007, U-sms-todos-008, U-sms-centro-001, U-sms-centro-002, U-sms-centro-003. Son `readonly`/`readonlyIf`/`showIf` de paneles, campos y botones, el widget `phone` y el `force-back` tras `save`: viven en los XML de `views/` y no se testean con JUnit.

- **Regresión obligatoria:** los tests unitarios de correos (`CorreoServiceImplTest` adaptado, `CorreoAsyncExecutorTest`/`PostCommitRunnerTest`/`CorreoEventObserverTest`/`CorreoAsyncExecutorProviderTest` trasladados a `EjecutorAsincronoTest`, `EjecutorAsincronoProviderTest` y `AppEventObserverTest`) **MUST** seguir cubriendo los mismos casos: ninguno de los cuatro tests que desaparecen pierde una comprobación.
