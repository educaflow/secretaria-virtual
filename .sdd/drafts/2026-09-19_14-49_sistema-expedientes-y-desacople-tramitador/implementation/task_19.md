---
type: implementation-task
template: system
---

# Tarea 19 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

**Alcance de esta tarea:** fila `Modificar`. El fichero **ya existe**: **MUST** editarlo aplicando **solo** el delta del Paso 9 (en los grupos `admins` y `users`, `<permission name="ContextoTramitacion.all"/>` → `<permission name="NuevoExpediente.all"/>`) y **conservando** todo lo demás. Según `implementation.md` §3 los datos iniciales/seguridad son tarea «de Java»: carga `k-datainit` y delega en `developer-code-implementer` (`implementation.md` §2), con la **superficie cerrada** de ese delta.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | En los grupos `admins` y `users`, `ContextoTramitacion.all` → `NuevoExpediente.all` (Paso 9) |

**Del diseño — Paso 9 (verbatim: extracto de este fichero)**

### Paso 9 — Seguridad

**Modificar** `src/main/resources/data-init/input/auth.xml`: en los grupos `admins` y `users`, sustituir `<permission name="ContextoTramitacion.all"/>` por `<permission name="NuevoExpediente.all"/>`. Nada más. El data-init global (priority `-1`) carga después del del sistema (priority `10`), así que el permiso ya existe cuando el grupo lo referencia.

`TipoExpediente.all` (grupo `admins`) se deja como está: el grupo `admins` se salta los permisos (`AuthUtils.isAdmin`), así que cambiarlo no protegería nada; la protección es la del Paso 4.

Verificación: arrancar con `./run.sh` y comprobar en el log que el data-init no falla; `grep -rn "ContextoTramitacion.all" src` sin resultados.

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Referencias `ContextoTramitacion.all` en los grupos `admins` y `users` (sustituidas por `NuevoExpediente.all`) | `src/main/resources/data-init/input/auth.xml` | El modelo de la ventana pasa a ser `NuevoExpediente` |

**Del diseño — Notas y supuestos aplicables (verbatim)**

14. **Fila antigua de permiso:** el data-init hace upsert y no borra, así que la fila `ContextoTramitacion.all` de la tabla de permisos queda huérfana en las bases de datos existentes (sin efecto: su objeto ya no existe). Borrarla es opcional y no forma parte del diseño.
