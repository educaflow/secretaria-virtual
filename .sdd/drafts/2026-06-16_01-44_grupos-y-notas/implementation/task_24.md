---
type: implementation-task
---

# Tarea 24 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md` para la clase `com.educaflow.system.gruposnotas.controller.NotaController`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks, acción, aserción/mensaje esperado) en la sección «Clase: `com.educaflow.system.gruposnotas.controller.NotaController`» de `design/test-unit-desc.md`. **MUST NOT** inventar tests que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/controller/NotaControllerTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests se escriben CONTRA ellas. Si una clase o método que la descripción cita no existe en el código, **detente y reporta** (BLOCKED).

Cubre, según la descripción: `guardarNota(ActionRequest, ActionResponse)` — verifica la **delegación** en `NotaService.guardarNota` con el bean (whitelist `valor`) + original extraídos y el uso de `allowPropertiesGuardarNota()`.

Supuestos de mocking (de `design/test-unit-desc.md`): `ModelServiceFactory` (`resolve(Nota.class)` → `NotaService` mock), `ActionRequest`/`ActionResponse` mock, `NotaService` mock con `allowPropertiesGuardarNota()` y `guardarNota(...)`. El `ActionRequestHelper` se trata igual que en `GrupoControllerTest`. El objetivo es verificar la delegación y el `AllowProperties` usado, no la mecánica interna del helper.
