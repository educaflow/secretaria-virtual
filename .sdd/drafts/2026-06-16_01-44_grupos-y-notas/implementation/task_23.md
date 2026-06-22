---
type: implementation-task
---

# Tarea 23 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md` para la clase `com.educaflow.system.gruposnotas.controller.GrupoController`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks, acción, aserción/mensaje esperado) en la sección «Clase: `com.educaflow.system.gruposnotas.controller.GrupoController`» de `design/test-unit-desc.md`. **MUST NOT** inventar tests que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/controller/GrupoControllerTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests se escriben CONTRA ellas. Si una clase o método que la descripción cita no existe en el código, **detente y reporta** (BLOCKED).

Cubre, según la descripción: `cerrar(ActionRequest, ActionResponse)` y `reabrir(ActionRequest, ActionResponse)` — verifican la **delegación** en `GrupoService.cerrar`/`reabrir` con el bean + original extraídos y el uso del `AllowProperties` correcto (`allowPropertiesCerrar()`/`allowPropertiesReabrir()`, deny-all).

Supuestos de mocking (de `design/test-unit-desc.md`): `ModelServiceFactory` (`resolve(Grupo.class)` → `GrupoService` mock), `ActionRequest`/`ActionResponse` mock, `GrupoService` mock con sus `allowPropertiesCerrar/Reabrir()` y `cerrar/reabrir(...)`. El `ActionRequestHelper` se construye dentro del método (`new ActionRequestHelper(actionRequest, Grupo.class)`); el test programa el `ActionRequest` mock para que el helper resuelva el bean/original, o usa `mockConstruction(ActionRequestHelper.class)`. El objetivo es verificar la delegación y el `AllowProperties` usado, no la mecánica interna del helper.
