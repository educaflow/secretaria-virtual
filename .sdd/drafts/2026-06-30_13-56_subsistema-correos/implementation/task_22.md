---
type: implementation-task
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunnerTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test.

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`). Estáticos del stack con `Mockito.mockStatic` (para mocks estáticos puntuales de un solo test, try-with-resources local).
- Nombres de test: `metodo_condicion_resultadoEsperado`.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner`  —  helper (utilidad estática)

**Responsabilidad:** ejecutar una tarea solo si la transacción JPA actual termina en commit, nunca en rollback.
**Colaboradores a mockear:** estáticos `com.axelor.db.JPA` (`em()`); mocks de `jakarta.persistence.EntityManager`, `org.hibernate.Session` (resultado de `unwrap(Session.class)`) y `org.hibernate.Transaction`.
**Origen diseño:** `design/rules/R-Correo-001.md` "Diseño detallado".

### Método: `static void runAfterCommit(Runnable tarea)`

- **`runAfterCommit_transaccionHaceCommit_ejecutaLaTarea`** — Tipo: happy. Verifica: `—` (soporte de R-Correo-001/R-Correo-002).
  - **Arrange:** `Mockito.mockStatic(JPA.class)`; `JPA.em()` → `EntityManager` mock; `entityManagerMock.unwrap(Session.class)` → `Session` mock; `sessionMock.getTransaction()` → `Transaction` mock; capturar con `ArgumentCaptor<Synchronization>` el argumento de `transactionMock.registerSynchronization(...)`; una `Runnable tarea` mock (o un `AtomicBoolean` que la lambda marca a `true`).
  - **Act:** `PostCommitRunner.runAfterCommit(tarea)`; después, invocar manualmente `synchronizationCapturado.afterCompletion(jakarta.transaction.Status.STATUS_COMMITTED)`.
  - **Assert:** `verify(tarea).run()` (o el `AtomicBoolean` es `true`).
- **`runAfterCommit_transaccionHaceRollback_noEjecutaLaTarea`** — Tipo: error/borde. Verifica: `—` ("si la transacción hace rollback, la tarea no se ejecuta").
  - **Arrange:** igual que el anterior.
  - **Act:** `PostCommitRunner.runAfterCommit(tarea)`; invocar `synchronizationCapturado.afterCompletion(jakarta.transaction.Status.STATUS_ROLLEDBACK)`.
  - **Assert:** `verify(tarea, never()).run()`.

**Ninguna otra clase se testea en esta tarea.**
