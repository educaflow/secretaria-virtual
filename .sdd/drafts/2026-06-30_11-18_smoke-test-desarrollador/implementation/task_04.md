---
type: implementation-task
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Esta tarea materializa la vista `SmokeTest.xml`. **El XML ya está materializado** en `design/views/SmokeTest.xml`: **MUST** copiarlo **literalmente** (verbatim) a su ubicación final `src/main/java/com/educaflow/subsystem/smoketest/views/SmokeTest.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/views/SmokeTest.xml` | Crear | k-vistas (grids.md, forms.md, actions.md) | `<action-view>` Main: grid + form CRUD |

> Raíz de los ficheros del subsistema: `src/main/java/com/educaflow/subsystem/smoketest/`.

### Paso 6 — Vistas `SmokeTest.xml`

Fichero materializado: `design/views/SmokeTest.xml` → copiar a `subsystem/smoketest/views/SmokeTest.xml`. Un único `<action-view>` (`@Main`) → un fichero (regla "un action-view por fichero").

Resumen estructural:
- `action-view subsysSmokeTest.SmokeTest@Main-action` (title "Smoke test", model `SmokeTest`): grid `@Main-grid` + form `@Main-form`; `view-param show-toolbar-form=false`, `forceEdit=true`.
- `grid @Main-grid` (`groups="admins"`): columnas `texto`, `fechaCreacion`, `fechaUltimaModificacion`; `orderBy="-fechaCreacion"` (más recientes primero; **U-smoke-test-002**, Ordenación por defecto del spec); `allowSearchFields="true"` (búsqueda por texto); `canNew="true"` ("Nuevo"); `canDelete="true"` ("Eliminar" por fila/selección); `canEditOnClick="true"` (abre el form al pulsar fila).
- `form @Main-form` (`groups="admins"`): panel "Smoke test" con `texto` editable y `fechaCreacion`/`fechaUltimaModificacion` con `readonly="true"` (**U-smoke-test-001 / RUI-001**). Botón "Guardar" → `@Main-btnSave-action`; `canBackOnSave="true"` (vuelve al listado tras guardar).
- Acciones: `action-group @Main-btnSave-action` = `[@Main-Remote-validateSave-action, save]`; `action-method @Main-Remote-validateSave-action` → `SmokeTestController.validateSave`.

> Las fechas en `readonly` son sólo UX (RUI-001). La **defensa** de que el cliente no las dicte es la whitelist + asignación incondicional del Paso 2 (k-secure-coding §1).

Verificar: el menú abre el grid; "Nuevo" abre el form; guardar con texto sella las fechas; borrar desde la fila elimina. `./run.sh` arranca sin errores de vista.

### Reglas de UI (U) que materializa esta vista

| U | Origen spec | Ubicación | Atributo |
|---|-------------|-----------|----------|
| U-smoke-test-001 | RUI-001 | `views/SmokeTest.xml` form `@Main-form`, campos `fechaCreacion` y `fechaUltimaModificacion` | `readonly="true"` (disparador continuo, condición Siempre). |
| U-smoke-test-002 | Ordenación por defecto (screen-smoke-test.md) | `views/SmokeTest.xml` grid `@Main-grid` | `orderBy="-fechaCreacion"` (más recientes primero). |
