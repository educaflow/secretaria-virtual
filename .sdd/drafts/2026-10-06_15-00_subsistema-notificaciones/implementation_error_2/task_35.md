---
type: implementation-task
template: system
---

# Tarea 35 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

Nota del descomponedor (decisión documentada): este XML de datos iniciales **no** está materializado en `design/` (solo dominios, vistas y menús lo están); se escribe a partir de la descripción del diseño siguiendo **exactamente** el formato de `k-datainit`, sin añadir nada que el diseño no declare.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/data-init/input-config.xml` | Crear | k-datainit | Binding de los permisos del subsistema |

### Paso 13 — Seguridad (permisos)

`subsystem/notificaciones/data-init/input-config.xml`: `<xml-inputs priority="10">` con un único `<input file="auth-notificaciones.xml" root="auth">` y el `bind` de `permission` de `correos/data-init/input-config.xml` (name, object, condition, conditionParams, can/*). Sin binding de grupos.

Verificación: reset de BD y arranque; el log de importación de data-init sin errores.
