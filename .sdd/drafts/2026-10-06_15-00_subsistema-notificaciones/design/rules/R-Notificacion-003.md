# R-Notificacion-003 — Enviar la notificación por su canal (un intento)

**Entidad:** Notificacion (y cada canal: Correo, Sms)
**Origen spec:** RN-Notificacion-003, RN-Notificacion-004, RN-Notificacion-005, RN-Notificacion-006, RN-Correo-001, RN-Correo-003, RN-Correo-004, RN-Sms-002
**Operación:** enviar (acción interna del sistema, lanzada tras el alta —R-Notificacion-002— y tras cada reenvío —R-Notificacion-004—)
**Momento:** en su propia transacción, en un hilo del pool de `EjecutorAsincrono`, **después** del commit de la transacción que lo programó
**Servicio host:** com.educaflow.subsystem.notificaciones.service.impl.NotificacionCanalServiceImpl (heredado por CorreoServiceImpl y SmsServiceImpl)
**Método host:** `void enviar(Long notificacionId)` (bloque «Otras funciones»), que encadena `fireActionRule_RegistrarIntentoEnvio`, el gancho `enviarPorCanal(T)` y `fireActionRule_MarcarEnvioCorrecto` / `fireActionRule_MarcarEnvioFallido`

## Análisis de la regla

1. **Qué la dispara.** `fireActionRule_ProgramarEnvioAsincrono(T)` pide a `EjecutorAsincrono.ejecutarTrasCommit(...)` que, cuando la transacción del alta o del reenvío haga commit, ejecute `enviar(id)` en un hilo del pool.
   Se pasa **solo el id**: la entidad pertenece al `EntityManager` de la transacción original y la tarea corre en otro hilo.
   Programarla antes del commit haría que el hilo del pool no viera todavía la fila.
2. **Qué lee.** Abre su propia transacción (`JPA.runInTransaction`) y relee la notificación con `JPA.em().find(model, id, LockModeType.PESSIMISTIC_WRITE)`, donde `model` es la clase del canal del servicio (`Correo.class`, `Sms.class`), que `DefaultModelService` guarda en su campo `model`.
   El bloqueo pesimista serializa dos envíos de la misma notificación (dos «Reenviar» seguidos del mismo registro FALLIDO): el segundo espera al commit del primero y, si este acabó en ENVIADO, no vuelve a enviar (RN-Notificacion-003) — en el SMS, además, no se vuelve a pagar.
3. **Idempotencia (RN-Notificacion-003).** Si la notificación ya está `ENVIADO`, termina sin hacer nada (sin registrar intento ni guardar).
   Una notificación que no existe es un error de programación (`IllegalStateException`), no un error de negocio.
4. **Registro del intento (RN-Notificacion-004).** `fireActionRule_RegistrarIntentoEnvio`: fecha del último intento = ahora; si `numeroReintentos == 0` (es el primer intento), fecha del primer intento = ahora; después, número de reintentos + 1.
5. **Envío por el canal (gancho `enviarPorCanal(T)`, abstracto en la base).**
   - **Correo (RN-Correo-001, RN-Correo-003, RN-Correo-004):** `mailSenderProvider.get().send(construirMail(correo))`. `construirMail` separa por comas y recorta (`trim`) las direcciones de «para», «en copia» y «en copia oculta», toma el remitente de `mail.address.from` (`AppSettings`), el asunto, el cuerpo **solo como texto plano** (el HTML va a `null`: el cuerpo puede incrustar texto libre, como el de la subsanación, que no debe interpretarse como marcado) y todos los adjuntos como `Fichero(nombreFichero, bytes, tipo)`. El `Provider<MailSender>` se resuelve **dentro** del envío para que una instalación sin servidor de correo configurado deje el correo FALLIDO con la descripción del fallo, en lugar de romper la creación del servicio.
   - **SMS (RN-Sms-002):** `smsSenderProvider.get().send(new base.infrastructure.sms.Sms(telefono, mensaje))`, con el mismo razonamiento para el `Provider<SmsSender>` (sin proveedor configurado → FALLIDO).
   - Un fallo del canal llega como `RuntimeException` (incluida la de la resolución del `Provider`).
6. **Resultado.**
   - Éxito (RN-Notificacion-005): `fireActionRule_MarcarEnvioCorrecto` → estado ENVIADO, fecha de envío = ahora, descripción del último fallo = null.
   - Fallo (RN-Notificacion-006): `fireActionRule_MarcarEnvioFallido(T, RuntimeException)` → estado FALLIDO, descripción = traza de la excepción (`ExceptionUtil.getTraceAsString`), fecha de envío = null.
   - En ambos casos `repository.save(notificacion)` dentro de la misma transacción.
7. **Garantías.** Las tres asignaciones de estado son **incondicionales** y solo las hace este método (más `fireActionRule_AsignarValoresIniciales` en el alta): por construcción se cumplen RES-Notificacion-006, 008, 009, 010, 011 y 012 (ningún cliente puede tocar estos campos: están fuera de todos los `AllowProperties` y `update` está prohibido).
   Las transiciones posibles son exactamente PENDIENTE→ENVIADO, PENDIENTE→FALLIDO, FALLIDO→ENVIADO y FALLIDO→FALLIDO (el reenvío solo se admite desde FALLIDO, V-Notificacion-014; ENVIADO corta en el paso 3).

## Diseño detallado

### Clases nuevas
- `com.educaflow.subsystem.notificaciones.service.impl.NotificacionCanalServiceImpl<T extends Notificacion>` — clase base abstracta del ciclo de vida de un canal (ver `design.md`, Paso 3). Aloja `enviar(Long)` y las `fireActionRule_*` de esta regla.
  - `protected abstract void enviarPorCanal(T notificacion)` — envía por el canal concreto; lanza `RuntimeException` si falla.

### Interfaces
- Ninguna nueva para el envío: los canales reutilizan `MailSender` y `SmsSender` de `base/infrastructure` (no se tocan), cableados por `NotificacionesModule` con sus `Provider`.

### Tipos propios
- Ninguno.

### Diagrama de secuencia
```
fireActionRule_ProgramarEnvioAsincrono(n)            [transacción del alta / reenvío]
  └─ ejecutorAsincrono.ejecutarTrasCommit(() -> enviar(id))
                                                     [commit]
enviar(id)                                           [hilo del pool, transacción propia]
  ├─ JPA.em().find(model, id, PESSIMISTIC_WRITE) → n | null → IllegalStateException
  ├─ n.estado == ENVIADO → return                    (RN-Notificacion-003)
  ├─ fireActionRule_RegistrarIntentoEnvio(n)         (RN-Notificacion-004)
  ├─ try  enviarPorCanal(n)                          (Correo: MailSender; Sms: SmsSender)
  │      └─ fireActionRule_MarcarEnvioCorrecto(n)    (RN-Notificacion-005)
  │  catch RuntimeException ex
  │      └─ fireActionRule_MarcarEnvioFallido(n, ex) (RN-Notificacion-006, RN-Correo-003)
  └─ repository.save(n)
```

### Errores
| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| La notificación no existe | `enviar` | `IllegalStateException` (error de programación; el pool lo registra) |
| Ya está ENVIADO (envío concurrente ya resuelto) | `enviar` | Termina sin hacer nada |
| Sin configuración de correo / SMS | `Provider.get()` dentro de `enviarPorCanal` | `RuntimeException` → FALLIDO con la traza |
| Fallo del servidor de correo o del proveedor de SMS | `MailSender.send` / `SmsSender.send` | `RuntimeException` → FALLIDO con la traza |

### Contenido del método `fireActionRule_*`
```java
// Firma:
void enviar(Long notificacionId);
//   Implementa R-Notificacion-003 (Origen spec: RN-Notificacion-003..006, RN-Correo-001, 003, 004, RN-Sms-002).
//   Diseño detallado en design/rules/R-Notificacion-003.md.
//   Secuencia:
//     1. JPA.runInTransaction(...)
//     2. find(model, id, PESSIMISTIC_WRITE); null → IllegalStateException
//     3. si estado == ENVIADO → return
//     4. fireActionRule_RegistrarIntentoEnvio(n)
//     5. try { enviarPorCanal(n); fireActionRule_MarcarEnvioCorrecto(n); }
//        catch (RuntimeException ex) { fireActionRule_MarcarEnvioFallido(n, ex); }
//     6. repository.save(n)
```
