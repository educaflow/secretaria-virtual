---
type: implementation-task
template: system
---

# Tarea 39 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- developer-create-view-tests

Nota del descomponedor (decisión documentada): las dos filas se agrupan porque las produce una única ejecución de `/developer-create-view-tests` sobre `agent_docs/view-rules.md` (ya ampliado por la tarea anterior). **MUST NOT** editarse a mano: se invoca el skill, que regenera de forma incremental.

### Ficheros a crear o modificar (filas de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` | Modificar | developer-create-view-tests | Regenerado desde `view-rules.md` (`VAR-7.2` y `VAR-7.3`; nunca a mano) |
| `src/test/java/com/educaflow/views/support/Index.java` | Modificar | developer-create-view-tests | Regenerado desde el glosario de `view-rules.md` (`PREDEFINIDAS` + `remote-refreshTab-action`; nunca a mano) |

### Paso 11 — Ampliación de `VAR-7.2`/`VAR-7.3` y del glosario (patrón nuevo, D3)

Después: `/developer-create-view-tests` regenera (nunca a mano) `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` (`VAR-7.2` y `VAR-7.3`) y `src/test/java/com/educaflow/views/support/Index.java` (`PREDEFINIDAS`, desde el glosario), y se sincronizan, con `/k-skill`:

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` en verde con las vistas del Paso 10 y sin cambios en el resto del proyecto.
