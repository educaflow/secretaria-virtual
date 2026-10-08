---
type: implementation-task
template: system
---

# Tarea 38 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- (ninguno: la tabla del diseño no asigna skill a esta fila, columna `Skill` = «—»)

## view-rules.md: VAR-7.2, VAR-7.3 y glosario con remote-refreshTab-action

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `agent_docs/view-rules.md` | Modificar | — | Glosario «Acciones globales/predefinidas» + `remote-refreshTab-action`; `VAR-7.3`: la admite entre las globales; `VAR-7.2`: rama «form sin grid al que volver» también para el maestro con `btnSave`, con `remote-refreshTab-action` entre `save` y `close` (D3) |

**Instrucción de materialización:** edición de Markdown en `agent_docs/view-rules.md` (raíz del proyecto), solo los tres sitios que indica el Paso 11; el resto se conserva. Respeta las reglas de formato Markdown del `CLAUDE.md` (corte por frases, jerárquico).

### Paso 11 — Ampliación de `VAR-7.2`/`VAR-7.3` y del glosario (patrón nuevo, D3) (extracto)

`agent_docs/view-rules.md` cambia en tres sitios, todos por la acción global `remote-refreshTab-action` (D3):

- (a) Glosario «**Acciones globales/predefinidas**»: se añade `remote-refreshTab-action` a la lista (tras `remote-validationDelete-action`).
  Se declara en `DefaultModelController.xml`, fuera de `views/`, así que sin esto `Categoria4IntegridadTest` no la resuelve en los grupos que la usan (2 `btnSave` y 2 `btnReenviar`).
- (b) `VAR-7.3`, condición: «una de las globales `remote-validationSave-action`/`remote-validationDelete-action`» pasa a «una de las globales `remote-validationSave-action`/`remote-validationDelete-action`/`remote-refreshTab-action`».
  Sigue sin admitirse ningún `Remote-…-action` propio: `refreshTab` no persiste nada.
- (c) `VAR-7.2`, se reescribe solo la rama del maestro sin grid al que volver. Texto nuevo de la fila `btnSave`/maestro y de `btnCancel`/maestro de la tabla de **Verificación**:
  - `btnSave` (maestro): «[`Local-…`]* → `remote-validationSave-action` → `save` → `force-back` (inmediatamente tras `save`; **nunca** `back`); **si ningún `<action-view>` que lo abra declara una `<view type="grid">`** (form abierto por código en popup), `… → save → remote-refreshTab-action → close`».
  - `btnCancel` (maestro): «contiene `back`; si ningún `<action-view>` que lo abra (una `<view type="form">` con su `name`) declara una `<view type="grid">`, contiene `close`» (desaparece la condición «no declara `btnSave`»).
  - En la **Decisión**, la frase «el popup con `popup="reload"` refresca el listado de debajo» se sustituye por: el form que abre el servidor por código en popup no tiene grid al que volver: `back`/`force-back` no hacen nada y lo que cierra es `close`, también tras guardar; se abre con `popup="true"`, y el listado de debajo lo refresca `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `refresh-tab`, que llega a la pestaña ignorando los popups), no el cierre del popup, por eso va entre `save` y `close`.

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` en verde con las vistas del Paso 10 y sin cambios en el resto del proyecto.
