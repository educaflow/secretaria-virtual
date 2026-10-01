---
type: implementation-task
template: system
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/startup/AppEventObserver.java` | Modificar | k-guice | Su `onAppShutdown` **ya existente** para el ejecutor (`detener()`); no hace falta ningún observer nuevo |

### Paso 3 — `base/infrastructure/async/EjecutorAsincrono`: **un solo** ejecutor para toda la aplicación

**Ficheros:** `base/infrastructure/async/EjecutorAsincrono.java` (Crear), `base/infrastructure/CLAUDE.md` (Modificar), `secretariavirtual/module/EjecutorAsincronoProvider.java` (Crear), `secretariavirtual/module/SecretariaVirtualModule.java` (Modificar), `secretariavirtual/startup/AppEventObserver.java` (Modificar), `correos/infrastructure/PostCommitRunner.java`, `correos/infrastructure/CorreoAsyncExecutor.java`, `correos/infrastructure/CorreoEventObserver.java` y `correos/module/CorreoAsyncExecutorProvider.java` (Eliminar), `correos/module/CorreosModule.java` y `correos/service/impl/CorreoServiceImpl.java` (Modificar), y los ficheros de test del final de la tabla.

Motivo (ver `decisiones.md` D3): copiar el mecanismo de correos rompería `cpdCheck`, que está enganchado a `check` y salta con duplicados de ≥100 tokens. La pieza común se extrae **una sola vez** y las dos piezas de correos (`PostCommitRunner` + pool) se funden en un único método, para que nadie tenga que acordarse de combinarlas. Y se extrae **una sola instancia**: ni la spec ni `design-guidelines.md` piden aislar los pools por subsistema, así que el proyecto pasa a tener **un** ejecutor asíncrono compartido, cableado donde ya vive lo transversal (`SecretariaVirtualModule` + `AppEventObserver`), en vez del trío (pool + `Provider` + observer) **por** subsistema. Son 6 clases menos que el andamiaje duplicado: `subsystem/sms` no aporta **ninguna** clase de infraestructura asíncrona y `subsystem/correos` pierde las tres que tenía.

#### Cableado del ejecutor único

```java
// Clase: com.educaflow.secretariavirtual.module.EjecutorAsincronoProvider implements Provider<EjecutorAsincrono>
public EjecutorAsincrono get();

// Clase: com.educaflow.secretariavirtual.module.SecretariaVirtualModule  (Modificar — delta)
protected void configure();

// Clase: com.educaflow.secretariavirtual.startup.AppEventObserver  (Modificar — delta)
public void onAppShutdown(@Observes ShutdownEvent event);
```

- **`AppEventObserver.onAppShutdown(...)`** — **delta:** se le inyecta el `EjecutorAsincrono` (`@Inject`) y su
  `onAppShutdown` —que **ya existe** y ya se ejecuta al apagar— añade `ejecutorAsincrono.detener()` antes del log
  de despedida. **MUST NOT** crearse ningún observer nuevo: la pieza que escucha el `ShutdownEvent` de la
  aplicación ya está escrita y es esta.

### Eliminaciones declaradas — fila relacionada (de `design/design.md`)

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `CorreoEventObserver` | `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoEventObserver.java` | — (mismo motivo). Su única responsabilidad real (`detener()` al `ShutdownEvent`) pasa al `AppEventObserver` que la aplicación **ya tiene**; su `onAppStart` solo escribía una línea de log |

### Verificación del Paso 3

**Verificar:** `./run.sh` compila y pasa `check` (incluido `cpdCheck`, que es el motivo del paso);
`EjecutorAsincronoTest` cubre las **tres** entradas de `ejecutarTrasCommit` —commit ejecuta la tarea, rollback no la ejecuta y **sin transacción activa lanza `IllegalStateException` sin haber enviado nada al pool**—, de modo que ninguna de las tres queda sin declarar ni sin probar;
`grep -rn "PostCommitRunner\|CorreoAsyncExecutor\|CorreoEventObserver\|ProgramarReenvioAsincrono" src/` no devuelve nada;
`grep -rn "private String trazaCompleta" src/main/java` no devuelve nada y `grep -rn --include=*.java "trazaCompleta" src/main/java` devuelve **tres** líneas: la declaración en `TextUtil` y las dos llamadas (`CorreoServiceImpl`, `SmsServiceImpl`);
`grep -rn "new EjecutorAsincrono\|bind(EjecutorAsincrono" src/main/java` devuelve **una** línea de cada;
el alta de un correo sigue dejando el correo en «Enviado»/«Fallido» (los E2E de correos de
`src/test/e2e/subsystem/correos/` siguen pasando).
