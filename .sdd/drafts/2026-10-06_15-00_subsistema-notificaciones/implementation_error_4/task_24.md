---
type: implementation-task
template: system
---

# Tarea 24 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/infrastructure/controller/DefaultModelController.xml` | Modificar | k-vistas (actions.md) | Fichero completo materializado en `design/views/DefaultModelController.xml` (se copia verbatim aquí, no a `subsystem/notificaciones/views/`): + acción global `remote-refreshTab-action` (D3) |

**Materialización (XML ya materializado):** el fichero está en `design/views/DefaultModelController.xml` (dentro de la carpeta de la iniciativa) y se debe **copiar literalmente** a `src/main/java/com/educaflow/base/infrastructure/controller/DefaultModelController.xml`, **sin regenerarlo** ni reformatearlo (contrato de materialización, `implementation.md` §1 y §3).
La fila es `Acción: Modificar`: el destino **ya existe**; antes de sobrescribir aplica la **comprobación de conservación** de `implementation.md` §3 (todo elemento con nombre del fichero real actual debe estar en el XML del diseño, salvo lo listado en `## Eliminaciones declaradas`).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Diseño — Paso 8 (extracto)

### Paso 8 — Controladores y ayudante de respuesta

`DefaultModelController.xml` (Modificar): el fichero completo resultante está materializado en `design/views/DefaultModelController.xml` y se **copia verbatim** a `base/infrastructure/controller/DefaultModelController.xml` (no a `subsystem/notificaciones/views/`, Paso 10).
- Preexistente (se conserva): cabecera `object-views`, el comentario de uso de las acciones globales y las dos `<action-method>` `remote-validationSave-action` y `remote-validationDelete-action`.
- Delta: la acción global sin `model` `remote-refreshTab-action` (`DefaultModelController.refreshTab`), tras las dos anteriores, y en el comentario de cabecera el uso «form abierto por código en popup (sin grid): `btnSave` = … → `save` → `remote-refreshTab-action` → `close`; y al final de cualquier remoto que cambie la fila desde ese popup».
Es la única pieza de refresco del listado: la usan el `btnSave` y el `btnReenviar` de `Main@Correo-form`/`Main@Sms-form` (el `btnReenviar` de `Centro@` delega en el de `Main@`, Paso 10).

## Diseño — Paso 10 (extracto)

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).
