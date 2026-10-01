---
type: implementation-task
template: system
---

# Tarea 26 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit
- k-secure-coding

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de binding de los permisos del subsistema y de su enlace al grupo `users` (`priority="10"`) |
| `src/main/java/com/educaflow/subsystem/sms/data-init/input/auth-sms.xml` | Crear | k-datainit | Permisos condicionales `Sms.propio-destinatario` y `Sms.propio-centro-gestion`, enlazados a su propio `<group code="users">` |

### Paso 10 — Seguridad y datos iniciales

**Ficheros:** `subsystem/sms/data-init/input-config.xml` (Crear) y `subsystem/sms/data-init/input/auth-sms.xml` (Crear). El `auth.xml` global **no** se toca (`k-datainit` §5).

- `input-config.xml` con `priority="10"` y un único `<input file="auth-sms.xml" root="auth">` con dos binds, los mismos que `subsystem/security/data-init/input-config.xml`: el de `com.axelor.auth.db.Permission` (`search="self.name = :name"`, `create="true" update="true"`, y los `bind` de `@name`, `@object`, `@condition`, `@conditionParams` y los cinco `can/@*`) y el del grupo, `<bind node="group" type="com.axelor.auth.db.Group" search="self.code = :code" create="false" update="true">` con `@code` → `code` y `permission` → `permissions` (`search="self.name = :name"`).
- `auth-sms.xml` con **dos permisos, los dos de solo lectura** (`create/write/remove/export = false`), porque ningún usuario del grupo `users` puede crear, modificar ni borrar SMS:
  - `Sms.propio-destinatario` — objeto `com.educaflow.subsystem.sms.db.Sms`. Regla de acceso: los SMS en
    estado `ENVIADO` cuyo DNI del destinatario es el DNI del usuario autenticado. Es el permiso de «Mis SMS»:
    cada usuario ve solo los SMS **enviados con éxito** a su propio DNI, de cualquier centro.
  - `Sms.propio-centro-gestion` — mismo objeto. Regla de acceso: los SMS de los centros en los que el usuario
    autenticado tiene el tipo de usuario `SUPERVISOR` o `ADMINISTRATIVO`. Es el permiso de «Del centro»: el
    mismo alcance que el `<domain>` de `Centro-Sms.xml`, para que la UI y el permiso no puedan divergir.
  - El implementador escribe la `condition`/`conditionParams` de cada uno tomando como patrón los permisos
    equivalentes de `subsystem/correos/data-init/input/auth-correos.xml` (destinatario y centro de gestión),
    sin copiar aquí la consulta.
- Al final de `auth-sms.xml`, un `<group code="users">` con `<permission name="Sms.propio-destinatario"/>` y
  `<permission name="Sms.propio-centro-gestion"/>` (como `auth-security.xml`: el grupo lo crea Axelor y el
  binder añade los permisos sin quitar los que ya tiene).
  **No** se enlaza nada al grupo `admins` ni existe un `Sms.all` (el Administrador no pasa por estos permisos
  condicionales).
- No hay otros datos iniciales: el subsistema no tiene catálogos ni datos maestros (las credenciales
  del proveedor son configuración por instalación, no datos de la aplicación).
- **MUST NOT** crear a mano ningún `i18n_*.csv` dentro de `data-init/input/`.

**Verificar:** tras arrancar,
`SELECT name, object FROM auth_permission WHERE name LIKE 'Sms.%'` devuelve las dos filas, y
`SELECT p.name FROM auth_group g JOIN auth_group_permission gp ON gp.group_id = g.id JOIN auth_permission p ON p.id = gp.permission_id WHERE g.code = 'users' AND p.name LIKE 'Sms.%'` las devuelve enlazadas al grupo.



### Seguridad (permisos y alcance)

| Regla del spec (apartado «Seguridad») | Ubicación |
|---|---|
| Administrador: ve todos los centros, solo él da de alta, reenvía cualquiera; no modifica ni borra | Menú `sms-todos-menuitem` con `groups="admins"`; `Main-Sms.xml` sin `<domain>`; V-Sms-010/V-Sms-015 con rama explícita de administrador; V-Sms-012/V-Sms-013 |
| Supervisor y Administrativo: solo lectura de sus centros + reenvío | `Centro-Sms.xml` (`<domain>` por `CentroUsuario` con `SUPERVISOR`/`ADMINISTRATIVO`) + permiso `Sms.propio-centro-gestion` (solo `read`) + V-Sms-015 (autorización real; ver `decisiones.md` D5) |
| Cualquier usuario: solo lectura de los SMS ENVIADOS a su propio DNI, de cualquier centro | `Mis-Sms.xml` (`<domain>` por DNI y estado) + permiso `Sms.propio-destinatario` (solo `read`) |
| Nadie del grupo `users` puede crear, modificar ni borrar | Los dos permisos con `create/write/remove/export="false"`; `Main-Sms.xml` solo accesible desde un menú `groups="admins"` |

> **Nota del descomponedor:** se añade `k-secure-coding` porque las `condition` de los permisos son la defensa multi-centro/por destinatario del acceso por permisos.
