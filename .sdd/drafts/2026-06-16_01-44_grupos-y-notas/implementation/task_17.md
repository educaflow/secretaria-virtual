---
type: implementation-task
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Añade los menús de Grupos y notas al fichero global de menús.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menús "Notas → Grupos", "Mis notas" y "Administración → Grupos (administración)" |

La porción de `<menuitem>` a añadir está materializada en `design/menus.xml`. **MUST** **fusionarla** dentro del fichero existente `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (no crear un fichero nuevo, no regenerar el existente), preservando los `<menuitem>` ya presentes, y validar el resultado (ver `implementation.md` §1, fusión de `menus.xml`).

### Descripción del diseño (Paso 8 — Menús)

Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` añadiendo la porción de `design/menus.xml`:

- `notas-menuitem` "Notas" (order 35, `if` tipo activo SUPERVISOR) → hijo "Grupos" → `sysGruposNotas.Grupo@Supervisor-action`.
- `misNotas-menuitem` "Mis notas" (order 36, `if` tipo activo ALUMNO) → `sysGruposNotas.AlumnoGrupo@MisNotas-action`.
- `administracionSv-grupos-menuitem` "Grupos (administración)" bajo `administracionSv-menuitem` (groups="admins", order 20) → `sysGruposNotas.Grupo@Administracion-action`.

La visibilidad de menú es solo conveniencia: la frontera real la imponen el `<domain>` de las action-view, los `allowProperties` y las validaciones de servidor.
