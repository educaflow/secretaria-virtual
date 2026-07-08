---
type: implementation-task
---

# Tarea 23 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-guice

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/correos/infrastructure/CorreoEventObserverTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test.

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`).
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- **Campos `@Inject` sin setter ni constructor** (`CorreoEventObserver.correoAsyncExecutor`): se rellenan por reflexión (`Field.setAccessible(true)`) tras instanciar la clase con `new`, ya que esta clase no tiene constructor ni setter para esa dependencia (no hay contenedor Guice en los tests). **Nota:** esta es una técnica nueva para este diseño, no una convención ya establecida en el proyecto.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver`  —  helper

**Responsabilidad:** parar ordenadamente el `CorreoAsyncExecutor` al detener la aplicación.
**Colaboradores a mockear:** `CorreoAsyncExecutor` (campo `@Inject`, se asigna por reflexión — ver Convenciones).
**Origen diseño:** `design.md` Paso 6.

### Método: `void onAppShutdown(ShutdownEvent event)`

- **`onAppShutdown_evento_delegaEnDetenerDelExecutor`** — Tipo: happy. Verifica: `—` (design-guidelines: cierre ordenado sin leaks de hilos).
  - **Arrange:** `new CorreoEventObserver()` con el campo `correoAsyncExecutor` sustituido por reflexión por un mock; un `ShutdownEvent` cualquiera (mock o instancia real si el constructor es accesible).
  - **Act:** `observer.onAppShutdown(event)`.
  - **Assert:** `verify(correoAsyncExecutorMock).detener()`.

### Método: `void onAppStart(StartupEvent event)`

**Sin lógica testable** — el método solo escribe una línea de log informativa (`log.info(...)`), sin ninguna rama ni efecto observable sobre ningún colaborador. **MUST NOT** crear ningún test para `onAppStart`.

**Ninguna otra clase se testea en esta tarea.**
