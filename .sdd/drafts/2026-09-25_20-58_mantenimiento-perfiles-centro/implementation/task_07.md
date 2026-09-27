---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

# Diseño: Mantenimiento de perfiles de trámites por centro

**Objetivo:** que el supervisor (en los centros que supervisa) y el administrador (en cualquier centro) puedan ver, crear, modificar y borrar las filas de `AceProfileCentro`, con las reglas que garantizan que cada fila dice sin ambigüedad a quién se da el perfil.
**Capa:** subsystem/security
**Especificación de origen:** .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-validaciones, k-code-quality, k-secure-coding, k-vistas, k-datainit

Las decisiones difíciles y sus alternativas están en `decisiones.md` (D1–D6); este documento es coherente con ellas.

## Ficheros a crear o modificar

Rutas relativas a `src/main/java/com/educaflow/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | + 2 `<menuitem>` (porción en `design/menus.xml`). |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los fragmentos de `auth-security.xml`, `input-config.xml` y `CLAUDE.md` del Paso 7 se aplican **añadiendo** a lo que ya hay (no se borra nada).

**Instrucción de materialización (fusión de `menus.xml`):** la porción ya está materializada en `design/menus.xml`; sus `<menuitem>` se **fusionan** (sin regenerarlos) en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`, que **ya existe** (`Acción: Modificar`), según `implementation.md` §3, y se valida con `xmllint` tras fusionar. El Paso 6 pide colocarlos como últimos hijos de sus padres existentes (`miCentro-menuitem` y `administracion-menuitem`).

## Pasos

### Paso 6 — Menús

Porción en `design/menus.xml`, a fusionar en `secretariavirtual/menus/menus.xml` como últimos hijos de sus padres existentes:

- `miCentro-perfilesTramites-menuitem` bajo `miCentro-menuitem` («Mi centro», ya con `if="__config__.menu.isSupervisor()"` y `groups="users"`): «Perfiles de trámites» → `subsysSecurity.Centro@AceProfileCentro-action`, `groups="users"`, `order="3"`.
- `administracion-perfilesTramitesPorCentro-menuitem` bajo `administracion-menuitem`: «Perfiles de trámites por centro» → `subsysSecurity.Main@AceProfileCentro-action`, `groups="admins"`, `order="4"`.

Resultado: el director (no supervisor, grupo `users`) no ve ninguna (ESC-020); el supervisor ve la suya y no la de administración (ESC-021). Un menú oculto no autoriza nada: la autorización son los permisos del Paso 7 y V-AceProfileCentro-008.

**Verificar:** `./gradlew test --tests 'com.educaflow.views.*'` (VAR-10.x: una línea por menú, atributos en orden, `order` único por submenú).
