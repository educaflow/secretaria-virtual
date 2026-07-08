---
type: implementation-task
---

# Tarea 24 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-guice

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.correos.module.MailSenderProvider`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/correos/module/MailSenderProviderTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test.

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.subsystem.correos.module.MailSenderProvider`  —  helper (Guice `Provider`)

**Responsabilidad:** construir el `MailSender` real leyendo las credenciales SMTP de `AppSettings`.
**Colaboradores a mockear:** estático `com.axelor.app.AppSettings` (`get()` devuelve una instancia mockeada de `AppSettings` sobre la que se stubean `get(String)`).
**Origen diseño:** `design.md` Paso 6.

### Método: `MailSender get()`

- **`get_leeCredencialesDeAppSettingsYDevuelveMailSenderImpl`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `Mockito.mockStatic(AppSettings.class)`; `AppSettings settingsMock = Mockito.mock(AppSettings.class)`; `AppSettings.get()` → `settingsMock`; `settingsMock.get("mail.smtp.host")` → `"smtp.test.com"`; `settingsMock.get("mail.smtp.user")` → `"user@test.com"`; `settingsMock.get("mail.smtp.password")` → `"secret"`.
  - **Act:** `MailSender result = provider.get()`.
  - **Assert:** `result` es instancia de `com.educaflow.base.infrastructure.mail.impl.MailSenderImplSmtp`; `verify(settingsMock).get("mail.smtp.host")`, `verify(settingsMock).get("mail.smtp.user")`, `verify(settingsMock).get("mail.smtp.password")` (las tres claves exactas se leen).

**Ninguna otra clase se testea en esta tarea.**
