---
type: implementation-task
template: system
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

**Alcance de esta tarea:** fila `Modificar`. El fichero **ya existe**: **MUST** editarlo aplicando **solo** el delta del Paso 9 (borrar el `<permission name="ContextoTramitacion.all">` y su comentario; sustituir la última frase del comentario de `Tramite.registrador`) y **conservando** todo lo demás. Según `implementation.md` §3 los datos iniciales/seguridad son tarea «de Java»: carga `k-datainit` y delega en `developer-code-implementer` (`implementation.md` §2), con la **superficie cerrada** de ese delta.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/data-init/input/auth-expedientes.xml` | Modificar | k-datainit | Se borra el permiso `ContextoTramitacion.all` y se actualiza el comentario de `Tramite.registrador` (referencia obsoleta a `ExpedienteSecurity` → `ExpedienteService.getPerfilesDeInicio` / `validateTriggerInitialEvent`) (Paso 9) |

**Del diseño — Paso 9 (verbatim: extracto de este fichero)**

### Paso 9 — Seguridad

**Modificar** `src/main/java/com/educaflow/subsystem/expedientes/data-init/input/auth-expedientes.xml`: borrar el `<permission name="ContextoTramitacion.all">` y su comentario (ya no existe el modelo).
Además, en el comentario del permiso `Tramite.registrador`, sustituir la última frase («Qué centros y qué opciones se le ofrecen al crear lo decide ExpedienteSecurity, que es la misma regla.», clase que ya no existe) por: «Qué centros y qué opciones se le ofrecen al crear lo decide la puerta del motor, ExpedienteService.getPerfilesDeInicio / validateTriggerInitialEvent, que es la misma regla.».
El resto del fichero no cambia.

Verificación: arrancar con `./run.sh` y comprobar en el log que el data-init no falla; `grep -rn "ContextoTramitacion.all" src` sin resultados.

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Permiso `ContextoTramitacion.all` | `subsystem/expedientes/data-init/input/auth-expedientes.xml` | El modelo deja de existir (D1) |
