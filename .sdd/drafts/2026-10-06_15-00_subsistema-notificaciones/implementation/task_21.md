---
type: implementation-task
template: system
---

# Tarea 21 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Acción global remote-refreshTab-action (DefaultModelController.xml)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/infrastructure/controller/DefaultModelController.xml` | Modificar | k-vistas (actions.md) | Fichero completo materializado en `design/views/DefaultModelController.xml` (se copia verbatim aquí, no a `subsystem/notificaciones/views/`): + acción global `remote-refreshTab-action` (D3) |

**Instrucción de materialización (XML ya materializado):** el fichero ya está materializado en `design/views/DefaultModelController.xml` (carpeta de la iniciativa). **MUST** copiarse literalmente a `src/main/java/com/educaflow/base/infrastructure/controller/DefaultModelController.xml`, sin regenerarlo, reescribirlo ni reformatearlo (`implementation.md` §1). La fila es `Acción: Modificar`: el destino **ya existe**; antes de sobrescribir aplica la **comprobación de conservación** de `implementation.md` §3 (contra `## Eliminaciones declaradas` del `design.md`).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 8 — Controladores y ayudante de respuesta (extracto)

`DefaultModelController.xml` (Modificar): el fichero completo resultante está materializado en `design/views/DefaultModelController.xml` y se **copia verbatim** a `base/infrastructure/controller/DefaultModelController.xml` (no a `subsystem/notificaciones/views/`, Paso 10).
- Preexistente (se conserva): cabecera `object-views`, el comentario de uso de las acciones globales y las dos `<action-method>` `remote-validationSave-action` y `remote-validationDelete-action`.
- Delta: la acción global sin `model` `remote-refreshTab-action` (`DefaultModelController.refreshTab`), tras las dos anteriores, y en el comentario de cabecera el uso «form abierto por código en popup (sin grid): `btnSave` = … → `save` → `remote-refreshTab-action` → `close`; y al final de cualquier remoto que cambie la fila desde ese popup».
Es la única pieza de refresco del listado: la usan el `btnSave` y el `btnReenviar` de `Main@Correo-form`/`Main@Sms-form` (el `btnReenviar` de `Centro@` delega en el de `Main@`, Paso 10).

## Notas y supuestos (las que aplican a esta tarea)

- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.
- **Reenvío con el form en popup** (D3, resuelto al diseñar): `refresh-tab` despacha `tab:refresh` a la pestaña activa (el listado de debajo), no al popup, mientras que `setReload(true)` recarga el form del popup; por eso `CorreoController.reenviar`/`SmsController.reenviar` responden `setReload(true)` y el `btnReenviar` de `Main@` termina en `remote-refreshTab-action`, la misma acción que el alta (nota anterior); el de `Centro@` delega en él.
