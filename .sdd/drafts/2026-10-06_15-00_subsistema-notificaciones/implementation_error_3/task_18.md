---
type: implementation-task
template: system
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## ActionResponseHelper.doResponseViewFormEnPopup

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/infrastructure/axelorhelper/ActionResponseHelper.java` | Modificar | k-sistemas (controladores.md) | + `doResponseViewFormEnPopup` (D3) |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

### Paso 8 (extracto verbatim del design.md: `ActionResponseHelper`)

### Paso 8 — Controladores y ayudante de respuesta

**`ActionResponseHelper`** (Modificar; el resto se conserva):

```java
public void doResponseViewFormEnPopup(String viewName, Class<? extends Model> modelClass, Long id, String title);
//   builder = vistaForm(viewName, modelClass, title)
//     .param("popup", "true")
//     .param("popup-save", "false");     // el listado de debajo lo refresca remote-refreshTab-action (D3)
//   id != null → .context("_showRecord", id) (detalle); id == null → form en alta.
//   response.setView(builder.map()).

private ActionView.ActionViewBuilder vistaForm(String viewName, Class<? extends Model> modelClass, String title);
//   Se extrae del doResponseViewForm existente la parte común (sin _profile ni forceTitle):
//   ActionView.define(title).model(modelClass.getName()).add("form", viewName)
//     .param("forceEdit", "true").param("show-confirm", "false").param("show-toolbar", "false").
//   doResponseViewForm pasa a usarlo y añade encima forceTitle, _profile y su rama _showRecord/newEntity (mismo
//   comportamiento que hoy); así la lista de params comunes vive en un único sitio.
```

Verificación (al cerrar el bloque 1–9): compila; `./gradlew -q test --tests '*architecture*'` (C9, C15, C28).

**Referencia:** la decisión D3 está en `design/decisiones.md`.
