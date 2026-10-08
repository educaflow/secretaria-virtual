---
type: implementation-task
template: system
---

# Tarea 29 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Main-Correo.xml` | Crear | k-vistas (forms.md, actions.md) | Form del correo en «Todas» (alta y detalle) + modal de adjunto |

**Materialización (XML ya materializado):** el fichero está en `design/views/Main-Correo.xml` (dentro de la carpeta de la iniciativa) y se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Correo.xml`, **sin regenerarlo** ni reformatearlo (contrato de materialización, `implementation.md` §1 y §3).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Diseño — Paso 10

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).

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
                   grid-view/form-view Ref@HistorialEstado (D8): se elige en el buscador, que muestra el
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

## Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

### V

| ID | Origen spec | Ubicación |
|---|---|---|
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

## Notas y supuestos (extracto)

- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.
