---
type: implementation-task
template: system
---

# Tarea 35 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- developer-create-view-tests

## Regeneración de Categoria7BotonesTest

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` | Modificar | developer-create-view-tests | Regenerado desde `view-rules.md` (nunca a mano) |

## Paso del diseño

### Paso 11 — Ampliación de `VAR-7.2` (patrón nuevo, D3)

`agent_docs/view-rules.md`, `VAR-7.2`, se reescribe solo la rama del maestro sin grid al que volver. Texto nuevo de la fila `btnSave`/maestro y de `btnCancel`/maestro de la tabla de **Verificación**:

- `btnSave` (maestro): «[`Local-…`]* → `remote-validationSave-action` → `save` → `force-back` (inmediatamente tras `save`; **nunca** `back`); **si ningún `<action-view>` que lo abra declara una `<view type="grid">`** (form abierto por código en popup), `… → save → close`».
- `btnCancel` (maestro): «contiene `back`; si ningún `<action-view>` que lo abra (una `<view type="form">` con su `name`) declara una `<view type="grid">`, contiene `close`» (desaparece la condición «no declara `btnSave`»).
- En la **Decisión**, una frase: el form que abre el servidor por código en popup no tiene grid al que volver: `back`/`force-back` no hacen nada y lo que cierra es `close`, también tras guardar (el popup con `popup="reload"` refresca el listado de debajo).

Después: `/developer-create-view-tests` regenera `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` (nunca a mano), y se sincronizan, con `/k-skill`, todos los sitios que hoy dicen que tras `save` el cierre MUST ser `force-back`, con la misma excepción «form abierto por código en popup sin grid → `save` → `close`»: `k-vistas/forms.md` (fila «Botón Guardar/Cancelar» de la tabla comparativa + sección «Form abierto por código en popup»), `k-vistas/actions.md` (§ `<action-group>` — secuencia de acciones principales), `k-validaciones/validaciones.md` (§4, cierre), `k-sistemas/controladores.md` (la línea de `force-back`) y `sdd-designer/template-system/vistas.md` (§1.5 y detector e) de §3).

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` en verde con las vistas del Paso 10 y sin cambios en el resto del proyecto.

**Materialización:** invocar el skill `/developer-create-view-tests` (regeneración incremental desde `agent_docs/view-rules.md`, ya modificado por la tarea 34). **MUST NOT** editar el `.java` a mano.
