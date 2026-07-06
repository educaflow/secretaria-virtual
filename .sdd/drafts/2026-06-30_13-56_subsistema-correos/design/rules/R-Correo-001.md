# R-Correo-001 — Envío asíncrono de un correo tras crearlo

**Entidad:** Correo
**Origen spec:** RN-Correo-001
**Operación:** insert
**Momento:** Después de repository.save
**Servicio host:** com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl
**Método host:** fireActionRule_ProgramarEnvioAsincrono(Correo correo)

## Análisis de la regla

Qué se dispara y cuándo: tras crear un `Correo` en estado `PENDIENTE` (justo después de `repository.save` dentro de `insert`), el sistema debe intentar enviarlo por correo electrónico **sin bloquear** la petición HTTP que lo originó (design-guidelines: "El envío del correo debe hacerse de forma asíncrona dentro de Tomcat").

Qué información lee y de dónde: el propio `Correo` recién creado — destinatarios (`para`/`enCopia`/`enCopiaOculta`), `asunto`, `cuerpo` y sus `adjuntos` (cada uno con `nombreFichero` + `contenido` de tipo `MetaFile`).

Qué acciones realiza y en qué orden:
1. Construye el mensaje (`Mail` de `base/infrastructure/mail`) a partir del `Correo`.
2. Invoca `MailSender.send(mail)` (la implementación SMTP ya existente del proyecto).
3. Según el resultado, actualiza el propio `Correo`: estado, fechas de intento/envío, número de reintentos y descripción del fallo.

Garantías de transaccionalidad/idempotencia que debe respetar:
- El hilo en segundo plano que hace el envío **no puede** ver el `Correo` recién creado hasta que la transacción HTTP que lo creó **haya hecho commit** (si lo intentara antes, la fila podría no ser visible todavía para una conexión JDBC distinta — condición de carrera). Por eso el envío no se somete al ejecutor en el propio `fireActionRule`, sino que se **programa para ejecutarse tras el commit** de la transacción actual.
- El envío en sí corre en un **hilo distinto** del que atendió la petición HTTP, así que necesita **su propia transacción/`EntityManager`** (el de la petición original ya se ha cerrado). Se usa `com.axelor.db.JPA.runInTransaction(Runnable)` — utilidad ya provista por Axelor exactamente para este caso (ejecutar código con su propia transacción fuera del ciclo de vida de una petición web, igual que hace un `Job` de Quartz).
- `SUCCESS` es terminal: si por cualquier razón se invoca el envío sobre un `Correo` que ya está en `SUCCESS`, no debe reintentarse (idempotencia).

Qué errores puede encontrar y cómo se tratan: cualquier excepción de `MailSender.send(...)` (la implementación actual las envuelve en `RuntimeException`) se captura, se convierte en la traza completa de la excepción (`descripcionUltimoFallo`) y el correo pasa a `FAIL`. Un fallo al enviar **nunca** debe perderse silenciosamente ni propagarse fuera del hilo del executor (evitaría que otros envíos pendientes se sigan procesando y generaría `UncaughtExceptionHandler` ruido en el log de Tomcat); se captura y se registra con el logger.

Entradas/salidas de cada colaborador: ver diagrama de secuencia.

### Alternativas consideradas (mecanismo de envío asíncrono)

`design-guidelines.md` pide explícitamente dedicar tiempo a elegir el mecanismo asíncrono dentro de Tomcat. Se valoraron cuatro opciones:

| Opción | Descartada por |
|---|---|
| **`ExecutorService` gestionado por Guice (elegida)** | — |
| Job de Quartz / `MetaSchedule` (el mismo mecanismo que ya usa `correos.envio.cron`) | El envío es **inmediato** (debe dispararse justo tras crear/reenviar el correo), no periódico; un `Job` de Quartz solo se ejecuta en su propio ciclo (p.ej. cada minuto), lo que introduciría una latencia innecesaria y no encaja con "enviar tras el commit de esta petición concreta". Quartz queda reservado para un futuro reenvío en bloque de los `FAIL` (fuera de alcance, ver Paso 7). |
| Hilo manual (`new Thread(() -> ...).start()`) por cada envío | Sin gestión de ciclo de vida: no hay límite de concurrencia (un pico de correos crearía un hilo por envío, sin pool ni cola), no hay forma de esperar a que terminen al parar la aplicación (`awaitTermination`) y el manejo de excepciones no capturadas requeriría un `UncaughtExceptionHandler` a mano. Reinventa lo que ya ofrece `ExecutorService` sin ninguna ventaja. |
| `CompletableFuture.runAsync(...)` con el `ForkJoinPool.commonPool()` por defecto | El pool común de `ForkJoinPool` es **compartido con el resto de la JVM** (cualquier otro `CompletableFuture`/`parallelStream()` de la aplicación lo usa también), sin aislamiento de recursos ni cierre ordenado propio: no se puede invocar `shutdown()`/`awaitTermination()` sobre él al parar Tomcat sin afectar a otras partes de la aplicación que también lo usen. Un `ExecutorService` propio y nombrado (`correo-envio-N`) aísla el pool del subsistema y permite un `detener()` explícito y seguro en `CorreoEventObserver.onAppShutdown`. |

La opción elegida (`CorreoAsyncExecutor`, un `ExecutorService` de tamaño fijo con hilos daemon, gestionado como singleton por Guice y con ciclo de vida atado a los eventos de arranque/parada de Axelor) es la única que cubre a la vez: envío inmediato (no periódico), aislamiento de recursos propio del subsistema y cierre ordenado sin leaks de hilos al redesplegar/parar Tomcat.

## Diseño detallado

### Clases nuevas

- `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor` — envoltorio de un `java.util.concurrent.ExecutorService` de tamaño fijo, gestionado por Guice como singleton y con ciclo de vida atado a los eventos de arranque/parada de la aplicación (evita fugas de hilos al redesplegar/parar Tomcat, tal y como pide `design-guidelines.md`).
  - `public CorreoAsyncExecutor(int tamanoPool)` — crea `Executors.newFixedThreadPool(tamanoPool, threadFactory)`; `threadFactory` crea hilos **daemon** (red de seguridad: si el hook de parada no llegara a ejecutarse, estos hilos no impiden que la JVM/Tomcat termine) con nombre `correo-envio-N`.
  - `public void submit(Runnable tarea)` — envía la tarea al pool; envuelve la ejecución en un `try/catch (RuntimeException ex)` que solo hace `log.error("Fallo no controlado en el envío asíncrono de un correo", ex)`, para que una excepción no capturada en una tarea no mate el hilo del pool ni oculte el resto de envíos.
  - `public void detener()` — `executorService.shutdown()`; `awaitTermination(10, TimeUnit.SECONDS)`; si no termina a tiempo, `executorService.shutdownNow()`. Se invoca desde `CorreoEventObserver.onAppShutdown`.
- `com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver` — observador de los eventos de ciclo de vida de Axelor (mismo mecanismo que ya usa `com.educaflow.secretariavirtual.startup.AppEventObserver`, pero autocontenido dentro del subsistema en vez de ampliar la clase global).
  - `public void onAppStart(@com.axelor.event.Observes com.axelor.events.StartupEvent event)` — log informativo (el pool ya se ha creado de forma perezosa por el `Provider` la primera vez que algo lo inyecta; no necesita lógica adicional).
  - `public void onAppShutdown(@com.axelor.event.Observes com.axelor.events.ShutdownEvent event)` — `correoAsyncExecutor.detener()`.
- `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner` — utilidad **estática**, sin estado, sin binding de Guice (no lo necesita: opera sobre el `EntityManager` de la transacción actual del hilo que la invoca).
  - `public static void runAfterCommit(Runnable tarea)` — obtiene la sesión de Hibernate subyacente (`com.axelor.db.JPA.em().unwrap(org.hibernate.Session.class)`) y registra en su transacción actual una `jakarta.transaction.Synchronization` cuyo `afterCompletion(int status)` ejecuta `tarea.run()` **solo si** `status == jakarta.transaction.Status.STATUS_COMMITTED`. Si la transacción hace rollback, la tarea no se ejecuta (no se envía un correo que en realidad no llegó a crearse).

### Interfaces

Ninguna interfaz nueva: `MailSender`/`Mail`/`Attach` ya existen en `com.educaflow.base.infrastructure.mail` y se reutilizan (design-guidelines: "no implementar un cliente SMTP propio"). El record `Mail` **se amplía** (no se sustituye) para soportar `cc`/`bcc` reales — ver "Ficheros a crear o modificar" del `design.md`, Paso 1, y "Notas de esta regla" más abajo.

### Tipos propios

Ninguno nuevo. Se reutiliza `com.educaflow.base.infrastructure.mail.Mail` (record, ampliado en Paso 1 con los componentes `cc`/`bcc`) y `com.educaflow.base.infrastructure.mail.Attach` (record) ya existentes.

### Diagrama de secuencia

```
CorreoServiceImpl.insert(correo)
  ├─ validateInsert(correo) → Optional vacío (válido)
  ├─ fireActionRule_AsignarValoresIniciales(correo)   → estado=PENDIENTE, fechaCreacion=now, numeroReintentos=0
  ├─ repository.save(correo)                          → asigna id, dentro de la transacción HTTP en curso
  └─ fireActionRule_ProgramarEnvioAsincrono(correo)
       └─ PostCommitRunner.runAfterCommit(() -> correoAsyncExecutor.submit(() -> this.enviarCorreo(correo.getId())))
            (la lambda NO se ejecuta todavía; solo queda registrada en la transacción actual)

……tiempo después, la transacción HTTP hace COMMIT……

Synchronization.afterCompletion(STATUS_COMMITTED)
  └─ correoAsyncExecutor.submit(() -> this.enviarCorreo(correoId))
       (se ejecuta en un hilo del pool, en paralelo a la petición HTTP que ya ha respondido)

CorreoServiceImpl.enviarCorreo(correoId)                 [se ejecuta dentro de JPA.runInTransaction(...)]
  ├─ Correo correo = repository.find(correoId)
  ├─ si correo == null || correo.getEstado() == SUCCESS → return (idempotencia / defensivo)
  ├─ fireActionRule_RegistrarIntentoEnvio(correo)       → fechaUltimoIntentoEnvio=now;
  │                                                        si primer intento, fechaPrimerIntentoEnvio=now;
  │                                                        numeroReintentos += 1  (0 → 1 en el primer intento)
  ├─ Mail mail = construirMail(correo)                  → to = separarDirecciones(para); cc = separarDirecciones(enCopia);
  │                                                        bcc = separarDirecciones(enCopiaOculta) — cada lista a su propia
  │                                                        cabecera MIME real (Mail.cc()/Mail.bcc(), ver "Notas de esta regla");
  │                                                        attachs = adjuntos convertidos vía MetaFileUtil.downloadContent(...)
  ├─ try:
  │    mailSender.send(mail)
  │    correo.setEstado(SUCCESS); correo.setFechaEnvio(now); correo.setDescripcionUltimoFallo(null)
  ├─ catch (RuntimeException ex):
  │    correo.setEstado(FAIL); correo.setDescripcionUltimoFallo(trazaCompleta(ex)); correo.setFechaEnvio(null)
  └─ repository.save(correo)                             → persiste el resultado (nunca vía update(), que está bloqueado; ver design.md)
```

### Errores

| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| `MailSender.send(mail)` lanza `RuntimeException` (SMTP caído, destinatario inválido, timeout…) | `MailSenderImpl.send` | Se captura en `enviarCorreo`; el correo pasa a `FAIL` con `descripcionUltimoFallo` = traza completa (`StringWriter`/`PrintWriter` sobre `ex.printStackTrace(...)`, o `org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(ex)` si el proyecto ya trae esa dependencia transitiva; si no, construir la traza a mano con `PrintWriter`) |
| Tarea del executor lanza una excepción no capturada por `enviarCorreo` (bug, `NullPointerException`, etc.) | `CorreoAsyncExecutor.submit` | Se captura en el propio `submit` y se registra con `log.error(...)`; el correo puede quedar en `PENDIENTE` (queda para un futuro reintento manual vía «Reenviar», ya que el fallo no llegó siquiera a marcarse) |
| La transacción de creación hace rollback (p.ej. otra `R-` posterior falla) | `PostCommitRunner` | La `Synchronization` no ejecuta la tarea si `status != STATUS_COMMITTED`: no se intenta enviar un correo que no llegó a persistirse |
| `enviarCorreo` se invoca sobre un `Correo` ya en `SUCCESS` (reintento tardío, doble disparo) | `CorreoServiceImpl.enviarCorreo` | Guarda de idempotencia: `if (correo.getEstado() == EstadoCorreo.SUCCESS) return;` antes de tocar nada |

### Contenido del método `fireActionRule_*`

```java
// Firma:
private void fireActionRule_ProgramarEnvioAsincrono(Correo correo);
//   Implementa R-Correo-001 (Origen spec: RN-Correo-001). Diseño detallado en design/rules/R-Correo-001.md.
//   Secuencia:
//     1. Captura correo.getId() en una variable final (el objeto "correo" pertenece al
//        EntityManager/hilo de la petición HTTP actual; el hilo del executor NO puede reutilizarlo).
//     2. PostCommitRunner.runAfterCommit(() -> correoAsyncExecutor.submit(() -> this.enviarCorreo(correoId)));
//   MUST NOT ejecutar el envío de forma síncrona ni antes del commit de la transacción actual.
```

El método `enviarCorreo(Long correoId)` (público, declarado en `CorreoService`, invocado también por R-Correo-002 — ver `design/rules/R-Correo-002.md`) se documenta íntegro en `design.md`, sección de servicios.

## Notas de esta regla (ver también "Notas y supuestos" del `design.md`)

- **`Mail` (base/infrastructure/mail) se amplía con `cc`/`bcc` reales** (en vez de fusionar `para`+`enCopia`+`enCopiaOculta` en la única lista `Mail.to()`, que expondría cada destinatario "en copia oculta" a todos los demás en la cabecera `To` del mensaje entregado — inaceptable para `VAL-Correo-012`/`013` y para la propia semántica de "copia oculta" de `entity-Correo.md`). Cambios exactos (ver `design.md`, Paso 1, tabla de ficheros):
  - `Mail` (record) gana dos componentes nuevos, `cc` y `bcc` (ambos `List<String>`), añadidos **al final** de la lista de componentes, con un **constructor de compatibilidad** de 6 argumentos (sin `cc`/`bcc`) que delega en el canónico pasando `List.of()` para ambos — así el único llamador real existente, `RegistroSalidaServiceImpl` (que sigue construyendo `Mail` con el `to` fusionado, sin CC/BCC), seguía compilando sin ningún cambio.
  - `JavaMailHelper.getMessage(...)` añade `message.setRecipients(Message.RecipientType.CC, ...)` y `message.setRecipients(Message.RecipientType.BCC, ...)` cuando las listas correspondientes no son nulas ni vacías (reutilizando el mismo `getAddresses(List<String>)` que ya usa para `TO`).
  - `MailSenderImpl.send(...)` cambia el envío real de `transport.sendMessage(message, message.getRecipients(RecipientType.TO))` a `transport.sendMessage(message, message.getAllRecipients())`: si no se cambia esta línea, CC/BCC quedarían en las cabeceras MIME del mensaje pero **no se entregarían de verdad** por SMTP a esos destinatarios (defecto detectado al revisar `MailSenderImpl` real: el envío efectivo usa explícitamente la lista de destinatarios pasada a `sendMessage`, no las cabeceras del `Message`).
  - `construirMail(Correo correo)` (`CorreoServiceImpl`, ver `design.md`) pasa `separarDirecciones(correo.getPara())` a `to`, `separarDirecciones(correo.getEnCopia())` a `cc` y `separarDirecciones(correo.getEnCopiaOculta())` a `bcc`: cada campo del `Correo` llega a su cabecera real, sin fusionar nada.
- `JPA.runInTransaction(...)` es reentrante (si ya hay una transacción activa, no abre una nueva ni hace commit anticipado), así que `enviarCorreo` es seguro de invocar tanto desde el hilo del executor como, en el futuro, de forma síncrona desde otro código de servidor.
