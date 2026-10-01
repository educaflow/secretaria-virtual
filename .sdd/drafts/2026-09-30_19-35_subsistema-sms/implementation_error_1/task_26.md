---
type: implementation-task
template: system
---

# Tarea 26 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de binding de los permisos del subsistema (`priority="10"`) |
| `src/main/java/com/educaflow/subsystem/sms/data-init/input/auth-sms.xml` | Crear | k-datainit | Permisos condicionales `Sms.propio-destinatario` y `Sms.propio-centro-gestion` |

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

### Trazabilidad — Seguridad (de `design/design.md`)

| Regla del spec (apartado «Seguridad») | Ubicación |
|---|---|
| Administrador: ve todos los centros, solo él da de alta, reenvía cualquiera; no modifica ni borra | Menú `sms-todos-menuitem` con `groups="admins"`; `Main-Sms.xml` sin `<domain>`; V-Sms-010/V-Sms-015 con rama explícita de administrador; V-Sms-012/V-Sms-013 |
| Supervisor y Administrativo: solo lectura de sus centros + reenvío | `Centro-Sms.xml` (`<domain>` por `CentroUsuario` con `SUPERVISOR`/`ADMINISTRATIVO`) + permiso `Sms.propio-centro-gestion` (solo `read`) + **V-Sms-015, que es la autorización real**: exige ser `SUPERVISOR` o `ADMINISTRATIVO` en el centro del SMS (o ser Administrador), no la mera pertenencia al centro. El `<domain>` y el permiso son filtro de lectura, no defensa de la acción |
| Cualquier usuario: solo lectura de los SMS ENVIADOS a su propio DNI, de cualquier centro | `Mis-Sms.xml` (`<domain>` por DNI y estado) + permiso `Sms.propio-destinatario` (solo `read`) |
| Nadie del grupo `users` puede crear, modificar ni borrar | Los dos permisos con `create/write/remove/export="false"`; `Main-Sms.xml` solo accesible desde un menú `groups="admins"` |

### Decisión del descomponedor (no es texto del diseño)

`input-config.xml` y `input/auth-sms.xml` van juntos (el manifiesto de binding y el único fichero que enlaza: no tienen sentido por separado). El enlace en el `auth.xml` global es una tarea aparte. Tomar como modelo de formato el `data-init` del subsistema de correos, tal como cita el diseño («como el de correos»).
