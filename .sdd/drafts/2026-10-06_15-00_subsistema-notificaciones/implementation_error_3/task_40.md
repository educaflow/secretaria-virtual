---
type: implementation-task
template: system
---

# Tarea 40 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-skill

## Sincronizar skill k-validaciones/validaciones.md

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `.claude/skills/k-validaciones/validaciones.md` | Modificar | k-skill | §4 («cierre: MUST ser `force-back`»): la misma excepción |

### Paso 11 (verbatim del design.md)

### Paso 11 — Ampliación de `VAR-7.2`/`VAR-7.3` y del glosario (patrón nuevo, D3)

`agent_docs/view-rules.md` cambia en tres sitios, todos por la acción global `remote-refreshTab-action` (D3):

- (a) Glosario «**Acciones globales/predefinidas**»: se añade `remote-refreshTab-action` a la lista (tras `remote-validationDelete-action`).
  Se declara en `DefaultModelController.xml`, fuera de `views/`, así que sin esto `Categoria4IntegridadTest` no la resuelve en los grupos que la usan (2 `btnSave` y 2 `btnReenviar`).
- (b) `VAR-7.3`, condición: «una de las globales `remote-validationSave-action`/`remote-validationDelete-action`» pasa a «una de las globales `remote-validationSave-action`/`remote-validationDelete-action`/`remote-refreshTab-action`».
  Sigue sin admitirse ningún `Remote-…-action` propio: `refreshTab` no persiste nada.
- (c) `VAR-7.2`, se reescribe solo la rama del maestro sin grid al que volver. Texto nuevo de la fila `btnSave`/maestro y de `btnCancel`/maestro de la tabla de **Verificación**:
  - `btnSave` (maestro): «[`Local-…`]* → `remote-validationSave-action` → `save` → `force-back` (inmediatamente tras `save`; **nunca** `back`); **si ningún `<action-view>` que lo abra declara una `<view type="grid">`** (form abierto por código en popup), `… → save → remote-refreshTab-action → close`».
  - `btnCancel` (maestro): «contiene `back`; si ningún `<action-view>` que lo abra (una `<view type="form">` con su `name`) declara una `<view type="grid">`, contiene `close`» (desaparece la condición «no declara `btnSave`»).
  - En la **Decisión**, la frase «el popup con `popup="reload"` refresca el listado de debajo» se sustituye por: el form que abre el servidor por código en popup no tiene grid al que volver: `back`/`force-back` no hacen nada y lo que cierra es `close`, también tras guardar; se abre con `popup="true"`, y el listado de debajo lo refresca `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `refresh-tab`, que llega a la pestaña ignorando los popups), no el cierre del popup, por eso va entre `save` y `close`.

Después: `/developer-create-view-tests` regenera (nunca a mano) `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` (`VAR-7.2` y `VAR-7.3`) y `src/test/java/com/educaflow/views/support/Index.java` (`PREDEFINIDAS`, desde el glosario), y se sincronizan, con `/k-skill`:

- todos los sitios que hoy dicen que tras `save` el cierre MUST ser `force-back`, con la misma excepción «form abierto por código en popup sin grid → `save` → `remote-refreshTab-action` → `close`»: `k-vistas/forms.md` (fila «Botón Guardar/Cancelar» de la tabla comparativa + sección «Form abierto por código en popup»), `k-vistas/actions.md` (§ `<action-group>` — secuencia de acciones principales), `k-validaciones/validaciones.md` (§4, cierre), `k-sistemas/controladores.md` (la línea de `force-back`) y `sdd-designer/template-system/vistas.md` (§1.5 y detector e) de §3);
- `k-vistas/actions.md` § Convenciones de nombres para las acciones: la «Excepción» de las acciones globales pasa a listar tres (`remote-validationSave-action`, `remote-validationDelete-action`, `remote-refreshTab-action`);
- la frase «`popup="reload"` refresca el listado de debajo» de `k-vistas/forms.md` § Form abierto por código en popup («Ese método pone `popup="reload"` (al cerrarse tras guardar, Axelor refresca el listado de debajo)») y de `k-sistemas/controladores.md` § Reglas del controlador (excepción de `force-back`) se sustituye por «`popup="true"`; el listado de debajo lo refresca `remote-refreshTab-action` (`refresh-tab`), no el cierre del popup».

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` en verde con las vistas del Paso 10 y sin cambios en el resto del proyecto.

**Referencia:** la decisión D3 está en `design/decisiones.md`.
