---
type: implementation-task
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase com.educaflow.subsystem.smoketest.controller.SmokeTestController.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/smoketest/controller/SmokeTestControllerTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita (p.ej. la descripción dice
      `insert(X)` y el código tiene `guardarX(X, Long)`), o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.

La sección concreta de `design/test-unit-desc.md` que describe esta clase es:
**"Clase: `com.educaflow.subsystem.smoketest.controller.SmokeTestController` — controlador"**, con sus colaboradores a mockear (`ModelServiceFactory` inyectado por reflexión, `SmokeTestService`, `ActionRequest`/`ActionResponse`, `ActionRequestHelper`/`ActionResponseHelper` con `Mockito.mockConstruction`) y los cuatro tests del método `validateSave` (`validateSave_altaTextoValido_noRespondeError`, `validateSave_altaTextoVacio_respondeBusinessMessagesError`, `validateSave_modificacionTextoValido_noRespondeError`, `validateSave_modificacionTextoVacio_respondeBusinessMessagesError`). Implementa todos los tests allí listados.
