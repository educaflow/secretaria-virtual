---
type: implementation-task
template: system
---

# Tarea 30 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Centro-Correo.xml` | Crear | k-vistas (forms.md) | Form del correo en «Del centro» |

**Materialización (XML ya materializado):** el fichero está en `design/views/Centro-Correo.xml` (dentro de la carpeta de la iniciativa) y se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Correo.xml`, **sin regenerarlo** ni reformatearlo (contrato de materialización, `implementation.md` §1 y §3).

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

**`Centro-Correo.xml`** — form `Centro@Correo-form` de solo lectura; `Centro@Correo-btnReenviar-action` contiene una sola acción, el grupo `Main@Correo-btnReenviar-action` (la secuencia del reenvío, con su `remote-refreshTab-action`, vive solo en `Main@Correo`); Salir `close`.

```
Datos del correo: igual que Main, con historialEstado(8, target-name="nameState", grid-view/form-view Ref@HistorialEstado; D8) al borde derecho y showIf="historialEstado != null"
  cccchhhhhhhh   [ligada a un estado]     cccc········   [sin estado: hueco al borde derecho, nada se desplaza]
Datos del envío:
  eeeeeerrrccc   ← estado(6) + numeroReintentos(3) + fechaCreacion(3)
  panel «Intentos» (showIf estado != PENDIENTE; grupo condicional → panel propio):
    pppuuu...fff [ENVIADO]    pppuuu...... [FALLIDO]    (panel oculto) [PENDIENTE]
  dddddddddddd   ← descripcionUltimoFallo(12) [solo FALLIDO]
Botones:  rr........ss   ← Reenviar(2, showIf FALLIDO, primero y sin offset) + Salir(2, offset 8)  [FALLIDO]
          ..........ss   ← (Reenviar oculto al borde izquierdo) + Salir                          [resto]
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-002 | RUI-notificaciones-centro-formulario-correo-001 | `Centro@Correo-form` `fechaEnvio` `showIf="estado == 'ENVIADO'"` |
| U-notificaciones-centro-003 | RUI-notificaciones-centro-formulario-correo-002 | `Centro@Correo-form` `descripcionUltimoFallo` `showIf="estado == 'FALLIDO'"` |
| U-notificaciones-centro-004 | RUI-notificaciones-centro-formulario-correo-003 | `Centro@Correo-form` `btnReenviar` `showIf="estado == 'FALLIDO'"` |
| U-notificaciones-centro-006 | RUI-notificaciones-centro-formulario-correo-005 | `Centro@Correo-form` panel `Intentos` `showIf="estado != 'PENDIENTE'"` |
| U-notificaciones-centro-007 | RUI-notificaciones-centro-formulario-correo-006 | `Centro@Correo-form` `historialEstado` `showIf="historialEstado != null"` |
