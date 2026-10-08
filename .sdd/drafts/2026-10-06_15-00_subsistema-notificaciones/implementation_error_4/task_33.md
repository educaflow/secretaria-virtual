---
type: implementation-task
template: system
---

# Tarea 33 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Centro-Sms.xml` | Crear | k-vistas (forms.md) | Form del SMS en «Del centro» |

**Materialización (XML ya materializado):** el fichero está en `design/views/Centro-Sms.xml` (dentro de la carpeta de la iniciativa) y se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Sms.xml`, **sin regenerarlo** ni reformatearlo (contrato de materialización, `implementation.md` §1 y §3).

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

**`Centro-Sms.xml`** — form `Centro@Sms-form` de solo lectura; `Centro@Sms-btnReenviar-action` contiene una sola acción, el grupo `Main@Sms-btnReenviar-action` (confirmación, validación, reenvío y `remote-refreshTab-action` viven solo en `Main@Sms`); Salir `close`.

```
Datos del SMS (readonly; historialEstado(8, target-name="nameState", grid-view/form-view Ref@HistorialEstado; D8) al borde derecho con showIf="historialEstado != null"):
  tttnnnnnnnnn   ← tipoNotificacion(3) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)                          [ligada a un estado]
  cccc········   ← historialEstado oculto: hueco al borde derecho, nada se desplaza  [sin estado]
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  tttt········   ← telefono(4, phone ES); el colOffset(8) de mensaje completa la fila
  mmmmmmmmmmmm   ← mensaje(12, Text)
Datos del envío y botones: idénticos a Centro-Correo.xml
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-008 | RUI-notificaciones-centro-formulario-sms-001 | `Centro@Sms-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-centro-009 | RUI-notificaciones-centro-formulario-sms-002 | `Centro@Sms-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-centro-010 | RUI-notificaciones-centro-formulario-sms-003 | `Centro@Sms-form` `btnReenviar` `showIf` FALLIDO |
| U-notificaciones-centro-012 | RUI-notificaciones-centro-formulario-sms-005 | `Main@Sms-Local-confirmarReenvio-action` (alert) vía `Main@Sms-btnReenviar-action`, al que delega `Centro@Sms-btnReenviar-action` |
| U-notificaciones-centro-013 | RUI-notificaciones-centro-formulario-sms-006 | `Centro@Sms-form` panel `Intentos` `showIf="estado != 'PENDIENTE'"` |
| U-notificaciones-centro-014 | RUI-notificaciones-centro-formulario-sms-007 | `Centro@Sms-form` `historialEstado` `showIf="historialEstado != null"` |
