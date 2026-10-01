---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/infrastructure/async/EjecutorAsincrono.java` | Crear | k-guice | **El** pool de hilos daemon de la aplicación (uno solo, compartido): ejecuta una tarea **solo si la transacción actual hace commit** |

### Paso 3 — `base/infrastructure/async/EjecutorAsincrono`: **un solo** ejecutor para toda la aplicación

**Ficheros:** `base/infrastructure/async/EjecutorAsincrono.java` (Crear), `base/infrastructure/CLAUDE.md` (Modificar), `secretariavirtual/module/EjecutorAsincronoProvider.java` (Crear), `secretariavirtual/module/SecretariaVirtualModule.java` (Modificar), `secretariavirtual/startup/AppEventObserver.java` (Modificar), `correos/infrastructure/PostCommitRunner.java`, `correos/infrastructure/CorreoAsyncExecutor.java`, `correos/infrastructure/CorreoEventObserver.java` y `correos/module/CorreoAsyncExecutorProvider.java` (Eliminar), `correos/module/CorreosModule.java` y `correos/service/impl/CorreoServiceImpl.java` (Modificar), y los ficheros de test del final de la tabla.

Motivo (ver `decisiones.md` D3): copiar el mecanismo de correos rompería `cpdCheck`, que está enganchado a `check` y salta con duplicados de ≥100 tokens. La pieza común se extrae **una sola vez** y las dos piezas de correos (`PostCommitRunner` + pool) se funden en un único método, para que nadie tenga que acordarse de combinarlas. Y se extrae **una sola instancia**: ni la spec ni `design-guidelines.md` piden aislar los pools por subsistema, así que el proyecto pasa a tener **un** ejecutor asíncrono compartido, cableado donde ya vive lo transversal (`SecretariaVirtualModule` + `AppEventObserver`), en vez del trío (pool + `Provider` + observer) **por** subsistema. Son 6 clases menos que el andamiaje duplicado: `subsystem/sms` no aporta **ninguna** clase de infraestructura asíncrona y `subsystem/correos` pierde las tres que tenía.

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
  `RuntimeException` no controlada se registre con `log.error` y no mate al hilo del pool. Es **un único** método
  a propósito: con dos piezas separadas (registrar + enviar) quien las usa puede olvidarse de atar el envío al
  commit y el hilo no vería todavía la fila (bug intermitente).
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

### Verificación del Paso 3

**Verificar:** `./run.sh` compila y pasa `check` (incluido `cpdCheck`, que es el motivo del paso);
`EjecutorAsincronoTest` cubre las **tres** entradas de `ejecutarTrasCommit` —commit ejecuta la tarea, rollback no la ejecuta y **sin transacción activa lanza `IllegalStateException` sin haber enviado nada al pool**—, de modo que ninguna de las tres queda sin declarar ni sin probar;
`grep -rn "PostCommitRunner\|CorreoAsyncExecutor\|CorreoEventObserver\|ProgramarReenvioAsincrono" src/` no devuelve nada;
`grep -rn "private String trazaCompleta" src/main/java` no devuelve nada y `grep -rn --include=*.java "trazaCompleta" src/main/java` devuelve **tres** líneas: la declaración en `TextUtil` y las dos llamadas (`CorreoServiceImpl`, `SmsServiceImpl`);
`grep -rn "new EjecutorAsincrono\|bind(EjecutorAsincrono" src/main/java` devuelve **una** línea de cada;
el alta de un correo sigue dejando el correo en «Enviado»/«Fallido» (los E2E de correos de
`src/test/e2e/subsystem/correos/` siguen pasando).

### Errores relacionados (de `design/rules/R-Sms-003.md`, §«Errores»)

Ver `design/rules/R-Sms-003.md` §«Errores»: filas «La transacción del alta/reenvío hace *rollback*», «**No hay transacción activa en el hilo**…» y «`RuntimeException` no controlada fuera del `try`…» (origen `EjecutorAsincrono`).
