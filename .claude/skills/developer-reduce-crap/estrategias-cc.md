# Técnicas para bajar la complejidad ciclomática

Catálogo de `developer-reduce-crap`.
Aplica primero las baratas y locales; la 9 (trocear) solo cuando el método hace cosas independientes.

## Cómo cuenta JaCoCo

- Suma 1 cada `if`, `&&`, `||`, `?:` (ternario), `case`, `for`/`while`/`do`.
- `catch` **no** suma.
- En Kotlin suman `?.`, `?:` (elvis), `!!` y cada rama de `when`.
- Una lambda es un método sintético aparte (`lambda$<metodo>$N` en Java, `<metodo>$lambda$N[$M…]` en Kotlin), con su propia fila en el CSV: su CC sale del método, pero también tiene que quedar ≤ umbral.
- **MUST**: ninguna técnica se da por buena hasta ver la bajada en `build/reports/crap/crap.csv`.

## 1. Guardas que lanzan excepción → llamada a librería

Solo para asserts de la aplicación (lo que "nunca debería darse"), **nunca** para validaciones que el usuario pueda corregir. Guava está en el classpath.

- ❌ INCORRECTO (2 ramas):
  ```java
  if (documento == null) throw new NullPointerException("documento");
  if (paginas <= 0) throw new IllegalArgumentException("paginas=" + paginas);
  ```
- ✅ CORRECTO (0 ramas):
  ```java
  Objects.requireNonNull(documento, "documento");
  Preconditions.checkArgument(paginas > 0, "paginas=%s", paginas);
  ```
- ❌ INCORRECTO: cambiar `messages.add(...)` de una validación de usuario por `checkArgument` (cambia una validación por un assert: el usuario deja de ver el mensaje).

## 2. Valor por defecto

- ❌ INCORRECTO: `String nombre = n != null ? n : "";` (1 rama)
- ✅ CORRECTO: `String nombre = Objects.requireNonNullElse(n, "");`
- ❌ INCORRECTO (3 ramas):
  ```java
  if (e != null && e.getCentro() != null && e.getCentro().getNombre() != null) return e.getCentro().getNombre();
  return null;
  ```
- ✅ CORRECTO: `return Optional.ofNullable(e).map(Expediente::getCentro).map(Centro::getNombre).orElse(null);`

## 3. Booleano redundante

- ❌ INCORRECTO: `if (a && b) { return true; } else { return false; }` (1 rama de más)
- ✅ CORRECTO: `return a && b;`

## 4. Comprobaciones de cadena

- ❌ INCORRECTO: `if (s == null || s.isEmpty())` (2 ramas)
- ✅ CORRECTO: `if (Strings.isNullOrEmpty(s))` (1 rama)
- Antes de escribir un helper nuevo, busca si el proyecto ya tiene uno (`base/util`).

## 5. `switch` o cadena de `if-else` por valor → tabla

- ❌ INCORRECTO (N ramas):
  ```java
  switch (tipo) { case PDF: return "application/pdf"; case XML: return "text/xml"; default: return "application/octet-stream"; }
  ```
- ✅ CORRECTO: un `EnumMap`/`Map.of` con `getOrDefault`, o un campo o método en el propio enum:
  ```java
  return tipo.getMimeType();
  ```

## 6. Reglas de validación → datos + bucle

- ❌ INCORRECTO (1 rama por regla):
  ```java
  if (c.getAlias() == null) messages.add("alias", "Falta el alias");
  if (c.getCaducidad().isBefore(hoy)) messages.add("caducidad", "Caducado");
  // … N reglas
  ```
- ✅ CORRECTO (≈2 ramas):
  ```java
  private static final List<Regla<CertificadoDigital>> REGLAS = List.of(
      new Regla<>("alias", "Falta el alias", c -> c.getAlias() == null),
      new Regla<>("caducidad", "Caducado", c -> c.getCaducidad().isBefore(LocalDate.now())));
  REGLAS.stream().filter(r -> r.falla(c)).forEach(r -> messages.add(r.campo(), r.mensaje()));
  ```
- Cada predicado sigue probándose: un test por regla.

## 7. Bucles con `if` de filtrado o búsqueda → Stream API

- ❌ INCORRECTO:
  ```java
  for (Item i : items) { if (i.getId().equals(id)) { return i; } }
  return null;
  ```
- ✅ CORRECTO: `return items.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);`
- Para borrar: `items.removeIf(i -> i.getId().equals(id));`

## 8. `equals`/`hashCode` a mano

- ✅ CORRECTO: `record` si la clase es un valor inmutable (sin cambiar su API pública).
- ✅ CORRECTO: `o instanceof ResultadoFirmaImpl r && Objects.equals(a, r.a) && …` en vez de `getClass() != o.getClass()` + cast + comparaciones con `null`.
- ❌ INCORRECTO: convertir en `record` una clase con setters o que Axelor/JPA instancia por reflexión (cambia la API).

## 9. Extraer método

Solo cuando el método hace **varias cosas claramente independientes** (preparar datos, validar, construir el resultado…); la CC se reparte y cada trozo se prueba aislado.

- ✅ CORRECTO: `copyMapToEntity` → `copiarEscalares(...)`, `copiarColecciones(...)`, `copiarReferencias(...)`, cada uno con un nombre que dice qué hace.
- ❌ INCORRECTO: partir un bucle en `parte1()`/`parte2()` que solo tienen sentido juntos (esconde la CC sin bajar el riesgo).
- Los métodos extraídos son `private` (o package-private si hace falta probarlos); la firma del original **no** cambia.

## 10. Condición compuesta → predicado con nombre

- ❌ INCORRECTO: `if (f != null && f.getCertificado() != null && !f.getCertificado().isCaducado() && f.isValida())`
- ✅ CORRECTO: `if (esFirmaVigente(f))`, con `esFirmaVigente` probado por separado.

## 11. Aplanar y quitar ramas muertas

- Borra condiciones imposibles y comprobaciones que ya garantiza el tipo o el llamante (tras confirmarlo con `ide_find_references`).
- ❌ INCORRECTO: `if (x != null)` justo después de `Objects.requireNonNull(x)`.
- ❌ INCORRECTO: quitar una rama "muerta" sin comprobar que ningún llamante puede llegar a ella.

## 12. Kotlin

- `require`, `check`, `requireNotNull`, `let` y `?:` son `inline`: la rama puede quedarse en el bytecode del método. Mide antes de contar con ellos.
- ✅ CORRECTO: `when` exhaustivo sobre un `enum`/`sealed` sin `else` de relleno.
- ❌ INCORRECTO: encadenar `?.let { … } ?: …` pensando que baja la CC (suele subirla).
