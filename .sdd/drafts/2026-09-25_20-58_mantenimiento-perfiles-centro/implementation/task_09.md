---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

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
| `subsystem/security/data-init/input-config.xml` | Modificar | k-datainit | + `<bind node="group">` en el `<input>` de `auth-security.xml`. |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los fragmentos de `auth-security.xml`, `input-config.xml` y `CLAUDE.md` del Paso 7 se aplican **añadiendo** a lo que ya hay (no se borra nada).

**Acción `Modificar`:** el fichero ya existe; se **añade** el bind dentro del `<input file="auth-security.xml" root="auth">` existente, sin borrar nada. Esta tarea cubre solo el apartado 7.2 del Paso 7 (el 7.1 es la Tarea 08, que añade el `<group code="users">` que este bind carga). La decisión D2 citada está en `design/decisiones.md`.

## Pasos

### Paso 7 — Seguridad (data-init del subsistema y documentación)

Todo dentro de `subsystem/security` (guía de diseño). Reglas de acceso en lenguaje natural:

- **Administrador** (grupo `admins`): ve, crea, modifica y borra filas de cualquier centro. No necesita permiso: Axelor no aplica permisos a `admins`, y el servicio no le aplica V-008.
- **Supervisor** (grupo `users` con el tipo de usuario `SUPERVISOR` en algún centro): ve, crea, modifica y borra solo filas de los centros que supervisa, y lee todos los trámites para poder elegirlos.
- **Cualquier otro usuario** del grupo `users`: tiene el permiso por pertenecer a `users`, pero la condición no le deja ver, modificar ni borrar ninguna fila, y V-008 le rechaza el alta.

**7.2 — `data-init/input-config.xml`**: dentro del `<input file="auth-security.xml" root="auth">` existente, tras el `<bind node="permission" …>`, añadir el mismo bind de grupo que usa el data-init global: `<bind node="group" type="com.axelor.auth.db.Group" search="self.code = :code" create="false" update="true">` con `<bind node="@code" to="code"/>` y `<bind node="permission" to="permissions" search="self.name = :name"><bind node="@name" to="name"/></bind>`. Suma a los permisos del grupo (`XMLBinder` hace `addAll`), no los reemplaza; el grupo `users` existe antes de cualquier data-init (`ModuleManager.createDefault`). La `priority="8"` no cambia.

**Verificar:** arrancar con `./run.sh` y comprobar en BD que el grupo `users` tiene los dos permisos (`psql` según `agent_docs/deploy.md`: `auth_group_permissions` del grupo `users` incluye `AceProfileCentro.supervisor` y `Tramite.supervisor`) y que tras un segundo arranque siguen ahí.
