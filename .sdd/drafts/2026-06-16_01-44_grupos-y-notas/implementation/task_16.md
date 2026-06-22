---
type: implementation-task
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la pantalla "Mis notas" del alumno.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/views/Grupo-MisNotas.xml` | Crear | k-vistas | Pantalla "Mis notas" (alumno) |

El XML completo y ya validado con `xmllint` está en `design/views/Grupo-MisNotas.xml`. **MUST** copiarlo **literalmente** a `src/main/java/com/educaflow/system/gruposnotas/views/Grupo-MisNotas.xml`, **sin regenerarlo** ni reescribirlo (ver `implementation.md` §1). Es la fuente de verdad del diseño.

### Descripción del diseño (Paso 5 — Vistas)

- **`Grupo-MisNotas.xml`** — `action-view sysGruposNotas.AlumnoGrupo@MisNotas-action` (pantalla "Mis notas" del alumno). Lleva `<domain>self.alumno = :__user__</domain>` (el alumno solo ve sus pertenencias). Grid de mis grupos (curso académico, ciclo, nombre, nota media); form "Mi grupo" (solo lectura) con panel "Mis notas" (panel-related → Nota, solo lectura); grid/form de mi nota (módulo, valor, fechas), todo readonly.

### Reglas de UI (U) materializadas en esta vista

| U | Origen spec | Ubicación |
|---|---|---|
| U-mis-notas-alumno | (acceso de rol; sin RUI propia) | Grupo-MisNotas.xml: action-view con `<domain>self.alumno = :__user__</domain>` y todos los paneles readonly |

> La pantalla "Mis notas" no define RUI; su comportamiento de solo-su-grupo/solo-lectura es alcance de rol (Seguridad), materializado con el `<domain>` y los `readonly`. La columna "nota media" del grid se resuelve por la propiedad transient `notaMedia` de `AlumnoGrupo` (CC-001).

**Verificar:** `validate.sh` imprime `VALIDACION-XML: OK`.
