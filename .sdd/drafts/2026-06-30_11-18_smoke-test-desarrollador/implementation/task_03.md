---
type: implementation-task
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El fichero de vistas ya está materializado en la carpeta `design/`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo: **cópialo verbatim** desde `design/views/SmokeTest.xml` a su ruta destino.

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/views/SmokeTest.xml` | Crear | k-vistas (grids.md, forms.md, actions.md) | Grid, formulario y acciones de SmokeTest |

Ruta destino completa: `src/main/java/com/educaflow/subsystem/smoketest/views/SmokeTest.xml`

---

## Paso 3 — Vistas: SmokeTest.xml

Crear `src/main/java/com/educaflow/subsystem/smoketest/views/SmokeTest.xml`.

El fichero materializado está en `design/views/SmokeTest.xml`. **Resumen estructural:**

- **`action-view` `subsysSmokeTest.SmokeTest@Main-action`** — abre el grid `@Main-grid` y el formulario `@Main-form`. Parámetros: `show-toolbar-form=false`, `forceEdit=true`.

- **Grid `subsysSmokeTest.SmokeTest@Main-grid`** — columnas: `texto`, `fechaCreacion`, `fechaUltimaModificacion`. Ordenación: `-fechaCreacion` (descendente, los más recientes primero, spec). `allowSearchFields="true"` para filtrar por texto. Los campos de fecha llevan `width="200px"` para acotar su columna al tamaño del formato datetime. Sin atributo `archived`.

- **Form `subsysSmokeTest.SmokeTest@Main-form`** — atributos `canAttach/canBack/canDelete/canNew/canSave/canMore` todos `false`; `canBackOnSave="true"`. Contiene:
  - Panel `SmokeTest`: `texto` (colSpan=12, editable), `fechaCreacion` (colSpan=6, `readonly="true"`, U-smoke-test-001), `fechaUltimaModificacion` (colSpan=6, `readonly="true"`, U-smoke-test-001).
  - Panel `buttons-panel`: `btnDelete` (btn-danger, left, `showIf="(id!=null)||(cid!=null)"`), `btnCancel` (outline, colOffset=6), `btnSave`.

- **Action-groups:**
  - `subsysSmokeTest.SmokeTest@Main-btnDelete-action` → `<action name="delete"/>`.
  - `subsysSmokeTest.SmokeTest@Main-btnCancel-action` → `<action name="back"/>`.
  - `subsysSmokeTest.SmokeTest@Main-btnSave-action` → `<action name="subsysSmokeTest.SmokeTest@Main-btnSave-validate-action"/>` + `<action name="save"/>`.

- **Action-validate `subsysSmokeTest.SmokeTest@Main-btnSave-validate-action`** — V-SmokeTest-001 (cliente), Origen spec: RES-001. `<error if="!texto" message="El texto es obligatorio"/>`.

**Verificar:** `xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd src/main/java/com/educaflow/subsystem/smoketest/views/SmokeTest.xml`

---

## Trazabilidad Origen spec → U → ubicación

### U — Reglas de UI

| ID | Origen spec | Ubicación | Descripción |
|----|-------------|-----------|-------------|
| U-smoke-test-001 | RUI-001 | `views/SmokeTest.xml`, `subsysSmokeTest.SmokeTest@Main-form`, campos `fechaCreacion` y `fechaUltimaModificacion` con `readonly="true"` | Las fechas son siempre de solo lectura en el formulario (disparador: continuo, condición: siempre). |
