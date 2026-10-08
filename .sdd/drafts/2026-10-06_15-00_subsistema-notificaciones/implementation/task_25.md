---
type: implementation-task
template: system
---

# Tarea 25 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Main@Correo (form de alta/detalle del correo + modal de adjunto)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Main-Correo.xml` | Crear | k-vistas (forms.md, actions.md) | Form del correo en «Todas» (alta y detalle) + modal de adjunto |

**Instrucción de materialización (XML ya materializado):** el fichero ya está materializado en `design/views/Main-Correo.xml` (carpeta de la iniciativa). **MUST** copiarse literalmente a `src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Correo.xml`, sin regenerarlo, reescribirlo ni reformatearlo (`implementation.md` §1). La fila es `Acción: Crear`: si el destino ya existe, es `CONFLICT` (`implementation.md` §3).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 10 — Vistas (extracto)

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8), y `design/views/Ref-HistorialEstado.xml`, que se copia a `subsystem/expedientes/views/` (vista de solo lectura del módulo dueño de `HistorialEstado`, VAR-1.2(b); excepción acotada a README §4.2 autorizada por el usuario, D8).

**Mensajes de las acciones de validación local:** Axelor evalúa el `error` de un `<check>` de `<action-condition>` (**siempre**, aunque el `if` sea falso) y el `message` de un `<error>` de `<action-validate>` (cuando su `if` se cumple) como GString de Groovy (`ActionCondition`/`ActionValidate`, `toExpression(…, true)`).
Por eso, en esos textos una `\` literal MUST ir escapada como `\\` y un `$` literal como `\$`; si no, la acción falla en tiempo de ejecución (status -1) y el popup no se cierra.
Es el caso de `Main@Correo.Adjunto-Local-validateSave-action` («… los caracteres / \\ ni caracteres de control», que se muestra con una sola `\`).

**Un `<check>` por campo en cada `<action-condition>`:** `ActionCondition.evaluate` guarda un único error por campo (`errors.put(field, …)`, con `""` si el `if` es falso), así que con varios `<check>` sobre el mismo campo el último pisa a los anteriores y el front no bloquea.
Por eso `Main@Correo.Adjunto-Local-validateSave-action`, que tiene varias reglas sobre `nombreFichero` y sobre `contenido`, es un `<action-validate>` con un `<error>` por regla, que `ActionValidate` corta en el primero que se cumple: el orden de los `<error>` (obligatorio del nombre, obligatorio del contenido, `/` `\` y de control, 255, «.»/«..», fichero vacío) es la prioridad del aviso.
Los `Local-validateSave` de `Main@Correo` y `Main@Sms` sí son `<action-condition>`, porque tienen un solo `<check>` por campo.
Las vistas se copian verbatim, así que si `Main-Correo.xml` ya estaba materializado hay que volver a lanzar `/sdd-implementer` para rematerializarlo.

**`Main-Correo.xml`** — bloque `Main@Correo` (form + acciones) y bloque `Main@Correo.Adjunto` (grid + form modal). Form `Main@Correo-form` (`onNew` → `Remote-prepararAlta`; `canBackOnSave="true"` porque tiene `btnSave`). Acciones (en `<?sv-primary-actions?>`, primero los grupos de los botones en el orden del form y después los eventos): `btnCancel` (`close`), `btnSave` (`Local-validateSave` → `remote-validationSave-action` → `save` → `remote-refreshTab-action` → `close`), `btnReenviar` (`Remote-validateReenviar` → `Remote-reenviar` → `remote-refreshTab-action`), `onNew`, `onChange-centro` (→ `set-historialEstado-null`), `Local-validateSave` (8 obligatorios), `set-historialEstado-null`, `Remote-prepararAlta` (NotificacionController), `Remote-validateReenviar`, `Remote-reenviar` (CorreoController). Detalle: grid de adjuntos (`canEditOnClick`, «Añadir adjunto»), form modal con `btnCancel` (`close`), `btnSave` (`Local-validateSave` → `save-modal`), `btnDelete` (`delete-modal`), `onNew` (`set-correo-parent`) y un `Local-validateSave` (`<action-validate>`, no `<action-condition>`: un `<error>` por cada una de las 6 comprobaciones evaluables en cliente, en orden de prioridad, que se para en el primero que se cumple; ver la nota de arriba).

```
Datos del correo (alta y detalle; en detalle todo readonly por readonlyIf del panel):
  tttnnnnnnnnn   ← tipoNotificacion(3, readonly) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)          [el estado del expediente depende del centro]
                   historialEstado: domain por centro, target-name="nameState", canSuggest="false",
                   grid-view/form-view subsysExpedientes.Ref@HistorialEstado (D8): se elige en el buscador, que muestra el
                   número del expediente, el expediente, la fase, el estado y la fecha
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  ppppeeeeffff   ← para(4) + enCopia(4) + enCopiaOculta(4)
  ssssssssss··   ← asunto(10); el colOffset(2) de cuerpo completa la fila
  bbbbbbbbbbbb   ← cuerpo(12, colOffset 2, Text) → arranca la fila siguiente en la columna 1
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
  Edición: bb......ccgg   ← Borrar(2) + Cancelar(2, offset 6) + Guardar(2)
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
| V-Correo-001 | VAL-Correo-005 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Main@Correo-Local-validateSave-action` |
| V-Correo-006 | VAL-Correo-010 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Correo-009 | VAL-Correo-012 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Adjunto-002 | RES-Adjunto-002 | `AdjuntoServiceImpl.validateInsert`; cliente: `Main@Correo.Adjunto-Local-validateSave-action` |
| V-Adjunto-003 | RES-Adjunto-003 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal (texto de RUI-…-formulario-adjunto-003) |
| V-Adjunto-009 | VAL-Adjunto-004 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |
| V-Adjunto-010 | VAL-Adjunto-005 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |
| V-Adjunto-011 | VAL-Adjunto-006 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |
| V-Adjunto-012 | VAL-Adjunto-007 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |

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
| U-notificaciones-todas-018 | RUI-notificaciones-todas-formulario-adjunto-003 | `contenido` `required="true"` + `Local-validateSave` del modal («Debe adjuntar el fichero») |
| U-notificaciones-todas-019 | RUI-notificaciones-todas-formulario-adjunto-004 | Límite de subida de la plataforma `data.upload.max-size = 10` (D5, cambio de spec APLICADO) |
| U-notificaciones-todas-020 | RUI-notificaciones-todas-formulario-adjunto-005 | `Main@Correo.Adjunto-Local-validateSave-action` (regex `/[\/\\\p{Cntrl}]/`) |
| U-notificaciones-todas-021 | RUI-notificaciones-todas-formulario-adjunto-006 | Modal de adjunto: `btnDelete` solo en el panel `buttonsEdicion` (`(id != null) \|\| (cid != null)`) |

## Notas y supuestos (las que aplican a esta tarea)

  - Los forms de correos y SMS no se reutilizan tal cual cambiando solo el prefijo y el modelo: se re-maquetan para encajar los campos nuevos de la spec y para cumplir el ASCII Layout de `k-vistas/forms.md` (p. ej. `Mis@Sms-form`, `Centro@Correo-form` con `asunto` a colSpan 10), y sus botones cierran con `close` porque se abren en popup (D3).
- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.
- **Reenvío con el form en popup** (D3, resuelto al diseñar): `refresh-tab` despacha `tab:refresh` a la pestaña activa (el listado de debajo), no al popup, mientras que `setReload(true)` recarga el form del popup; por eso `CorreoController.reenviar`/`SmsController.reenviar` responden `setReload(true)` y el `btnReenviar` de `Main@` termina en `remote-refreshTab-action`, la misma acción que el alta (nota anterior); el de `Centro@` delega en él.
  Se resuelve en la vista: el `historialEstado` de `Main@Correo-form`/`Main@Sms-form` lleva, además del `domain` por centro, `target-name="nameState"`, `canSuggest="false"` y las vistas de referencia `subsysExpedientes.Ref@HistorialEstado-grid`/`-form` de `subsystem/expedientes/views/Ref-HistorialEstado.xml` (vista nueva de solo lectura en el módulo dueño de la entidad, por VAR-1.2(b); única excepción a README §4.2, autorizada por el usuario, D8); el de `Centro@Correo-form`/`Centro@Sms-form` lleva `target-name="nameState"` y las mismas vistas de referencia.
  El usuario elige en el buscador, que muestra número de expediente, expediente, fase, estado y fecha, y el campo enseña el estado («Pendiente de verificación»); la columna «Expediente» de los listados sigue saliendo de `nombreExpediente`.
