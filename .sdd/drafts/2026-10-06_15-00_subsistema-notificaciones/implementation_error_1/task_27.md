---
type: implementation-task
template: system
---

# Tarea 27 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Main-Correo.xml

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Main-Correo.xml` | Crear | k-vistas (forms.md, actions.md) | Form del correo en «Todas» (alta y detalle) + modal de adjunto |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/views/Main-Correo.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Correo.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1).

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

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con `VAR-7.2` ya ampliada).

## Trazabilidad Origen spec → V/R/U → ubicación

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-001 | RES-Notificacion-001 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Main@{Correo,Sms}-Local-validateSave-action` |
| V-Notificacion-014 | VAL-Notificacion-001 | `NotificacionCanalServiceImpl.validateReenviar`; expuesta por `CorreoController/SmsController.validateReenviar` (`Main@{Correo,Sms}-Remote-validateReenviar-action`) |
| V-Correo-001 | VAL-Correo-005 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Main@Correo-Local-validateSave-action` |
| V-Adjunto-002 | RES-Adjunto-002 | `AdjuntoServiceImpl.validateInsert`; cliente: `Main@Correo.Adjunto-Local-validateSave-action` |

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-todas-003 | RUI-notificaciones-todas-formulario-correo-001 | `Main@Correo-form` `tipoNotificacion` `readonly` + `onNew` → `Main@Correo-Remote-prepararAlta-action` (`NotificacionController.prepararAlta`) |
| U-notificaciones-todas-004 | RUI-notificaciones-todas-formulario-correo-002 | `Main@Correo-form` panel `Correo` `readonlyIf="(id != null) \|\| (cid != null)"` |
| U-notificaciones-todas-005 | RUI-notificaciones-todas-formulario-correo-003 | `Main@Correo-form` panel `Envio` `showIf="(id != null) \|\| (cid != null)"` |
| U-notificaciones-todas-006 | RUI-notificaciones-todas-formulario-correo-004 | `Main@Correo-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-todas-007 | RUI-notificaciones-todas-formulario-correo-005 | `Main@Correo-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-todas-008 | RUI-notificaciones-todas-formulario-correo-006 | `Main@Correo-form` paneles `buttonsAlta` / `buttonsFallido` / `buttonsDetalle` |
| U-notificaciones-todas-009 | RUI-notificaciones-todas-formulario-correo-007 | `Main@Correo-form` panel `buttonsFallido` (`btnReenviar`) |
| U-notificaciones-todas-010 | RUI-notificaciones-todas-formulario-correo-008 | `Main@Correo-Local-validateSave-action` en `Main@Correo-btnSave-action` |
| U-notificaciones-todas-012 | RUI-notificaciones-todas-formulario-correo-010 | `Main@Correo-form` `historialEstado` `domain="self.expediente.centro = :centro"` |
| U-notificaciones-todas-013 | RUI-notificaciones-todas-formulario-correo-011 | `Main@Correo-onChange-centro-action` → `Main@Correo-set-historialEstado-null-action` |
| U-notificaciones-todas-014 | RUI-notificaciones-todas-formulario-correo-012 | `Main@Correo-form` `requiredIf="(id == null) && (cid == null)"` en motivo, centro, DNI, nombre, apellidos, para, asunto, cuerpo |
| U-notificaciones-todas-015 | RUI-notificaciones-todas-listado-adjuntos-001 | `Main@Correo-form`: `adjuntos` (con «Añadir adjunto») solo en alta y `adjuntosConsulta` (`canNew="false"`) en detalle |
| U-notificaciones-todas-016 | RUI-notificaciones-todas-formulario-adjunto-001 | `Main@Correo.Adjunto-onNew-action` → `set-correo-parent-action` |
| U-notificaciones-todas-017 | RUI-notificaciones-todas-formulario-adjunto-002 | `nombreFichero` `required="true"` + `Main@Correo.Adjunto-Local-validateSave-action` |
| U-notificaciones-todas-020 | RUI-notificaciones-todas-formulario-adjunto-005 | `Main@Correo.Adjunto-Local-validateSave-action` (regex `/[\/\\\p{Cntrl}]/`) |

## Notas y supuestos

- **Selector del estado del expediente**: se conserva el campo tal como estaba en correos/SMS (sin vista de referencia: `HistorialEstado` vive en `expedientes`, que este diseño no puede tocar); solo se le añade el `domain` por centro.
