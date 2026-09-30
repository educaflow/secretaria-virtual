# Idiomas Java modernos

## Optional en lugar de null

Todo método que puede no devolver un resultado debe devolver `Optional<T>`, no `T`.

La señal es cualquiera de estas: condición documentada, nombre del método (`findBy*`, `buscar*`, `get*IfExists`, `resolver*`), o semántica implícita (busca entre colecciones un elemento que puede no existir).

**Violación:** tipo de retorno `T` (no primitivo) cuando el método puede no encontrar un resultado.

**Correcto:** tipo de retorno `Optional<T>`. Usar `Optional.empty()` en el caso vacío. Los métodos llamantes usan `map`, `flatMap`, `orElse`, `orElseThrow` o `ifPresent` — nunca `get()` sin `isPresent()` previo.

No usar `Optional` como campo de entidad JPA, como parámetro de método ni dentro de colecciones.

---

## Objects.requireNonNull / TextUtil.requireNonBlank para parámetros obligatorios

Los parámetros obligatorios (constructores, compact constructors de records, factories, métodos públicos) se comprueban con una llamada, no con un `if` a mano.
Son asserts de la aplicación: lo que "nunca debería darse".

- Parámetro no `String` → `Objects.requireNonNull(obj, "<obj> no puede ser null")`.
- Parámetro `String` → **MUST** usar `TextUtil.requireNonBlank(obj, "<obj> no puede ser null ni blank")` (`com.educaflow.base.util`), que además rechaza `""` y solo espacios.
  **MUST NOT** usar `Objects.requireNonNull` con un `String`: deja pasar `""` y `" "`.

- ✅ CORRECTO: `TextUtil.requireNonBlank(authToken, "authToken no puede ser null ni blank");`
- ✅ CORRECTO: `this.modelLoader = Objects.requireNonNull(modelLoader, "modelLoader no puede ser null");`
- ❌ INCORRECTO: `Objects.requireNonNull(authToken, "authToken no puede ser null");` (es un `String`: acepta `""` y `" "`)
- ❌ INCORRECTO: `if (authToken == null || authToken.isBlank()) { throw new IllegalArgumentException(...); }` (reimplementa a mano `requireNonBlank` y añade ramas a la CC)

---

## Streams para colecciones

Ver `metodos.md` — sección "Operaciones sobre colecciones".

---

## Records para datos inmutables

Cuando se necesita un objeto que transporta datos sin comportamiento (DTO de entrada, resultado compuesto, parámetros agrupados), usar un `record` Java.

Un `record` es inmutable por defecto, genera `equals`, `hashCode` y `toString` automáticamente, y no necesita getters/setters explícitos.

```java
record DatosFirma(Partner firmante, List<MetaFile> documentos, String motivoFirma) {}
```

No usar records para: entidades JPA (que extienden `Model`), objetos con estado mutable, clases que necesitan herencia de implementación.

---

## Pattern matching para instanceof

Usar pattern matching en lugar del cast explícito:

```java
// Incorrecto
if (obj instanceof String) {
    String s = (String) obj;
    // ...
}

// Correcto
if (obj instanceof String s) {
    // ...
}
```

---

## Switch expressions

Usar switch expressions en lugar de switch statements cuando el resultado es un valor:

```java
// Incorrecto
String label;
switch (estado) {
    case PENDIENTE: label = "Pendiente"; break;
    case FIRMADO:   label = "Firmado";   break;
    default:        label = "Otro";
}

// Correcto
String label = switch (estado) {
    case PENDIENTE -> "Pendiente";
    case FIRMADO   -> "Firmado";
    default        -> "Otro";
};
```

---

## var

Usar `var` cuando el tipo es obvio por la expresión de la derecha y escribirlo explícitamente no aporta información adicional:

```java
var service = Beans.get(TareaFirmaService.class);  // OK: el tipo se lee directamente
var lista   = new ArrayList<MetaFile>();            // OK: tipo evidente
```

No usar `var` cuando el tipo no es inmediatamente obvio al leer la línea, ni como campo de instancia.

---

## Colecciones inmutables

Preferir colecciones inmutables cuando la colección no va a modificarse tras crearse:

```java
List.of(a, b, c)      // en lugar de new ArrayList<>(Arrays.asList(a, b, c))
Map.of(k1, v1, k2, v2) // en lugar de new HashMap<>() + put
Set.of(a, b, c)        // en lugar de new HashSet<>()
```

Si la colección necesita modificarse después de crearse, usar `new ArrayList<>(List.of(...))` para obtener una copia mutable.