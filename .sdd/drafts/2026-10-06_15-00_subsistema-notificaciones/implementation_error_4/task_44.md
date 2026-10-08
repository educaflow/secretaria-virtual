---
type: implementation-task
template: system
---

# Tarea 44 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Quita `correos-*` y `sms-*`; añade `notificaciones-*` |

**Materialización (`menus.xml`, `Acción: Modificar`):** el destino `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` **ya existe**. Primero se eliminan los `<menuitem>` declarados en `## Eliminaciones declaradas` (abajo); después se insertan los `<menuitem>` de `design/menus.xml` **literalmente** (sin regenerarlos) en el sitio que ocupaba `correos-menuitem`, como dice el Paso 12. Si ya existe un `<menuitem>` con el mismo `name` → `CONFLICT` (`implementation.md` §3). Tras fusionar **MUST** validarse con `xmllint` contra `object-views.xsd` (`implementation.md` §3).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Diseño — Paso 12 (extracto)

### Paso 12 — Menús y visibilidad

`secretariavirtual/menus/menus.xml`: se eliminan las líneas de `correos-menuitem` (y sus 3 hijas, `order="50"`) y de `sms-menuitem` (y sus 3 hijas, `order="55"`), y en el sitio de `correos-menuitem` se pegan las 4 líneas de `design/menus.xml` (`notificaciones-menuitem`, `order="50"`, con «Recibidas» `admins,users`, «Del centro» `users` y «Todas» `admins`).

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

## Eliminaciones declaradas (filas que aplican)

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `secretariavirtual/menus/menus.xml` | `correos-menuitem`, `correos-recibidos-menuitem`, `correos-delCentro-menuitem`, `correos-todos-menuitem`, `sms-menuitem`, `sms-recibidos-menuitem`, `sms-delCentro-menuitem`, `sms-todos-menuitem` | § Objetivo, § Pantallas (menú «Notificaciones») |

**Nota del descomponedor:** `implementation.md` §3 describe la fusión como inserción antes de `</object-views>`; el diseño (Paso 12) pide pegarlos en el sitio de `correos-menuitem` y retirar los antiguos. Prevalece el diseño, que es el contrato de esta tarea.
