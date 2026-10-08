---
type: implementation-task
template: system
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-code-quality
- k-secure-coding

## ActionResponseHelper.doResponseViewFormEnPopup

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/infrastructure/axelorhelper/ActionResponseHelper.java` | Modificar | k-sistemas (controladores.md) | + `doResponseViewFormEnPopup` (D3) |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

### Paso 8 — Controladores y ayudante de respuesta (extracto)

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

## Notas y supuestos (las que aplican a esta tarea)

- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.
