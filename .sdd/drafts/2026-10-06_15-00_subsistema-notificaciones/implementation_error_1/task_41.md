---
type: implementation-task
template: system
---

# Tarea 41 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Menú «Notificaciones»

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Quita `correos-*` y `sms-*`; añade `notificaciones-*` |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** fusión de `design/menus.xml` en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (`implementation.md` §3, tarea de `menus.xml`), **sin regenerar** los `<menuitem>` del diseño. El destino **ya existe**. Además de insertar los `<menuitem>`, el diseño declara la **eliminación** de los `correos-*` y `sms-*` (ver `## Eliminaciones declaradas`) y su **posición**: en el sitio de `correos-menuitem`, no al final. Tras fusionar, validar con `xmllint` como indica `implementation.md` §3.

> **Nota del descomponedor:** `implementation.md` §3 inserta por defecto antes de `</object-views>`; aquí manda el Paso 12 del diseño (en el sitio de `correos-menuitem`). La posición no cambia el orden visible (lo fija `order="50"`).

## Paso del diseño

### Paso 12 — Menús y visibilidad

`secretariavirtual/menus/menus.xml`: se eliminan las líneas de `correos-menuitem` (y sus 3 hijas, `order="50"`) y de `sms-menuitem` (y sus 3 hijas, `order="55"`), y en el sitio de `correos-menuitem` se pegan las 4 líneas de `design/menus.xml` (`notificaciones-menuitem`, `order="50"`, con «Recibidas» `admins,users`, «Del centro» `users` y «Todas» `admins`).

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `secretariavirtual/menus/menus.xml` | `correos-menuitem`, `correos-recibidos-menuitem`, `correos-delCentro-menuitem`, `correos-todos-menuitem`, `sms-menuitem`, `sms-recibidos-menuitem`, `sms-delCentro-menuitem`, `sms-todos-menuitem` | § Objetivo, § Pantallas (menú «Notificaciones») |
