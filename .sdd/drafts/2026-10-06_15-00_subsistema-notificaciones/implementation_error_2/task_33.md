---
type: implementation-task
template: system
---

# Tarea 33 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Tarea de `menus.xml`: los `<menuitem>` a añadir están **ya materializados** en `design/menus.xml`; se **fusionan** (sin regenerarlos) en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` y, tras fusionar, **MUST** validarse con `xmllint` (ver `implementation.md` §3). Acción `Modificar`: el destino **ya existe**.
Nota del descomponedor (decisión documentada): el diseño (Paso 12) pide además **quitar** los `correos-*` y `sms-*` (declarados en `## Eliminaciones declaradas`) y pegar los `notificaciones-*` **en el sitio de `correos-menuitem`**; esa ubicación concreta del diseño prevalece sobre la genérica «justo antes de `</object-views>`» de `implementation.md`. Si al fusionar ya existe un `<menuitem>` `notificaciones-*` con el mismo `name`, se reporta `CONFLICT` como dice `implementation.md` §3.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Quita `correos-*` y `sms-*`; añade `notificaciones-*` |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 12 — Menús y visibilidad

`secretariavirtual/menus/menus.xml`: se eliminan las líneas de `correos-menuitem` (y sus 3 hijas, `order="50"`) y de `sms-menuitem` (y sus 3 hijas, `order="55"`), y en el sitio de `correos-menuitem` se pegan las 4 líneas de `design/menus.xml` (`notificaciones-menuitem`, `order="50"`, con «Recibidas» `admins,users`, «Del centro» `users` y «Todas» `admins`).

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

### Eliminaciones declaradas (fila de esta tarea)

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `secretariavirtual/menus/menus.xml` | `correos-menuitem`, `correos-recibidos-menuitem`, `correos-delCentro-menuitem`, `correos-todos-menuitem`, `sms-menuitem`, `sms-recibidos-menuitem`, `sms-delCentro-menuitem`, `sms-todos-menuitem` | § Objetivo, § Pantallas (menú «Notificaciones») |
