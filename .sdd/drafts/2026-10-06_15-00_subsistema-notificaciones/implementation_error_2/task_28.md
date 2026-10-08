---
type: implementation-task
template: system
---

# Tarea 28 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El fichero XML **ya está materializado** en `design/views/Main-Sms.xml` (validado con `xmllint` por el diseñador). **MUST** copiarse **literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Sms.xml`, **sin regenerarlo, reescribirlo ni reformatearlo** (ver `implementation.md` §1 y §3). Acción `Crear`: si el destino ya existe, se reporta `CONFLICT` según `implementation.md` §3.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Main-Sms.xml` | Crear | k-vistas (forms.md, actions.md) | Form del SMS en «Todas» (alta y detalle) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).

**`Main-Sms.xml`** — análogo a `Main-Correo.xml` sin adjuntos; `btnReenviar` añade `Local-confirmarReenvio` (alert) antes de la validación remota.

```
Datos del SMS:
  tttnnnnnnnnn   ← tipoNotificacion(3, readonly) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  tttt········   ← telefono(4, phone ES); el colOffset(8) de mensaje completa la fila
  mmmmmmmmmmmm   ← mensaje(12, Text)
Datos del envío y botones: idénticos a Main-Correo.xml
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-001 | RES-Notificacion-001 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Main@{Correo,Sms}-Local-validateSave-action` |
| V-Notificacion-003 | RES-Notificacion-014 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Local-validateSave` |
| V-Notificacion-005 | VAL-Correo-003, VAL-Sms-003 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Local-validateSave` |
| V-Notificacion-006 | VAL-Correo-004, VAL-Sms-004 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Local-validateSave` |
| V-Notificacion-009 | RES-Notificacion-013 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Local-validateSave` |
| V-Notificacion-014 | VAL-Notificacion-001 | `NotificacionCanalServiceImpl.validateReenviar`; expuesta por `CorreoController/SmsController.validateReenviar` (`Main@{Correo,Sms}-Remote-validateReenviar-action`) |
| V-Sms-001 | VAL-Sms-005 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Main@Sms-Local-validateSave-action` |
| V-Sms-003 | VAL-Sms-007 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-todas-022 | RUI-notificaciones-todas-formulario-sms-001 | `Main@Sms-form` `tipoNotificacion` `readonly` + `onNew` → `Main@Sms-Remote-prepararAlta-action` |
| U-notificaciones-todas-023 | RUI-notificaciones-todas-formulario-sms-002 | `Main@Sms-form` panel `Sms` `readonlyIf` |
| U-notificaciones-todas-024 | RUI-notificaciones-todas-formulario-sms-003 | `Main@Sms-form` panel `Envio` `showIf` |
| U-notificaciones-todas-025 | RUI-notificaciones-todas-formulario-sms-004 | `Main@Sms-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-todas-026 | RUI-notificaciones-todas-formulario-sms-005 | `Main@Sms-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-todas-027 | RUI-notificaciones-todas-formulario-sms-006 | `Main@Sms-form` paneles `buttonsAlta` / `buttonsFallido` / `buttonsDetalle` |
| U-notificaciones-todas-028 | RUI-notificaciones-todas-formulario-sms-007 | `Main@Sms-form` panel `buttonsFallido` |
| U-notificaciones-todas-029 | RUI-notificaciones-todas-formulario-sms-008 | `Main@Sms-Local-validateSave-action` en `Main@Sms-btnSave-action` |
| U-notificaciones-todas-030 | RUI-notificaciones-todas-formulario-sms-009 | `Main@Sms-form` `telefono` `widget="phone" x-only-countries="ES"` |
| U-notificaciones-todas-031 | RUI-notificaciones-todas-formulario-sms-010 | `SmsController.reenviar` → `setNotify` |
| U-notificaciones-todas-032 | RUI-notificaciones-todas-formulario-sms-011 | `Main@Sms-form` `historialEstado` `domain="self.expediente.centro = :centro"` |
| U-notificaciones-todas-033 | RUI-notificaciones-todas-formulario-sms-012 | `Main@Sms-onChange-centro-action` → `Main@Sms-set-historialEstado-null-action` |
| U-notificaciones-todas-034 | RUI-notificaciones-todas-formulario-sms-013 | `Main@Sms-form` `requiredIf` en motivo, centro, DNI, nombre, apellidos, teléfono, mensaje |
| U-notificaciones-todas-035 | RUI-notificaciones-todas-formulario-sms-014 | `Main@Sms-Local-confirmarReenvio-action` (alert) en `Main@Sms-btnReenviar-action` |
