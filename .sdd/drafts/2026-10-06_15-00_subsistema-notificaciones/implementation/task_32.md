---
type: implementation-task
template: system
---

# Tarea 32 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Centro@Notificacion («Del centro»)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Centro-Notificacion.xml` | Crear | k-vistas (grids.md, actions.md) | «Del centro» |

**Instrucción de materialización (XML ya materializado):** el fichero ya está materializado en `design/views/Centro-Notificacion.xml` (carpeta de la iniciativa). **MUST** copiarse literalmente a `src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Notificacion.xml`, sin regenerarlo, reescribirlo ni reformatearlo (`implementation.md` §1). La fila es `Acción: Crear`: si el destino ya existe, es `CONFLICT` (`implementation.md` §3).

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

**`Centro-Notificacion.xml`** — `action-view` con `<domain>self.centro.id IN (:idsCentrosGestionados)</domain>` y `<context … expr="eval: com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil.idsCentrosGestionados(__user__)"/>` (la misma expresión que los `conditionParams` de los permisos; la clase es `@ScriptAllowed`). Grid con columnas centro, tipo, estado, DNI, nombre, apellidos, motivo, destino, expediente, fechas; `hilite` `danger` si FALLIDO; `action` = `Remote-abrirDelCentro`.

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-001 | RUI-notificaciones-centro-listado-001 | `Centro@Notificacion-grid` `<hilite color="danger" if="estado == 'FALLIDO'">` |
