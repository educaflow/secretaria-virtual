---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/module/EjecutorAsincronoProvider.java` | Crear | k-guice | Construye el `EjecutorAsincrono` leyendo `async.pool-size` (por defecto 2) |

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

- **`EjecutorAsincronoProvider.get()`** — devuelve
  `new EjecutorAsincrono(AppSettings.get().getInt("async.pool-size", 2))`. Es el **único** sitio del proyecto que
  lee un tamaño de pool (antes había uno por subsistema). Hace falta `Provider` (no basta `bind(...).to(...)`)
  porque el tamaño viene de configuración y no de otro bean (`k-guice` §3.3/§4.2).

### Notas y supuestos 7 (de `design/design.md`)

7. **La clave del tamaño del pool NO se versiona, y ahora es UNA sola.** `axelor-config.properties` lleva solo las **tres** propiedades que enumeran las guías (las que llevan secreto y van vacías); las guías fijan exactamente esas tres para este trabajo. El tamaño del único pool de la aplicación lo lee `EjecutorAsincronoProvider` con `AppSettings.get().getInt("async.pool-size", 2)`, igual que hoy hace `CorreoAsyncExecutorProvider` con `mail.send.pool-size`, que **tampoco** está en el fichero versionado (`grep -n "mail" src/main/resources/axelor-config.properties` no devuelve nada). Quien quiera otro tamaño pone la clave en su configuración privada; así hay un único sitio donde está escrito el valor por defecto y no dos que puedan divergir. Con el ejecutor único, además, la aplicación pasa de **dos** claves de pool (`mail.send.pool-size` y la que habría traído SMS) a **una**, que es exactamente lo contrario de esconder configuración. **Nota para `/sdd-implementer`:** si en el futuro se decide versionar esta clave, va en `src/main/resources/axelor-config.properties` (`CLAUDE.md` § Configuración) y como `async.pool-size=2`, **nunca** como una clave por subsistema.

### Verificación del Paso 3

**Verificar:** `./run.sh` compila y pasa `check` (incluido `cpdCheck`, que es el motivo del paso);
`EjecutorAsincronoTest` cubre las **tres** entradas de `ejecutarTrasCommit` —commit ejecuta la tarea, rollback no la ejecuta y **sin transacción activa lanza `IllegalStateException` sin haber enviado nada al pool**—, de modo que ninguna de las tres queda sin declarar ni sin probar;
`grep -rn "PostCommitRunner\|CorreoAsyncExecutor\|CorreoEventObserver\|ProgramarReenvioAsincrono" src/` no devuelve nada;
`grep -rn "private String trazaCompleta" src/main/java` no devuelve nada y `grep -rn --include=*.java "trazaCompleta" src/main/java` devuelve **tres** líneas: la declaración en `TextUtil` y las dos llamadas (`CorreoServiceImpl`, `SmsServiceImpl`);
`grep -rn "new EjecutorAsincrono\|bind(EjecutorAsincrono" src/main/java` devuelve **una** línea de cada;
el alta de un correo sigue dejando el correo en «Enviado»/«Fallido» (los E2E de correos de
`src/test/e2e/subsystem/correos/` siguen pasando).

### Decisión del descomponedor (no es texto del diseño)

De «Cableado del ejecutor único» esta tarea implementa solo `EjecutorAsincronoProvider.get()`; el delta de `SecretariaVirtualModule` y el de `AppEventObserver` son tareas propias.
