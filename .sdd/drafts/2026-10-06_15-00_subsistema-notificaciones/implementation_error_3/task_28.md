---
type: implementation-task
template: system
---

# Tarea 28 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Eleccion-Notificacion

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Eleccion-Notificacion.xml` | Crear | k-vistas (forms.md, actions.md) | Elección de canal (popup) |

**Instrucción de materialización (XML ya materializado):** el fichero está ya materializado en `design/views/Eleccion-Notificacion.xml` (validado con `xmllint` por el diseñador). Se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/notificaciones/views/Eleccion-Notificacion.xml`, **sin regenerarlo, reescribirlo ni modificarlo** (ver `implementation.md` §1 y §3).

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

**`Eleccion-Notificacion.xml`** — `action-view` de form en popup (`popup`, `popup-save=false`, `show-toolbar=false`, `show-toolbar-form=false`, `forceEdit`). Form `Eleccion@Notificacion-form` (model `Notificacion`), panel «Canal» y botones Cancelar (`close`) y Continuar (`readonlyIf="tipoNotificacion == null"`, grupo `Remote-continuarAlta` → `close`).

```
Canal:    tttt········   ← tipoNotificacion(4, RadioSelect horizontal) — único campo, sin relacionados
Botones:  ········ccxx   ← Cancelar(2, offset 8) + Continuar(2)
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

### Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

#### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-todas-001 | RUI-notificaciones-todas-eleccion-canal-001 | `Eleccion@Notificacion-form` `tipoNotificacion` sin valor por defecto (el dominio no declara `default`) |
| U-notificaciones-todas-002 | RUI-notificaciones-todas-eleccion-canal-002 | `Eleccion@Notificacion-form` `btnContinuar` `readonlyIf="tipoNotificacion == null"` |
