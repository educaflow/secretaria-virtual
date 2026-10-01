# R-Sms-003 — Intentar el envío del SMS al proveedor en segundo plano, tras el commit

**Entidad:** Sms
**Origen spec:** RN-Sms-002, RN-Sms-003
**Operación:** insert, reenviar
**Momento:** Después de repository.save/remove
**Servicio host:** com.educaflow.subsystem.sms.service.impl.SmsServiceImpl
**Método host:** `private void fireActionRule_ProgramarEnvioAsincrono(Sms sms)`

> Se documenta aparte porque cumple cuatro de los criterios de `reglas-complejas.md` §1: integra con un
> sistema externo (el proveedor de SMS) más allá de un wrapper trivial, necesita una clase auxiliar
> compartida (el ejecutor asíncrono), tiene efectos transaccionales no triviales (tarea atada al commit +
> transacción propia en otro hilo + idempotencia) y aplica una política de reintento.

## Análisis de la regla

**Qué se dispara y cuándo.** Dos disparadores, un único comportamiento (RN-Sms-003 dice literalmente
«de la misma forma que tras el alta»):

1. `SmsServiceImpl.insert`, **después** de `repository.save` — alta a mano del Administrador o alta
   programática de un trámite u otro subsistema.
2. `SmsServiceImpl.reenviar`, tras `validateReenviar` — el Supervisor, el Administrativo o el
   Administrador pulsa «Reenviar» sobre un SMS FALLIDO.

Por eso hay **un solo** `fireActionRule_ProgramarEnvioAsincrono`: si hubiera uno por disparador, la
política de envío tendría dos dueños y el siguiente cambio habría que hacerlo dos veces.

**Por qué «tras el commit» y no «ya».** La tarea se ejecuta en **otro hilo**, con su propia
transacción. Si se lanzara antes del commit, ese hilo podría no ver todavía la fila (`repository.find`
devolvería `null`) y el SMS se quedaría PENDIENTE para siempre, de forma intermitente. Y si la
transacción acabara en *rollback*, se habría enviado un SMS que en BD no existe. La única ventana
correcta es «la transacción actual ha hecho commit».

**Por qué en segundo plano.** RN-Sms-002 exige que quien crea el SMS no espere: la llamada HTTP al
proveedor tarda cientos de milisegundos y puede agotar su *timeout*. Ejecutar la tarea en el
`afterCompletion` del hilo de la petición no sería «segundo plano»: el usuario seguiría esperando la
respuesta. Hace falta un pool de hilos (el `EjecutorAsincrono` único de la aplicación).

**Qué información lee y de dónde.** Solo el `id` del SMS (un `Long`), nunca la entidad: el objeto de la
transacción que ya hizo commit pertenece a otro `EntityManager` y no puede usarse en otro hilo. La
tarea recarga el SMS por su `id` con `repository.find(...)`. Las credenciales y el número emisor salen
de `AppSettings` dentro de `SmsSenderProvider` (nunca del bean ni del cliente).

**Qué acciones realiza, en orden** (todas dentro de `JPA.runInTransaction`, en el hilo del pool):

1. Recarga el SMS por su `id`. Son **dos salidas distintas, en dos ramas distintas**:
   - **Si no existe** (`repository.find` devuelve `null`): lanza `IllegalStateException("No existe el SMS " + smsId)`;
     ver `decisiones.md` D8.
   - **Si ya está ENVIADO**: `return` sin efecto; ver D8.
2. Registra el intento (R-Sms-004): fecha del último intento, fecha del primero si era el primero y
   `numeroReintentos + 1`. Se registra **antes** de llamar al proveedor, para que un intento que muera
   de forma anómala también quede contado.
3. Construye el `Sms` de transporte (`com.educaflow.base.infrastructure.sms.Sms`) con el teléfono en
   E.164 y el mensaje —**en la propia línea del `send`**, sin método auxiliar: no hay traducción que
   hacer—, pide el `SmsSender` al `Provider` y envía.
4. Según el resultado: R-Sms-005 (ENVIADO + fecha de envío + sin descripción de fallo) o R-Sms-006
   (FALLIDO + descripción del fallo + sin fecha de envío).
5. `repository.save(sms)` — un único `save` que persiste lo que hayan dejado las tres reglas.

**Garantías.** Cada intento deja el SMS en ENVIADO o en FALLIDO **con motivo**; no hay camino que lo
deje PENDIENTE después de un intento, y el único caso que no puede darse (el SMS desaparecido) falla
de forma ruidosa en vez de pasar inadvertido. El `catch` es de `RuntimeException`, así que cubre por igual el
rechazo del proveedor, un fallo de red y la ausencia de credenciales (ver «Errores»).

**Política de reintento.** No hay reintento automático: el reintento es **manual** (el botón
«Reenviar», V-Sms-014 exige estado FALLIDO). El reenvío en bloque está fuera de alcance, así que el
diseño **no** añade ni un finder por estado ni un job programado.

## Diseño detallado

### Clases nuevas

El ejecutor asíncrono que usa esta regla es el `EjecutorAsincrono` **único** de la aplicación; su
contrato y su cableado están en `design.md` paso 3.

- `com.educaflow.subsystem.sms.module.SmsSenderProvider implements Provider<SmsSender>`
  - `public SmsSender get()` — construye `TwilioCredential` con `sms.credentials.twilio.accountSid` y
    `sms.credentials.twilio.authToken` y devuelve
    `SmsSenderFactory.getTwilioSmsSender(credencial, settings.get("sms.twilio.from"))`.

### Interfaces

Ninguna nueva: el contrato del emisor es `com.educaflow.base.infrastructure.sms.SmsSender`, que ya
existe, y el del transporte es el record `com.educaflow.base.infrastructure.sms.Sms`.

### Tipos propios

Ninguno: la tarea asíncrona solo necesita el `Long smsId`. **MUST NOT** pasar la entidad al hilo.

### Diagrama de secuencia

```
SmsServiceImpl.insert(sms)                    [hilo de la petición, transacción T1]
  ├─ validateInsert(sms) .................... V-Sms-001..011 → aborta si hay mensajes
  ├─ fireActionRule_NormalizarTelefono ...... R-Sms-001 (telefono a E.164)
  ├─ fireActionRule_AsignarValoresIniciales . R-Sms-002 (PENDIENTE, fechaCreacion, 0 reintentos)
  ├─ repository.save(sms) ................... devuelve el Sms con id
  └─ fireActionRule_ProgramarEnvioAsincrono
       └─ ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviarSms(smsId))
            └─ [al COMMIT de T1] pool «async-N» → SmsServiceImpl.enviarSms(smsId)

SmsServiceImpl.enviarSms(smsId)               [hilo del pool, transacción T2 propia]
  ├─ repository.find(smsId) → null → IllegalStateException  (RES-Sms-002 dice que nunca se borra)
  ├─ estado == ENVIADO → return (idempotencia: ENVIADO es terminal)
  ├─ fireActionRule_RegistrarIntentoEnvio ... R-Sms-004 (fechas de intento + reintentos)
  ├─ new base.infrastructure.sms.Sms(...) ... el record de transporte, en la línea del send
  ├─ smsSenderProvider.get() ................ SmsSenderProvider → SmsSenderFactory.getTwilioSmsSender
  ├─ smsSender.send(smsDeTransporte) ........ TwilioSmsSender → API REST del proveedor
  │    ├─ ok    → fireActionRule_MarcarEnvioCorrecto  R-Sms-005 (ENVIADO + fechaEnvio)
  │    └─ error → fireActionRule_MarcarEnvioFallido   R-Sms-006 (FALLIDO + traza)
  └─ repository.save(sms)

SmsServiceImpl.reenviar(entidad, entidadOriginal)   [hilo de la petición, transacción T1']
  ├─ validateReenviar(...) .................. V-Sms-014, V-Sms-015
  └─ fireActionRule_ProgramarEnvioAsincrono(entidadOriginal)   ← el MISMO método que el alta
```

### Errores

| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| La transacción del alta/reenvío hace *rollback* | `EjecutorAsincrono.ejecutarTrasCommit` | La tarea se descarta: no se envía nada de un SMS que no existe en BD |
| **No hay transacción activa en el hilo** que llama a `ejecutarTrasCommit` | `EjecutorAsincrono.ejecutarTrasCommit` | `IllegalStateException` en el acto; contrato completo en `design.md` paso 3 |
| El SMS ya no existe al recargarlo | `SmsServiceImpl.enviarSms` | `IllegalStateException` con el id (`"No existe el SMS " + smsId`); ver `decisiones.md` D8 |
| El SMS ya está ENVIADO | `SmsServiceImpl.enviarSms` | `return` sin efecto: ENVIADO es terminal e idempotente ante un doble disparo |
| Credenciales del proveedor ausentes o vacías (`TwilioCredential` las rechaza) | `SmsSenderProvider.get()`, **dentro** del `try` | R-Sms-006: FALLIDO con la traza. Por eso el servicio inyecta `Provider<SmsSender>` y no `SmsSender` (`decisiones.md` D4): si se inyectara el emisor, el fallo ocurriría al construir el servicio y el alta moriría con un error técnico en vez de dejar el SMS FALLIDO |
| El proveedor rechaza el envío o no se le puede contactar | `TwilioSmsSender.send` (lanza `RuntimeException`) | R-Sms-006: FALLIDO + descripción del fallo (traza completa) + `fechaEnvio` a `null`. **MUST NOT** loguearse ni guardarse ningún dato personal adicional (`k-secure-coding` §6) |
| `RuntimeException` no controlada fuera del `try` (p. ej. al recargar el SMS) | `EjecutorAsincrono` (envoltorio de la tarea) | `log.error` con el fallo; el hilo del pool sobrevive y sigue atendiendo tareas |

### Contenido del método `fireActionRule_*`

Momento: **después** de `repository.save`. Llamadores: `insert` y `reenviar`.

```java
// Firma:
private void fireActionRule_ProgramarEnvioAsincrono(Sms sms);
//   Secuencia:
//     1. Long smsId = sms.getId() — la tarea corre en otro hilo y MUST NOT capturar la entidad,
//        que pertenece al EntityManager de esta transacción.
//     2. ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviarSms(smsId)).
```
