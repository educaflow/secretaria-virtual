---
type: implementation-task
template: system
---

# Tarea 30 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Main-Sms.xml

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Main-Sms.xml` | Crear | k-vistas (forms.md, actions.md) | Form del SMS en «Todas» (alta y detalle) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/views/Main-Sms.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Sms.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1).

> Contexto: el bloque de la vista de correo al que el diseño remite («análogo a» / «idénticos a»):

**`Main-Correo.xml`** — bloque `Main@Correo` (form + acciones) y bloque `Main@Correo.Adjunto` (grid + form modal). Form `Main@Correo-form` (`onNew` → `Remote-prepararAlta`; `canBackOnSave="true"` porque tiene `btnSave`). Acciones (en `<?sv-primary-actions?>`, primero los grupos de los botones en el orden del form y después los eventos): `btnCancel` (`close`), `btnSave` (`Local-validateSave` → `remote-validationSave-action` → `save` → `close`), `btnReenviar` (`Remote-validateReenviar` → `Remote-reenviar`), `onNew`, `onChange-centro` (→ `set-historialEstado-null`), `Local-validateSave` (8 obligatorios), `set-historialEstado-null`, `Remote-prepararAlta`, `Remote-validateReenviar`, `Remote-reenviar` (CorreoController). Detalle: grid de adjuntos (`canEditOnClick`, «Añadir adjunto»), form modal con `btnCancel` (`close`), `btnSave` (`Local-validateSave` → `save-modal`), `btnDelete` (`delete-modal`), `onNew` (`set-correo-parent`) y un `Local-validateSave` con las 6 comprobaciones evaluables en cliente.

```
Datos del correo (alta y detalle; en detalle todo readonly por readonlyIf del panel):
  tttnnnnnnnnn   ← tipoNotificacion(3, readonly) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)          [el estado del expediente depende del centro]
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  ppppeeeeffff   ← para(4) + enCopia(4) + enCopiaOculta(4)
  ssssssssssss   ← asunto(12)                               [texto de hasta 255, fila propia sobre el cuerpo]
  bbbbbbbbbbbb   ← cuerpo(12, Text)
Adjuntos: panel-related «adjuntos» (alta, con «Añadir adjunto») / «adjuntosConsulta» (detalle, Ref) — excluyentes
Datos del envío (solo detalle):
  eeeeeerrrccc   ← estado(6, RadioSelect) + numeroReintentos(3) + fechaCreacion(3)
  pppuuu...fff   ← fechaPrimerIntento(3) + fechaUltimoIntento(3) + fechaEnvio(3, offset 3) [ENVIADO]
  pppuuu......   ← ídem con fechaEnvio oculta al borde derecho                            [PENDIENTE/FALLIDO]
  dddddddddddd   ← descripcionUltimoFallo(12, Text)                                       [solo FALLIDO]
Botones (paneles de estado excluyentes):
  Alta:          ........ccgg   ← Cancelar(2, offset 8) + Guardar(2)
  Detalle FALL.: rr........ss   ← Reenviar(2) + Salir(2, offset 8)
  Detalle resto: ..........ss   ← Salir(2, offset 10)
Modal de adjunto:
  nnnnnnnnffff   ← nombreFichero(8) + contenido(4, binary-link)   (correo oculto)
  Alta:    ........ccgg   ← Cancelar(2, offset 8) + Guardar(2)
  Edición: bb......ssgg   ← Borrar(2) + Salir(2, offset 6) + Guardar(2)
```

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

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con `VAR-7.2` ya ampliada).

## Trazabilidad Origen spec → V/R/U → ubicación

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-001 | RES-Notificacion-001 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Main@{Correo,Sms}-Local-validateSave-action` |
| V-Notificacion-014 | VAL-Notificacion-001 | `NotificacionCanalServiceImpl.validateReenviar`; expuesta por `CorreoController/SmsController.validateReenviar` (`Main@{Correo,Sms}-Remote-validateReenviar-action`) |
| V-Sms-001 | VAL-Sms-005 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Main@Sms-Local-validateSave-action` |

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-012 | RUI-notificaciones-centro-formulario-sms-005 | `Main@Sms-Local-confirmarReenvio-action` (alert) en `Centro@Sms-btnReenviar-action` |
| U-notificaciones-todas-022 | RUI-notificaciones-todas-formulario-sms-001 | `Main@Sms-form` `tipoNotificacion` `readonly` + `onNew` → `Main@Sms-Remote-prepararAlta-action` |
| U-notificaciones-todas-023 | RUI-notificaciones-todas-formulario-sms-002 | `Main@Sms-form` panel `Sms` `readonlyIf` |
| U-notificaciones-todas-024 | RUI-notificaciones-todas-formulario-sms-003 | `Main@Sms-form` panel `Envio` `showIf` |
| U-notificaciones-todas-025 | RUI-notificaciones-todas-formulario-sms-004 | `Main@Sms-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-todas-026 | RUI-notificaciones-todas-formulario-sms-005 | `Main@Sms-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-todas-027 | RUI-notificaciones-todas-formulario-sms-006 | `Main@Sms-form` paneles `buttonsAlta` / `buttonsFallido` / `buttonsDetalle` |
| U-notificaciones-todas-028 | RUI-notificaciones-todas-formulario-sms-007 | `Main@Sms-form` panel `buttonsFallido` |
| U-notificaciones-todas-029 | RUI-notificaciones-todas-formulario-sms-008 | `Main@Sms-Local-validateSave-action` en `Main@Sms-btnSave-action` |
| U-notificaciones-todas-030 | RUI-notificaciones-todas-formulario-sms-009 | `Main@Sms-form` `telefono` `widget="phone" x-only-countries="ES"` |
| U-notificaciones-todas-032 | RUI-notificaciones-todas-formulario-sms-011 | `Main@Sms-form` `historialEstado` `domain="self.expediente.centro = :centro"` |
| U-notificaciones-todas-033 | RUI-notificaciones-todas-formulario-sms-012 | `Main@Sms-onChange-centro-action` → `Main@Sms-set-historialEstado-null-action` |
| U-notificaciones-todas-034 | RUI-notificaciones-todas-formulario-sms-013 | `Main@Sms-form` `requiredIf` en motivo, centro, DNI, nombre, apellidos, teléfono, mensaje |
| U-notificaciones-todas-035 | RUI-notificaciones-todas-formulario-sms-014 | `Main@Sms-Local-confirmarReenvio-action` (alert) en `Main@Sms-btnReenviar-action` |

## Notas y supuestos

- **Selector del estado del expediente**: se conserva el campo tal como estaba en correos/SMS (sin vista de referencia: `HistorialEstado` vive en `expedientes`, que este diseño no puede tocar); solo se le añade el `domain` por centro.
