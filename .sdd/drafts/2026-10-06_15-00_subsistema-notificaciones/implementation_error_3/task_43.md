---
type: implementation-task
template: system
---

# Tarea 43 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Menús de notificaciones

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Quita `correos-*` y `sms-*`; añade `notificaciones-*` |

**Instrucción de materialización (fusión de `menus.xml`):** los `<menuitem>` ya están materializados en `design/menus.xml`; se **fusionan** en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` sin regenerarlos y se valida el resultado con `xmllint` (ver `implementation.md` §3). La fila es `Acción: Modificar`: el destino ya existe.

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 12 (extracto verbatim del design.md: `menus.xml`)

### Paso 12 — Menús y visibilidad

`secretariavirtual/menus/menus.xml`: se eliminan las líneas de `correos-menuitem` (y sus 3 hijas, `order="50"`) y de `sms-menuitem` (y sus 3 hijas, `order="55"`), y en el sitio de `correos-menuitem` se pegan las 4 líneas de `design/menus.xml` (`notificaciones-menuitem`, `order="50"`, con «Recibidas» `admins,users`, «Del centro» `users` y «Todas» `admins`).

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

### Eliminaciones declaradas (filas que aplican)

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `secretariavirtual/menus/menus.xml` | `correos-menuitem`, `correos-recibidos-menuitem`, `correos-delCentro-menuitem`, `correos-todos-menuitem`, `sms-menuitem`, `sms-recibidos-menuitem`, `sms-delCentro-menuitem`, `sms-todos-menuitem` | § Objetivo, § Pantallas (menú «Notificaciones») |

**Decisión de la descomposición (ambigüedad):** `implementation.md` §3 indica insertar los `<menuitem>` justo antes de `</object-views>`, pero el Paso 12 del diseño, más específico, dice que se peguen **en el sitio de `correos-menuitem`** y que además se eliminen los `<menuitem>` de `correos-*` y `sms-*` listados en «Eliminaciones declaradas». Prevalece el Paso 12 del diseño (posición y eliminaciones); el resto del procedimiento de §3 (comprobación de `name` duplicado y validación con `xmllint`) se aplica igual.
