---
type: implementation-task
template: system
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista de referencia Ref@HistorialEstado (en expedientes, D8)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/expedientes/views/Ref-HistorialEstado.xml` | Crear | k-vistas (SKILL.md «Vistas de referencia», grids.md, forms.md) | Fichero completo materializado en `design/views/Ref-HistorialEstado.xml` (se copia verbatim aquí, no a `subsystem/notificaciones/views/`): vistas de solo lectura `subsysExpedientes.Ref@HistorialEstado-grid`/`-form`, selector y consulta del «Estado del expediente». Excepción acotada a README §4.2 autorizada por el usuario (D8): no se toca nada más de `expedientes` (ni dominio, ni servicio, ni otras vistas) |

**Instrucción de materialización (XML ya materializado):** el fichero ya está materializado en `design/views/Ref-HistorialEstado.xml` (carpeta de la iniciativa). **MUST** copiarse literalmente a `src/main/java/com/educaflow/subsystem/expedientes/views/Ref-HistorialEstado.xml`, sin regenerarlo, reescribirlo ni reformatearlo (`implementation.md` §1). La fila es `Acción: Crear`: si el destino ya existe, es `CONFLICT` (`implementation.md` §3).

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

**`Ref-HistorialEstado.xml`** (destino `subsystem/expedientes/views/`) — `subsysExpedientes.Ref@HistorialEstado-grid` (`canViewOnClick`, `orderBy="-fecha"`; columnas `expediente.numeroExpediente` «Num. Exped.», `expediente.name` «Expediente», `namePhase`, `nameState`, `fecha`) + `subsysExpedientes.Ref@HistorialEstado-form` (readonly) con Salir `close`. `model` = `com.educaflow.subsystem.expedientes.db.HistorialEstado`; el fichero se crea en `subsystem/expedientes/views/` (módulo dueño de la entidad, VAR-1.2(b), precedente `subsystem/common/views/Ref-Centro.xml`) con los nombres `subsysExpedientes.Ref@HistorialEstado-grid`/`-form`; es la única pieza que el diseño crea en `expedientes`, por excepción autorizada por el usuario (D8). Lo usa el `historialEstado` de `Main@Correo-form` y `Main@Sms-form` (selector) y el de `Centro@Correo-form` y `Centro@Sms-form` (consulta): en los `Main@` el `domain` del campo (`self.expediente.centro = :centro`) se aplica también al buscador.

```
  nnnnxxxxxxxx   ← expediente.numeroExpediente(4) + expediente.name(8)
  ffffeeeeeddd   ← namePhase(4) + nameState(5) + fecha(3)
  ..........ss   ← Salir(2, offset 10)
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Notas y supuestos (las que aplican a esta tarea)

- **Selector del estado del expediente** (D8): `HistorialEstado` no tiene nombre de presentación y su dominio vive en `expedientes`, que este diseño no modifica, así que sin más el selector solo mostraba ids.
  Se resuelve en la vista: el `historialEstado` de `Main@Correo-form`/`Main@Sms-form` lleva, además del `domain` por centro, `target-name="nameState"`, `canSuggest="false"` y las vistas de referencia `subsysExpedientes.Ref@HistorialEstado-grid`/`-form` de `subsystem/expedientes/views/Ref-HistorialEstado.xml` (vista nueva de solo lectura en el módulo dueño de la entidad, por VAR-1.2(b); única excepción a README §4.2, autorizada por el usuario, D8); el de `Centro@Correo-form`/`Centro@Sms-form` lleva `target-name="nameState"` y las mismas vistas de referencia.
  El usuario elige en el buscador, que muestra número de expediente, expediente, fase, estado y fecha, y el campo enseña el estado («Pendiente de verificación»); la columna «Expediente» de los listados sigue saliendo de `nombreExpediente`.
