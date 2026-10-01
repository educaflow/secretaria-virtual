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

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Modificar | k-sistemas (servicios.md) | Inyecta `EjecutorAsincrono` en vez de `CorreoAsyncExecutor`; su **único** `fireActionRule_ProgramarEnvioAsincrono` llama a `ejecutarTrasCommit` y lo llaman `insert` y `reenviar` (se borra el gemelo `fireActionRule_ProgramarReenvioAsincrono`, copia byte a byte); se borra el privado `trazaCompleta`, que pasa a `TextUtil`; fuera los imports de `PostCommitRunner`, `StringWriter` y `PrintWriter` |

### Paso 3 — `base/infrastructure/async/EjecutorAsincrono`: **un solo** ejecutor para toda la aplicación

**Ficheros:** `base/infrastructure/async/EjecutorAsincrono.java` (Crear), `base/infrastructure/CLAUDE.md` (Modificar), `secretariavirtual/module/EjecutorAsincronoProvider.java` (Crear), `secretariavirtual/module/SecretariaVirtualModule.java` (Modificar), `secretariavirtual/startup/AppEventObserver.java` (Modificar), `correos/infrastructure/PostCommitRunner.java`, `correos/infrastructure/CorreoAsyncExecutor.java`, `correos/infrastructure/CorreoEventObserver.java` y `correos/module/CorreoAsyncExecutorProvider.java` (Eliminar), `correos/module/CorreosModule.java` y `correos/service/impl/CorreoServiceImpl.java` (Modificar), y los ficheros de test del final de la tabla.

Motivo (ver `decisiones.md` D3): copiar el mecanismo de correos rompería `cpdCheck`, que está enganchado a `check` y salta con duplicados de ≥100 tokens. La pieza común se extrae **una sola vez** y las dos piezas de correos (`PostCommitRunner` + pool) se funden en un único método, para que nadie tenga que acordarse de combinarlas. Y se extrae **una sola instancia**: ni la spec ni `design-guidelines.md` piden aislar los pools por subsistema, así que el proyecto pasa a tener **un** ejecutor asíncrono compartido, cableado donde ya vive lo transversal (`SecretariaVirtualModule` + `AppEventObserver`), en vez del trío (pool + `Provider` + observer) **por** subsistema. Son 6 clases menos que el andamiaje duplicado: `subsystem/sms` no aporta **ninguna** clase de infraestructura asíncrona y `subsystem/correos` pierde las tres que tenía.

#### Cambios en correos (comportamiento idéntico; **el resto de cada clase se conserva**)

```java
// Clase: com.educaflow.subsystem.correos.module.CorreosModule  (Modificar — delta)
protected void configure();

// Clase: com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl  (Modificar — delta)
private void fireActionRule_ProgramarEnvioAsincrono(Correo correo);
private void fireActionRule_ProgramarReenvioAsincrono(Correo correo);   // se BORRA
private String trazaCompleta(Throwable excepcion);                      // se BORRA
```
- **`CorreoServiceImpl.fireActionRule_ProgramarEnvioAsincrono(Correo)`** — **delta:** el campo inyectado pasa de
  `CorreoAsyncExecutor` a `EjecutorAsincrono`, y donde hoy hace
  `PostCommitRunner.runAfterCommit(() -> correoAsyncExecutor.submit(...))` pasa a hacer
  `ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviarCorreo(correoId))`. Se eliminan los imports de
  `PostCommitRunner` y `CorreoAsyncExecutor`. El comentario del método superviviente cita las dos reglas que lo
  disparan (R-Correo-001 y R-Correo-002).
- **Delta 2 — se BORRA `fireActionRule_ProgramarReenvioAsincrono(Correo)`** (hoy `CorreoServiceImpl.java:321`),
  que es copia byte a byte del anterior (`:312`), y la línea 77 de `reenviar` pasa a llamar al **mismo**
  `fireActionRule_ProgramarEnvioAsincrono(entidadOriginal)`. La política de envío queda con **un** solo dueño,
  igual que en `SmsServiceImpl` (ver `rules/R-Sms-003.md`: «si hubiera uno por disparador, la política de envío
  tendría dos dueños»).
- **Delta 3 — se BORRA el privado `trazaCompleta(Throwable)`** (hoy `CorreoServiceImpl.java:386`). Su cuerpo se
  **mueve** tal cual a `TextUtil.trazaCompleta` (paso 2) y la única línea que lo llamaba pasa a llamar al de
  `TextUtil`; se eliminan los imports de `StringWriter` y `PrintWriter`. Mismo texto guardado, mismo
  comportamiento. Motivo: este fichero se reescribe igualmente en este paso, así que dejar el método aquí y
  copiarlo en `SmsServiceImpl` daría **dos** dueños a «qué texto se guarda en `descripcionUltimoFallo`». No
  cambia ninguna otra línea.

### Eliminaciones declaradas — fila relacionada (de `design/design.md`)

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Método privado `trazaCompleta(Throwable)` de `CorreoServiceImpl` | `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | — (sin ID de spec: `k-code-quality/disenyo.md` §«Una decisión con varios dueños»). **Sale del fichero, no desaparece**: su cuerpo se mueve tal cual a `TextUtil.trazaCompleta` (paso 2), público, y las dos llamadas (correos y SMS) van ahí. Evita que «qué texto se guarda en `descripcionUltimoFallo`» tenga un dueño por subsistema, justo lo que D3 decide para el mecanismo asíncrono. Se aprovecha que el paso 3 reescribe este fichero igualmente |

### Notas y supuestos 4 y 8 (de `design/design.md`)

4. **El subsistema refactoriza `subsystem/correos`** aunque el spec no lo declare como sistema modificado. No es una ampliación funcional: es la única forma de replicar su mecanismo de envío asíncrono sin romper `cpdCheck`, que está enganchado a `check` y haría fallar `./run.sh`. El delta sobre correos es mecánico y **resta**: desaparecen `PostCommitRunner`, `CorreoAsyncExecutor`, `CorreoAsyncExecutorProvider` y `CorreoEventObserver`, `CorreosModule` se queda con un `bind` y `CorreoServiceImpl` cambia el tipo de un campo inyectado, funde sus dos `fireActionRule_Programar*Asincrono` (copias byte a byte) en uno solo al que llaman `insert` y `reenviar`, reescribe esa única línea y suelta su privado `trazaCompleta`, que pasa a `TextUtil` (ver nota 8). El comportamiento observable no cambia (misma tarea, mismo momento, mismo tamaño de pool por defecto); lo único que cambia fuera del código es el nombre de la clave de configuración del pool (`mail.send.pool-size` → `async.pool-size`, ver «Eliminaciones declaradas»). Ver `decisiones.md` D3.

8. **`trazaCompleta` se MUEVE a `TextUtil`, y ningún servicio lo declara ya como privado.** No se copia de correos a SMS: el paso 3 reescribe `CorreoServiceImpl` de todas formas (campo inyectado, fusión de los dos `fireActionRule_Programar*Asincrono`, imports), así que «tocar correos una segunda vez» dejó de ser un coste. Con la copia, «qué texto se guarda en `descripcionUltimoFallo`» tendría un dueño en cada subsistema; con el método en `base/util` tiene uno solo, y `base/util/CLAUDE.md` —que obliga a mirar ahí antes de reimplementar cualquier helper— lo anuncia. **MUST NOT** reutilizarse `AsciiTableUtil.getStackTrace`: es privado y devuelve otra cosa (una `List<String>` indentada para tabla), no la traza estándar. Ver la fila `Modificar` de `base/util/TextUtil.java` y la de `base/util/CLAUDE.md` en «Ficheros a crear o modificar», y la entrada correspondiente de «Eliminaciones declaradas».

### Verificación del Paso 3

**Verificar:** `./run.sh` compila y pasa `check` (incluido `cpdCheck`, que es el motivo del paso);
`EjecutorAsincronoTest` cubre las **tres** entradas de `ejecutarTrasCommit` —commit ejecuta la tarea, rollback no la ejecuta y **sin transacción activa lanza `IllegalStateException` sin haber enviado nada al pool**—, de modo que ninguna de las tres queda sin declarar ni sin probar;
`grep -rn "PostCommitRunner\|CorreoAsyncExecutor\|CorreoEventObserver\|ProgramarReenvioAsincrono" src/` no devuelve nada;
`grep -rn "private String trazaCompleta" src/main/java` no devuelve nada y `grep -rn --include=*.java "trazaCompleta" src/main/java` devuelve **tres** líneas: la declaración en `TextUtil` y las dos llamadas (`CorreoServiceImpl`, `SmsServiceImpl`);
`grep -rn "new EjecutorAsincrono\|bind(EjecutorAsincrono" src/main/java` devuelve **una** línea de cada;
el alta de un correo sigue dejando el correo en «Enviado»/«Fallido» (los E2E de correos de
`src/test/e2e/subsystem/correos/` siguen pasando).

### Decisión del descomponedor (no es texto del diseño)

La adaptación de `CorreoServiceImplTest` (sustituir los `mockStatic(PostCommitRunner.class)`) es una tarea de test posterior; esta tarea solo toca el código de producción. **MUST** conservarse el resto de la clase.
