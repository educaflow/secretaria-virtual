---
type: implementation-task
---

# Tarea 25 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-guice

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.correos.module.CorreoAsyncExecutorProvider`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProviderTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test.

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.subsystem.correos.module.CorreoAsyncExecutorProvider`  —  helper (Guice `Provider`)

**Responsabilidad:** construir el `CorreoAsyncExecutor` leyendo el tamaño de pool configurado (o su valor por defecto).
**Colaboradores a mockear:** estático `com.axelor.app.AppSettings`.
**Origen diseño:** `design.md` Paso 6.

### Método: `CorreoAsyncExecutor get()`

- **`get_conPropiedadConfigurada_usaElTamanoIndicado`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `Mockito.mockStatic(AppSettings.class)`; `AppSettings settingsMock = Mockito.mock(AppSettings.class)`; `AppSettings.get()` → `settingsMock`; `settingsMock.getInt("mail.send.pool-size", 2)` → `4`.
  - **Act:** `CorreoAsyncExecutor result = provider.get()`.
  - **Assert:** `result` no es `null` (instancia de `CorreoAsyncExecutor`); `verify(settingsMock).getInt("mail.send.pool-size", 2)` (se pasa el valor por defecto correcto junto con la clave).
- **`get_sinPropiedadConfigurada_usaDosComoValorPorDefecto`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** igual, pero `settingsMock.getInt("mail.send.pool-size", 2)` → `2` (simula que `AppSettings` real aplicaría el valor por defecto pasado si la propiedad no existe).
  - **Act:** `provider.get()`.
  - **Assert:** `result` no es `null`; se confirma que el `Provider` invoca `getInt` con el literal `2` como valor por defecto (no otro número), que es lo único verificable desde fuera sin un getter de tamaño de pool en `CorreoAsyncExecutor`.

**Ninguna otra clase se testea en esta tarea.**
