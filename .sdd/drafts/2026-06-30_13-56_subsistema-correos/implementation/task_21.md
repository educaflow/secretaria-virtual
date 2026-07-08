---
type: implementation-task
---

# Tarea 21 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-guice

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/correos/infrastructure/CorreoAsyncExecutorTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test.

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`).
- Nombres de test: `metodo_condicion_resultadoEsperado`.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor`  —  helper (infraestructura)

**Responsabilidad:** envoltorio de un `ExecutorService` de tamaño fijo con hilos daemon nombrados, con ejecución de tareas aislada de fallos y parada ordenada.
**Colaboradores a mockear:** ninguno — se usa un `ExecutorService` real (tamaño de pool pequeño, p.ej. 1) para observar el comportamiento real de los hilos.
**Origen diseño:** `design.md` Paso 6, `design/rules/R-Correo-001.md` "Diseño detallado".

### Método: `CorreoAsyncExecutor(int tamanoPool)` + `void submit(Runnable tarea)`

- **`submit_tareaValida_seEjecutaEnUnHiloDaemonConNombreCorreoEnvio`** — Tipo: happy. Verifica: `—` (design-guidelines: evitar leaks de hilos).
  - **Arrange:** `new CorreoAsyncExecutor(1)`; una tarea que captura `Thread.currentThread()` en un `AtomicReference` y libera un `CountDownLatch`.
  - **Act:** `executor.submit(tarea)`; esperar el `latch.await(...)`.
  - **Assert:** el hilo capturado tiene `isDaemon() == true` y `getName()` empieza por `"correo-envio-"`.
- **`submit_tareaLanzaRuntimeException_noPropagaYElPoolSigueUtilizable`** — Tipo: borde. Verifica: `—` (design-guidelines: aislamiento de fallos, "MUST NOT dejar que una excepción no capturada mate el hilo del pool").
  - **Arrange:** `new CorreoAsyncExecutor(1)`; primera tarea que lanza `new RuntimeException("boom")`; segunda tarea (enviada justo después) que libera un `CountDownLatch`.
  - **Act:** `executor.submit(tareaQueFalla); executor.submit(tareaQueMarcaLatch); latch.await(...)`.
  - **Assert:** el `latch` llega a 0 dentro del timeout (el hilo del pool sigue vivo y procesa la segunda tarea pese al fallo de la primera); no se lanza ninguna excepción fuera de `submit`.

### Método: `void detener()`

- **`detener_sinTareasPendientes_terminaYRechazaNuevosEnvios`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `new CorreoAsyncExecutor(1)`.
  - **Act:** `executor.detener()`.
  - **Assert:** una llamada posterior a `executor.submit(...)` lanza `java.util.concurrent.RejectedExecutionException` (el `ExecutorService` interno ya está cerrado).
- **`detener_conTareaEnCurso_esperaSuFinalizacionAntesDeCerrar`** — Tipo: borde. Verifica: `—` ("awaitTermination" antes de forzar el cierre).
  - **Arrange:** `new CorreoAsyncExecutor(1)`; enviar una tarea que tarda unos milisegundos y al terminar marca un `AtomicBoolean completada = true`.
  - **Act:** `executor.detener()` (se invoca justo tras el `submit`, sin esperar aparte).
  - **Assert:** `completada` es `true` tras `detener()` (la tarea ya en curso se deja terminar dentro del margen de `awaitTermination`, no se aborta con `shutdownNow` de inmediato).

**Ninguna otra clase se testea en esta tarea.**
