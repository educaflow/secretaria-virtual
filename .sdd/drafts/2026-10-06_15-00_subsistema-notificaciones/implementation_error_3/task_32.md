---
type: implementation-task
template: system
---

# Tarea 32 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Main-Sms

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Main-Sms.xml` | Crear | k-vistas (forms.md, actions.md) | Form del SMS en «Todas» (alta y detalle) |

**Instrucción de materialización (XML ya materializado):** el fichero está ya materializado en `design/views/Main-Sms.xml` (validado con `xmllint` por el diseñador). Se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Sms.xml`, **sin regenerarlo, reescribirlo ni modificarlo** (ver `implementation.md` §1 y §3).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 10 (extracto verbatim del design.md: introducción común y la vista de esta tarea)

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).

**Mensajes de las acciones de validación local:** Axelor evalúa el `error` de un `<check>` de `<action-condition>` (**siempre**, aunque el `if` sea falso) y el `message` de un `<error>` de `<action-validate>` (cuando su `if` se cumple) como GString de Groovy (`ActionCondition`/`ActionValidate`, `toExpression(…, true)`).
Por eso, en esos textos una `\` literal MUST ir escapada como `\\` y un `$` literal como `\$`; si no, la acción falla en tiempo de ejecución (status -1) y el popup no se cierra.
Es el caso de `Main@Correo.Adjunto-Local-validateSave-action` («… los caracteres / \\ ni caracteres de control», que se muestra con una sola `\`).

**Un `<check>` por campo en cada `<action-condition>`:** `ActionCondition.evaluate` guarda un único error por campo (`errors.put(field, …)`, con `""` si el `if` es falso), así que con varios `<check>` sobre el mismo campo el último pisa a los anteriores y el front no bloquea.
Por eso `Main@Correo.Adjunto-Local-validateSave-action`, que tiene varias reglas sobre `nombreFichero` y sobre `contenido`, es un `<action-validate>` con un `<error>` por regla, que `ActionValidate` corta en el primero que se cumple: el orden de los `<error>` (obligatorio del nombre, obligatorio del contenido, `/` `\` y de control, 255, «.»/«..», fichero vacío) es la prioridad del aviso.
Los `Local-validateSave` de `Main@Correo` y `Main@Sms` sí son `<action-condition>`, porque tienen un solo `<check>` por campo.
Las vistas se copian verbatim, así que si `Main-Correo.xml` ya estaba materializado hay que volver a lanzar `/sdd-implementer` para rematerializarlo.

**`Main-Sms.xml`** — análogo a `Main-Correo.xml` sin adjuntos; `btnReenviar` añade `Local-confirmarReenvio` (alert) antes de la validación remota.

```
Datos del SMS:
  tttnnnnnnnnn   ← tipoNotificacion(3, readonly) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  tttt········   ← telefono(4, phone ES); el colOffset(8) de mensaje completa la fila
  mmmmmmmmmmmm   ← mensaje(12, Text)
Datos del envío y botones: idénticos a Main-Correo.xml

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

### Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

#### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Sms-001 | VAL-Sms-005 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Main@Sms-Local-validateSave-action` |

#### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-012 | RUI-notificaciones-centro-formulario-sms-005 | `Main@Sms-Local-confirmarReenvio-action` (alert) vía `Main@Sms-btnReenviar-action`, al que delega `Centro@Sms-btnReenviar-action` |
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

### Notas del design.md (`## Notas y supuestos`)

  - Los forms de correos y SMS no se reutilizan tal cual cambiando solo el prefijo y el modelo: se re-maquetan para encajar los campos nuevos de la spec y para cumplir el ASCII Layout de `k-vistas/forms.md` (p. ej. `Mis@Sms-form`, `Centro@Correo-form` con `asunto` a colSpan 10), y sus botones cierran con `close` porque se abren en popup (D3).
- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.
- **Reenvío con el form en popup** (D3, resuelto al diseñar): `refresh-tab` despacha `tab:refresh` a la pestaña activa (el listado de debajo), no al popup, mientras que `setReload(true)` recarga el form del popup; por eso `CorreoController.reenviar`/`SmsController.reenviar` responden `setReload(true)` y el `btnReenviar` de `Main@` termina en `remote-refreshTab-action`, la misma acción que el alta (nota anterior); el de `Centro@` delega en él.
- **Selector del estado del expediente**: se conserva el campo tal como estaba en correos/SMS (sin vista de referencia: `HistorialEstado` vive en `expedientes`, que este diseño no puede tocar); solo se le añade el `domain` por centro.
