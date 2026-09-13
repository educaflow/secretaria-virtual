---
type: implementation-task
template: system
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Fusiona la porción de menús del diseño en el fichero único de menús del proyecto.

**El XML ya está materializado en `design/menus.xml`.** **MUST NOT** regenerarlo ni reescribirlo. Lee `design/menus.xml`, extrae sus `<menuitem>` e insértalos en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` justo antes de `</object-views>`, según `implementation.md` §3.

**Esta fusión es un no-op esperado**: las tres entradas ya existen en el fichero real con ese mismo contenido. Como consecuencia, un `<menuitem name="...">` con el mismo `name` ya presente **NO** debe tratarse aquí como el `CONFLICT` habitual: lo correcto es **dejar el fichero real intacto** y verificar que el resultado es idéntico al de partida. **Decisión tomada en esta descomposición ante la ambigüedad** entre la regla genérica de `implementation.md` §3 (`<menuitem>` duplicado → `CONFLICT`) y el Paso 10 del diseño, que declara explícitamente el delta como nulo y la fusión como un no-op: manda el diseño, porque la regla genérica existe para impedir pisar un menú ajeno, no para prohibir un delta vacío. Si al comparar aparece **cualquier** diferencia con el fichero real, entonces sí reporta `CONFLICT` con el detalle.

**MUST NOT** crearse ningún `menus-<subsistema>.xml`.

Tras la fusión, **MUST** validar con `xmllint`:

```bash
xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd \
  src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
```

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | **Sin delta**: el bloque «Sistema educativo» se reproduce tal cual; la fusión es un no-op. |

## Paso 10 del diseño (verbatim)

### Paso 10 — Menús

Sin delta. Las tres entradas que usan los escenarios (`Sistema educativo → Ciclos`, `→ Grados`, `→ Niveles`) ya existen en el fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` con el contenido que reproduce `design/menus.xml`; la fusión es un no-op. **MUST NOT** crearse ningún `menus-<subsistema>.xml`.

**Verificación del paso:** `git diff --stat src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` no devuelve nada.

