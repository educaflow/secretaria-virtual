---
type: implementation-task
template: system
---

# Tarea 29 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El fichero XML **ya está materializado** en `design/views/Centro-Sms.xml` (validado con `xmllint` por el diseñador). **MUST** copiarse **literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Sms.xml`, **sin regenerarlo, reescribirlo ni reformatearlo** (ver `implementation.md` §1 y §3). Acción `Crear`: si el destino ya existe, se reporta `CONFLICT` según `implementation.md` §3.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Centro-Sms.xml` | Crear | k-vistas (forms.md) | Form del SMS en «Del centro» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).

**`Centro-Sms.xml`** — form `Centro@Sms-form` de solo lectura; `Centro@Sms-btnReenviar-action` contiene una sola acción, el grupo `Main@Sms-btnReenviar-action` (confirmación, validación, reenvío y `remote-refreshTab-action` viven solo en `Main@Sms`); Salir `close`.

```
Datos del SMS (readonly; historialEstado(8) al borde derecho con showIf="historialEstado != null"):
  tttnnnnnnnnn   ← tipoNotificacion(3) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)                          [ligada a un estado]
  cccc········   ← historialEstado oculto: hueco al borde derecho, nada se desplaza  [sin estado]
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  tttt········   ← telefono(4, phone ES); el colOffset(8) de mensaje completa la fila
  mmmmmmmmmmmm   ← mensaje(12, Text)
Datos del envío y botones: idénticos a Centro-Correo.xml

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-008 | RUI-notificaciones-centro-formulario-sms-001 | `Centro@Sms-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-centro-009 | RUI-notificaciones-centro-formulario-sms-002 | `Centro@Sms-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-centro-010 | RUI-notificaciones-centro-formulario-sms-003 | `Centro@Sms-form` `btnReenviar` `showIf` FALLIDO |
| U-notificaciones-centro-011 | RUI-notificaciones-centro-formulario-sms-004 | `SmsController.reenviar` → `setNotify` |
| U-notificaciones-centro-012 | RUI-notificaciones-centro-formulario-sms-005 | `Main@Sms-Local-confirmarReenvio-action` (alert) vía `Main@Sms-btnReenviar-action`, al que delega `Centro@Sms-btnReenviar-action` |
| U-notificaciones-centro-013 | RUI-notificaciones-centro-formulario-sms-006 | `Centro@Sms-form` panel `Intentos` `showIf="estado != 'PENDIENTE'"` |
| U-notificaciones-centro-014 | RUI-notificaciones-centro-formulario-sms-007 | `Centro@Sms-form` `historialEstado` `showIf="historialEstado != null"` |
