---
type: implementation-task
template: system
---

# Tarea 37 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- Ninguno (la tabla del diseño no asigna skill a este fichero)

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `agent_docs/view-rules.md` | Modificar | — | Glosario «Acciones globales/predefinidas» + `remote-refreshTab-action`; `VAR-7.3`: la admite entre las globales; `VAR-7.2`: rama «form sin grid al que volver» también para el maestro con `btnSave`, con `remote-refreshTab-action` entre `save` y `close` (D3) |

## Diseño — Paso 11

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

**Nota del descomponedor:** fichero de documentación (raíz del repositorio, no `src/main/java`): se edita directamente, sin `developer-code-implementer`; solo cambian los tres sitios (a), (b), (c), conservando el resto del fichero y el formato Markdown de `CLAUDE.md`.
