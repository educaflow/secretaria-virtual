---
type: implementation-task
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la vista de Grupo para el supervisor.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/views/Grupo-Supervisor.xml` | Crear | k-vistas | `sysGruposNotas.Grupo@Supervisor-action` (árbol completo) |

Este fichero **ya está materializado** en `design/views/Grupo-Supervisor.xml`. **MUST** copiarlo **literalmente** (verbatim) a su ubicación destino `src/main/java/com/educaflow/system/gruposnotas/views/Grupo-Supervisor.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Del diseño, Paso 5 — Vistas:

- **`views/Grupo-Supervisor.xml`** — `sysGruposNotas.Grupo@Supervisor-action` (grid + form de Grupo) con `<domain>self.centro = :centroActivoUsuario</domain>` (ESC-021). Árbol maestro-detalle: Grupo → panel "Módulos" (`ModuloGrupo@Supervisor` grid/form) → panel "Notas" (`Nota@Supervisor` grid/form), y Grupo → panel "Alumnos" (`AlumnoGrupo@Supervisor` grid/form). Botones: Borrar, "Cerrar grupo" (showIf estado=='ABIERTO' → `Remote-cerrarGrupo`), Cancelar, Guardar. `onNew` rellena centro/cursoAcademico del centro activo (RUI-001/002) y readonly. Form en readonly si CERRADO (RUI-004). El selector de alumno filtra alumnos del centro del supervisor (RUI-011 + UX de VAL-012/ESC-020). Nota: `valor` readonly si grupo CERRADO (RUI-005).

**Verificar:** `validate.sh` → `VALIDACION-XML: OK`; cada `<action-view>` en su propio fichero.

### Reglas de UI (U) que materializa esta vista
| U | Origen spec | Ubicación |
|---|-------------|-----------|
| U-grupos-supervisor-001 | RUI-001 | `views/Grupo-Supervisor.xml` `onNew` → `set-defaults-action` (centro = centro activo) + `centro` readonly |
| U-grupos-supervisor-002 | RUI-002 | `views/Grupo-Supervisor.xml` `onNew` → `set-defaults-action` (cursoAcademico = centro activo) + `cursoAcademico` readonly |
| U-grupos-supervisor-003 | RUI-003 | `views/Grupo-Supervisor.xml` botón "Cerrar grupo" `showIf="estado == 'ABIERTO'"` (y ausencia de "Reabrir grupo" — ESC-011) |
| U-grupos-supervisor-004 | RUI-004 | `views/Grupo-Supervisor.xml` panel "Datos del grupo" `readonlyIf="estado == 'CERRADO'"` (y paneles related) |
| U-grupos-supervisor-005 | RUI-005 | `views/Grupo-Supervisor.xml` `Nota` form `valor` `readonlyIf="moduloGrupo.grupo.estado == 'CERRADO'"` |
| U-grupos-supervisor-006 | RUI-011 | `views/Grupo-Supervisor.xml` `AlumnoGrupo` `onNew` → `set-grupo-parent-action` (`grupo` = `__parent__`) |
