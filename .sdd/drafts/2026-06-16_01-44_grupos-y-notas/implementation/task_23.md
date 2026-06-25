---
type: implementation-task
---

# Tarea 23 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase com.educaflow.system.gruposnotas.controller.GrupoController.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno. La sección concreta es **"Clase: `com.educaflow.system.gruposnotas.controller.GrupoController`"** de `design/test-unit-desc.md`.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/controller/GrupoControllerTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita, o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.

> Colaboradores a mockear (de `design/test-unit-desc.md`): `ActionRequest`/`ActionResponse` (mock), `ModelServiceFactory` (mock; `resolve(Grupo.class)` → `GrupoService` mock), `GrupoService` (mock), `ActionRequestHelper`/`ActionResponseHelper` si se usan (mock o estático), `SecurityUtil`/`AuthUtils` si el helper lo necesita. Tests: orquestación de `cerrarGrupo`/`reabrirGrupo` (camino feliz refresca y delega en el servicio; camino con errores de validación responde business messages y NO ejecuta la acción). Nombres `metodo_condicion_resultadoEsperado`.
