---
type: implementation-task
template: system
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | Eliminar | — | Su función es ahora `EjecutorAsincrono.ejecutarTrasCommit` (ver «Eliminaciones declaradas») |
| `src/test/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunnerTest.java` | Eliminar | — | Su contenido vive ahora en `EjecutorAsincronoTest` |

### Paso 3 — `base/infrastructure/async/EjecutorAsincrono`: **un solo** ejecutor para toda la aplicación

**Ficheros:** `base/infrastructure/async/EjecutorAsincrono.java` (Crear), `base/infrastructure/CLAUDE.md` (Modificar), `secretariavirtual/module/EjecutorAsincronoProvider.java` (Crear), `secretariavirtual/module/SecretariaVirtualModule.java` (Modificar), `secretariavirtual/startup/AppEventObserver.java` (Modificar), `correos/infrastructure/PostCommitRunner.java`, `correos/infrastructure/CorreoAsyncExecutor.java`, `correos/infrastructure/CorreoEventObserver.java` y `correos/module/CorreoAsyncExecutorProvider.java` (Eliminar), `correos/module/CorreosModule.java` y `correos/service/impl/CorreoServiceImpl.java` (Modificar), y los ficheros de test del final de la tabla.

Motivo (ver `decisiones.md` D3): copiar el mecanismo de correos rompería `cpdCheck`, que está enganchado a `check` y salta con duplicados de ≥100 tokens. La pieza común se extrae **una sola vez** y las dos piezas de correos (`PostCommitRunner` + pool) se funden en un único método, para que nadie tenga que acordarse de combinarlas. Y se extrae **una sola instancia**: ni la spec ni `design-guidelines.md` piden aislar los pools por subsistema, así que el proyecto pasa a tener **un** ejecutor asíncrono compartido, cableado donde ya vive lo transversal (`SecretariaVirtualModule` + `AppEventObserver`), en vez del trío (pool + `Provider` + observer) **por** subsistema. Son 6 clases menos que el andamiaje duplicado: `subsystem/sms` no aporta **ninguna** clase de infraestructura asíncrona y `subsystem/correos` pierde las tres que tenía.

### Eliminaciones declaradas — filas relacionadas (de `design/design.md`)

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `PostCommitRunner` (y su método estático `runAfterCommit`) | `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | — (sin ID de spec: lo exige `CLAUDE.md` §`cpdCheck`, «un duplicado se arregla extrayendo el código común en su sitio»). Su función la presta ahora `EjecutorAsincrono.ejecutarTrasCommit`, que además impide olvidarse de atar el envío al commit. Ver `decisiones.md` D3 |
| Clases de test `PostCommitRunnerTest`, `CorreoAsyncExecutorTest`, `CorreoAsyncExecutorProviderTest` y `CorreoEventObserverTest` | `src/test/java/com/educaflow/subsystem/correos/` | — (consecuencia de las anteriores). Sus casos se trasladan **íntegros** a `EjecutorAsincronoTest`, `EjecutorAsincronoProviderTest` y `AppEventObserverTest`; no se pierde ninguna comprobación |

### Paso 3 — tests (de `design/design.md`)

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

### Verificación del Paso 3

**Verificar:** `./run.sh` compila y pasa `check` (incluido `cpdCheck`, que es el motivo del paso);
`EjecutorAsincronoTest` cubre las **tres** entradas de `ejecutarTrasCommit` —commit ejecuta la tarea, rollback no la ejecuta y **sin transacción activa lanza `IllegalStateException` sin haber enviado nada al pool**—, de modo que ninguna de las tres queda sin declarar ni sin probar;
`grep -rn "PostCommitRunner\|CorreoAsyncExecutor\|CorreoEventObserver\|ProgramarReenvioAsincrono" src/` no devuelve nada;
`grep -rn "private String trazaCompleta" src/main/java` no devuelve nada y `grep -rn --include=*.java "trazaCompleta" src/main/java` devuelve **tres** líneas: la declaración en `TextUtil` y las dos llamadas (`CorreoServiceImpl`, `SmsServiceImpl`);
`grep -rn "new EjecutorAsincrono\|bind(EjecutorAsincrono" src/main/java` devuelve **una** línea de cada;
el alta de un correo sigue dejando el correo en «Enviado»/«Fallido» (los E2E de correos de
`src/test/e2e/subsystem/correos/` siguen pasando).

### Decisión del descomponedor (no es texto del diseño)

La fila es `Acción: Eliminar`: el fichero de producción y su test (también `Eliminar` en la tabla) se borran juntos porque el test no compila sin la clase (acoplamiento fuerte). No se crea nada en su lugar en esta tarea: la clase sustituta la crean otras tareas. Antes de borrar, comprobar que no queda ninguna referencia en `src/main` (las tareas de `CorreoServiceImpl` y `CorreosModule` las quitan); si quedara alguna fuera de lo declarado por el diseño, **parar y reportar** (BLOCKED). `CorreoServiceImplTest` todavía usa `mockStatic(PostCommitRunner.class)` hasta que su tarea de test lo adapte: esa referencia **sí** está declarada por el diseño y no es motivo de BLOCKED (el build final la verifica).
