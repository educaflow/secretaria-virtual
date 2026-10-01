---
type: design
template: system
---

# Diseño: Subsistema de SMS

**Objetivo:** registrar y enviar por centro los SMS que la aplicación manda a las personas (un móvil de España y un texto que cabe en un solo SMS), consultarlos según el papel de cada usuario y reenviar los que fallan, replicando el subsistema de correos.
**Capa:** subsystem/sms
**Especificación de origen:** .sdd/drafts/2026-09-30_19-35_subsistema-sms/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-guice (el subsistema cablea `SmsSender` con un `Provider`, y el paso 3 cablea el `EjecutorAsincrono` único de la aplicación), k-datainit (el subsistema trae su propia `data-init` con los permisos)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `build.gradle` | Modificar | — | Dependencia `com.googlecode.libphonenumber:libphonenumber` (solo la usa `NumeroTelefono`) |
| `src/main/resources/axelor-config.properties` | Modificar | — | `sms.credentials.twilio.accountSid`, `sms.credentials.twilio.authToken` y `sms.twilio.from` (las tres vacías); `mail.send.pool-size` se sustituye por `async.pool-size` |
| `src/main/java/com/educaflow/base/util/NumeroTelefono.java` | Crear | k-code-quality | Teléfono: validar, saber si es móvil, si es de España y dar el E.164. Única clase que importa `libphonenumber` |
| `src/main/java/com/educaflow/base/util/MensajeSmsUtil.java` | Crear | k-code-quality | `cabeEnUnSms(String)`: decide la codificación y cuenta GSM-7 (límite 160, extendidos 2 unidades) o UCS-2 (límite 70) |
| `src/main/java/com/educaflow/base/util/TextUtil.java` | Modificar | k-code-quality | Un método más: `trazaCompleta(Throwable)` público, **movido** desde el privado de `CorreoServiceImpl` (único dueño de `descripcionUltimoFallo` para correos y SMS) |
| `src/main/java/com/educaflow/base/util/CLAUDE.md` | Modificar | k-code-quality | Documentar `NumeroTelefono`, `MensajeSmsUtil` y el nuevo `TextUtil.trazaCompleta` en «Clases disponibles» **y acotar en «Convenciones» la excepción de `NumeroTelefono`** (clase de instancia) |
| `src/main/java/com/educaflow/base/infrastructure/async/EjecutorAsincrono.java` | Crear | k-guice | **El** pool de hilos daemon de la aplicación (uno solo, compartido): ejecuta una tarea **solo si la transacción actual hace commit** |
| `src/main/java/com/educaflow/base/infrastructure/CLAUDE.md` | Modificar | — | Documentar el paquete nuevo `async` |
| `src/main/java/com/educaflow/secretariavirtual/module/EjecutorAsincronoProvider.java` | Crear | k-guice | Construye el `EjecutorAsincrono` leyendo `async.pool-size` (por defecto 2) |
| `src/main/java/com/educaflow/secretariavirtual/module/SecretariaVirtualModule.java` | Modificar | k-guice | Una línea: bindea `EjecutorAsincrono` como singleton (es donde el proyecto cablea lo transversal) |
| `src/main/java/com/educaflow/secretariavirtual/startup/AppEventObserver.java` | Modificar | k-guice | Su `onAppShutdown` **ya existente** para el ejecutor (`detener()`); no hace falta ningún observer nuevo |
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoAsyncExecutor.java` | Eliminar | — | Su pool es ahora el `EjecutorAsincrono` compartido (ver «Eliminaciones declaradas») |
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoEventObserver.java` | Eliminar | — | Su `detener()` lo hace ahora `AppEventObserver.onAppShutdown` |
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | Eliminar | — | Su función es ahora `EjecutorAsincrono.ejecutarTrasCommit` (ver «Eliminaciones declaradas») |
| `src/main/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProvider.java` | Eliminar | — | Lo sustituye `EjecutorAsincronoProvider` (una sola lectura de `*.pool-size` en todo el proyecto) |
| `src/main/java/com/educaflow/subsystem/correos/module/CorreosModule.java` | Modificar | k-guice | Quedan fuera los dos `bind` del pool y del observer; **se conserva** el de `MailSender` |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Modificar | k-sistemas (servicios.md) | Inyecta `EjecutorAsincrono` en vez de `CorreoAsyncExecutor`; su **único** `fireActionRule_ProgramarEnvioAsincrono` llama a `ejecutarTrasCommit` y lo llaman `insert` y `reenviar` (se borra el gemelo `fireActionRule_ProgramarReenvioAsincrono`, copia byte a byte); se borra el privado `trazaCompleta`, que pasa a `TextUtil`; fuera los imports de `PostCommitRunner`, `StringWriter` y `PrintWriter` |
| `src/main/java/com/educaflow/subsystem/sms/domains/Sms.xml` | Crear | k-sistemas (modelos.md) | Entidad `Sms` + enum `EstadoSms` |
| `src/main/java/com/educaflow/subsystem/sms/service/SmsService.java` | Crear | k-sistemas (servicios.md) | Interfaz: `reenviar` + su validador + su `allowProperties` |
| `src/main/java/com/educaflow/subsystem/sms/service/impl/SmsServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones, k-secure-coding | V-Sms-001…015, R-Sms-001…006, whitelists |
| `src/main/java/com/educaflow/subsystem/sms/controller/SmsController.java` | Crear | k-sistemas (controladores.md) | `@CallMethod` `validateReenviar` y `reenviar` |
| `src/main/java/com/educaflow/subsystem/sms/module/SmsModule.java` | Crear | k-guice | Un único binding: el `SmsSender` (ningún `ModelService`, ningún pool propio) |
| `src/main/java/com/educaflow/subsystem/sms/module/SmsSenderProvider.java` | Crear | k-guice | `SmsSender` de Twilio a partir de las propiedades de configuración |
| `src/main/java/com/educaflow/subsystem/sms/views/Main-Sms.xml` | Crear | k-vistas (grids.md, forms.md, actions.md) | Pantalla «Administración de SMS» (Administrador) |
| `src/main/java/com/educaflow/subsystem/sms/views/Centro-Sms.xml` | Crear | k-vistas (grids.md, forms.md) | Pantalla «SMS de mis centros» (Supervisor/Administrativo) |
| `src/main/java/com/educaflow/subsystem/sms/views/Mis-Sms.xml` | Crear | k-vistas (grids.md, forms.md) | Pantalla «Mis SMS» (cualquier usuario) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir el menú «SMS» (order 55, justo tras «Correos») |
| `src/main/java/com/educaflow/subsystem/sms/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de binding de los permisos del subsistema y de su enlace al grupo `users` (`priority="10"`) |
| `src/main/java/com/educaflow/subsystem/sms/data-init/input/auth-sms.xml` | Crear | k-datainit | Permisos condicionales `Sms.propio-destinatario` y `Sms.propio-centro-gestion`, enlazados a su propio `<group code="users">` |
| `src/test/java/com/educaflow/base/infrastructure/async/EjecutorAsincronoTest.java` | Crear | — | Tests trasladados de `PostCommitRunnerTest` + `CorreoAsyncExecutorTest` |
| `src/test/java/com/educaflow/secretariavirtual/module/EjecutorAsincronoProviderTest.java` | Crear | — | Test trasladado de `CorreoAsyncExecutorProviderTest` (lee `async.pool-size`, por defecto 2) |
| `src/test/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunnerTest.java` | Eliminar | — | Su contenido vive ahora en `EjecutorAsincronoTest` |
| `src/test/java/com/educaflow/subsystem/correos/infrastructure/CorreoAsyncExecutorTest.java` | Eliminar | — | Su contenido vive ahora en `EjecutorAsincronoTest` |
| `src/test/java/com/educaflow/subsystem/correos/infrastructure/CorreoEventObserverTest.java` | Eliminar | — | La clase que probaba desaparece; la parada del pool la prueba `AppEventObserverTest` |
| `src/test/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProviderTest.java` | Eliminar | — | Su contenido vive ahora en `EjecutorAsincronoProviderTest` |
| `src/test/java/com/educaflow/secretariavirtual/startup/AppEventObserverTest.java` | Crear | — | Que `onAppShutdown` llame a `ejecutorAsincrono.detener()` (caso trasladado de `CorreoEventObserverTest`) |
| `src/test/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImplTest.java` | Modificar | — | Sustituir los 4 `Mockito.mockStatic(PostCommitRunner.class)` por `verify(ejecutorAsincrono).ejecutarTrasCommit(any())` |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño. Los `i18n_*.csv` **MUST NOT** crearse a mano.

---

## Pasos

Los pasos 2 y 3 son prerrequisitos técnicos (`base/util` y `base/infrastructure`) del dominio y del servicio: se intercalan entre «ficheros estáticos y recursos» y «dominios» del orden obligatorio, porque el `domains/Sms.xml` y el `SmsServiceImpl` no se pueden compilar sin ellos.

### Paso 1 — Dependencia y propiedades de configuración

**Fichero:** `build.gradle` (Modificar) y `src/main/resources/axelor-config.properties` (Modificar).

- En `build.gradle`, junto al bloque `//Envio de SMS con Twilio`, añadir:
  `implementation 'com.googlecode.libphonenumber:libphonenumber:8.13.55'` con el comentario de que **solo** `com.educaflow.base.util.NumeroTelefono` puede importarla.
- En `axelor-config.properties`, un bloque nuevo con **exactamente** las tres propiedades que enumera
  `design-guidelines.md`:

  ```properties
  #Envio de SMS desde la secretaría virtual
  sms.credentials.twilio.accountSid=
  sms.credentials.twilio.authToken=
  sms.twilio.from=
  ```

  Las tres van **vacías**: los valores reales viven en la configuración privada
  (`../secretaria-virtual-private/axelor-config.dev.properties`), que sobrescribe este fichero.
  **MUST NOT** escribir aquí ninguna credencial real (`k-secure-coding` §8).
- En `axelor-config.properties`, **sustituir** la línea `mail.send.pool-size = 2` y su comentario
  `#Envio de correos desde la secretaría virtual` (bloque de Quartz) por:

  ```properties
  #Pool compartido de tareas en segundo plano (EjecutorAsincrono: envío de correos y de SMS)
  async.pool-size = 2
  ```

  Es la clave que lee `EjecutorAsincronoProvider` (paso 3); `mail.send.pool-size` deja de leerse y **MUST NOT**
  quedarse en el fichero.

**Verificar:** `./gradlew -q compileJava` resuelve la dependencia nueva;
`grep -n "sms\." src/main/resources/axelor-config.properties` muestra las tres propiedades y ninguna con valor;
`grep -n "pool-size" src/main/resources/axelor-config.properties` muestra solo `async.pool-size = 2`.

### Paso 2 — `base/util`: `NumeroTelefono`, `MensajeSmsUtil` y `TextUtil.trazaCompleta`

**Ficheros:** `base/util/NumeroTelefono.java` (Crear), `base/util/MensajeSmsUtil.java` (Crear), `base/util/TextUtil.java` (Modificar), `base/util/CLAUDE.md` (Modificar).

#### `com.educaflow.base.util.NumeroTelefono`

```java
public NumeroTelefono(String telefono);
private boolean esValido();
private boolean esMovil();
private boolean esDeEspana();
public boolean esMovilDeEspana();
public String enFormatoE164();
```

Encapsula un teléfono escrito por una persona. Es la **única** clase del proyecto que importa
`libphonenumber`, y ninguno de sus métodos públicos expone un tipo de esa librería (ni `PhoneNumber`, ni
`PhoneNumberUtil`, ni `PhoneNumberType`, ni `NumberParseException`): el objetivo es poder cambiar la librería
sin tocar el resto del código (`design-guidelines.md`). País por defecto: España (`"ES"`). **Estado:** un
campo privado con el número ya parseado (tipo de `libphonenumber`), `null` si el texto no se pudo parsear; se
parsea **una** vez en el constructor y todas las consultas lo reutilizan.

- **`NumeroTelefono(String)`** — no lanza nunca; si el texto (incluido `null`/blanco) no se puede parsear con el
  país por defecto, deja el número interno a `null`.
- **`esValido()`** (privado) — `true` si hay número parseado y la librería lo da por válido; nunca lanza.
- **`esMovil()`** (privado) — `true` si hay número parseado y su tipo es `MOBILE` o `FIXED_LINE_OR_MOBILE`
  (`963000000` → `false`); nunca lanza.
- **`esDeEspana()`** (privado) — `true` si hay número parseado y su prefijo de país es el 34 (`+33…` → `false`);
  nunca lanza.
- **`esMovilDeEspana()`** — la conjunción de los tres privados (RES-Sms-004); nunca lanza. Ver `decisiones.md` D1.
- **`enFormatoE164()`** — devuelve el E.164 (`+34600111222`); precondición `esMovilDeEspana()`, si no lanza
  `IllegalStateException` (**MUST NOT** devolver `null` ni el texto original). Su único llamador es R-Sms-001,
  después de V-Sms-006. Ver D1.

#### `com.educaflow.base.util.MensajeSmsUtil`

```java
public static boolean cabeEnUnSms(String mensaje);
private static boolean esCodificableEnGsm7(String mensaje);
private static int contarUnidadesGsm7(String mensaje);
```

Helper stateless (`static`, sin estado) de la familia de `DniUtil`/`EMailUtil`/`IbanUtil`. **Único** dueño de
la decisión «este texto cabe en un solo SMS» (`design-guidelines.md`; `decisiones.md` D2): ni la vista ni el
modelo la replican.

- **`cabeEnUnSms(String)`** — `true` si cabe en **una** parte (sin concatenación): GSM-7 ≤ 160 unidades si es
  codificable en GSM-7; si no, `mensaje.length() <= 70` (UCS-2). Una sola expresión que compone los dos privados.
  Precondición: mensaje no `null` ni en blanco, con
  `TextUtil.requireNonBlank(mensaje, "El mensaje del SMS es obligatorio")`. Ver `decisiones.md` D2.
- **`esCodificableEnGsm7(String)`** — `true` si **todos** los caracteres están en la tabla básica o en la
  extendida de GSM-7. Un solo carácter fuera de las dos (por ejemplo «á», «ó», «ú», que **no** pertenecen al
  alfabeto GSM-7) obliga a UCS-2.
- **`contarUnidadesGsm7(String)`** — suma las unidades del texto en GSM-7: 1 por carácter de la tabla básica y 2
  por carácter de la extendida (que viaja con un escape delante). Solo se llama cuando `esCodificableEnGsm7` es
  `true`.

Las dos tablas son **constantes privadas** de la clase, tomadas de 3GPP TS 23.038:

```text
BASICO    = "@£$¥èéùìòÇ\nØø\rÅå" + "Δ_ΦΓΛΩΠΨΣΘΞ" + "ÆæßÉ"
          + " !\"#¤%&'()*+,-./" + "0123456789:;<=>?"
          + "¡ABCDEFGHIJKLMNO"  + "PQRSTUVWXYZÄÖÑÜ§"
          + "¿abcdefghijklmno"  + "pqrstuvwxyzäöñüà"
EXTENDIDO = "\f^{}\\[~]|€"      (cada uno cuenta 2 unidades)
```

#### `com.educaflow.base.util.TextUtil` (Modificar — delta)

```java
public static String trazaCompleta(Throwable excepcion);
```

`StringWriter` + `PrintWriter`: devuelve el stack trace completo (con causas) como `String`, tal cual lo escribe
`printStackTrace`. Es el **mismo** cuerpo que hoy es el privado `CorreoServiceImpl.trazaCompleta`
(`CorreoServiceImpl.java:386`), que el paso 3 **borra**: el método se **mueve** aquí, no se copia. A partir de
este delta, «qué texto se guarda en `descripcionUltimoFallo`» tiene **un** solo dueño para los dos subsistemas
(`k-code-quality/disenyo.md` §«Una decisión con varios dueños»), y `base/util/CLAUDE.md` obliga a mirar aquí
antes de reimplementar cualquier helper. **MUST NOT** reutilizarse `AsciiTableUtil.getStackTrace`: es privado y
devuelve una `List<String>` con otro formato (indentado para tabla), no la traza estándar. No cambia ninguna
otra línea de `TextUtil`.

- En `base/util/CLAUDE.md`, **tres cambios**:
  - añadir las dos entradas a «Clases disponibles», por orden alfabético, con el mismo formato que las
    demás (una línea por método público: de `NumeroTelefono`, solo `esMovilDeEspana` y `enFormatoE164`);
  - añadir a la entrada ya existente de `TextUtil` la línea de `trazaCompleta` («devuelve el stack trace
    completo de un `Throwable` como `String`; úsalo siempre que haya que guardar o mostrar el detalle
    de un error, en vez de reimplementar el `StringWriter`»);
  - **acotar la frase de «Convenciones»** que hoy dice «Son helpers **stateless**: métodos `static` y sin
    estado mutable», porque `NumeroTelefono` es la primera clase **de instancia** del paquete. Reescribirla
    como: «Son helpers **stateless**: métodos `static` y sin estado mutable. **Excepción**: una clase de
    instancia solo cuando encapsula un parseo caro que todas sus consultas reutilizan y cuyo estado es
    **inmutable** tras el constructor (`NumeroTelefono`).» MUST NOT dejar la convención diciendo una cosa
    y la lista de clases otra.

**Verificar:** `./gradlew -q test` pasa; `grep -rn "libphonenumber" src/main/java | grep -v NumeroTelefono.java` no devuelve nada (la librería no se ha filtrado a ninguna otra clase).

### Paso 3 — `base/infrastructure/async/EjecutorAsincrono`: **un solo** ejecutor para toda la aplicación

**Ficheros:** `base/infrastructure/async/EjecutorAsincrono.java` (Crear), `base/infrastructure/CLAUDE.md` (Modificar), `secretariavirtual/module/EjecutorAsincronoProvider.java` (Crear), `secretariavirtual/module/SecretariaVirtualModule.java` (Modificar), `secretariavirtual/startup/AppEventObserver.java` (Modificar), `correos/infrastructure/PostCommitRunner.java`, `correos/infrastructure/CorreoAsyncExecutor.java`, `correos/infrastructure/CorreoEventObserver.java` y `correos/module/CorreoAsyncExecutorProvider.java` (Eliminar), `correos/module/CorreosModule.java` y `correos/service/impl/CorreoServiceImpl.java` (Modificar), y los ficheros de test del final de la tabla.

Motivo: extrae el mecanismo de correos a un único ejecutor compartido; ver `decisiones.md` D3.

#### `com.educaflow.base.infrastructure.async.EjecutorAsincrono`

```java
public EjecutorAsincrono(int tamanoPool);
public void ejecutarTrasCommit(Runnable tarea);
public void detener();
```

Pool de hilos daemon de tamaño fijo que ejecuta tareas **solo si la transacción actual hace commit**. Es un
**singleton** de la aplicación entera (lo bindea `SecretariaVirtualModule`): cualquier sistema/subsistema que
necesite trabajo en segundo plano lo **inyecta** tal cual. **MUST NOT** subclasearse por subsistema ni bindearse
una segunda vez.

- **`EjecutorAsincrono(int)`** — `Executors.newFixedThreadPool` con una `ThreadFactory` propia que nombra los
  hilos «async-N» (N incremental) y los marca daemon.
- **`ejecutarTrasCommit(Runnable)`** — registra un `Synchronization` en la transacción Hibernate actual
  (`JPA.em().unwrap(Session.class).getTransaction()`) y, en `afterCompletion`, si el estado es
  `STATUS_COMMITTED`, envía la tarea al pool; si hubo rollback la descarta. La tarea se envuelve para que una
  `RuntimeException` no controlada se registre con `log.error` y no mate al hilo del pool.
- **`detener()`** — `shutdown()`, `awaitTermination(10, SECONDS)` y `shutdownNow()` si no terminó; ante
  `InterruptedException` propaga la interrupción (`Thread.currentThread().interrupt()`) y hace `shutdownNow()`.

El contrato de `ejecutarTrasCommit` es **total**: declara las **tres** entradas posibles y ninguna queda a
interpretación (`k-code-quality/disenyo.md` §«El retorno defensivo que delega»):

| Entrada | Tratamiento |
|---|---|
| Transacción activa que hace **commit** | La tarea se envía al pool |
| Transacción activa que hace **rollback** | La tarea se **descarta**: no se trabaja sobre una fila que no existe en BD |
| **Ninguna transacción activa** en el hilo | **MUST** lanzar `IllegalStateException` en el acto, **antes** de registrar nada |

La tercera entrada es un error de **programación** del llamante (un `*ServiceImpl` o un controlador sin
`@Transactional`), no un caso de negocio: `registerSynchronization` sobre una transacción no activa no está
especificado. **MUST NOT** degradarse a «ejecutar la tarea ya» —el hilo del pool podría no ver todavía la fila,
que es justo el bug intermitente que este método existe para impedir— ni a «descartarla en silencio» —el SMS se
quedaría PENDIENTE para siempre sin que nadie se enterase—. Al fallar en el acto, quien la llame fuera de
transacción lo ve en el primer intento y añade el `@Transactional` que le falta, en vez de tener que sospecharlo.

#### Cableado del ejecutor único

```java
// Clase: com.educaflow.secretariavirtual.module.EjecutorAsincronoProvider implements Provider<EjecutorAsincrono>
public EjecutorAsincrono get();

// Clase: com.educaflow.secretariavirtual.module.SecretariaVirtualModule  (Modificar — delta)
protected void configure();

// Clase: com.educaflow.secretariavirtual.startup.AppEventObserver  (Modificar — delta)
public void onAppShutdown(@Observes ShutdownEvent event);
```

- **`EjecutorAsincronoProvider.get()`** — construye un `EjecutorAsincrono` nuevo con el tamaño de pool que lee
  de la propiedad de configuración `async.pool-size` (vía `AppSettings`), con 2 como valor por defecto si no
  está definida. Es el **único** sitio del proyecto que
  lee un tamaño de pool (antes había uno por subsistema). Hace falta `Provider` (no basta `bind(...).to(...)`)
  porque el tamaño viene de configuración y no de otro bean (`k-guice` §3.3/§4.2).
- **`SecretariaVirtualModule.configure()`** — **delta:** un binding más, que enlaza `EjecutorAsincrono` a
  `EjecutorAsincronoProvider` con ámbito singleton. No cambia
  ninguna otra línea de la clase.
- **`AppEventObserver.onAppShutdown(...)`** — **delta:** se le inyecta el `EjecutorAsincrono` (`@Inject`) y su
  `onAppShutdown` —que **ya existe** y ya se ejecuta al apagar— añade `ejecutorAsincrono.detener()` antes del log
  de despedida. **MUST NOT** crearse ningún observer nuevo: la pieza que escucha el `ShutdownEvent` de la
  aplicación ya está escrita y es esta.

#### Cambios en correos (comportamiento idéntico; **el resto de cada clase se conserva**)

```java
// Clase: com.educaflow.subsystem.correos.module.CorreosModule  (Modificar — delta)
protected void configure();

// Clase: com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl  (Modificar — delta)
private void fireActionRule_ProgramarEnvioAsincrono(Correo correo);
private void fireActionRule_ProgramarReenvioAsincrono(Correo correo);   // se BORRA
private String trazaCompleta(Throwable excepcion);                      // se BORRA
```

- **`CorreosModule.configure()`** — **delta:** desaparecen `bind(CorreoAsyncExecutor…)` y
  `bind(CorreoEventObserver.class)`; se **conserva** `bind(MailSender.class).toProvider(MailSenderProvider.class)`.
- **`CorreoServiceImpl.fireActionRule_ProgramarEnvioAsincrono(Correo)`** — **delta:** el campo inyectado pasa de
  `CorreoAsyncExecutor` a `EjecutorAsincrono`, y donde hoy hace
  `PostCommitRunner.runAfterCommit(() -> correoAsyncExecutor.submit(...))` pasa a hacer
  `ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviarCorreo(correoId))`. Se eliminan los imports de
  `PostCommitRunner` y `CorreoAsyncExecutor`. Si el método conserva un comentario, dice solo el «por qué» que el
  código no revela (se envía tras el commit porque antes el hilo del pool puede no ver la fila), **sin** citar
  ningún identificador de regla del diseño ni del spec, igual que `fireActionRule_ProgramarEnvioAsincrono(Sms)`.
- **Delta 2 — se BORRA `fireActionRule_ProgramarReenvioAsincrono(Correo)`** (hoy `CorreoServiceImpl.java:321`),
  que es copia byte a byte del anterior (`:312`), y la línea 77 de `reenviar` pasa a llamar al **mismo**
  `fireActionRule_ProgramarEnvioAsincrono(entidadOriginal)`. La política de envío queda con **un** solo dueño,
  igual que en `SmsServiceImpl` (ver `rules/R-Sms-003.md`: «si hubiera uno por disparador, la política de envío
  tendría dos dueños»).
- **Delta 3 — se BORRA el privado `trazaCompleta(Throwable)`** (hoy `CorreoServiceImpl.java:386`). Su cuerpo se
  **mueve** tal cual a `TextUtil.trazaCompleta` (paso 2) y la única línea que lo llamaba pasa a llamar al de
  `TextUtil`; se eliminan los imports de `StringWriter` y `PrintWriter`. No cambia ninguna otra línea.

- En `base/infrastructure/CLAUDE.md`, añadir a la lista de paquetes:
  `- async — Ejecución de tareas en segundo plano atadas al commit de la transacción actual (EjecutorAsincrono, único y compartido por toda la aplicación).`
- Tests: crear `src/test/java/com/educaflow/base/infrastructure/async/EjecutorAsincronoTest.java` con los
  casos de los dos tests que se eliminan (commit ejecuta la tarea / rollback no la ejecuta / hilos daemon
  con nombre / una tarea que lanza no rompe el pool / `detener` para el pool) **más el caso nuevo de la
  tercera entrada del contrato: sin transacción activa, `IllegalStateException` y ninguna tarea enviada
  al pool**;
  `EjecutorAsincronoProviderTest` recoge el caso de `CorreoAsyncExecutorProviderTest` (lee `async.pool-size`,
  por defecto 2); `AppEventObserverTest` recoge el de `CorreoEventObserverTest` (`onAppShutdown` llama a
  `detener()`); y en `CorreoServiceImplTest` se sustituyen los cuatro `Mockito.mockStatic(PostCommitRunner.class)`
  por `verify(ejecutorAsincrono).ejecutarTrasCommit(any())` (y `never()` donde el test verifica que no se
  programa envío). No se pierde ninguna comprobación.

**Verificar:** `./run.sh` compila y pasa `check` (incluido `cpdCheck`, que es el motivo del paso);
`EjecutorAsincronoTest` cubre las **tres** entradas de `ejecutarTrasCommit` —commit ejecuta la tarea, rollback no la ejecuta y **sin transacción activa lanza `IllegalStateException` sin haber enviado nada al pool**—, de modo que ninguna de las tres queda sin declarar ni sin probar;
`grep -rn "PostCommitRunner\|CorreoAsyncExecutor\|CorreoEventObserver\|ProgramarReenvioAsincrono" src/` no devuelve nada;
`grep -rn "private String trazaCompleta" src/main/java` no devuelve nada y `grep -rn --include=*.java "trazaCompleta" src/main/java` devuelve **tres** líneas: la declaración en `TextUtil` y las dos llamadas (`CorreoServiceImpl`, `SmsServiceImpl`);
`grep -rn "new EjecutorAsincrono\|bind(EjecutorAsincrono" src/main/java` devuelve **una** línea de cada;
el alta de un correo sigue dejando el correo en «Enviado»/«Fallido» (los E2E de correos de
`src/test/e2e/subsystem/correos/` siguen pasando).

### Paso 4 — Dominio `Sms`

**Fichero:** `subsystem/sms/domains/Sms.xml` (Crear) — XML completo en `design/domains/Sms.xml`, válido contra `domain-models.xsd`.

**Resumen estructural:** módulo `sms`, paquete `com.educaflow.subsystem.sms.db`.

- Datos del destinatario (`cliente`): `dniDestinatario`, `nombre`, `apellidos` (string, texto libre: el DNI no enlaza con ninguna ficha).
- `telefono` (string, `cliente`): el móvil, guardado en E.164 (RES-Sms-004).
- `mensaje` (string `multiline`, `cliente`): el texto. **Sin `max`**: el límite es de VAL-Sms-008, que depende de la codificación (160 unidades GSM-7 o 70 caracteres UCS-2); un `max` sería un segundo dueño de la misma decisión con un mensaje genérico de JPA.
- `centro` (many-to-one a `subsystem.common.db.Centro`, `cliente`) y `historialEstado` (many-to-one a `subsystem.expedientes.db.HistorialEstado`, `cliente`, opcional).
- Datos del envío, todos `servidor`: `estado` (enum `EstadoSms`), `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo`.
- `nombreExpediente`: campo derivado `formula="true"` (CC-Sms-008, `momento: lectura`) con un subselect correlacionado sobre `expedientes_historial_estado` + `expedientes_expediente`; `null` si el SMS no tiene estado de expediente. No se persiste.
- Enum `EstadoSms`: `PENDIENTE` («Pendiente»), `ENVIADO` («Enviado»), `FALLIDO` («Fallido») — los nombres que fija `entity-Sms.md`.
- **Sin** `finder-method` y **sin** `repository="abstract"`: no hay ninguna consulta propia (el reenvío en bloque está fuera de alcance, así que no hay `findByEstado`).
- La dependencia va `subsystem/sms → subsystem/expedientes` (solo lectura de `HistorialEstado`), nunca al revés, igual que en correos.

**Verificar:** `./gradlew -q build` genera `build/src-gen/.../subsystem/sms/db/Sms.java` y `EstadoSms.java`; al arrancar, la tabla `sms_sms` existe con la columna `historial_estado`.

### Paso 5 — Servicio `SmsService` / `SmsServiceImpl`

**Ficheros:** `subsystem/sms/service/SmsService.java` (Crear) y `subsystem/sms/service/impl/SmsServiceImpl.java` (Crear). Ningún fichero de otro subsistema: V-Sms-015 usa los `tieneTipoUsuario` que `subsystem/common/domains/User.xml` ya tiene, y V-Sms-011 se queda con el `HistorialEstado` que la propia entidad trae.

`ModelServiceFactory` descubre la implementación por su FQN, así que **MUST NOT** crearse ningún binding Guice para ella.

#### Interfaz `com.educaflow.subsystem.sms.service.SmsService extends ModelService<Sms>`

```java
Sms reenviar(Sms entidad, Sms entidadOriginal);
Optional<BusinessMessages> validateReenviar(Sms entidad, Sms entidadOriginal);
AllowProperties allowPropertiesReenviar();
```

Solo declara la acción propia del subsistema y sus dos compañeras obligatorias. **MUST NOT** re-declarar
`insert`/`update`/`remove` ni sus `validate*`/`allowProperties*`: vienen de `ModelService<Sms>` con defaults en
`DefaultModelService<Sms>`.

#### Clase `com.educaflow.subsystem.sms.service.impl.SmsServiceImpl`

`extends DefaultModelService<Sms> implements SmsService`.

```java
public SmsServiceImpl(Class<Sms> model, Repository<Sms> repository);   // super(model, repository)

@Override public Sms insert(Sms sms);
@Override public Sms update(Sms nuevo, Sms original);
@Override public void remove(Sms sms);
@Override public Sms reenviar(Sms entidad, Sms entidadOriginal);

/****************************************************************************************/
/******************************** Métodos de Validación *********************************/
/****************************************************************************************/
@Override public Optional<BusinessMessages> validateInsert(Sms sms);
private void validarDestinatario(Sms sms, BusinessMessages messages);
private void validarTelefono(Sms sms, BusinessMessages messages);
private void validarMensaje(Sms sms, BusinessMessages messages);
private void validarCentro(Sms sms, BusinessMessages messages);
private void validarHistorialEstado(Sms sms, BusinessMessages messages);
@Override public Optional<BusinessMessages> validateUpdate(Sms nuevo, Sms original);
@Override public Optional<BusinessMessages> validateRemove(Sms sms);
@Override public Optional<BusinessMessages> validateReenviar(Sms entidad, Sms entidadOriginal);

/**************************************************************************************/
/********************************   AllowProperties   *********************************/
/**************************************************************************************/
@Override public AllowProperties allowPropertiesInsert();
@Override public AllowProperties allowPropertiesReenviar();
@Override public AllowProperties allowPropertiesUpdate();
@Override public AllowProperties allowPropertiesRemove();

/*************************************************************************************/
/********************************    Action Rules    *********************************/
/*************************************************************************************/
private void fireActionRule_NormalizarTelefono(Sms sms);
private void fireActionRule_AsignarValoresIniciales(Sms sms);
private void fireActionRule_ProgramarEnvioAsincrono(Sms sms);
private void fireActionRule_RegistrarIntentoEnvio(Sms sms);
private void fireActionRule_MarcarEnvioCorrecto(Sms sms);
private void fireActionRule_MarcarEnvioFallido(Sms sms, RuntimeException excepcion);

/*************************************************************************************/
/********************************    Otras funciones    ******************************/
/*************************************************************************************/
private void enviarSms(Long smsId);
```

**Dependencias** (ninguna es `ModelService`, así que van con `@Inject`):
`jakarta.inject.Provider<SmsSender> smsSenderProvider` (ver `decisiones.md` D4) y
`com.educaflow.base.infrastructure.async.EjecutorAsincrono ejecutorAsincrono` (solo se inyecta; ver paso 3).
El constructor lo invoca `ModelServiceFactory` por reflexión, así que esa firma es obligatoria.

El nombre simple `Sms` es el de la **entidad** (`com.educaflow.subsystem.sms.db.Sms`). El record de transporte
`com.educaflow.base.infrastructure.sms.Sms` se referencia por su FQN —solo aparece en la línea del `send`, dentro
de `enviarSms`— y **MUST NOT** importarse, para que no choque con la entidad.

##### Acciones

- **`insert(Sms)`** — secuencia: (1) `validateInsert(sms).ifPresent(BusinessMessages::throwIfInvalid)` →
  V-Sms-001…011; (2) `fireActionRule_NormalizarTelefono(sms)` (R-Sms-001, Antes); (3)
  `fireActionRule_AsignarValoresIniciales(sms)` (R-Sms-002, Antes); (4) `sms = repository.save(sms)` —**nunca**
  `super.insert`; (5) `fireActionRule_ProgramarEnvioAsincrono(sms)` (R-Sms-003, Después); (6) `return sms`.
  Es también la puerta del alta programática de trámites y otros subsistemas
  (`modelServiceFactory.resolve(Sms.class).insert(sms)`): no hay DTO propio porque los siete campos `cliente` de
  la entidad son exactamente los datos del alta.
- **`update(Sms nuevo, Sms original)`** — RES-Sms-001: el SMS es inmutable tras su creación, así que no hay flujo
  normal, solo el rechazo: lanza `UnsupportedOperationException` con un mensaje traducible (`I18n.get`) que
  transmita que el SMS no se puede modificar una vez creado (el mismo que V-Sms-012)
  (`k-secure-coding` §9.2: el par `validateUpdate` que siempre rechaza + esta excepción).
- **`remove(Sms)`** — RES-Sms-002, mismo patrón: lanza `UnsupportedOperationException` con un mensaje
  traducible (`I18n.get`) que transmita que los SMS no se pueden borrar (el mismo que V-Sms-013).
- **`reenviar(Sms entidad, Sms entidadOriginal)`** — secuencia: (1)
  `validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid)` → la guarda de
  `entidadOriginal == null`, V-Sms-014 y V-Sms-015; (2) `fireActionRule_ProgramarEnvioAsincrono(entidadOriginal)`
  (R-Sms-003, el **mismo** método que `insert`); (3) `return entidadOriginal`. **MUST NOT** `repository.save`
  aquí: el reenvío no cambia ningún campo de forma síncrona; todo el cambio de estado ocurre dentro de
  `enviarSms`, en la transacción del hilo del pool (`rules/R-Sms-003.md`).

##### Métodos de validación (V-)

`validateInsert(Sms)` acumula en un `BusinessMessages` y devuelve `messages.isValid() ? empty : of(messages)`,
delegando en los cinco privados para que cada grupo de reglas se lea junto: `validarDestinatario` (V-Sms-001…004),
`validarTelefono` (V-Sms-005, V-Sms-006), `validarMensaje` (V-Sms-007, V-Sms-008), `validarCentro` (V-Sms-009,
V-Sms-010) y `validarHistorialEstado` (V-Sms-011). Los cuatro primeros se llaman sin condición; el de historial,
**solo** cuando el SMS trae centro **y** estado de expediente, condición que se ve en el propio
`validateInsert` (no dentro del privado): sin `historialEstado` la regla no aplica (es el caso normal: ningún escenario del spec crea un
SMS con él, y entrar convertiría cada alta en un NPE o en el mensaje falso «El estado del expediente indicado no
existe»), y sin centro ya ha hablado V-Sms-009. La condición de cuándo aplica está donde se declaran las reglas,
no dentro del privado (`k-code-quality/disenyo.md` §«Una regla que decide sola si aplica»).

**`validarDestinatario(Sms, BusinessMessages)`**

- **V-Sms-001** (Origen spec: VAL-Sms-001) `dniDestinatario` indicado: `null` o blanco → mensaje que transmita
  que el DNI del destinatario es obligatorio.
- **V-Sms-002** (Origen spec: VAL-Sms-002) `dniDestinatario` válido: **solo** si el anterior pasó (rama
  `else if`, porque sin DNI no se puede comprobar su letra); se comprueba con `DniUtil.isValid(...)`. Mensaje:
  que el DNI no es válido y que compruebe la letra. **MUST NOT** volcar el DNI completo en el mensaje ni en
  ningún log (`DniUtil.enmascarar` si hiciera falta).
- **V-Sms-003** (Origen spec: VAL-Sms-003) `nombre` indicado, y **V-Sms-004** (Origen spec: VAL-Sms-004)
  `apellidos` indicados. Independientes entre sí: si faltan los dos, el usuario ve los dos mensajes (lo exige
  ESC-005).

**`validarTelefono(Sms, BusinessMessages)`**

- **V-Sms-005** (Origen spec: VAL-Sms-005) `telefono` indicado: `null` o blanco → mensaje de obligatorio.
- **V-Sms-006** (Origen spec: VAL-Sms-006, RES-Sms-004) móvil de España válido: **solo** si el anterior pasó;
  construye `new NumeroTelefono(sms.getTelefono())` y exige `esMovilDeEspana()` — **una** sola pregunta, porque
  la clasificación de negocio es de `NumeroTelefono`, no del servicio (un número sin prefijo se entiende de
  España, que es el país por defecto de `NumeroTelefono`). Un fijo (`963000000`) o un número incompleto
  (`60011`) fallan por la misma regla y con el mismo mensaje, que debe transmitir qué se espera y un ejemplo
  (`600111222`).

**`validarMensaje(Sms, BusinessMessages)`**

- **V-Sms-007** (Origen spec: VAL-Sms-007) `mensaje` indicado: `null` o blanco
  (`TextUtil.isNullOrBlank(sms.getMensaje())`) → obligatorio (null o blanco: es la precondición de `cabeEnUnSms`,
  ver `decisiones.md` D2).
- **V-Sms-008** (Origen spec: VAL-Sms-008) cabe en un solo SMS: **solo** si el anterior pasó; delega
  **enteramente** en `MensajeSmsUtil.cabeEnUnSms(...)` — el servicio **MUST NOT** contar caracteres ni conocer
  las tablas GSM-7. Mensaje: que no cabe en un solo SMS, con los dos límites (160, o 70 si contiene acentos u
  otros caracteres especiales).

**`validarCentro(Sms, BusinessMessages)`**

- **V-Sms-009** (Origen spec: VAL-Sms-009) `centro` indicado: `sms.getCentro() == null` → mensaje de obligatorio.
- **V-Sms-010** (Origen spec: VAL-Sms-010) **solo** si el anterior pasó (va en el `else` de V-Sms-009, igual que
  las otras tres parejas de este `validateInsert` y que `CorreoServiceImpl.validarCentro`): si el usuario
  autenticado (`SecurityUtil.getUser()`) **no** es administrador (`SecurityUtil.isAdmin`), el centro
  indicado **MUST** ser uno de los suyos (lo responde `User.perteneceAlCentro`). Rama explícita para el
  administrador, nunca quitar el filtro (`k-secure-coding` §4). El usuario se obtiene **siempre** con
  `SecurityUtil.getUser()`, nunca con `AuthUtils.getUser()` ni del JSON. Mensaje: que no puede crear SMS para un
  centro que no es suyo. Al ir en el `else`, un alta sin centro produce **un** solo mensaje (el de V-Sms-009) y
  no el falso «no es suyo» sobre un centro que no existe.

**`validarHistorialEstado(Sms, BusinessMessages)`**

- **V-Sms-011** (Origen spec: VAL-Sms-011) solo se evalúa si el SMS trae centro y estado de expediente (la
  condición está en `validateInsert`, arriba), así que el método **no** tiene guarda de entrada. Comprueba, en
  una única condición null-safe, las dos mitades que exige `k-secure-coding` §3.6 para una referencia al padre
  que dicta el cliente, sobre el `HistorialEstado` que la propia entidad ya trae: falla si la referencia no está
  resuelta (no tiene id o no tiene expediente) o si el centro de su expediente es distinto del centro del SMS.
  Mensaje: que el estado del expediente indicado no existe.

Las dos mitades —(a) referencia resuelta, (b) mismo centro—, por qué no basta con (a), por qué no se relee por
repositorio (C27) y por qué diverge de correos (V-Correo-014): ver `decisiones.md` D7.

**Resto de validadores**

- **`validateUpdate(Sms nuevo, Sms original)`** — **V-Sms-012** (Origen spec: RES-Sms-001): sin condición —el SMS
  es inmutable, siempre se rechaza con un único mensaje (`BusinessMessages.single`) que transmita que el SMS no
  se puede modificar una vez creado.
- **`validateRemove(Sms)`** — **V-Sms-013** (Origen spec: RES-Sms-002): siempre se rechaza con un único mensaje
  (`BusinessMessages.single`) que transmita que los SMS no se pueden borrar.
- **`validateReenviar(Sms entidad, Sms entidadOriginal)`** — se comprueba **siempre** sobre `entidadOriginal` (el
  estado real en BD): `entidad` solo trae el id, porque `allowPropertiesReenviar()` es una whitelist vacía.
  - Lo primero: si no hay `entidadOriginal` (el contexto puede no traer id), devuelve ya el mensaje de V-Sms-014
    sin evaluar nada más.
  - **V-Sms-014** (Origen spec: VAL-Sms-012) el estado del SMS original **MUST** ser `FALLIDO`; si es otro
    → mensaje que transmita que solo se pueden reenviar SMS que han fallado. Es el **mismo** texto que usa la
    guarda del `null` de arriba: sin `entidadOriginal` no hay ningún SMS fallido que reenviar.
  - **V-Sms-015** (Origen spec: VAL-Sms-013) si el usuario **no** es administrador, **MUST gestionar** el centro
    del SMS, es decir ser SUPERVISOR o ADMINISTRATIVO en él. El usuario sale de `SecurityUtil.getUser()` y el
    centro, del SMS original; falla cuando el usuario no es administrador (`SecurityUtil.isAdmin`) y
    `User.tieneTipoUsuario` no le reconoce en ese centro ni el tipo `SUPERVISOR` ni el `ADMINISTRATIVO`.
    Mensaje: que transmita que el usuario no puede reenviar SMS de un centro que no gestiona (el de VAL-Sms-013).
  - Las dos reglas son independientes entre sí y se acumulan en el mismo `BusinessMessages`.

Por qué V-Sms-015 no se queda en `perteneceAlCentro` (diverge de V-Correo-018), por qué no se unifica con V-Sms-010
y dónde más está la lista `{SUPERVISOR, ADMINISTRATIVO}`: ver `decisiones.md` D5.

##### AllowProperties

- **`allowPropertiesInsert()`** — whitelist **explícita** (`createAllowProperties`) con los siete campos
  `cliente`: `dniDestinatario`, `nombre`, `apellidos`, `telefono`, `mensaje`, `centro`, `historialEstado`. Ver
  «Frontera de confianza».
- **`allowPropertiesReenviar()`** — whitelist **vacía** (`createAllowProperties` de un `Map` vacío). Ver
  «Frontera de confianza».
- **`allowPropertiesUpdate()`** y **`allowPropertiesRemove()`** — las dos, whitelist **vacía**
  (`createAllowProperties` de un `Map` vacío), una línea cada una. Ver «Frontera de confianza».

##### Action rules (R-)

- **`fireActionRule_NormalizarTelefono(Sms)`** — aplica **R-Sms-001** (Origen spec: RN-Sms-001, RES-Sms-004).
  Momento: **Antes** de `repository.save`. Asignación **incondicional**: sustituye el `telefono` del SMS por su
  forma E.164, que calcula `NumeroTelefono.enFormatoE164()` a partir del teléfono recibido. **MUST NOT** añadir
  ninguna guarda que se salte la normalización cuando el teléfono «ya parece» estar en E.164: la canonicalización tiene que ocurrir en todas las entradas (formulario,
  alta programática y endpoint REST automático) para que RES-Sms-004 se cumpla siempre. Es seguro porque
  V-Sms-006 ya rechazó todo lo que no sea un móvil de España, de modo que `enFormatoE164()` nunca puede lanzar
  aquí (ver `decisiones.md` D6).
- **`fireActionRule_AsignarValoresIniciales(Sms)`** — aplica **R-Sms-002** (Origen spec: CC-Sms-001…CC-Sms-007 y
  RES-Sms-003). Momento: **Antes** de `repository.save`. Asignación **incondicional** de los siete campos
  `servidor` (`k-secure-coding` §3.3): `estado = EstadoSms.PENDIENTE`,
  `fechaCreacion = LocalDateTime.now(Convert.defaultZoneId)`, `numeroReintentos = 0`, y
  `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio` y `descripcionUltimoFallo` a `null`.
  **MUST NOT** envolver ninguna en `if (campo == null)`: con la guarda, un atacante que mandara el campo relleno
  por el endpoint REST genérico vería respetado su valor (`k-secure-coding` §3.3). Poner `fechaEnvio` a `null`
  también sostiene RES-Sms-003 (solo hay fecha de envío en ENVIADO).
- **`fireActionRule_ProgramarEnvioAsincrono(Sms)`** — aplica **R-Sms-003** (Origen spec: RN-Sms-002, RN-Sms-003).
  Momento: **Después** de `repository.save`. Diseño detallado en `design/rules/R-Sms-003.md`.
- **`fireActionRule_RegistrarIntentoEnvio(Sms)`** — aplica **R-Sms-004** (Origen spec: CC-Sms-003, CC-Sms-004,
  CC-Sms-005). Momento: **Antes** del `repository.save` de `enviarSms`. Asignación **incondicional**:
  `fechaUltimoIntentoEnvio = ahora` y `numeroReintentos = numeroReintentos + 1`; `fechaPrimerIntentoEnvio = ahora`
  **solo** si seguía a `null` (CC-Sms-003: «ya no cambia en los reintentos»). Ese `if` **no** es el antipatrón de
  mass-assignment: no depende de nada que mande el cliente (el único parámetro externo es el id), sino de si ya
  había un primer intento en BD.
- **`fireActionRule_MarcarEnvioCorrecto(Sms)`** — aplica **R-Sms-005** (Origen spec: CC-Sms-001, CC-Sms-006,
  RES-Sms-003). Asignación **incondicional**: `estado = ENVIADO`, `fechaEnvio = ahora`,
  `descripcionUltimoFallo = null`.
- **`fireActionRule_MarcarEnvioFallido(Sms, RuntimeException)`** — aplica **R-Sms-006** (Origen spec: CC-Sms-001,
  CC-Sms-007, RES-Sms-003). Asignación **incondicional**: `estado = FALLIDO`,
  `descripcionUltimoFallo = TextUtil.trazaCompleta(excepcion)`, `fechaEnvio = null` (RES-Sms-003: fuera de
  ENVIADO nunca hay fecha de envío).

##### Otras funciones

```java
private void enviarSms(Long smsId);
```

Cuerpo de la tarea asíncrona de R-Sms-003, en un hilo del pool con su propia transacción:
`JPA.runInTransaction(() -> { … })`. Por qué es privado y lanza ante un SMS inexistente (diverge de
`CorreoService.enviarCorreo`): `decisiones.md` D8. Secuencia, errores y diagrama: `design/rules/R-Sms-003.md`.

**Verificar:** `./gradlew -q test` pasa; `grep -n "super.insert\|super.update\|super.remove" src/main/java/com/educaflow/subsystem/sms/service/impl/SmsServiceImpl.java` no devuelve nada; `grep -n "== null)" …/SmsServiceImpl.java` solo aparece en validaciones, en la guarda de `fechaPrimerIntentoEnvio` y en el `if (sms == null) throw` de `enviarSms`; `grep -n "JpaRepository" …/SmsServiceImpl.java` no devuelve nada (V-Sms-011 no lee por repositorio ninguna entidad de otro subsistema, así que la regla **C27** de `agent_docs/architecture-rules.md` no se roza); `grep -nE "V-Sms-|VAL-Sms-|RES-Sms-|CC-Sms-|RN-Sms-|ESC-|k-secure-coding|k-validaciones" …/SmsServiceImpl.java` no devuelve ninguna línea de mensaje ni de excepción (los identificadores de spec no se vuelcan al código ni a los mensajes).

### Paso 6 — Controlador `SmsController`

**Fichero:** `subsystem/sms/controller/SmsController.java` (Crear). Un controlador por entidad; **MUST NOT** exponer `insert`/`update`/`remove` ni `validateSave`/`validateDelete` (los dan el endpoint REST automático y las acciones globales `remote-validation*`).

```java
// Clase: com.educaflow.subsystem.sms.controller.SmsController
//   @Inject private ModelServiceFactory modelServiceFactory;   (única inyección)

@CallMethod
public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse);

@CallMethod
@Transactional   // el reenvío programa la tarea sobre la transacción actual: su commit es lo que la dispara
public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse);
```

Los dos métodos resuelven el servicio con `modelServiceFactory.resolve(Sms.class)` y no tienen lógica de negocio
propia. El nombre de cada uno **MUST** coincidir con el `{nombreFuncionJava}` de su acción y con el método del
servicio, y los parámetros se llaman **siempre** `actionRequest` y `actionResponse`.

- **`validateReenviar`** — invocado por `subsysSms.Main@Sms-Remote-validateReenviar-action` (`Main-Sms.xml`),
  antes de reenviar. Obtiene `entidadOriginal = actionRequestHelper.getOriginalModel()` y
  `entidad = actionRequestHelper.getModel(smsService.allowPropertiesReenviar())` —la **misma** whitelist que usa
  la operación, **pedida** al servicio y **MUST NOT** construida inline
  (`k-sistemas/controladores.md` §«Parámetros, AllowProperties y respuesta»): la ruta de validación y la de
  ejecución tienen que ver exactamente los mismos campos, así que quien mañana añada uno a
  `allowPropertiesReenviar()` lo verá también en el validador; con la whitelist vacía de hoy el comportamiento no
  cambia—. Llama a `smsService.validateReenviar(entidad, entidadOriginal)` y, si hay mensajes, los entrega con
  `actionResponseHelper.doResponseBusinessMessagesAsError(...)`.
  Pasa `entidadOriginal` tal cual; **MUST NOT** añadir guarda (la tiene el servicio, paso 5).
- **`reenviar`** — invocado por `subsysSms.Main@Sms-Remote-reenviar-action` (`Main-Sms.xml` y `Centro-Sms.xml`).
  Obtiene `entidadOriginal` y `entidad` igual que el anterior (whitelist **pedida** al servicio, **MUST NOT**
  construirse inline), llama a `smsService.reenviar(entidad, entidadOriginal)` y responde con
  `actionResponse.setNotify(I18n.get("El reenvío del SMS se ha puesto en marcha."))` y
  `actionResponse.setSignal("refresh-tab", null)`, que recarga la pestaña para ver los reintentos.
  Lleva `@Transactional` porque el reenvío programa la tarea sobre la transacción actual: sin él,
  `EjecutorAsincrono.ejecutarTrasCommit` falla con `IllegalStateException`, que es su tercera entrada declarada
  (paso 3). **MUST NOT** comprobar roles aquí: V-Sms-015 vive en el servicio.

**Verificar:** `./gradlew -q compileJava`; `grep -n "actionRequest\|actionResponse" …/SmsController.java` confirma que los parámetros se llaman así; `grep -n "createAllowProperties" …/SmsController.java` no devuelve nada; `grep -c "getModel(smsService.allowPropertiesReenviar())" …/SmsController.java` devuelve **2** (la whitelist es la misma en la ruta de validación y en la de ejecución).

### Paso 7 — Cableado Guice: módulo y provider del subsistema

**Ficheros:** `subsystem/sms/module/SmsSenderProvider.java` y `subsystem/sms/module/SmsModule.java` (los dos Crear).

El subsistema **no aporta ninguna clase de infraestructura asíncrona**: el pool es el `EjecutorAsincrono` único de la aplicación (paso 3), que `SmsServiceImpl` se limita a inyectar. `SmsSenderProvider` está descrito en `design/rules/R-Sms-003.md` §«Clases nuevas» (firma y comentario de cada método).

`SmsModule` tiene **un único binding, y ninguno más**, y va sin `Singleton` porque el emisor se fabrica en cada intento de envío. El `Provider` es obligatorio (no basta `bind(...).to(...)`): construye el emisor a partir de propiedades de configuración, no de otros beans (`k-guice` §3.3/§4.2, `decisiones.md` D4). Además, `SmsModule` **MUST NOT** bindear `SmsServiceImpl` ni ningún otro `ModelService` (los descubre `ModelServiceFactory`), **MUST NOT** bindear ni subclasear el `EjecutorAsincrono` (lo bindea `SecretariaVirtualModule` una sola vez para toda la aplicación, paso 3, y aquí solo se inyecta) y **MUST NOT** registrarse en `SecretariaVirtualModule`: Axelor descubre los `AxelorModule` solo.

```java
// Clase: com.educaflow.subsystem.sms.module.SmsModule extends AxelorModule
protected void configure();
//   bind(SmsSender.class).toProvider(SmsSenderProvider.class);
```

**Verificar:** la aplicación arranca (`./run.sh`) sin `Guice/MissingConstructor`; al parar, el pool se detiene sin dejar hilos `async-*`; `grep -rn "bind(" src/main/java/com/educaflow/subsystem/sms/module/SmsModule.java` devuelve **una** línea.

### Paso 8 — Vistas

**Ficheros:** `subsystem/sms/views/Main-Sms.xml`, `Centro-Sms.xml`, `Mis-Sms.xml` (Crear) — XML completos en `design/views/`, válidos contra `object-views.xsd`. Un `<action-view>` por fichero, las cinco PI `sv-*` por bloque y en orden, `buttons-panel` explícito (nunca la toolbar nativa) y `save` → `force-back`.

#### `views/Main-Sms.xml` — pantalla «Administración de SMS» (Administrador)

- `action-view` `subsysSms.Main@Sms-action` (grid + form, `show-toolbar-form=false`, `forceEdit=true`).
- `grid` `subsysSms.Main@Sms-grid`: `canNew="true"`, `newButtonTitle="Nuevo SMS"`, `orderBy="-fechaCreacion"` (del más reciente al más antiguo), `allowSearchFields="true"`, `canEditOnClick="true"`. Columnas en el orden del spec: estado, dniDestinatario, nombre, apellidos, telefono, mensaje, centro, nombreExpediente, fechaCreacion, fechaEnvio.
- `form` `subsysSms.Main@Sms-form`: dos paneles + `buttons-panel`. Sirve de alta (editable) y de detalle. La solo-lectura se declara **una sola vez, en el `<panel>`**, nunca campo a campo (`k-vistas/forms.md` admite los condicionales «de campos **y paneles**», y así lo hace ya `subsystem/importacion/views/Main-TareaImportacion.xml`): `<panel name="Sms" readonlyIf="(id != null) || (cid != null)">` para el detalle (U-sms-todos-001) y `<panel name="Envio" readonly="true">` **fijo**, porque ese panel solo aparece en el detalle (su `showIf`) y sus siete campos son `servidor`: el usuario no los edita nunca (ESC-013 y `screen-sms-todos.md`: «todos en solo lectura»). Un campo nuevo en cualquiera de los dos paneles hereda la decisión sin que nadie tenga que acordarse de repetir el atributo.
- Acciones: `-btnCancel-action` (`back`), `-btnSave-action` (`Local-validateSave` → `remote-validationSave-action` → `save` → `force-back`), `-btnReenviar-action` (`Remote-validateReenviar` → `Remote-reenviar`), el `action-condition` local de campos obligatorios y los dos `action-method` que llaman a `SmsController`.
- **No hay** `action-group` de borrado ni botón «Borrar»: desviación del estándar que declara `screen-sms-todos.md` (un SMS no se puede borrar).

**ASCII Layout — panel «Datos del SMS»** (idéntico en alta y en detalle; lo único que cambia es el `readonlyIf` del panel, U-sms-todos-001):

```
cccccc······   ← centro(6); el colOffset(6) de dniDestinatario cierra la fila
dddnnnnaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)               [destinatario]
tttthhhhhhhh   ← telefono(4, widget phone) + historialEstado(8)
mmmmmmmmmmmm   ← mensaje(12, widget Text)                                    [texto del SMS]
```

Dimensionado con la tabla de proporcionalidad de `k-vistas/forms.md`: `centro` es un selector de nombre medio (6), `dniDestinatario` un identificador (3), `nombre` (4) y `apellidos` (5) nombres cortos; DNI, nombre y apellidos —el grupo «destinatario»— van juntos en su fila y en su orden natural, como en `Main-Correo.xml`, `telefono` un identificador con el selector de país del widget `phone` (4) y `historialEstado` una referencia de nombre largo (8). El orden es el de `screen-sms-todos.md` salvo en un punto: `historialEstado` se adelanta a `mensaje` (ver «Notas y supuestos» 1, que enumera todas las desviaciones de orden del diseño).

**ASCII Layout — panel «Datos del envío»** (solo en el detalle, U-sms-todos-002; el panel entero `readonly="true"`), un dibujo por estado:

```
estado == 'ENVIADO'
eeerrr······   ← estado(3) + numeroReintentos(3); el colOffset(6) de fechaCreacion cierra la fila
cccpppuuuvvv   ← fechaCreacion(3) + fechaPrimerIntentoEnvio(3) + fechaUltimoIntentoEnvio(3)
                 + fechaEnvio(3)          [las cuatro fechas juntas, en orden temporal]
dddddddddddd   ← descripcionUltimoFallo oculta: fila propia, no desplaza a nadie

estado == 'FALLIDO'
eeerrr······
cccpppuuu···   ← fechaEnvio oculta (U-sms-todos-003): es el último elemento de su fila, así que su
                 hueco queda al borde derecho y no desplaza a nadie
dddddddddddd   ← descripcionUltimoFallo(12, widget Text; U-sms-todos-004): fila propia

estado == 'PENDIENTE'
eeerrr······
cccpppuuu···   ← fechaEnvio oculta (U-sms-todos-003): es el último elemento de su fila, así que su
                 hueco queda al borde derecho y no desplaza a nadie
dddddddddddd   ← descripcionUltimoFallo oculta: fila propia, no desplaza a nadie
```

Las cuatro fechas van en la **misma fila y en su orden natural** (creación → primer intento → último intento → envío), que es la secuencia temporal del envío: `k-vistas/forms.md` §Agrupación semántica lo exige, y por eso `fechaEnvio` baja de la 4.ª posición del spec a la 6.ª (ver «Notas y supuestos» 1), y la tabla de proporcionalidad da **3** a un campo de fecha, así que ninguna se infla a 6. Los dos condicionales llevan el `showIf` **en el propio campo** (uno al borde derecho de su fila, el otro en fila propia): no hace falta ningún panel anidado, y **MUST NOT** envolverlos en uno (un panel anidado abre fila nueva).

**ASCII Layout — `buttons-panel`** (dos paneles anidados con `showIf` mutuamente excluyentes que cubren todos los estados; U-sms-todos-005 y U-sms-todos-006):

```
buttonsAlta      (id == null) && (cid == null)
........ccgg   ← colOffset(8) + btnCancelAlta «Cancelar»(2) + btnSave «Guardar»(2)

buttonsDetalle   (id != null) || (cid != null)
  estado == 'FALLIDO'
rr........ss   ← btnReenviar «Reenviar»(2) + colOffset(8) + btnCancelSalir «Salir»(2)
  estado != 'FALLIDO'
··........ss   ← btnReenviar oculto reserva sus 2 columnas al borde IZQUIERDO, así que «Salir» sigue
                 pegado al derecho
```

Los principales quedan pegados al borde derecho (`colOffset + colSpan = 12`) y el único secundario («Reenviar») a la izquierda. No hace falta un panel por estado dentro del detalle: «Reenviar» es el **primer** botón del panel y **no lleva `colOffset`**, la excepción explícita de `k-vistas/forms.md` §«Botones condicionales por estado» — la misma que ya usa `Centro-Sms.xml`. Así hay **un** panel y **un** botón «Salir» en vez de dos gemelos con el mismo `action-group`. Los nombres de botón empiezan por el `btnXxx` de su `onClick`.

#### `views/Centro-Sms.xml` — pantalla «SMS de mis centros» (Supervisor y Administrativo)

- `action-view` `subsysSms.Centro@Sms-action` con `<domain>` (antes del `<context>`, lo exige el XSD) que limita a los centros donde el usuario es `SUPERVISOR` o `ADMINISTRATIVO`, con `<context name="usuarioId" expr="eval: __user__?.id"/>`. **MUST NOT** usarse `:__user__.campo` con punto (no resuelve en Hibernate y el listado saldría vacío).
- `grid` `subsysSms.Centro@Sms-grid`: `canNew="false"`, `canViewOnClick="true"`, `orderBy="-fechaCreacion"`, columna `centro` en primer lugar para poder filtrar cuando el usuario tiene varios centros (ESC-021).
- `form` `subsysSms.Centro@Sms-form`: los mismos dos paneles, los dos con `readonly="true"` **en el `<panel>`** (ni un solo `readonly` de campo).
- Acciones: `-btnReenviar-action`, que **reutiliza** los dos `action-method` de `Main-Sms.xml` (el mismo `@CallMethod`, el mismo aviso), y `-btnCancel-action` (`back`).

**ASCII Layout — paneles «Datos del SMS» y «Datos del envío»:** idénticos a los de `Main-Sms.xml` (mismos `colSpan`/`colOffset`, mismo orden de campos). Las únicas diferencias: «Datos del SMS» lleva `readonly="true"` en el panel en lugar del `readonlyIf` (aquí nunca se edita) y el panel de envío no lleva `showIf` (aquí siempre hay SMS creado); «Datos del envío» ya es `readonly="true"` en el panel en los dos formularios. Los `showIf` de `fechaEnvio` (U-sms-centro-001) y `descripcionUltimoFallo` (U-sms-centro-002) son los mismos.

**ASCII Layout — `buttons-panel`** (panel plano; U-sms-centro-003):

```
estado == 'FALLIDO'
rr........ss   ← btnReenviar(2) + colOffset(8) + btnCancel «Salir»(2)

estado != 'FALLIDO'
··........ss   ← btnReenviar oculto reserva sus 2 columnas al borde IZQUIERDO, así que «Salir» sigue
                 pegado al derecho
```

No hace falta panel por estado: el condicional es el **primero** del panel y **sin `colOffset`**, la excepción explícita de `k-vistas/forms.md` §«Botones condicionales por estado».

#### `views/Mis-Sms.xml` — pantalla «Mis SMS» (cualquier usuario)

- `action-view` `subsysSms.Mis@Sms-action` con `<domain>self.dniDestinatario = :dniUsuarioActual and self.estado = :estadoEnviado</domain>` y los dos `<context>` (`eval: __user__?.dni` y `ENVIADO`): solo los SMS **enviados con éxito** a su DNI, de cualquier centro (ESC-024).
- `grid` `subsysSms.Mis@Sms-grid`: `canNew="false"`, `canViewOnClick="true"`, `orderBy="-fechaEnvio"`, columnas mensaje, telefono, nombreExpediente, fechaEnvio.
- `form` `subsysSms.Mis@Sms-form`: un solo panel con `readonly="true"` en el propio `<panel>`, y «Salir».

**ASCII Layout — panel «Datos del SMS»:**

```
tttfff······   ← telefono(3) + fechaEnvio(3); el colOffset(6) de mensaje completa la fila
mmmmmmmmmmmm   ← mensaje(12, widget Text)
```

El orden invierte el de `screen-mis-sms.md` (mensaje, fecha de envío, teléfono) para dejar el texto multilínea el último, en su fila propia, igual que en los otros dos formularios (ver «Notas y supuestos» 1).

**ASCII Layout — `buttons-panel`:**

```
··········ss   ← colOffset(10) + btnCancel «Salir»(2)
```

El teléfono usa en los tres formularios `widget="phone" x-only-countries="ES"` (U-sms-todos-007: el selector de país solo ofrece España). Es **comodidad**, no defensa: la garantía la da V-Sms-006 en el servidor.

**Verificar:** las tres vistas cargan sin error de metadatos al arrancar; `grep -nE '<form .*can(Back|Delete|Save)="true"' src/main/java/com/educaflow/subsystem/sms/views/*.xml` no devuelve nada; `grep -n "Remote-validateSave\|Remote-validateDelete" src/main/java/com/educaflow/subsystem/sms/views/*.xml` no devuelve nada.

### Paso 9 — Menú

**Fichero:** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (Modificar) — la porción a fusionar está en `design/menus.xml`.

- Raíz `sms-menuitem` («SMS», `groups="admins,users"`, `order="55"`: entre «Correos» (50) y «Mi centro» (60), sin renumerar nada).
- Hojas: `sms-recibidos-menuitem` → `subsysSms.Mis@Sms-action` (`admins,users`), `sms-delCentro-menuitem` → `subsysSms.Centro@Sms-action` (`users`), `sms-todos-menuitem` → `subsysSms.Main@Sms-action` (`admins`).
- **MUST NOT** crear un `menus-sms.xml`; **MUST NOT** escribir el atributo `if` (lo pone el preprocesador); **MUST NOT** añadir ningún `case` en `MenuSecurityServiceImpl` (ver «Notas y supuestos»).

**Verificar:** con `admin` se ven «Recibidos» y «Todos» y no «Del centro»; con `supervisor1@mislata.es` se ven «Recibidos» y «Del centro» y no «Todos» (ESC-022); con `alumno1@mislata.es` se ven «Recibidos» y «Del centro» (esta última vacía, ESC-025).

### Paso 10 — Seguridad y datos iniciales

**Ficheros:** `subsystem/sms/data-init/input-config.xml` (Crear) y `subsystem/sms/data-init/input/auth-sms.xml` (Crear). El `auth.xml` global **no** se toca (`k-datainit` §5).

- `input-config.xml` con `priority="10"` y un único `<input file="auth-sms.xml" root="auth">` con dos binds, los mismos que `subsystem/security/data-init/input-config.xml`: el de `com.axelor.auth.db.Permission` (`search="self.name = :name"`, `create="true" update="true"`, y los `bind` de `@name`, `@object`, `@condition`, `@conditionParams` y los cinco `can/@*`) y el del grupo, `<bind node="group" type="com.axelor.auth.db.Group" search="self.code = :code" create="false" update="true">` con `@code` → `code` y `permission` → `permissions` (`search="self.name = :name"`).
- `auth-sms.xml` con **dos permisos, los dos de solo lectura** (`create/write/remove/export = false`), porque ningún usuario del grupo `users` puede crear, modificar ni borrar SMS:
  - `Sms.propio-destinatario` — objeto `com.educaflow.subsystem.sms.db.Sms`. Regla de acceso: los SMS en
    estado `ENVIADO` cuyo DNI del destinatario es el DNI del usuario autenticado. Es el permiso de «Mis SMS»:
    cada usuario ve solo los SMS **enviados con éxito** a su propio DNI, de cualquier centro.
  - `Sms.propio-centro-gestion` — mismo objeto. Regla de acceso: los SMS de los centros en los que el usuario
    autenticado tiene el tipo de usuario `SUPERVISOR` o `ADMINISTRATIVO`. Es el permiso de «Del centro»: el
    mismo alcance que el `<domain>` de `Centro-Sms.xml`, para que la UI y el permiso no puedan divergir.
  - El implementador escribe la `condition`/`conditionParams` de cada uno tomando como patrón los permisos
    equivalentes de `subsystem/correos/data-init/input/auth-correos.xml` (destinatario y centro de gestión),
    sin copiar aquí la consulta.
- Al final de `auth-sms.xml`, un `<group code="users">` con `<permission name="Sms.propio-destinatario"/>` y
  `<permission name="Sms.propio-centro-gestion"/>` (como `auth-security.xml`: el grupo lo crea Axelor y el
  binder añade los permisos sin quitar los que ya tiene).
  **No** se enlaza nada al grupo `admins` ni existe un `Sms.all` (el Administrador no pasa por estos permisos
  condicionales).
- No hay otros datos iniciales: el subsistema no tiene catálogos ni datos maestros (las credenciales
  del proveedor son configuración por instalación, no datos de la aplicación).
- **MUST NOT** crear a mano ningún `i18n_*.csv` dentro de `data-init/input/`.

**Verificar:** tras arrancar,
`SELECT name, object FROM auth_permission WHERE name LIKE 'Sms.%'` devuelve las dos filas, y
`SELECT p.name FROM auth_group g JOIN auth_group_permission gp ON gp.group_id = g.id JOIN auth_permission p ON p.id = gp.permission_id WHERE g.code = 'users' AND p.name LIKE 'Sms.%'` las devuelve enlazadas al grupo.

### Paso 11 — Verificación final

- Compilar, pasar los tests y arrancar: **`./run.sh`** (hace `./gradlew clean build`, que ejecuta los tests, `cpdCheck`, `crapCheck` y `GenerateDocs`, y arranca en el 8080).
- Comprobar en la aplicación, con `admin`/`admin`: «SMS → Todos» → «Nuevo SMS» → alta con el centro «CIPFP Mislata», DNI `86862719E`, teléfono `600111222` y mensaje «Mañana no hay clase» → vuelve al listado y el SMS aparece con el teléfono `+34600111222`; a los pocos segundos, «Enviado» o «Fallido» con la descripción del fallo.
- Comprobar que **correos sigue funcionando** (es el subsistema que este diseño refactoriza): alta de un correo desde «Correos → Todos» y estado final «Enviado»/«Fallido».
- Los tests E2E de `design/test-e2e-desc.md` los ejecuta `/sdd-debug-with-test-e2e-desc`.

---

## Frontera de confianza — AllowProperties por acción

### `SmsServiceImpl.insert` (endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.sms.db.Sms`)

Entidad: `Sms`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-Sms.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| `dniDestinatario` | cliente | sí | Input directo del usuario (en `Input AllowProperties`). Validado por V-Sms-001/002. |
| `nombre` | cliente | sí | Input directo del usuario. V-Sms-003. |
| `apellidos` | cliente | sí | Input directo del usuario. V-Sms-004. |
| `telefono` | cliente | sí | Input directo del usuario. V-Sms-005/006 lo validan y R-Sms-001 lo **canonicaliza** a E.164 de forma incondicional (ver `decisiones.md` D6). |
| `mensaje` | cliente | sí | Input directo del usuario. V-Sms-007/008. |
| `centro` | cliente | sí | Lo elige el usuario entre sus centros; la defensa es V-Sms-010 en el servicio (el Administrador, cualquiera), nunca la vista. |
| `historialEstado` | cliente | sí | Referencia que dicta el cliente (`k-secure-coding` §3.6): en whitelist de `insert` y validada por V-Sms-011 **cuando el cliente la manda** (el campo es opcional y la regla se excluye si no viene, que es el caso normal), comprobando que la referencia llega resuelta y que su expediente es del **mismo centro** que el SMS: ver `decisiones.md` D7. |
| `estado` | servidor | **NO** | Asignado en `insert` → `fireActionRule_AsignarValoresIniciales` (PENDIENTE) y recalculado en `enviarSms` → `fireActionRule_MarcarEnvioCorrecto`/`MarcarEnvioFallido`. |
| `fechaCreacion` | servidor | **NO** | Asignada en `insert` → `fireActionRule_AsignarValoresIniciales` (`LocalDateTime.now(Convert.defaultZoneId)`). |
| `fechaPrimerIntentoEnvio` | servidor | **NO** | `null` al alta; la fija `fireActionRule_RegistrarIntentoEnvio` en el primer intento. |
| `fechaUltimoIntentoEnvio` | servidor | **NO** | `null` al alta; la fija `fireActionRule_RegistrarIntentoEnvio` en cada intento. |
| `fechaEnvio` | servidor | **NO** | `null` al alta y en cada fallo; la fija `fireActionRule_MarcarEnvioCorrecto` (RES-Sms-003). |
| `numeroReintentos` | servidor | **NO** | `0` al alta; lo incrementa `fireActionRule_RegistrarIntentoEnvio`. |
| `descripcionUltimoFallo` | servidor | **NO** | `null` al alta y al enviar bien; la fija `fireActionRule_MarcarEnvioFallido`. |
| `nombreExpediente` | servidor | **NO** | Campo derivado `formula="true"` (CC-Sms-008, `momento: lectura`): no se persiste, no hay setter que el cliente pueda dictar. |

### `SmsServiceImpl.reenviar` (invocado desde `SmsController.reenviar`)

Entidad: `Sms`. **Forma elegida**: `createAllowProperties` con un mapa **vacío**.
**Origen spec:** acción `Reenviar` de `entity-Sms.md` (no declara ninguna `Input AllowProperties`).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| *(ninguno)* | — | **NO** | El reenvío no acepta ningún dato del cliente: solo el `id`, que `ActionRequestHelper` resuelve al margen de la whitelist. Todo lo que se valida (estado FALLIDO y centro del usuario) se lee de `entidadOriginal`, es decir de BD, no del JSON. |

### `SmsServiceImpl.update` y `SmsServiceImpl.remove`

Entidad: `Sms`. **Forma elegida**: `createAllowProperties` con un mapa **vacío**, en
`allowPropertiesUpdate()` y en `allowPropertiesRemove()`.
**Origen spec:** acción `Modificar` de `entity-Sms.md` (Input AllowProperties: ninguna) y RES-Sms-002 para remove (el spec no declara ninguna acción Borrar).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| *(ninguno)* | — | **NO** | RES-Sms-001 / RES-Sms-002: el SMS es inmutable y no se borra, así que ninguna de las dos acciones acepta ni asigna nada. |

Las dos van **además** del rechazo de negocio (V-Sms-012 / V-Sms-013 siempre devuelven mensaje y los
métodos lanzan `UnsupportedOperationException`), no en su lugar: son **dos puertas distintas**. Sin
sobrescribirlas quedaría en pie el `createAllowAllProperties()` que `DefaultModelService` da por
defecto, que es literalmente el detector de vulnerabilidad de `k-secure-coding` §3.4 y contradice la
tabla de §3.2 («whitelist obligatoria si hay algún campo `servidor` que la acción no asigna»:
`update`/`remove` no asignan ninguno de los siete). Son dos métodos de una línea y cierran el filtro
del JSON en la capa que `k-secure-coding` señala como la única que filtra el endpoint REST automático.

No hay alta programática vía DTO: los siete campos `cliente` de la entidad son exactamente los datos
del alta, así que otros subsistemas llaman a `insert(Sms)` y pasan por la misma whitelist y las mismas
V-.

---

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (`V-Sms-NNN`)

| V | Origen spec | Ubicación |
|---|-------------|-----------|
| V-Sms-001 | VAL-Sms-001 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ capa cliente `subsysSms.Main@Sms-Local-validateSave-action`, `<check field="dniDestinatario">`) |
| V-Sms-002 | VAL-Sms-002 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (solo servidor: `DniUtil.isValid`) |
| V-Sms-003 | VAL-Sms-003 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ `<check field="nombre">`) |
| V-Sms-004 | VAL-Sms-004 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ `<check field="apellidos">`) |
| V-Sms-005 | VAL-Sms-005 | `SmsServiceImpl.validateInsert` → `validarTelefono` (+ `<check field="telefono">`) |
| V-Sms-006 | VAL-Sms-006, RES-Sms-004 | `SmsServiceImpl.validateInsert` → `validarTelefono` (`NumeroTelefono.esMovilDeEspana()`) |
| V-Sms-007 | VAL-Sms-007 | `SmsServiceImpl.validateInsert` → `validarMensaje` (`TextUtil.isNullOrBlank`) (+ `<check field="mensaje">`) |
| V-Sms-008 | VAL-Sms-008 | `SmsServiceImpl.validateInsert` → `validarMensaje` (`MensajeSmsUtil.cabeEnUnSms`) |
| V-Sms-009 | VAL-Sms-009 | `SmsServiceImpl.validateInsert` → `validarCentro` (+ `<check field="centro">`) |
| V-Sms-010 | VAL-Sms-010 | `SmsServiceImpl.validateInsert` → `validarCentro`, en el `else` de V-Sms-009 (`SecurityUtil.isAdmin` / `perteneceAlCentro`) |
| V-Sms-011 | VAL-Sms-011 | `SmsServiceImpl.validateInsert`, solo con centro y estado de expediente presentes → `validarHistorialEstado` (la referencia llega resuelta **y** su expediente es del mismo centro que el SMS, sin releerla por repositorio; ver `decisiones.md` D7) |
| V-Sms-012 | RES-Sms-001 | `SmsServiceImpl.validateUpdate` (siempre rechaza) + `SmsServiceImpl.update` (`UnsupportedOperationException`) |
| V-Sms-013 | RES-Sms-002 | `SmsServiceImpl.validateRemove` (siempre rechaza) + `SmsServiceImpl.remove` (`UnsupportedOperationException`) |
| V-Sms-014 | VAL-Sms-012 | `SmsServiceImpl.validateReenviar` (estado != FALLIDO), invocada por `SmsController.validateReenviar` desde `subsysSms.Main@Sms-Remote-validateReenviar-action` |
| V-Sms-015 | VAL-Sms-013 | `SmsServiceImpl.validateReenviar` (administrador, o SUPERVISOR/ADMINISTRATIVO del centro del SMS según `User.tieneTipoUsuario`) |

### Reglas de negocio (`R-Sms-NNN`)

| R | Origen spec | Ubicación |
|---|-------------|-----------|
| R-Sms-001 | RN-Sms-001, RES-Sms-004 | `SmsServiceImpl.fireActionRule_NormalizarTelefono` (Antes de `repository.save`, en `insert`) |
| R-Sms-002 | CC-Sms-001, CC-Sms-002, CC-Sms-003, CC-Sms-004, CC-Sms-005, CC-Sms-006, CC-Sms-007, RES-Sms-003 | `SmsServiceImpl.fireActionRule_AsignarValoresIniciales` (Antes de `repository.save`, en `insert`) |
| R-Sms-003 | RN-Sms-002, RN-Sms-003 | `SmsServiceImpl.fireActionRule_ProgramarEnvioAsincrono` (Después de `repository.save` en `insert`, y en `reenviar`). Detalle: `design/rules/R-Sms-003.md` |
| R-Sms-004 | CC-Sms-003, CC-Sms-004, CC-Sms-005 | `SmsServiceImpl.fireActionRule_RegistrarIntentoEnvio` (Antes del `repository.save` de `enviarSms`) |
| R-Sms-005 | CC-Sms-001, CC-Sms-006, RES-Sms-003 | `SmsServiceImpl.fireActionRule_MarcarEnvioCorrecto` (Antes del `repository.save` de `enviarSms`) |
| R-Sms-006 | CC-Sms-001, CC-Sms-007, RES-Sms-003 | `SmsServiceImpl.fireActionRule_MarcarEnvioFallido` (Antes del `repository.save` de `enviarSms`; el texto lo da `TextUtil.trazaCompleta`) |

### Campos derivados de solo lectura

| Campo | Origen spec | Ubicación |
|-------|-------------|-----------|
| `nombreExpediente` | CC-Sms-008 (`momento: lectura`) | `design/domains/Sms.xml`, `<string name="nombreExpediente" formula="true">` (subselect SQL; no se persiste) |

### Reglas de UI (`U-<slug-pantalla>-NNN`)

| U | Origen spec | Ubicación |
|---|-------------|-----------|
| U-sms-todos-001 | RUI-sms-todos-formulario-001 | `views/Main-Sms.xml`: `<panel name="Sms" readonlyIf="(id != null) || (cid != null)">` (una sola vez, en el panel) |
| U-sms-todos-002 | RUI-sms-todos-formulario-002 | `views/Main-Sms.xml`, `<panel name="Envio" showIf="(id != null) || (cid != null)" readonly="true">` (el `readonly` en el panel) |
| U-sms-todos-003 | RUI-sms-todos-formulario-003 | `views/Main-Sms.xml`, `<field name="fechaEnvio" showIf="estado == 'ENVIADO'">` |
| U-sms-todos-004 | RUI-sms-todos-formulario-004 | `views/Main-Sms.xml`, `<field name="descripcionUltimoFallo" showIf="estado == 'FALLIDO'">` |
| U-sms-todos-005 | RUI-sms-todos-formulario-005 | `views/Main-Sms.xml`, paneles `buttonsAlta` (Cancelar/Guardar) y `buttonsDetalle` (Salir), con `showIf` excluyentes |
| U-sms-todos-006 | RUI-sms-todos-formulario-006 | `views/Main-Sms.xml`, `<button name="btnReenviar" showIf="estado == 'FALLIDO'">` dentro de `buttonsDetalle` |
| U-sms-todos-007 | RUI-sms-todos-formulario-007 | `views/Main-Sms.xml`, `<field name="telefono" widget="phone" x-only-countries="ES">` (mismo widget en `Centro-Sms.xml` y `Mis-Sms.xml`) |
| U-sms-todos-008 | RUI-sms-todos-formulario-008 | `views/Main-Sms.xml`, `action-group subsysSms.Main@Sms-btnSave-action`: `<action name="force-back"/>` tras `save` (+ `canBackOnSave="true"` en el `<form>`) |
| U-sms-centro-001 | RUI-sms-centro-formulario-001 | `views/Centro-Sms.xml`, `<field name="fechaEnvio" showIf="estado == 'ENVIADO'">` |
| U-sms-centro-002 | RUI-sms-centro-formulario-002 | `views/Centro-Sms.xml`, `<field name="descripcionUltimoFallo" showIf="estado == 'FALLIDO'">` |
| U-sms-centro-003 | RUI-sms-centro-formulario-003 | `views/Centro-Sms.xml`, `<button name="btnReenviar" showIf="estado == 'FALLIDO'">` |

### Seguridad (permisos y alcance)

| Regla del spec (apartado «Seguridad») | Ubicación |
|---|---|
| Administrador: ve todos los centros, solo él da de alta, reenvía cualquiera; no modifica ni borra | Menú `sms-todos-menuitem` con `groups="admins"`; `Main-Sms.xml` sin `<domain>`; V-Sms-010/V-Sms-015 con rama explícita de administrador; V-Sms-012/V-Sms-013 |
| Supervisor y Administrativo: solo lectura de sus centros + reenvío | `Centro-Sms.xml` (`<domain>` por `CentroUsuario` con `SUPERVISOR`/`ADMINISTRATIVO`) + permiso `Sms.propio-centro-gestion` (solo `read`) + V-Sms-015 (autorización real; ver `decisiones.md` D5) |
| Cualquier usuario: solo lectura de los SMS ENVIADOS a su propio DNI, de cualquier centro | `Mis-Sms.xml` (`<domain>` por DNI y estado) + permiso `Sms.propio-destinatario` (solo `read`) |
| Nadie del grupo `users` puede crear, modificar ni borrar | Los dos permisos con `create/write/remove/export="false"`; `Main-Sms.xml` solo accesible desde un menú `groups="admins"` |

---

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline). Clases con lógica que testear: `NumeroTelefono`, `MensajeSmsUtil`, `TextUtil` (solo el método nuevo `trazaCompleta`), `EjecutorAsincrono`, `EjecutorAsincronoProvider`, `AppEventObserver`, `SmsServiceImpl`, `SmsController`, `SmsSenderProvider`. Para cubrir `SmsServiceImpl.enviarSms` (privado) el test mockea `EjecutorAsincrono`, **captura el `Runnable`** que recibe `ejecutarTrasCommit` y lo ejecuta con `JPA` mockeado estáticamente, igual que ya hacen los tests de correos. De `NumeroTelefono` y `MensajeSmsUtil` solo se testean sus métodos **públicos** (`esMovilDeEspana`/`enFormatoE164` y `cabeEnUnSms`): los privados quedan cubiertos a través de ellos.
- **Tests E2E**: `design/test-e2e-desc.md` (T-001…T-025, uno por cada `ESC-NNN` del spec).
- **Regresión obligatoria del refactor del paso 3**: los tests unitarios de correos y los E2E ya persistidos en `src/test/e2e/subsystem/correos/` **MUST** seguir pasando sin cambios de comportamiento.

---

## Reglas del spec descartadas

Ninguna: las 4 `RES-`, las 13 `VAL-`, las 3 `RN-`, las 8 `CC-` y las 11 `RUI-` del spec están ubicadas en la matriz de trazabilidad.

---

## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `PostCommitRunner` (y su método estático `runAfterCommit`) | `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | — (sin ID de spec: lo exige `CLAUDE.md` §`cpdCheck`, «un duplicado se arregla extrayendo el código común en su sitio»). Lo sustituye `EjecutorAsincrono.ejecutarTrasCommit`; ver `decisiones.md` D3 |
| Clase `CorreoAsyncExecutor` (pool, `ThreadFactory`, `submit`, `detener`) | `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoAsyncExecutor.java` | — (mismo motivo). Lo sustituye el `EjecutorAsincrono` **único** de la aplicación, que `CorreoServiceImpl` inyecta; ver D3 |
| Clase `CorreoAsyncExecutorProvider` | `src/main/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProvider.java` | — (mismo motivo). Lo sustituye `EjecutorAsincronoProvider`, el único sitio del proyecto que lee un tamaño de pool. **Efecto de configuración:** la clave `mail.send.pool-size` deja de leerse y la sustituye `async.pool-size` (mismo default, 2); la clave se sustituye en `axelor-config.properties` (paso 1) |
| Clase `CorreoEventObserver` | `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoEventObserver.java` | — (mismo motivo). Su única responsabilidad real (`detener()` al `ShutdownEvent`) pasa al `AppEventObserver` que la aplicación **ya tiene**; su `onAppStart` solo escribía una línea de log |
| Método privado `trazaCompleta(Throwable)` de `CorreoServiceImpl` | `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | — (sin ID de spec). Se mueve a `TextUtil.trazaCompleta` (paso 2) |
| Los dos `bind` del pool y del observer en `CorreosModule` | `src/main/java/com/educaflow/subsystem/correos/module/CorreosModule.java` | — (consecuencia de las tres anteriores). Se **conserva** el `bind(MailSender.class)` |
| Clases de test `PostCommitRunnerTest`, `CorreoAsyncExecutorTest`, `CorreoAsyncExecutorProviderTest` y `CorreoEventObserverTest` | `src/test/java/com/educaflow/subsystem/correos/` | — (consecuencia de las anteriores). Sus casos se trasladan **íntegros** a `EjecutorAsincronoTest`, `EjecutorAsincronoProviderTest` y `AppEventObserverTest`; no se pierde ninguna comprobación |

## Tests E2E supersedidos

Ninguno. El refactor del paso 3 no cambia el comportamiento observable de correos (misma tarea, mismo momento, mismo pool), así que los `src/test/e2e/subsystem/correos/t-*.spec.ts` siguen siendo válidos y **MUST** seguir pasando. El subsistema de SMS es nuevo: `src/test/e2e/subsystem/sms/` no existe todavía, de ahí que la numeración de `test-e2e-desc.md` empiece en `T-001`.

---

## Notas y supuestos

1. **Desviaciones del orden de campos del spec, todas por maquetación.** `screen-sms-todos.md`, `screen-sms-centro.md` y `screen-mis-sms.md` enumeran los campos de cada panel pero, a diferencia de las columnas del grid, no los declaran «(en orden)». Los campos de cada panel son exactamente los que pide el spec, ni uno más ni uno menos; lo que se desvía es el orden, y solo en estos tres puntos:
    - **«Datos del SMS» de `Main-Sms.xml` y `Centro-Sms.xml`** (spec: centro, DNI, nombre, apellidos, teléfono, mensaje, estado del expediente) — `historialEstado` se adelanta a `mensaje`, para que comparta fila con `telefono` (4+8=12) y `mensaje` —que es multilínea y ocupa las 12 columnas— quede el último, en su fila propia. Con el orden literal, `historialEstado` quedaría solo en una última fila detrás del texto largo, con 4 columnas vacías a su derecha.
    - **«Datos del envío» de `Main-Sms.xml` y `Centro-Sms.xml`** (spec: estado, número de reintentos, fecha de creación, fecha de envío, fecha del primer intento, fecha del último intento, descripción del último fallo) — `fechaEnvio` baja de la 4.ª posición a la 6.ª, detrás de `fechaUltimoIntentoEnvio`, para que las cuatro fechas queden en la **misma fila y en su orden temporal** (creación → primer intento → último intento → envío), como exige `k-vistas/forms.md` §Agrupación semántica. De paso, `fechaEnvio` —el único condicional de esa fila (U-sms-todos-003 / U-sms-centro-001)— queda como último elemento de la fila, así que al ocultarse su hueco cae al borde derecho y no desplaza a nadie.
    - **«Datos del SMS» de `Mis-Sms.xml`** (spec: mensaje, fecha de envío, teléfono) — `mensaje` pasa del primer lugar al último por el mismo motivo que en el primer punto (multilínea, 12 columnas, fila propia al final), y `telefono` se adelanta a `fechaEnvio` para que la primera fila lleve primero el dato que identifica al destinatario y después la fecha, en el mismo orden relativo que el grid de la pantalla y que los otros dos formularios.
2. **`telefono` es `cliente` aunque R-Sms-001 lo reescriba a E.164**: ver `decisiones.md` D6.
3. **El menú «SMS → Del centro» lo ven todos los `users`, a propósito**: ver `decisiones.md` D5.
4. **El subsistema refactoriza `subsystem/correos`** aunque el spec no lo declare como sistema modificado: es la única forma de replicar su envío asíncrono sin romper `cpdCheck`. Ver `decisiones.md` D3.
5. **`SmsSender` se inyecta como `Provider<SmsSender>`**, no como `SmsSender` (correos hace lo segundo). Ver `decisiones.md` D4.
6. **`trazaCompleta` se mueve de `CorreoServiceImpl` a `TextUtil`**; ningún servicio lo declara como privado. Ver paso 2.
7. **Las acciones de reenvío se declaran solo en `Main-Sms.xml` y `Centro-Sms.xml` las reutiliza**, como correos: ver paso 8.
8. **V-Sms-010 también se aplica al alta programática.** Un trámite que cree un SMS para un centro que no sea uno de los del usuario autenticado verá la operación rechazada, porque VAL-Sms-010 está escrita así en el spec (con la única excepción del Administrador). Si en el futuro un trámite necesita crear SMS para el centro del expediente con independencia de los centros del usuario, habrá que ampliar esa regla en la spec antes que en el código.
9. **La entidad y el record de transporte se llaman los dos `Sms`**: ver paso 5.
10. **V-Sms-011 (VAL-Sms-011) es más estricta que la letra del spec, a propósito** (`k-secure-coding` §9, «defensa en profundidad»; el mensaje es el que fija el spec): exige además el **mismo centro**. Ver `decisiones.md` D7.
