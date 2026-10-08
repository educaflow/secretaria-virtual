---
type: implementation-task
template: system
---

# Tarea 20 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/infrastructure/controller/DefaultModelController.java` | Modificar | k-sistemas (controladores.md) | + `refreshTab` (D3) |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

## Diseño — Paso 8 (extracto)

### Paso 8 — Controladores y ayudante de respuesta

**`DefaultModelController`** (`base/infrastructure/controller/`, Modificar; el resto se conserva): es el dueño único de «refrescar el listado de la pestaña desde un form en popup» (D3).

```java
@CallMethod public void refreshTab(ActionRequest actionRequest, ActionResponse actionResponse);
//   actionResponse.setSignal("refresh-tab", null). No lee el contexto ni llama a servicios: axelor-front manda
//   refresh-tab a getActiveTabId(-1), la pestaña ignorando los popups (por eso llega al listado aunque el alta
//   se abra sobre la elección de canal).
```

Es la única pieza de refresco del listado: la usan el `btnSave` y el `btnReenviar` de `Main@Correo-form`/`Main@Sms-form` (el `btnReenviar` de `Centro@` delega en el de `Main@`, Paso 10).

## Notas y supuestos (extracto)

- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.
