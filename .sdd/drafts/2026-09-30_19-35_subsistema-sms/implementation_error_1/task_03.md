---
type: implementation-task
template: system
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/util/NumeroTelefono.java` | Crear | k-code-quality | Teléfono: validar, saber si es móvil, si es de España y dar el E.164. Única clase que importa `libphonenumber` |

### Paso 2 — `base/util`: `NumeroTelefono`, `MensajeSmsUtil` y `TextUtil.trazaCompleta`

**Ficheros:** `base/util/NumeroTelefono.java` (Crear), `base/util/MensajeSmsUtil.java` (Crear), `base/util/TextUtil.java` (Modificar), `base/util/CLAUDE.md` (Modificar).

#### `com.educaflow.base.util.NumeroTelefono`

```java
public NumeroTelefono(String telefono);
private boolean esValido();
private boolean esMovil();
private boolean esDeEspana();
public boolean esMovilDeEspana();
public String enFormatoE164();
```

Encapsula un teléfono escrito por una persona. Es la **única** clase del proyecto que importa
`libphonenumber`, y ninguno de sus métodos públicos expone un tipo de esa librería (ni `PhoneNumber`, ni
`PhoneNumberUtil`, ni `PhoneNumberType`, ni `NumberParseException`): el objetivo es poder cambiar la librería
sin tocar el resto del código (`design-guidelines.md`). País por defecto: España (`"ES"`). **Estado:** un
campo privado con el número ya parseado (tipo de `libphonenumber`), `null` si el texto no se pudo parsear; se
parsea **una** vez en el constructor y todas las consultas lo reutilizan.

- **`NumeroTelefono(String)`** — **no lanza nunca** por un teléfono mal escrito: el texto viene del cliente y
  «mal escrito» es un caso de negocio (V-Sms-006), no una excepción. Parsea con el país por defecto y, si la
  librería no puede, deja el número interno a `null`.
- **`esValido()`** — `true` solo si hay número parseado **y** la librería lo considera válido. Con texto
  `null`/blanco o no parseable devuelve `false` (nunca lanza).
- **`esMovil()`** — `true` solo si hay número parseado y su tipo es móvil (`MOBILE` o
  `FIXED_LINE_OR_MOBILE`, este último para países que no distinguen). Un fijo español (`963000000`) da `false`.
- **`esDeEspana()`** — `true` solo si hay número parseado y su prefijo de país es el 34. Un `+33…` da `false`.
- **`esMovilDeEspana()`** — la conjunción de las tres consultas anteriores: hay número parseado, la librería lo
  da por válido, su tipo es móvil y su prefijo de país es el 34. Es la clasificación de **negocio** que pide
  RES-Sms-004 («el teléfono es un móvil de España»), y vive aquí —no en el servicio— para que tenga **un** solo
  dueño (`k-code-quality/disenyo.md` §«Una decisión con varios dueños»): quien necesite la pregunta la hace, y
  nadie tiene que acordarse de componer las tres. Nunca lanza.
- **`enFormatoE164()`** — devuelve el número en E.164 (`+34600111222`). Si `esMovilDeEspana()` es `false` lanza
  `IllegalStateException`: pedir el formato canónico de algo que no es el teléfono que el sistema acepta es un
  error de programación, no de negocio (`k-code-quality/disenyo.md` §«El retorno defensivo que delega»: **MUST
  NOT** devolver `null` ni el texto original «por si acaso»). Su único llamador es R-Sms-001, que corre siempre
  **después** de V-Sms-006.

Los tres primeros son **privados** a propósito: son los factores internos de `esMovilDeEspana()` y ningún
llamador del proyecto necesita ninguno por separado, así que la superficie pública de la clase son exactamente
dos métodos, `esMovilDeEspana()` y `enFormatoE164()`. Publicarlos invitaría justo a la composición a mano que
`decisiones.md` D1 quiere evitar, y a usar `esValido()` como guarda. Por lo mismo la guarda de
`enFormatoE164()` es `esMovilDeEspana()` y **no** `esValido()`: así su precondición es exactamente la que
garantiza V-Sms-006 (con `esValido()` un móvil francés válido pasaría la guarda y se guardaría un `+33…` en BD,
rompiendo RES-Sms-004).

### Verificación del Paso 2

**Verificar:** `./gradlew -q test` pasa; `grep -rn "libphonenumber" src/main/java | grep -v NumeroTelefono.java` no devuelve nada (la librería no se ha filtrado a ninguna otra clase).
