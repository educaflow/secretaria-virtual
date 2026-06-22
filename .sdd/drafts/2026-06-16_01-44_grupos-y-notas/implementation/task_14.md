---
type: implementation-task
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la pantalla "Grupos" del supervisor.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/views/Grupo-Supervisor.xml` | Crear | k-vistas | Pantalla "Grupos" (supervisor) |

El XML completo y ya validado con `xmllint` está en `design/views/Grupo-Supervisor.xml`. **MUST** copiarlo **literalmente** a `src/main/java/com/educaflow/system/gruposnotas/views/Grupo-Supervisor.xml`, **sin regenerarlo** ni reescribirlo (ver `implementation.md` §1). Es la fuente de verdad del diseño.

### Descripción del diseño (Paso 5 — Vistas)

Un `<action-view>` por fichero (k-sistemas/k-vistas). Prefijo `sysGruposNotas`.

- **`Grupo-Supervisor.xml`** — `action-view sysGruposNotas.Grupo@Supervisor-action` (pantalla "Grupos" del supervisor). Lleva `<domain>self.centro = :centroActivoUsuario</domain>` + `<context>` con `__user__?.centroActivo?.id` (multi-centro, k-secure-coding §4). Contiene: grid de grupos (columnas curso académico, ciclo, nombre, estado); form de grupo con paneles "Datos del grupo", "Módulos" (panel-related → ModuloGrupo), "Alumnos" (panel-related → AlumnoGrupo); botones "Cerrar grupo" (showIf ABIERTO → action-method `cerrar`), "Guardar", "Cancelar"; grids/forms anidados ModuloGrupo → Nota (botón "Guardar" → action-method `guardarNota`) y AlumnoGrupo (selector de alumno con `domain` filtrado por centro y tipo ALUMNO). `onNew` fija estado ABIERTO y, además, `centro = __user__?.centroActivo` y `cursoAcademico = __user__?.centroActivo?.curso` (RUI-001/RUI-002: rellenos y readonly en el alta; UX, el servidor los fija incondicionalmente vía R-Grupo-002, y además alimenta el selector de alumno del panel Alumnos vía `__parent__?.centro`); en AlumnoGrupo, el `onNew` fija el grupo padre y el centro auxiliar.

### Reglas de UI (U) materializadas en esta vista

| U | Origen spec | Ubicación |
|---|---|---|
| U-grupos-supervisor-001 | RUI-001 | Grupo-Supervisor.xml: campo `centro` readonly + onNew `set-centro-curso-academico-action` (`centro = eval: __user__?.centroActivo`) que lo rellena en el alta (UX; el servidor lo fija incondicionalmente, R-Grupo-002) |
| U-grupos-supervisor-002 | RUI-002 | Grupo-Supervisor.xml: campo `cursoAcademico` readonly + onNew `set-centro-curso-academico-action` (`cursoAcademico = eval: __user__?.centroActivo?.curso`) que lo rellena en el alta (UX; el servidor lo fija incondicionalmente, R-Grupo-002) |
| U-grupos-supervisor-003 | RUI-003 | Grupo-Supervisor.xml: botón "Cerrar grupo" `showIf="estado == 'ABIERTO' && id != null"` (y ausencia de "Reabrir grupo") |
| U-grupos-supervisor-004 | RUI-004 | Grupo-Supervisor.xml: panel "Datos del grupo" y panel "Alumnos" `readonlyIf="estado == 'CERRADO'"`; valor de nota `readonlyIf` grupo CERRADO |
| U-grupos-supervisor-005 | — | Grupo-Supervisor.xml: campo `alumno` con `domain` (alumnos del centro del grupo, tipo ALUMNO). U añadida por el diseño (filtro del selector); no proviene de ninguna RUI. Su respaldo de servidor es VAL-012 (V-AlumnoGrupo-003), que materializa ESC-020 |
| U-grupos-supervisor-006 | RUI-005 | Grupo-Supervisor.xml: campo `valor` de la nota `readonlyIf="moduloGrupo.grupo.estado == 'CERRADO'"` |

**Verificar:** `validate.sh` imprime `VALIDACION-XML: OK`.
