---
type: implementation-task
template: system
---

# Tarea 41 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-skill

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `.claude/skills/k-validaciones/validaciones.md` | Modificar | k-skill | §4 («cierre: MUST ser `force-back`»): la misma excepción |

## Diseño — Paso 11 (extracto)

### Paso 11 — Ampliación de `VAR-7.2`/`VAR-7.3` y del glosario (patrón nuevo, D3)

Después: `/developer-create-view-tests` regenera (nunca a mano) `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` (`VAR-7.2` y `VAR-7.3`) y `src/test/java/com/educaflow/views/support/Index.java` (`PREDEFINIDAS`, desde el glosario), y se sincronizan, con `/k-skill`:

- todos los sitios que hoy dicen que tras `save` el cierre MUST ser `force-back`, con la misma excepción «form abierto por código en popup sin grid → `save` → `remote-refreshTab-action` → `close`»: `k-vistas/forms.md` (fila «Botón Guardar/Cancelar» de la tabla comparativa + sección «Form abierto por código en popup»), `k-vistas/actions.md` (§ `<action-group>` — secuencia de acciones principales), `k-validaciones/validaciones.md` (§4, cierre), `k-sistemas/controladores.md` (la línea de `force-back`) y `sdd-designer/template-system/vistas.md` (§1.5 y detector e) de §3);
- `k-vistas/actions.md` § Convenciones de nombres para las acciones: la «Excepción» de las acciones globales pasa a listar tres (`remote-validationSave-action`, `remote-validationDelete-action`, `remote-refreshTab-action`);
- la frase «`popup="reload"` refresca el listado de debajo» de `k-vistas/forms.md` § Form abierto por código en popup («Ese método pone `popup="reload"` (al cerrarse tras guardar, Axelor refresca el listado de debajo)») y de `k-sistemas/controladores.md` § Reglas del controlador (excepción de `force-back`) se sustituye por «`popup="true"`; el listado de debajo lo refresca `remote-refreshTab-action` (`refresh-tab`), no el cierre del popup».

**Nota del descomponedor:** fichero de skill, no Java: se edita directamente aplicando `/k-skill`, solo con el cambio que la fila y el Paso 11 declaran para `.claude/skills/k-validaciones/validaciones.md`. El worktree puede traer ya modificaciones en este fichero: compruébalo antes y no dupliques un cambio que ya esté (la edición es idempotente).
