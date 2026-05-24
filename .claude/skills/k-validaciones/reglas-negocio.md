# Reglas de negocio (`R-XXX`)

## 1. ¿Qué es una regla de negocio?

Una **regla de negocio** (`R-XXX`) es una acción automática que el sistema **ejecuta** cuando ocurre un evento sobre la entidad (insertar, actualizar, borrar, cambiar de estado, etc.). Modifica el estado del sistema o produce efectos colaterales: calcular un campo derivado, generar un PDF, enviar un correo, crear o cancelar un registro relacionado, etc.

Una regla de negocio **siempre tiene efecto** sobre el sistema; nunca bloquea — si la operación no debe permitirse, eso es una validación `V-XXX`, no una regla `R-XXX`.

Las reglas de negocio se identifican con un código estable `R-XXX` que se asigna durante el análisis y se mantiene a lo largo del diseño y la implementación para garantizar la trazabilidad.

---

## 2. Cómo se documenta (durante el análisis)

Cada `R-XXX` se documenta en una tabla con la información mínima necesaria para implementarla.

### 2.1 Tabla `R-XXX`

| ID    | Descripción de la regla de negocio                                          | Entidad     | Método           | Momento  | Más información                                                |
|-------|-----------------------------------------------------------------------------|-------------|------------------|----------|----------------------------------------------------------------|
| R-001 | El total de la factura es la suma del importe de todas las líneas           | Factura     | insert / update  | Antes    | Calcula `total = sum(lineas.importe)` y lo asigna al registro  |
| R-002 | Enviar un correo con el documento al solicitante                            | Expediente  | cambiarEstado    | Después  | Solo si el estado pasa a APROBADO                              |
| R-003 | Asignar el número de expediente secuencial dentro del centro                | Expediente  | insert           | Antes    | Formato `EXP-{año}-{secuencial}` por centro                    |
| R-004 | Generar el PDF del expediente y adjuntarlo al registro                      | Expediente  | cambiarEstado    | Después  | Al pasar a APROBADO; se firma con el certificado del centro    |
| R-005 | Registrar quién aprobó y cuándo                                             | Expediente  | cambiarEstado    | Antes    | Rellena `aprobadoPor`, `aprobadoEn` al pasar a APROBADO        |
| R-006 | Al modificar el NIF de una persona, propagar el cambio a sus expedientes    | Persona     | update           | Después  | Solo si `nif` cambió respecto al valor anterior                |
| R-007 | Al cerrar un curso, cancelar todas las matrículas activas                   | Curso       | cambiarEstado    | Después  | Recorre matrículas con estado ACTIVA y las pasa a CANCELADA    |
| R-008 | Al borrar un alumno, archivar sus documentos en lugar de eliminarlos        | Alumno      | remove           | Antes    | Mueve los documentos asociados al archivo histórico            |

Significado de las columnas:

- **Entidad**: la entidad sobre la que opera la regla.
- **Método**: el método del servicio donde se aplica (`insert`, `update`, `remove`, `cambiarEstado` u otro método concreto).
- **Momento**: `Antes` (pre, antes de la operación) o `Después` (post, tras la operación).
- **Más información**: condiciones de aplicación, dependencias, datos que se modifican.

### 2.2 Guías para redactar la regla

- Describir **qué hace el sistema**, no qué tiene que hacer el usuario: *"Al aprobar el expediente, se genera el PDF y se adjunta al registro"* en vez de *"El usuario debe generar el PDF al aprobar"*.
- Si la regla depende de una condición, especificarla en "Más información": *"Solo si el estado pasa a APROBADO"*, *"Solo si el campo X cambió respecto al valor anterior"*.
- Si la regla **impide** guardar/cambiar estado cuando no se cumple, **no es una `R-XXX`** — es una validación `V-XXX`. Las reglas de negocio siempre tienen efecto; las validaciones solo bloquean.
- Una regla con muchas condiciones distintas suele ser en realidad varias reglas — partirla en `R-XXX` separadas mejora la trazabilidad al diseño.

### 2.3 Cómo elegir el momento (Antes / Después)

| Momento     | Cuándo usarlo                                                                                                                                                     |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Antes**   | La regla escribe sobre el **mismo registro** y los cambios deben persistirse junto al `save`.                                                                     |
| **Después** | La regla tiene **efectos colaterales** sobre otros registros, envíos externos o documentos; necesita saber que la operación principal tuvo éxito antes de actuar. |

---

## 3. Cómo se implementa

### 3.1 Principio: un método `fireActionRule_*` por regla

Cada `R-XXX` documentada en el análisis se materializa como un método privado en el `*ServiceImpl` con el prefijo `fireActionRule_`. El nombre del método describe **la acción que realiza la regla**, no la condición que la dispara.

```java
private void fireActionRule_AsignarNumeracion(MiEntidad entidad) { ... }
private void fireActionRule_RecalcularTotal(MiEntidad entidad) { ... }
private void fireActionRule_PropagrarCambioNif(MiEntidad entidad, MiEntidad original) { ... }
```

Convenciones del método:

- **Nombre**: `fireActionRule_<NombreDescriptivo>` — descripción de la acción, no de la condición.
- **Firma**: recibe la entidad; recibe también `original` si la regla depende del cambio entre el valor anterior y el nuevo.
- **Devuelve `void`** — ejecuta el efecto directamente sobre la entidad u otros registros.
- **No lanza `BusinessException`** — si hay que bloquear la operación, eso es una validación `V-XXX` en `validate*`, no una regla `R-XXX`.

### 3.2 Dónde se invoca: `Antes` o `Después` de la persistencia

Las reglas se llaman desde la acción del servicio colocando la llamada antes o después del punto de persistencia, que **siempre** es `repository.save(...)` / `repository.remove(...)` — **nunca** `super.insert/update/remove` (prohibido en toda la `*Impl`, también al sobrescribir `insert/update/remove`; ver `[[k-sistemas]]` §"Persistir: siempre `repository`, nunca `super.*`"). Cuando sobrescribes `insert/update/remove`, la `validateXxx(...).ifPresent(throwIfInvalid)` la pones tú como primera línea.

```java
@Override
public MiEntidad insert(MiEntidad entidad) {
    validateInsert(entidad).ifPresent(BusinessMessages::throwIfInvalid);   // V-XXX

    fireActionRule_AsignarNumeracion(entidad);     // R-XXX Antes — escribe sobre el mismo registro
    entidad = repository.save(entidad);            // persiste (NUNCA super.insert)
    fireActionRule_EnviarNotificacion(entidad);    // R-XXX Después — efecto colateral externo
    return entidad;
}

@Override
public MiEntidad update(MiEntidad entidad, MiEntidad original) {
    validateUpdate(entidad, original).ifPresent(BusinessMessages::throwIfInvalid);   // V-XXX

    fireActionRule_RecalcularTotal(entidad);       // R-XXX Antes — campo derivado
    entidad = repository.save(entidad);            // persiste (NUNCA super.update)
    fireActionRule_PropagrarCambioNif(entidad, original);  // R-XXX Después — propaga a otras entidades
    return entidad;
}
```

| Momento     | Dónde en el código              | Por qué                                                                                            |
|-------------|---------------------------------|----------------------------------------------------------------------------------------------------|
| **Antes**   | Antes de `repository.save/remove`   | Para que los cambios escritos sobre el mismo registro se persistan junto al resto del `save`.      |
| **Después** | Después de `repository.save/remove` | Para garantizar que la operación principal tuvo éxito antes de ejecutar el efecto colateral externo. |

---

## 4. Trazabilidad: del análisis al diseño

- Cada `R-XXX` del análisis aparece en al menos un paso del diseño.
- Cada paso del diseño que implementa reglas lista qué `R-XXX` cubre. Ejemplo:
  - *"Paso 5 — `FooServiceImpl.update`. Cubre R-002 (`fireActionRule_PropagarCambioNif`, Después) y R-004 (`fireActionRule_RecalcularTotal`, Antes)."*
- Antes de cerrar el diseño, construir la matriz `R-XXX → paso(s)`. Ninguna fila puede quedar vacía.

---

## 5. Referencias

- `k-sistemas/servicios.md` — estructura del `*ServiceImpl`, sobreescritura de `insert`/`update`/`remove`, descubrimiento por `ModelServiceFactory`.
- `validaciones.md` — para distinguir reglas de negocio (`R-XXX`, ejecutan) de validaciones (`V-XXX`, bloquean).