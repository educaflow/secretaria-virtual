---
type: implementation-task
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la vista "Mis notas" del alumno.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/views/AlumnoGrupo-MisNotas.xml` | Crear | k-vistas | `sysGruposNotas.AlumnoGrupo@MisNotas-action` (árbol del alumno) |

Este fichero **ya está materializado** en `design/views/AlumnoGrupo-MisNotas.xml`. **MUST** copiarlo **literalmente** (verbatim) a su ubicación destino `src/main/java/com/educaflow/system/gruposnotas/views/AlumnoGrupo-MisNotas.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Del diseño, Paso 5 — Vistas:

- **`views/AlumnoGrupo-MisNotas.xml`** — `sysGruposNotas.AlumnoGrupo@MisNotas-action` (grid + form de AlumnoGrupo) con `<domain>self.alumno = :__user__</domain>` (ESC-026). Solo lectura: lista de mis grupos (curso académico, ciclo, nombre, nota media) → form "Mi grupo" con panel "Mis notas" (`Nota@MisNotas` grid/form, módulo, valor, fecha de calificación), todo readonly.

**Verificar:** `validate.sh` → `VALIDACION-XML: OK`; cada `<action-view>` en su propio fichero.
