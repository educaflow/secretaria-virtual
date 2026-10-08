---
type: implementation-task
template: system
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## DefaultModelController.refreshTab

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/infrastructure/controller/DefaultModelController.java` | Modificar | k-sistemas (controladores.md) | + `refreshTab` (D3) |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

### Paso 8 (extracto verbatim del design.md: `DefaultModelController`)

### Paso 8 — Controladores y ayudante de respuesta

**`DefaultModelController`** (`base/infrastructure/controller/`, Modificar; el resto se conserva): es el dueño único de «refrescar el listado de la pestaña desde un form en popup» (D3).

```java
@CallMethod public void refreshTab(ActionRequest actionRequest, ActionResponse actionResponse);
//   actionResponse.setSignal("refresh-tab", null). No lee el contexto ni llama a servicios: axelor-front manda
//   refresh-tab a getActiveTabId(-1), la pestaña ignorando los popups (por eso llega al listado aunque el alta
//   se abra sobre la elección de canal).
```

`DefaultModelController.xml` (Modificar): el fichero completo resultante está materializado en `design/views/DefaultModelController.xml` y se **copia verbatim** a `base/infrastructure/controller/DefaultModelController.xml` (no a `subsystem/notificaciones/views/`, Paso 10).
- Preexistente (se conserva): cabecera `object-views`, el comentario de uso de las acciones globales y las dos `<action-method>` `remote-validationSave-action` y `remote-validationDelete-action`.
- Delta: la acción global sin `model` `remote-refreshTab-action` (`DefaultModelController.refreshTab`), tras las dos anteriores, y en el comentario de cabecera el uso «form abierto por código en popup (sin grid): `btnSave` = … → `save` → `remote-refreshTab-action` → `close`; y al final de cualquier remoto que cambie la fila desde ese popup».
Es la única pieza de refresco del listado: la usan el `btnSave` y el `btnReenviar` de `Main@Correo-form`/`Main@Sms-form` (el `btnReenviar` de `Centro@` delega en el de `Main@`, Paso 10).

Verificación (al cerrar el bloque 1–9): compila; `./gradlew -q test --tests '*architecture*'` (C9, C15, C28).

### Nota del design.md (`## Notas y supuestos`)

- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.

**Referencia:** la decisión D3 está en `design/decisiones.md`.
