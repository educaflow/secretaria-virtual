---
type: implementation-task
template: system
---

# Tarea 25 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir el menú «SMS» (order 55, justo tras «Correos») |

**XML ya materializado:** la porción a fusionar está en `design/menus.xml`. Sus `<menuitem>` se **insertan** (sin regenerarlos) en el `menus.xml` único del proyecto, `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`, justo antes de `</object-views>`, y después se valida con `xmllint` (`implementation.md` §3). La fila es `Acción: Modificar`: el destino **ya existe** y **MUST** conservarse todo su contenido actual (comprobación de conservación de `implementation.md` §3; aquí solo se añaden `<menuitem>`, no se quita ninguno).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño. Los `i18n_*.csv` **MUST NOT** crearse a mano.

### Paso 9 — Menú (de `design/design.md`)

**Fichero:** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (Modificar) — la porción a fusionar está en `design/menus.xml`.

- Raíz `sms-menuitem` («SMS», `groups="admins,users"`, `order="55"`: entre «Correos» (50) y «Mi centro» (60), sin renumerar nada).
- Hojas: `sms-recibidos-menuitem` → `subsysSms.Mis@Sms-action` (`admins,users`), `sms-delCentro-menuitem` → `subsysSms.Centro@Sms-action` (`users`), `sms-todos-menuitem` → `subsysSms.Main@Sms-action` (`admins`).
- **MUST NOT** crear un `menus-sms.xml`; **MUST NOT** escribir el atributo `if` (lo pone el preprocesador); **MUST NOT** añadir ningún `case` en `MenuSecurityServiceImpl` (ver «Notas y supuestos»).

**Verificar:** con `admin` se ven «Recibidos» y «Todos» y no «Del centro»; con `supervisor1@mislata.es` se ven «Recibidos» y «Del centro» y no «Todos» (ESC-022); con `alumno1@mislata.es` se ven «Recibidos» y «Del centro» (esta última vacía, ESC-025).

### Notas y supuestos 3 (de `design/design.md`)

3. **El menú «SMS → Del centro» no se oculta a nadie del grupo `users`**, a diferencia del de correos: ESC-025 hace que un alumno lo abra y vea el listado vacío, y `screen-sms-centro.md` dice «solo muestra **datos** a quien es Supervisor o Administrativo». Por eso **no** se añade ningún `case` a `MenuSecurityServiceImpl` (`subsystem/security` no se toca). La seguridad real no depende del menú: la ponen el `<domain>` del `action-view` y el permiso `Sms.propio-centro-gestion`. Ver `decisiones.md` D5.

### Trazabilidad — Seguridad (de `design/design.md`)

| Regla del spec (apartado «Seguridad») | Ubicación |
|---|---|
| Administrador: ve todos los centros, solo él da de alta, reenvía cualquiera; no modifica ni borra | Menú `sms-todos-menuitem` con `groups="admins"`; `Main-Sms.xml` sin `<domain>`; V-Sms-010/V-Sms-015 con rama explícita de administrador; V-Sms-012/V-Sms-013 |
| Supervisor y Administrativo: solo lectura de sus centros + reenvío | `Centro-Sms.xml` (`<domain>` por `CentroUsuario` con `SUPERVISOR`/`ADMINISTRATIVO`) + permiso `Sms.propio-centro-gestion` (solo `read`) + **V-Sms-015, que es la autorización real**: exige ser `SUPERVISOR` o `ADMINISTRATIVO` en el centro del SMS (o ser Administrador), no la mera pertenencia al centro. El `<domain>` y el permiso son filtro de lectura, no defensa de la acción |
| Cualquier usuario: solo lectura de los SMS ENVIADOS a su propio DNI, de cualquier centro | `Mis-Sms.xml` (`<domain>` por DNI y estado) + permiso `Sms.propio-destinatario` (solo `read`) |
| Nadie del grupo `users` puede crear, modificar ni borrar | Los dos permisos con `create/write/remove/export="false"`; `Main-Sms.xml` solo accesible desde un menú `groups="admins"` |
