---
type: implementation-task
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Fusiona los menús del sistema en el fichero único de menús del proyecto.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir/fusionar los menús del sistema (ya presentes — fusión idempotente) |

Los `<menuitem>` a fusionar **ya están materializados** en `design/menus.xml`. **MUST** fusionarlos (no copiar el fichero entero, sino integrar los `<menuitem>`) en el fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. La fusión es **idempotente**: si los `<menuitem>` ya están presentes, **MUST NOT** duplicarlos.

Del diseño, Paso 6 — Menús. Modificar el fichero único fusionando los `<menuitem>` de `design/menus.xml`:

- `notas-menuitem` (raíz "Notas", `if` SUPERVISOR) + `notas-grupos-menuitem` (hoja "Grupos" → `sysGruposNotas.Grupo@Supervisor-action`).
- `misNotas-menuitem` (raíz/hoja "Mis notas" → `sysGruposNotas.AlumnoGrupo@MisNotas-action`, `if` ALUMNO).
- `administracionSv-grupos-menuitem` (hoja de `administracionSv-menuitem`, "Grupos (administración)" → `sysGruposNotas.Grupo@Administracion-action`, `groups="admins"`).

> Estas tres líneas **ya están presentes** en el menús del proyecto (verificado): la fusión es **idempotente** (no se duplica nada).
