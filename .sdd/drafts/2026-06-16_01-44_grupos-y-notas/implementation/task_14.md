---
type: implementation-task
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la vista de Grupo para la administración (administrador, todos los centros).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/views/Grupo-Administracion.xml` | Crear | k-vistas | `sysGruposNotas.Grupo@Administracion-action` (árbol completo) |

Este fichero **ya está materializado** en `design/views/Grupo-Administracion.xml`. **MUST** copiarlo **literalmente** (verbatim) a su ubicación destino `src/main/java/com/educaflow/system/gruposnotas/views/Grupo-Administracion.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Del diseño, Paso 5 — Vistas:

- **`views/Grupo-Administracion.xml`** — `sysGruposNotas.Grupo@Administracion-action` sin domain de centro (todos los centros), columna `centro` en el grid. Form con `centro` y `cursoAcademico` editables solo en alta (RUI-006). Botones añaden "Reabrir grupo" (showIf estado=='CERRADO' → `Remote-reabrirGrupo`, RUI-008) además de "Cerrar grupo" (RUI-007). Form readonly si CERRADO salvo botones (RUI-009). `valor` readonly si grupo CERRADO (RUI-010). Mismo árbol de paneles con discriminador `@Administracion`.

**Verificar:** `validate.sh` → `VALIDACION-XML: OK`; cada `<action-view>` en su propio fichero.

### Reglas de UI (U) que materializa esta vista
| U | Origen spec | Ubicación |
|---|-------------|-----------|
| U-grupos-administrador-001 | RUI-006 | `views/Grupo-Administracion.xml` `centro`/`cursoAcademico` `readonlyIf="id != null"` (editables solo en alta) |
| U-grupos-administrador-002 | RUI-007 | `views/Grupo-Administracion.xml` botón "Cerrar grupo" `showIf="estado == 'ABIERTO'"` |
| U-grupos-administrador-003 | RUI-008 | `views/Grupo-Administracion.xml` botón "Reabrir grupo" `showIf="estado == 'CERRADO'"` |
| U-grupos-administrador-004 | RUI-009 | `views/Grupo-Administracion.xml` panel "Datos del grupo" `readonlyIf="estado == 'CERRADO'"` (botón Reabrir aparte) |
| U-grupos-administrador-005 | RUI-010 | `views/Grupo-Administracion.xml` `Nota` form `valor` `readonlyIf="moduloGrupo.grupo.estado == 'CERRADO'"` |
| U-grupos-administrador-006 | RUI-012 | `views/Grupo-Administracion.xml` `AlumnoGrupo` `onNew` → `set-grupo-parent-action` (`grupo` = `__parent__`) |
