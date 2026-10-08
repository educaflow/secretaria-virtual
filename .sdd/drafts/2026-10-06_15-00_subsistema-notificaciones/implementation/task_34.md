---
type: implementation-task
template: system
---

# Tarea 34 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Menú notificaciones (fusión en menus.xml)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Quita `correos-*` y `sms-*`; añade `notificaciones-*` |

**Instrucción de materialización (menús):** la porción ya está materializada en `design/menus.xml`; se **fusiona** en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` sin regenerarla (`implementation.md` §3, tarea de `menus.xml`), con la particularidad que fija el Paso 12: antes se eliminan las líneas de `correos-menuitem` (y sus 3 hijas) y de `sms-menuitem` (y sus 3 hijas), declaradas en `## Eliminaciones declaradas`, y los `<menuitem>` de `design/menus.xml` se pegan en el sitio de `correos-menuitem`. Tras fusionar, validar con `xmllint` (`implementation.md` §3).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 12 — Menús y visibilidad (extracto)

`secretariavirtual/menus/menus.xml`: se eliminan las líneas de `correos-menuitem` (y sus 3 hijas, `order="50"`) y de `sms-menuitem` (y sus 3 hijas, `order="55"`), y en el sitio de `correos-menuitem` se pegan las 4 líneas de `design/menus.xml` (`notificaciones-menuitem`, `order="50"`, con «Recibidas» `admins,users`, «Del centro» `users` y «Todas» `admins`).

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `secretariavirtual/menus/menus.xml` | `correos-menuitem`, `correos-recibidos-menuitem`, `correos-delCentro-menuitem`, `correos-todos-menuitem`, `sms-menuitem`, `sms-recibidos-menuitem`, `sms-delCentro-menuitem`, `sms-todos-menuitem` | § Objetivo, § Pantallas (menú «Notificaciones») |
