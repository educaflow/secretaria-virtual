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

## TextUtil.isNullOrBlank para las guardas

Cuando un `String` que falta **no** es un error sino un caso del negocio (una guarda de cortesía, ver `metodos.md`), se comprueba con `TextUtil.isNullOrBlank(texto)` (`com.educaflow.base.util`).
**MUST NOT** escribirse a mano `texto == null || texto.isBlank()`: son dos ramas de CC y un idioma que el proyecto ya tiene.

- ✅ CORRECTO: `if (TextUtil.isNullOrBlank(email)) { return; }`
- ❌ INCORRECTO: `if (email == null || email.isBlank()) { return; }` (reimplementa `isNullOrBlank` y suma CC)
- ❌ INCORRECTO: `if (email == null) { return; }` (deja pasar `""` y `" "`)

---

## Records para datos inmutables

Cuando se necesita un objeto que transporta datos sin comportamiento (DTO de entrada, resultado compuesto, parámetros agrupados), usar un `record` Java.

Un `record` es inmutable por defecto, genera `equals`, `hashCode` y `toString` automáticamente, y no necesita getters/setters explícitos.

```java
record DatosFirma(Partner firmante, List<MetaFile> documentos, String motivoFirma) {}
```

No usar records para: entidades JPA (que extienden `Model`), objetos con estado mutable, clases que necesitan herencia de implementación.

---

## Pattern matching, switch expressions, var y colecciones inmutables

- **MUST** usar pattern matching en `instanceof` (`if (obj instanceof String s)`), nunca el cast explícito.
- **MUST** usar switch expression (`case X -> …`) cuando el resultado del `switch` es un valor.
- `var` solo cuando el tipo se lee en la propia línea (`var lista = new ArrayList<MetaFile>();`); **MUST NOT** usarse como campo de instancia ni cuando el tipo no es obvio.
- Colección que no se modifica tras crearse → `List.of`/`Map.of`/`Set.of`; si hay que modificarla, `new ArrayList<>(List.of(...))`.
