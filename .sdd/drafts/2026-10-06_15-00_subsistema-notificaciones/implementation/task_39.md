---
type: implementation-task
template: system
---

# Tarea 39 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- developer-create-view-tests

## Regenerar los tests de vistas (Categoria7BotonesTest + Index)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` | Modificar | developer-create-view-tests | Regenerado desde `view-rules.md` (`VAR-7.2` y `VAR-7.3`; nunca a mano) |
| `src/test/java/com/educaflow/views/support/Index.java` | Modificar | developer-create-view-tests | Regenerado desde el glosario de `view-rules.md` (`PREDEFINIDAS` + `remote-refreshTab-action`; nunca a mano) |

**Instrucción de materialización:** estos dos `.java` **no** se escriben a mano: se regeneran ejecutando `/developer-create-view-tests` sobre `agent_docs/view-rules.md` ya modificado (tarea anterior). Ambos salen de la misma ejecución del generador.

### Paso 11 — Ampliación de `VAR-7.2`/`VAR-7.3` y del glosario (patrón nuevo, D3) (extracto)

Después: `/developer-create-view-tests` regenera (nunca a mano) `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` (`VAR-7.2` y `VAR-7.3`) y `src/test/java/com/educaflow/views/support/Index.java` (`PREDEFINIDAS`, desde el glosario), y se sincronizan, con `/k-skill`:

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` en verde con las vistas del Paso 10 y sin cambios en el resto del proyecto.
