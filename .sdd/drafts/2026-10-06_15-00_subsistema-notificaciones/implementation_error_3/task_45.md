---
type: implementation-task
template: system
---

# Tarea 45 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

## Data-init: input-config.xml de notificaciones

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/data-init/input-config.xml` | Crear | k-datainit | Binding de los permisos del subsistema |

### Paso 13 (extracto verbatim del design.md: `input-config.xml`)

### Paso 13 — Seguridad (permisos)

`subsystem/notificaciones/data-init/input-config.xml`: `<xml-inputs priority="10">` con un único `<input file="auth-notificaciones.xml" root="auth">` y el `bind` de `permission` de `correos/data-init/input-config.xml` (name, object, condition, conditionParams, can/*). Sin binding de grupos.

Verificación: reset de BD y arranque; el log de importación de data-init sin errores.

**Nota de la descomposición:** el `bind` de `permission` se toma de `correos/data-init/input-config.xml` del historial de git (`git show HEAD:src/main/java/com/educaflow/subsystem/correos/data-init/input-config.xml`), porque la tarea de eliminación de `subsystem/correos/**` lo borra antes.
