---
type: implementation-task
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoAsyncExecutor.java` | Crear | k-guice | Executor gestionado para el envío asíncrono |

## Texto del diseño (verbatim, `design.md`, Paso 6 — Módulo Guice)

El envío asíncrono necesita tres piezas que **no** son `ModelService` y cuya construcción no es trivial (dependen de configuración): `MailSender` (credenciales SMTP), `CorreoAsyncExecutor` (tamaño de pool) y el observador de ciclo de vida. Ver `[[k-guice]]`.

#### `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor`

```java
package com.educaflow.subsystem.correos.infrastructure;

public class CorreoAsyncExecutor {

    private final java.util.concurrent.ExecutorService executorService;

    public CorreoAsyncExecutor(int tamanoPool);
    //   Crea Executors.newFixedThreadPool(tamanoPool, threadFactory); threadFactory produce hilos
    //   daemon=true con nombre "correo-envio-N" (red de seguridad: si el hook de parada no llegara a
    //   ejecutarse, estos hilos no bloquean la parada de la JVM/Tomcat — design-guidelines exige
    //   evitar leaks de memoria/hilos).

    public void submit(Runnable tarea);
    //   executorService.submit(() -> { try { tarea.run(); } catch (RuntimeException ex) {
    //     log.error("Fallo no controlado en el envío asíncrono de un correo", ex); } });
    //   MUST NOT dejar que una excepción no capturada mate el hilo del pool.

    public void detener();
    //   executorService.shutdown(); if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
    //     executorService.shutdownNow();
}
```

**Verificar:** `./run.sh` arranca sin `Guice/MissingConstructor` ni errores de bindeo; el log de arranque no muestra excepciones de `CorreosModule`.

## Diseño detallado de referencia (verbatim, `design/rules/R-Correo-001.md` — sección "Diseño detallado", clase `CorreoAsyncExecutor`)

- `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor` — envoltorio de un `java.util.concurrent.ExecutorService` de tamaño fijo, gestionado por Guice como singleton y con ciclo de vida atado a los eventos de arranque/parada de la aplicación (evita fugas de hilos al redesplegar/parar Tomcat, tal y como pide `design-guidelines.md`).
  - `public CorreoAsyncExecutor(int tamanoPool)` — crea `Executors.newFixedThreadPool(tamanoPool, threadFactory)`; `threadFactory` crea hilos **daemon** (red de seguridad: si el hook de parada no llegara a ejecutarse, estos hilos no impiden que la JVM/Tomcat termine) con nombre `correo-envio-N`.
  - `public void submit(Runnable tarea)` — envía la tarea al pool; envuelve la ejecución en un `try/catch (RuntimeException ex)` que solo hace `log.error("Fallo no controlado en el envío asíncrono de un correo", ex)`, para que una excepción no capturada en una tarea no mate el hilo del pool ni oculte el resto de envíos.
  - `public void detener()` — `executorService.shutdown()`; `awaitTermination(10, TimeUnit.SECONDS)`; si no termina a tiempo, `executorService.shutdownNow()`. Se invoca desde `CorreoEventObserver.onAppShutdown`.

### Alternativas consideradas (mecanismo de envío asíncrono) — verbatim, `design/rules/R-Correo-001.md`

`design-guidelines.md` pide explícitamente dedicar tiempo a elegir el mecanismo asíncrono dentro de Tomcat. Se valoraron cuatro opciones:

| Opción | Descartada por |
|---|---|
| **`ExecutorService` gestionado por Guice (elegida)** | — |
| Job de Quartz / `MetaSchedule` (el mismo mecanismo que ya usa `correos.envio.cron`) | El envío es **inmediato** (debe dispararse justo tras crear/reenviar el correo), no periódico; un `Job` de Quartz solo se ejecuta en su propio ciclo (p.ej. cada minuto), lo que introduciría una latencia innecesaria y no encaja con "enviar tras el commit de esta petición concreta". Quartz queda reservado para un futuro reenvío en bloque de los `FAIL` (fuera de alcance). |
| Hilo manual (`new Thread(() -> ...).start()`) por cada envío | Sin gestión de ciclo de vida: no hay límite de concurrencia (un pico de correos crearía un hilo por envío, sin pool ni cola), no hay forma de esperar a que terminen al parar la aplicación (`awaitTermination`) y el manejo de excepciones no capturadas requeriría un `UncaughtExceptionHandler` a mano. Reinventa lo que ya ofrece `ExecutorService` sin ninguna ventaja. |
| `CompletableFuture.runAsync(...)` con el `ForkJoinPool.commonPool()` por defecto | El pool común de `ForkJoinPool` es **compartido con el resto de la JVM** (cualquier otro `CompletableFuture`/`parallelStream()` de la aplicación lo usa también), sin aislamiento de recursos ni cierre ordenado propio: no se puede invocar `shutdown()`/`awaitTermination()` sobre él al parar Tomcat sin afectar a otras partes de la aplicación que también lo usen. Un `ExecutorService` propio y nombrado (`correo-envio-N`) aísla el pool del subsistema y permite un `detener()` explícito y seguro en `CorreoEventObserver.onAppShutdown`. |

## Superficie cerrada

**MUST** crear únicamente la clase `CorreoAsyncExecutor` con exactamente el constructor y los dos métodos públicos listados (`submit`, `detener`). **MUST NOT** añadir métodos, campos públicos ni binding de Guice (el binding va en la Tarea 10, `CorreosModule`/`CorreoAsyncExecutorProvider`). Si detectas que hace falta algo no listado, **detente y reporta** `BLOCKED`.
