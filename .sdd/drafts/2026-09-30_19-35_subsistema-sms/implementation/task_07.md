---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/infrastructure/async/EjecutorAsincrono.java` | Crear | k-guice | **El** pool de hilos daemon de la aplicación (uno solo, compartido): ejecuta una tarea **solo si la transacción actual hace commit** |

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

**Verificar:** `./run.sh` compila y pasa `check` (incluido `cpdCheck`, que es el motivo del paso);

`grep -rn "new EjecutorAsincrono\|bind(EjecutorAsincrono" src/main/java` devuelve **una** línea de cada;



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `PostCommitRunner` (y su método estático `runAfterCommit`) | `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | — (sin ID de spec: lo exige `CLAUDE.md` §`cpdCheck`, «un duplicado se arregla extrayendo el código común en su sitio»). Lo sustituye `EjecutorAsincrono.ejecutarTrasCommit`; ver `decisiones.md` D3 |
| Clase `CorreoAsyncExecutor` (pool, `ThreadFactory`, `submit`, `detener`) | `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoAsyncExecutor.java` | — (mismo motivo). Lo sustituye el `EjecutorAsincrono` **único** de la aplicación, que `CorreoServiceImpl` inyecta; ver D3 |

Errores de `design/rules/R-Sms-003.md` que trata esta clase:

| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| La transacción del alta/reenvío hace *rollback* | `EjecutorAsincrono.ejecutarTrasCommit` | La tarea se descarta: no se envía nada de un SMS que no existe en BD |
| **No hay transacción activa en el hilo** que llama a `ejecutarTrasCommit` | `EjecutorAsincrono.ejecutarTrasCommit` | `IllegalStateException` en el acto; contrato completo en `design.md` paso 3 |
| `RuntimeException` no controlada fuera del `try` (p. ej. al recargar el SMS) | `EjecutorAsincrono` (envoltorio de la tarea) | `log.error` con el fallo; el hilo del pool sobrevive y sigue atendiendo tareas |

Decisión de diseño citada: `design/decisiones.md` D3.
