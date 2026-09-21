---
type: implementation-task
template: system
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

**Alcance de esta tarea:** fila `Modificar`. El fichero **ya existe** en `src/main/java/com/educaflow/tramites/views/Tramites.xml`. El fichero **ya está materializado y validado** en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design/views/Tramites.xml`. **MUST** copiarlo **literalmente** (`cp`) a `src/main/java/com/educaflow/tramites/views/Tramites.xml` **sin regenerarlo, reformatearlo ni editarlo** (`implementation.md` §1). Como el destino **ya existe**, antes de sobrescribir **MUST** aplicar la **comprobación de conservación** de `implementation.md` §3: todo elemento con nombre del fichero real actual (árbol, nodos, `cards`, acciones) debe estar presente en el XML del diseño; el único cambio permitido es el delta declarado abajo (no hay `## Eliminaciones declaradas` para este fichero). Si falla → `CONFLICT`. **MUST NOT** fusionar a mano.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/views/Tramites.xml` | Modificar | k-vistas (tree.md) | Las dos referencias a la acción que abre «Nuevo expediente» (copia verbatim de `design/views/Tramites.xml`) |

**Del diseño — Paso 7 (verbatim: introducción y `Tramites.xml`; las demás vistas son de las tareas 13 y 14)**

### Paso 7 — Vistas

**Crear** `system/expedientes/views/Main-NuevoExpediente.xml` y `system/expedientes/views/Main-TipoExpediente.xml` copiando los de `design/views/`.
**Modificar** `tramites/views/Tramites.xml` copiando `design/views/Tramites.xml`.
**Borrar** `subsystem/expedientes/views/Main-ContextoTramitacion.xml` y `subsystem/expedientes/views/TipoExpediente.xml`.

#### `views/Tramites.xml` (modificado)

- Preexistente (se conserva): todo el fichero (árbol `sysTramites-nuevo-tree`, ayuda, `cards`).
- Delta: el `onClick` del nodo `Tramite` del árbol y el `$action(...)` del botón de las `cards` pasan de `subsysExpedientes.Main@ContextoTramitacion-action` a `sysExpedientes.Main@NuevoExpediente-action`. Es el único cambio permitido en `tramites/views/` (guías).

**Del diseño — Verificación del Paso 7 (verbatim)**

Verificación: `bash .claude/skills/sdd-designer/template-system/validate.sh .sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design` → `VALIDACION-XML: OK`; `grep -rn "Main@ContextoTramitacion\|subsysExpedientes.TipoExpediente@Main" src` sin resultados.
