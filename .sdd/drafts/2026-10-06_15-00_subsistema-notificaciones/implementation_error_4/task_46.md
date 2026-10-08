---
type: implementation-task
template: system
---

# Tarea 46 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/data-init/input-config.xml` | Crear | k-datainit | Binding de los permisos del subsistema |

## Diseño — Paso 13 (extracto)

### Paso 13 — Seguridad (permisos)

`subsystem/notificaciones/data-init/input-config.xml`: `<xml-inputs priority="10">` con un único `<input file="auth-notificaciones.xml" root="auth">` y el `bind` de `permission` de `correos/data-init/input-config.xml` (name, object, condition, conditionParams, can/*). Sin binding de grupos.

**Nota del descomponedor:** `subsystem/correos/data-init/input-config.xml` se borra en la tarea 01; si ya no existe, toma el `bind` de `permission` de su versión en git (`git show HEAD:src/main/java/com/educaflow/subsystem/correos/data-init/input-config.xml`) o del formato de `/k-datainit`.
