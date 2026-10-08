---
type: implementation-task
template: system
---

# Tarea 41 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-skill

Acción `Modificar`: fichero de skill; se cambia **solo** lo que el diseño declara para `k-vistas/actions.md` y se conserva el resto, aplicando `/k-skill` (lo exige el `CLAUDE.md` del proyecto al modificar skills).

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `.claude/skills/k-vistas/actions.md` | Modificar | k-skill | § `<action-group>` — secuencia de acciones principales: la excepción «form abierto por código en popup sin grid → `save` → `remote-refreshTab-action` → `close`» junto al `force-back` obligatorio, y `remote-refreshTab-action` en el comentario de las acciones globales; § Convenciones de nombres para las acciones, la «Excepción» pasa a tres globales (`remote-validationSave-action`, `remote-validationDelete-action`, `remote-refreshTab-action`) (coherencia con `VAR-7.2`/`VAR-7.3`) |

### Paso 11 — Ampliación de `VAR-7.2`/`VAR-7.3` y del glosario (patrón nuevo, D3)

  - En la **Decisión**, la frase «el popup con `popup="reload"` refresca el listado de debajo» se sustituye por: el form que abre el servidor por código en popup no tiene grid al que volver: `back`/`force-back` no hacen nada y lo que cierra es `close`, también tras guardar; se abre con `popup="true"`, y el listado de debajo lo refresca `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `refresh-tab`, que llega a la pestaña ignorando los popups), no el cierre del popup, por eso va entre `save` y `close`.

Después: `/developer-create-view-tests` regenera (nunca a mano) `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` (`VAR-7.2` y `VAR-7.3`) y `src/test/java/com/educaflow/views/support/Index.java` (`PREDEFINIDAS`, desde el glosario), y se sincronizan, con `/k-skill`:

- todos los sitios que hoy dicen que tras `save` el cierre MUST ser `force-back`, con la misma excepción «form abierto por código en popup sin grid → `save` → `remote-refreshTab-action` → `close`»: `k-vistas/forms.md` (fila «Botón Guardar/Cancelar» de la tabla comparativa + sección «Form abierto por código en popup»), `k-vistas/actions.md` (§ `<action-group>` — secuencia de acciones principales), `k-validaciones/validaciones.md` (§4, cierre), `k-sistemas/controladores.md` (la línea de `force-back`) y `sdd-designer/template-system/vistas.md` (§1.5 y detector e) de §3);
- `k-vistas/actions.md` § Convenciones de nombres para las acciones: la «Excepción» de las acciones globales pasa a listar tres (`remote-validationSave-action`, `remote-validationDelete-action`, `remote-refreshTab-action`);
- la frase «`popup="reload"` refresca el listado de debajo» de `k-vistas/forms.md` § Form abierto por código en popup («Ese método pone `popup="reload"` (al cerrarse tras guardar, Axelor refresca el listado de debajo)») y de `k-sistemas/controladores.md` § Reglas del controlador (excepción de `force-back`) se sustituye por «`popup="true"`; el listado de debajo lo refresca `remote-refreshTab-action` (`refresh-tab`), no el cierre del popup».
