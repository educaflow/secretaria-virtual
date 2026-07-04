# Reglas de negocio (`RN-`) y campos calculados (`CC-`)

Una regla de negocio (`RN-`) es una acción automática que el sistema ejecuta como reacción a un evento sobre la entidad (insertar, actualizar, borrar, cambiar de estado): calcula un campo derivado, genera un PDF, envía un correo, crea o cancela un registro relacionado. **Siempre tiene efecto; nunca bloquea** — si la operación no debe permitirse, eso es una validación (`VAL-`). Qué es y cómo se clasifica lo define la spec; aquí solo **cómo se implementa**.

Este fichero cubre las dos materializaciones de servidor que la spec produce en esta familia:

- **ActionRules** (§1) — métodos `fireActionRule_*` del `*ServiceImpl`, para las `RN-` y para los `CC-` de escritura.
- **Campos calculados** (§2) — los `CC-`: según su `momento`, una ActionRule `Antes` (escritura) o un campo derivado del modelo XML (lectura).

---

## 1. ActionRules (`fireActionRule_*`)

### 1.1 Un método `fireActionRule_*` por regla

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

### 1.2 Dónde se invoca: `Antes` o `Después` de la persistencia

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

## 2. Campos calculados (`CC-`)

Un campo calculado (`CC-`) lo calcula siempre el servidor, nunca el cliente (por eso un `CC-` con `sobreescribible: nunca` jamás aparece en `AllowProperties`). El atributo `momento` de la spec decide el mecanismo:

| `momento` (spec) | Mecanismo | Dónde vive | ¿Se persiste? |
|---|---|---|---|
| `escritura` | `fireActionRule_*` `Antes` de `repository.save` | `*ServiceImpl` | Sí |
| `lectura` | campo derivado: cuerpo Java o `formula="true"` (SQL) | modelo XML del dominio | No |

### 2.1 `momento: escritura` — una ActionRule `Antes`

Es el caso habitual (totales, numeración secuencial, sellos de quién/cuándo). Se implementa **exactamente igual que una `RN-` `Antes`** (§1): un `fireActionRule_<Nombre>` que calcula el valor y lo asigna a la entidad antes de `repository.save`, para que se persista junto al registro.

```java
@Override
public RegistroSalida insert(RegistroSalida entidad) {
    validateInsert(entidad).ifPresent(BusinessMessages::throwIfInvalid);

    fireActionRule_AsignarNumeroRegistro(entidad);   // CC- escritura → Antes del save
    entidad = repository.save(entidad);
    return entidad;
}

// CC-001 numeroRegistro — cálculo: secuencial por centro y año, formato NNNNN/AAAA
private void fireActionRule_AsignarNumeroRegistro(RegistroSalida entidad) {
    String anyo = String.valueOf(LocalDate.now().getYear());
    long numero = numeradorRepository.getSiguienteNumeroRegistroSalida(entidad.getCentro().getCode(), anyo);
    entidad.setNumeroRegistro(String.format("%05d", numero) + "/" + anyo);
}
```

- La asignación es **incondicional**: machaca lo que enviara el cliente, aunque el campo ni aparezca en la vista (defensa mass-assignment; ver `[[k-secure-coding]]` §3).
- Si el cálculo depende de otros registros (p.ej. total = suma de líneas), el `fireActionRule_*` los consulta vía repositorio — sigue siendo `Antes` porque escribe sobre el mismo registro.

**Excepción `sobreescribible`**: si la spec lista roles que pueden forzar un valor manual, la condición vive dentro del propio `fireActionRule_*` — solo entonces la asignación deja de ser incondicional:

```java
// CC-002 descuentoEspecial — sobreescribible: [ADMIN]
private void fireActionRule_AsignarDescuentoEspecial(Pedido entidad) {
    if (usuarioActualTieneRol("ADMIN") && entidad.getDescuentoEspecial() != null) {
        return;                                      // rol autorizado con valor manual: se respeta
    }
    entidad.setDescuentoEspecial(BigDecimal.ZERO);   // resto de casos: se recalcula siempre
}
```

### 2.2 `momento: lectura` — campo derivado en el modelo XML

No hay método en el servicio ni columna en la base de datos: el campo se declara en el **modelo XML del dominio** y se calcula cada vez que se lee la entidad. Dos variantes:

**a) Cuerpo Java** — el campo lleva un bloque `<![CDATA[...]]>` con código Java que deriva el valor de otros campos de la propia entidad ya cargada. Ejemplo real (`subsystem/common/domains/Persona.xml`):

```xml
<string name="nombreApellidos" namecolumn="true" search="nombre,apellidos">
    <![CDATA[
        StringBuilder sb = new StringBuilder();
        if (this.getNombre() != null) {
            sb.append(this.getNombre());
        }
        if (this.getApellidos() != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(this.getApellidos());
        }
        return sb.toString();
    ]]>
</string>
```

**b) `formula="true"` (SQL nativo)** — el valor lo calcula la base de datos con un fragmento SQL (admite subselects sobre otras tablas):

```xml
<integer name="totalLineas" formula="true">
    <![CDATA[ (SELECT COUNT(*) FROM pedido_linea l WHERE l.pedido = id) ]]>
</integer>
```

Criterio de elección:

- El valor se deriva **solo de campos de la propia entidad** ya cargada → cuerpo Java (a).
- El cálculo necesita **otros registros/tablas** o debe poder **filtrarse/ordenarse** en un grid a nivel SQL → `formula="true"` (b).
- En la variante (a), si el campo se usa en autocompletado, el atributo `search="..."` lista las columnas reales sobre las que buscar (el campo derivado no existe en la BD).

Sintaxis completa de ambas variantes: [`../k-sistemas/references/models.md`](../k-sistemas/references/models.md) §Formula y ejemplo `fullName`.

### 2.3 Ejemplos ✅/❌

- ✅ CORRECTO: `CC-` `momento: escritura` asignado en un `fireActionRule_*` antes de `repository.save`.
- ✅ CORRECTO: `CC-` `momento: lectura` como campo con cuerpo Java o `formula="true"` en el dominio.
- ❌ INCORRECTO: calcular el `CC-` en la vista (`onChange`/`<action-record>`) y aceptar el valor del cliente (el cliente puede enviar cualquier valor por REST; la vista solo es UX — ver `[[k-secure-coding]]`).
- ❌ INCORRECTO: implementar un `CC-` de escritura como `fireActionRule_*` `Después` de `repository.save` (el valor calculado no se persiste).
- ❌ INCORRECTO: implementar un `CC-` de lectura con un `fireActionRule_*` que lo persiste (se desincroniza en cuanto cambian los campos de los que deriva; si la spec dice `lectura`, se deriva al leer).

---

## 3. Referencias

- [`validaciones.md`](validaciones.md) — para distinguir reglas de negocio (`RN-`, ejecutan) de validaciones (`VAL-`, bloquean).
- [`../k-sistemas/references/models.md`](../k-sistemas/references/models.md) — campos `formula`/derivados para `CC-` de lectura.
- `k-sistemas/servicios.md` — estructura del `*ServiceImpl`, sobreescritura de `insert`/`update`/`remove`, descubrimiento por `ModelServiceFactory`.
