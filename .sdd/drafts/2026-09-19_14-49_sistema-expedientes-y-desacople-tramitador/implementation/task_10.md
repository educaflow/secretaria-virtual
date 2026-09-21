---
type: implementation-task
template: system
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- (ninguno: tarea solo de eliminación de ficheros, sin código que escribir)

**Alcance de esta tarea:** solo **eliminación de dos ficheros** (filas `Borrar`): la clase `ContextoTramitacionService` y su test `ContextoTramitacionServiceTest`. Su lógica ya quedó repartida entre `ExpedienteService` (tarea 07) y `NuevoExpedienteServiceImpl` (tarea 09). La hace el implementador directamente con `rm`/`git rm` (no hay código que escribir, no se delega en `developer-code-implementer`). **MUST NOT** borrar ni tocar nada más. Los llamadores que aún queden los retiran las tareas 11-12; **no** verifiques compilación aquí.

**Del diseño — filas de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/services/ContextoTramitacionService.java` | Borrar | — | Su lógica se reparte entre la puerta del motor y el servicio del sistema (D2) (Paso 5) |
| `src/test/java/com/educaflow/subsystem/expedientes/services/ContextoTramitacionServiceTest.java` | Borrar | — | Prueba una clase que desaparece; la sustituyen los tests de `test-unit-desc.md` (Paso 5) |

**Del diseño — Paso 5, borrado (verbatim)**

**Borrar** `subsystem/expedientes/services/ContextoTramitacionService.java` (su lógica queda repartida entre `ExpedienteService` y este servicio, D2) y su test `src/test/java/com/educaflow/subsystem/expedientes/services/ContextoTramitacionServiceTest.java`: sus casos de centros ordenados pasan a los tests de `NuevoExpedienteServiceImpl`, y los de papel deducido y opciones de «para quién» a los de `ExpedienteService`, `FormaPresentacion`, `ContextoTramitacion` y `NuevoExpedienteServiceImpl` que describe `test-unit-desc.md`.

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Clase completa | `subsystem/expedientes/services/ContextoTramitacionService.java` | Guías («de él salen … ContextoTramitacionService») |
| Test completo | `src/test/java/com/educaflow/subsystem/expedientes/services/ContextoTramitacionServiceTest.java` | La clase probada desaparece (Paso 5) |
