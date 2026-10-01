---
type: implementation-task
template: system
---

# Tarea 27 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Enlazar los dos permisos al `<group code="users">` |

### Paso 10 — Seguridad y datos iniciales (de `design/design.md`)

**Ficheros:** `subsystem/sms/data-init/input-config.xml` (Crear), `subsystem/sms/data-init/input/auth-sms.xml` (Crear), `src/main/resources/data-init/input/auth.xml` (Modificar).

- `input-config.xml` con `priority="10"` (como el de correos, para que los permisos existan antes de que el `auth.xml` global, `priority="-1"`, los enlace a los grupos) y un único `<input file="auth-sms.xml" root="auth">` con el binding de `com.axelor.auth.db.Permission` (`search="self.name = :name"`, `create="true" update="true"`, y los `bind` de `@name`, `@object`, `@condition`, `@conditionParams` y los cinco `can/@*`).
- `auth-sms.xml` con **dos permisos, los dos de solo lectura** (`create/write/remove/export = false`), porque ningún usuario del grupo `users` puede crear, modificar ni borrar SMS:
  - `Sms.propio-destinatario` — objeto `com.educaflow.subsystem.sms.db.Sms`,
    `condition="self.dniDestinatario = ? and self.estado = 'ENVIADO'"`,
    `conditionParams="__user__.dni"`. Es el permiso de «Mis SMS»: cada usuario ve solo los SMS
    **enviados con éxito** a su propio DNI, de cualquier centro.
  - `Sms.propio-centro-gestion` — mismo objeto,
    `condition="self.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo IN ('SUPERVISOR', 'ADMINISTRATIVO'))"`,
    `conditionParams="__user__"`. Es el permiso de «Del centro»: el mismo alcance que el `<domain>` de
    `Centro-Sms.xml`, para que la UI y el permiso no puedan divergir.
- En el `auth.xml` global, dentro de `<group code="users">` y junto a los de correos, añadir
  `<permission name="Sms.propio-destinatario"/>` y `<permission name="Sms.propio-centro-gestion"/>`.
  **No** se añade nada al grupo `admins` ni existe un `Sms.all`: es exactamente el modelo de correos
  (el Administrador no pasa por estos permisos condicionales).
- No hay otros datos iniciales: el subsistema no tiene catálogos ni datos maestros (las credenciales
  del proveedor son configuración por instalación, no datos de la aplicación).
- **MUST NOT** crear a mano ningún `i18n_*.csv` dentro de `data-init/input/`.

**Verificar:** tras arrancar,
`SELECT name, object FROM auth_permission WHERE name LIKE 'Sms.%'` devuelve las dos filas, y
`SELECT p.name FROM auth_group g JOIN auth_group_permission gp ON gp.group_id = g.id JOIN auth_permission p ON p.id = gp.permission_id WHERE g.code = 'users' AND p.name LIKE 'Sms.%'` las devuelve enlazadas al grupo.

### Decisión del descomponedor (no es texto del diseño)

Esta tarea cubre solo el delta del `auth.xml` global (las dos líneas `<permission>` en `<group code="users">`, junto a las de correos). Es `Modificar`: **MUST** conservarse todo lo demás del fichero.
