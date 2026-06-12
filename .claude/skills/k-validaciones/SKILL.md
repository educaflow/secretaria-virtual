---
name: k-validaciones
description: Documentar e implementar las tres categorías de reglas sobre las entidades — validaciones (`V-XXX`, bloquean), reglas de negocio (`R-XXX`, actúan sobre el sistema) y reglas de UI (`U-XXX`, solo cambian el formulario) — con sus tablas de análisis, capas de implementación y trazabilidad al diseño. Incluye el mapeo desde las cinco categorías numeradas de la especificación (`RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN`) hacia V/R/U.
---

# k-validaciones

Este skill describe tres tipos de comprobaciones que se aplican sobre las entidades de la aplicación — **validaciones**, **reglas de negocio** y **reglas de UI** — y cómo documentarlas durante el análisis e implementarlas en el código.

## Conceptos

- **Validación** (`V-XXX`): condición que un dato o un registro debe cumplir para que una operación sea aceptada. Si no se cumple, el sistema **impide** la operación y muestra un mensaje al usuario. Una validación nunca modifica el estado del sistema. Ejemplo: *"El email debe tener el formato `usuario@dominio.com`"*.
- **Regla de negocio** (`R-XXX`): acción automática que el sistema **ejecuta** cuando ocurre un evento sobre la entidad (insertar, actualizar, borrar, cambiar de estado, etc.). Modifica el estado del sistema o produce efectos colaterales: calcular un campo derivado, generar un PDF, enviar un correo, crear o cancelar un registro relacionado, etc. Ejemplo: *"El total de la factura es la suma del importe de sus líneas"*.
- **Regla de UI** (`U-XXX`): cambio en el aspecto o el estado del formulario en función del valor de un campo, del usuario o del registro padre (mostrar/ocultar, readonly, required, valor por defecto, filtrado de dominio). No bloquea operaciones ni modifica el estado del sistema. Ejemplo: *"El campo `motivoRechazo` se muestra solo cuando el estado es `RECHAZADO`"*.

Regla mnemotécnica: **una validación dice "no" y bloquea; una regla de negocio dice "ahora hago esto" y actúa; una regla de UI dice "ahora ves esto" y solo afecta al formulario**.

> **Ortogonal a V/R/U: el "origen del valor" de cada campo.** La clasificación V/R/U describe *qué* hace cada regla. Aparte, cada campo de la entidad se clasifica en el análisis (`entity-*.md`) por **origen del valor**: `cliente` (lo aporta el usuario, validable con V, permitido en `AllowProperties`) o `servidor` (lo dicta el servidor en una R-…; típicamente una `R-XXX` con momento `Antes` de `Crear` lo asigna/recalcula **incondicionalmente**). Las dos clasificaciones se cruzan: un campo `servidor` puede tener varias `V-XXX` que validen el valor que el servidor le acaba poniendo, y siempre tendrá una `R-XXX` `Antes` que lo asigne. Ver `[[k-secure-coding]]` §3 para por qué esta distinción importa (mass-assignment) y cómo se implementa la asignación incondicional de campos `servidor`.

## Cómo encajan las categorías de la especificación

La especificación (`specification.md`, plantilla de `sdd-specification-system`) clasifica las reglas en **cinco** categorías numeradas con sus propios prefijos; el análisis las mapea a las **tres** categorías V/R/U de este skill, conservando el ID del spec en la columna "Origen spec":

| Categoría del spec | Prefijo | Se convierte en | Notas |
|---|---|---|---|
| Restricción (invariante de entidad) | `RES-NNN` | `V-XXX` | Aplica a todos los eventos; típicamente declarativa en el modelo XML (única, obligatoria, comparación de fechas). |
| Validación (de un evento) | `VAL-NNN` | `V-XXX` | Anclada a la operación del evento. |
| Regla de negocio | `RN-NNN` | `R-XXX` | La `fase` del spec (`antes_de_commit`/`después_de_commit`) orienta el momento `Antes`/`Después`. |
| Regla de UI | `RUI-NNN` | `U-XXX` | Anclada a la(s) pantalla(s) afectadas. |
| Campo calculado | `CC-NNN` | campo `servidor` + `R-XXX` `Antes` (momento `escritura`), o campo derivado de solo lectura (momento `lectura`) | El atributo `sobreescribible` del spec se documenta en la R. |

La decisión final depende del **efecto real** (bloquea → V, actúa → R, cambia formulario → U), no de la categoría del spec: si contradicen, el analista lo pregunta al usuario.

## Cómo se definen

Los tres tipos se documentan **durante el análisis funcional** en tablas con identificadores estables (`V-XXX`, `R-XXX` y `U-XXX`) que luego se trazan al diseño y a la implementación:

- **Validaciones** — tabla `V-XXX` con columnas `ID | Campo(s) | Descripción | Condición | Mensaje al usuario | Modelo XML | Servidor (validate*) | Cliente`. Cada `V-XXX` se asigna a una o varias de las **tres capas** posibles (Modelo XML declarativo, Servidor `validate*`, Cliente XML opcional) y debe ir como mínimo en Modelo o Servidor (principio "servidor es la fuente de verdad").
- **Reglas de negocio** — tabla `R-XXX` con columnas `ID | Descripción | Entidad | Método | Momento | Más información`. Cada `R-XXX` indica el método del servicio (`insert`, `update`, `remove`, `cambiarEstado`…) y el momento (`Antes` / `Después`) en que se ejecuta. En la implementación se materializa como un método `fireActionRule_<Nombre>(...)` en el `*ServiceImpl`.
- **Reglas de UI** — tabla `U-XXX` con columnas `ID | Disparador | Efecto | Campo/Panel afectado | Condición | Mecanismo`. Cada `U-XXX` indica cuándo se dispara (`onNew`, `onLoad`, `onChange:campo` o continuo), qué efecto produce en la pantalla y cómo se implementa (`showIf`/`hideIf`/`readonlyIf`/`requiredIf`, `<action-attrs>`, `<action-record>`).

Cada identificador del análisis aparece en al menos un paso del diseño, y cada paso del diseño que toca validaciones/reglas lista qué `V-XXX`/`R-XXX`/`U-XXX` cubre — así se construye la matriz de trazabilidad antes de cerrar el diseño.

## Coexistencia de `V-XXX` y `R-XXX` en una misma operación

`V-XXX` (validaciones) y `R-XXX` (reglas de negocio) no compiten — coexisten en el mismo flujo. En `insert`/`update`/`remove` del `*ServiceImpl` el orden es:

```java
@Override
public T insert(T entidad) {
    validateInsert(entidad).ifPresent(BusinessMessages::throwIfInvalid);   // V-XXX (primera línea)
    fireActionRule_X_Antes(entidad);     // R-XXX que escriben sobre el mismo registro
    entidad = repository.save(entidad);  // persiste (NUNCA super.insert)
    fireActionRule_Y_Despues(entidad);   // R-XXX con efectos colaterales (correos, PDFs, otros registros)
    return entidad;
}
```

- Las `V-XXX` viven en `validateInsert`/`validateUpdate`/`validateRemove`. Si **no** sobrescribes `insert/update/remove`, las dispara `DefaultModelService` (salvaguarda); si los sobrescribes, las disparas **tú** como primera línea con `validateXxx(...).ifPresent(throwIfInvalid)`. Si fallan, abortan la operación.
- Las `R-XXX` viven en métodos `fireActionRule_<Nombre>` y se ejecutan alrededor de `repository.save/remove` (**nunca** `super.*`): antes si modifican el mismo registro (cambios persistirán junto al `save`), después si tienen efectos colaterales.

---

## Validaciones — dónde está cada cosa

- **`validaciones.md`** — documento principal de validaciones. Explica:
  - El principio fundamental (el servidor es la fuente de verdad) y los dos mecanismos del servidor: declarativo en el dominio XML y imperativo en `validateInsert`/`validateUpdate`/`validateRemove`.
  - La salvaguarda automática de `DefaultModelService` y cómo se llaman desde la vista (`<action-method>` remoto + validaciones locales opcionales).
  - El patrón `action-group` **Local → Remote → save** que encadena las tres etapas en cada operación, y su extensión a operaciones custom (`btnAprobar`, `btnDelete`, etc.).
  - El controlador como puente (`@CallMethod validateSave`) entre la vista y el servicio.
  - La **tabla `V-XXX`** completa con ejemplos y la **tabla de atributos del modelo XML** (`required`, `unique`, `min`/`max`, `precision`/`scale`, `<unique-constraint>`…) con la regla que implementa cada uno.
  - Las **guías de redacción de los mensajes** (incluir valor recibido, empezar por el campo, decir cómo debe ser en vez de cómo no debe ser).
  - La **trazabilidad `V-XXX → paso del diseño`**.

- **`examples/ejemplos-validaciones.md`** — catálogo de **17 patrones XML** de validación cliente extraídos de vistas reales de axelor-open-suite y reformulados a las convenciones del proyecto, listos para copiar y adaptar:
  - **P1–P6** con `<action-condition>` — errores pegados a un campo: obligatorio, comparación de fechas, validación cruzada de dos campos, comparación con padre (`__parent__`), fecha en el futuro (`__config__.date`), varios obligatorios agrupados.
  - **P7–P17** con `<action-validate>` — diálogos a nivel de formulario: varios `<error>`, rango, lista vacía, estado prohibido, permiso por grupo (`__user__.group`), `<alert>` para confirmar, `<alert>` con interpolación `${campo}`, `<info>`, `<notify>`, `<error>` con `action=` correctiva.
  - Cada patrón incluye el XML completo y los casos en los que aplica.

  - **`reference/validaciones.md`** — **catálogo de tipos de validaciones** agrupado por ámbito, pensado para que el analista identifique rápidamente qué validaciones aplican a un campo:
    - Validaciones **sobre el propio campo** (obligatorio, longitud, rango, formato, dígito de control, lista cerrada…).
    - Validaciones **entre campos del mismo registro** (mayor/menor que, igual, distinto, mutuamente excluyentes, suma de líneas igual al total…).
    - Validaciones **entre registros** (unicidad global o de ámbito, integridad referencial, cardinalidad de hijos, prerrequisitos…).
    - Validaciones **de negocio** (operación no admitida según condición, requiere rol, ventana temporal, inmutabilidad por estado, transiciones de estado…).
    - Cada fila incluye descripción de la regla, cuándo se aplica, plantilla de mensaje y ejemplo redactado.

---

## Reglas de negocio — dónde está cada cosa

- **`reglas-negocio.md`** — documento único de reglas de negocio. Explica:
  - La **implementación**: cada `R-XXX` se materializa como un método privado `fireActionRule_<NombreDescriptivo>` en el `*ServiceImpl`, invocado desde `insert`/`update`/`remove`/operaciones custom, **antes** de `super.*` si escribe sobre el mismo registro o **después** si tiene efectos colaterales externos.
  - Las **convenciones** del método: nombre descriptivo de la acción (no de la condición), recibe la entidad y `original` si depende del cambio, devuelve `void`, **no lanza `BusinessException`** (si bloquea, es una `V-XXX` no una `R-XXX`).
  - La **documentación al analizar**: tabla `R-XXX` con columnas `ID | Descripción | Entidad | Método | Momento | Más información` y guías de redacción (describir qué hace el sistema, no qué hace el usuario; condiciones en "Más información"; partir reglas con varias condiciones en varias `R-XXX`).
  - La **trazabilidad `R-XXX → paso del diseño`**.

---

## Reglas de UI — dónde está cada cosa

- **`reglas-ui.md`** — documento único de reglas de UI. Explica:
  - La **distinción** entre `U-XXX` (solo afecta al formulario), `V-XXX` (bloquea operaciones) y `R-XXX` (modifica el estado del sistema), con la regla mnemotécnica "no puedes / ahora hago / ahora ves".
  - La **tabla `U-XXX`** con columnas `ID | Disparador | Efecto | Campo/Panel afectado | Condición | Mecanismo` y ejemplos representativos (paneles con `showIf`, valores por defecto con `<action-record>`, dominios dinámicos con `<action-attrs>`, etc.).
  - La **tabla de decisión de mecanismos**: cuándo usar atributos inline (`showIf`/`hideIf`/`readonlyIf`/`requiredIf`), cuándo `<action-attrs>` desde un evento y cuándo `<action-record>` desde `onNew`/`onChange`.
  - Las **guías de redacción** (describir qué ve el usuario, partir reglas con varios efectos, no confundir con V-XXX/R-XXX).
  - La **trazabilidad `U-XXX → paso del diseño`**.