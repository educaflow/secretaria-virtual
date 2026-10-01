---
type: implementation-task
template: system
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Modificar | k-sistemas (servicios.md) | Inyecta `EjecutorAsincrono` en vez de `CorreoAsyncExecutor`; su **único** `fireActionRule_ProgramarEnvioAsincrono` llama a `ejecutarTrasCommit` y lo llaman `insert` y `reenviar` (se borra el gemelo `fireActionRule_ProgramarReenvioAsincrono`, copia byte a byte); se borra el privado `trazaCompleta`, que pasa a `TextUtil`; fuera los imports de `PostCommitRunner`, `StringWriter` y `PrintWriter` |

### Paso 3 — `base/infrastructure/async/EjecutorAsincrono`: **un solo** ejecutor para toda la aplicación

**Ficheros:** `base/infrastructure/async/EjecutorAsincrono.java` (Crear), `base/infrastructure/CLAUDE.md` (Modificar), `secretariavirtual/module/EjecutorAsincronoProvider.java` (Crear), `secretariavirtual/module/SecretariaVirtualModule.java` (Modificar), `secretariavirtual/startup/AppEventObserver.java` (Modificar), `correos/infrastructure/PostCommitRunner.java`, `correos/infrastructure/CorreoAsyncExecutor.java`, `correos/infrastructure/CorreoEventObserver.java` y `correos/module/CorreoAsyncExecutorProvider.java` (Eliminar), `correos/module/CorreosModule.java` y `correos/service/impl/CorreoServiceImpl.java` (Modificar), y los ficheros de test del final de la tabla.

Motivo: extrae el mecanismo de correos a un único ejecutor compartido; ver `decisiones.md` D3.

#### Cambios en correos (comportamiento idéntico; **el resto de cada clase se conserva**)

```java

// Clase: com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl  (Modificar — delta)
private void fireActionRule_ProgramarEnvioAsincrono(Correo correo);
private void fireActionRule_ProgramarReenvioAsincrono(Correo correo);   // se BORRA
private String trazaCompleta(Throwable excepcion);                      // se BORRA
```

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

`grep -rn "PostCommitRunner\|CorreoAsyncExecutor\|CorreoEventObserver\|ProgramarReenvioAsincrono" src/` no devuelve nada;
`grep -rn "private String trazaCompleta" src/main/java` no devuelve nada y `grep -rn --include=*.java "trazaCompleta" src/main/java` devuelve **tres** líneas: la declaración en `TextUtil` y las dos llamadas (`CorreoServiceImpl`, `SmsServiceImpl`);



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Método privado `trazaCompleta(Throwable)` de `CorreoServiceImpl` | `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | — (sin ID de spec). Se mueve a `TextUtil.trazaCompleta` (paso 2) |
