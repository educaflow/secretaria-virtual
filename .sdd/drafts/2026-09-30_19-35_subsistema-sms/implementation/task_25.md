---
type: implementation-task
template: system
---

# Tarea 25 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir el menú «SMS» (order 55, justo tras «Correos») |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño. Los `i18n_*.csv` **MUST NOT** crearse a mano.

La porción de menú ya está **materializada** en `design/menus.xml`. Acción `Modificar`: el destino `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` **ya existe**; se **fusionan** (insertan) los `<menuitem>` de `design/menus.xml` justo antes de `</object-views>`, **sin regenerarlos**, conservando todo lo que ya hay, y se valida el resultado con `xmllint` (`implementation.md` §3). Si ya existe un `<menuitem>` con el mismo `name`, es `CONFLICT`.

### Paso 9 — Menú

**Fichero:** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (Modificar) — la porción a fusionar está en `design/menus.xml`.

- Raíz `sms-menuitem` («SMS», `groups="admins,users"`, `order="55"`: entre «Correos» (50) y «Mi centro» (60), sin renumerar nada).
- Hojas: `sms-recibidos-menuitem` → `subsysSms.Mis@Sms-action` (`admins,users`), `sms-delCentro-menuitem` → `subsysSms.Centro@Sms-action` (`users`), `sms-todos-menuitem` → `subsysSms.Main@Sms-action` (`admins`).
- **MUST NOT** crear un `menus-sms.xml`; **MUST NOT** escribir el atributo `if` (lo pone el preprocesador); **MUST NOT** añadir ningún `case` en `MenuSecurityServiceImpl` (ver «Notas y supuestos»).

**Verificar:** con `admin` se ven «Recibidos» y «Todos» y no «Del centro»; con `supervisor1@mislata.es` se ven «Recibidos» y «Del centro» y no «Todos» (ESC-022); con `alumno1@mislata.es` se ven «Recibidos» y «Del centro» (esta última vacía, ESC-025).



### Seguridad (permisos y alcance)

| Regla del spec (apartado «Seguridad») | Ubicación |
|---|---|
| Administrador: ve todos los centros, solo él da de alta, reenvía cualquiera; no modifica ni borra | Menú `sms-todos-menuitem` con `groups="admins"`; `Main-Sms.xml` sin `<domain>`; V-Sms-010/V-Sms-015 con rama explícita de administrador; V-Sms-012/V-Sms-013 |



3. **El menú «SMS → Del centro» lo ven todos los `users`, a propósito**: ver `decisiones.md` D5.
