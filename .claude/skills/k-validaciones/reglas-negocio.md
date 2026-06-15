# Reglas de negocio (`RN-`)

Una regla de negocio (`RN-`) es una acción automática que el sistema ejecuta como reacción a un evento sobre la entidad (insertar, actualizar, borrar, cambiar de estado): calcula un campo derivado, genera un PDF, envía un correo, crea o cancela un registro relacionado. **Siempre tiene efecto; nunca bloquea** — si la operación no debe permitirse, eso es una validación (`VAL-`). Qué es y cómo se clasifica lo define la spec; aquí solo **cómo se implementa**.

---

## 1. Un método `fireActionRule_*` por regla

Cada `RN-` se materializa como un método privado en el `*ServiceImpl` con el prefijo `fireActionRule_`. El nombre describe **la acción que realiza**, no la condición que la dispara.

```java
private void fireActionRule_AsignarNumeracion(MiEntidad entidad) { ... }
private void fireActionRule_RecalcularTotal(MiEntidad entidad) { ... }
private void fireActionRule_PropagarCambioNif(MiEntidad entidad, MiEntidad original) { ... }
```

Convenciones del método:

- **Nombre**: `fireActionRule_<NombreDescriptivo>` — descripción de la acción, no de la condición.
- **Firma**: recibe la entidad; recibe también `original` si la regla depende del cambio entre el valor anterior y el nuevo.
- **Devuelve `void`** — ejecuta el efecto directamente sobre la entidad u otros registros.
- **No lanza `BusinessException`** — si hay que bloquear la operación, eso es una `VAL-` en `validate*`, no una `RN-`.

---

## 2. Dónde se invoca: `Antes` o `Después` de la persistencia

El atributo `fase` que la spec asigna a cada `RN-` determina el momento:

| `fase` (spec) | Momento | Dónde en el código | Por qué |
|---|---|---|---|
| `antes_de_commit` | **Antes** | Antes de `repository.save/remove` | La regla escribe sobre el **mismo registro** y los cambios deben persistirse junto al `save`. |
| `después_de_commit` | **Después** | Después de `repository.save/remove` | La regla tiene **efectos colaterales** (otros registros, envíos externos, documentos) y necesita saber que la operación principal tuvo éxito. |

La persistencia es **siempre** `repository.save(...)` / `repository.remove(...)` — **nunca** `super.insert/update/remove` (prohibido en toda la `*Impl`, también al sobrescribir `insert/update/remove`; ver `[[k-sistemas]]` §"Persistir: siempre `repository`, nunca `super.*`"). Cuando sobrescribes `insert/update/remove`, la `validateXxx(...).ifPresent(throwIfInvalid)` la pones tú como primera línea.

```java
@Override
public MiEntidad insert(MiEntidad entidad) {
    validateInsert(entidad).ifPresent(BusinessMessages::throwIfInvalid);   // VAL-

    fireActionRule_AsignarNumeracion(entidad);     // RN- Antes — escribe sobre el mismo registro
    entidad = repository.save(entidad);            // persiste (NUNCA super.insert)
    fireActionRule_EnviarNotificacion(entidad);    // RN- Después — efecto colateral externo
    return entidad;
}

@Override
public MiEntidad update(MiEntidad entidad, MiEntidad original) {
    validateUpdate(entidad, original).ifPresent(BusinessMessages::throwIfInvalid);   // VAL-

    fireActionRule_RecalcularTotal(entidad);       // RN- Antes — campo derivado
    entidad = repository.save(entidad);            // persiste (NUNCA super.update)
    fireActionRule_PropagarCambioNif(entidad, original);  // RN- Después — propaga a otras entidades
    return entidad;
}
```

---

## 3. Campos calculados (`CC-`)

Un campo calculado (`CC-`) lo calcula siempre el servidor, nunca el cliente. Se implementa según su `momento`:

- **`momento: escritura`** → una `RN-` `Antes`: un `fireActionRule_<Nombre>` que calcula el valor y lo asigna a la entidad **antes** de `repository.save`, para que se persista. Es el caso habitual (totales, numeración secuencial, sellos de quién/cuándo).
- **`momento: lectura`** → un campo **derivado** que se calcula en memoria al leer y no se persiste: campo calculado / `formula="true"` en el modelo del dominio (ver [`restricciones.md`](restricciones.md) y [`../k-sistemas/references/models.md`](../k-sistemas/references/models.md)).

El atributo `sobreescribible` de la spec (qué roles pueden forzar un valor manual) se respeta dentro del propio `fireActionRule_*`: si el rol del usuario está autorizado y envió un valor, no lo recalcula. Ver `[[k-secure-coding]]` §3.

---

## 4. Referencias

- [`validaciones.md`](validaciones.md) — para distinguir reglas de negocio (`RN-`, ejecutan) de validaciones (`VAL-`, bloquean).
- [`../k-sistemas/references/models.md`](../k-sistemas/references/models.md) — campos `formula`/derivados para `CC-` de lectura.
- `k-sistemas/servicios.md` — estructura del `*ServiceImpl`, sobreescritura de `insert`/`update`/`remove`, descubrimiento por `ModelServiceFactory`.
