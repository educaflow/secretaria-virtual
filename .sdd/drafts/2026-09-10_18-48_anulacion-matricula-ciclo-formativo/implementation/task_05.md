---
type: implementation-task
template: expediente
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-validaciones
- k-code-quality
- k-i18n

Aplica los **cuatro** cambios del catálogo común de reglas de validación de `base/infrastructure/validation/rules/` que el diseño declara: mensaje opcional en `Required`, `Pattern`, `MinLength` y `MaxLength`; regla **nueva** `NoAdmitido(mensaje)`; y la ampliación **aditiva** de `FirmaPdf`. Todos los cambios son **aditivos**: ninguna llamada existente cambia de firma.

**Nota del descomponedor (decisión documentada):** este bloque tampoco pertenece al inventario estándar de un tipo de expediente, pero la tabla `## 6` del diseño lo lista y el `### Paso 5` lo ordena aquí, antes del `domains.xml` y de los validadores de fase que usan esas reglas; por eso es una tarea propia en esta posición.

La especificación del diseño es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ninguna regla, parámetro, método ni clase que la especificación no liste, ni cambiarse ningún literal de mensaje existente.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/RequiredRules.kt` | Modificar | `k-validaciones` | `Required` pasa a `data class Required(val mensaje: String = "Es requerido")`. El parámetro sustituye **solo** el mensaje de la rama «valor ausente»; las ramas «No puede estar vacío» y «No puede ser cero» conservan el suyo. Los tres textos pasan además por `I18n.get(...)` (Paso 5) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/StringRules.kt` | Modificar | `k-validaciones` | Mensaje opcional en `Pattern` (valor por defecto = el literal actual) y en `MinLength`/`MaxLength` (`mensaje: String? = null`; con `null` se sigue devolviendo el mensaje **calculado** actual). Los textos pasan además por `I18n.get(...)` (Paso 5) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/ConditionalRules.kt` | Modificar | `k-validaciones` | Regla **nueva** del catálogo común `NoAdmitido(mensaje)`: falla siempre con ese mensaje; se usa **solo** dentro de una rama `ifValueIn`/`ifValueNotIn`, junto a las cuales vive (Paso 5, `decisiones.md` D7 y §14 nota 21) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/PdfRules.kt` | Modificar | `k-validaciones` | Segundo parámetro opcional `mensajeFirmaInvalida` en `FirmaPdf`, con `null` por defecto (§10.1 y Paso 5) |

## Paso del diseño (verbatim)

### Paso 5 — Reglas de validación de `base`

Aplica los cuatro cambios de la tabla §6 sobre `base/infrastructure/validation/rules/`: mensaje opcional en `Required`, `Pattern`, `MinLength` y `MaxLength`, regla nueva `NoAdmitido(mensaje)` que falla siempre con ese mensaje, y en `PdfRules.kt` la ampliación **aditiva** de `FirmaPdf`.

**Todos los mensajes de estas reglas —el recibido por parámetro y el literal por defecto— se devuelven con `I18n.get(...)`**, igual que hace `ClaveCertificadoValida`. Hoy `Required`, `Pattern`, `MinLength` y `MaxLength` devuelven sus literales **sin** `I18n.get`, así que esto **sí es un cambio de comportamiento** y **MUST** declararse como tal en vez de negarlo:

- **Qué NO cambia:** ninguna llamada existente cambia de **firma** (los parámetros son opcionales y con valor por defecto, así que `Required()`, `Pattern("…")`, `MinLength(5)`, `MaxLength(10)` y `FirmaPdf(model::getX)` siguen compilando) ni de **texto en castellano** (el literal es su propia clave de traducción: sin entrada de traducción, `I18n.get` devuelve el mismo texto).
- **Qué SÍ cambia, y es deliberado:** en **valenciano**, esos mensajes pasan a salir traducidos en lugar de en castellano. Es lo que `k-i18n` exige de todo texto que ve el usuario, y arreglarlo aquí es gratis porque el cambio ya toca esas líneas. **MUST NOT** dejarse la mitad del catálogo pasando por `I18n.get` y la otra mitad no.
- **Los literales no se tocan**: «Es requerido», «No puede estar vacío», «No puede ser cero», «El valor no cumple con el patrón especificado» y los dos mensajes calculados de longitud siguen siendo **exactamente** los mismos textos; lo único que se les añade es el paso por el traductor.

**`NoAdmitido` — la única regla NUEVA del catálogo, y dónde va.** `data class NoAdmitido(val mensaje: String) : ValidationRule`, con `validate(value, bean)` devolviendo **siempre** `BusinessMessages.single(I18n.get(mensaje))`, sin mirar el valor.

- Va en **`ConditionalRules.kt`**, junto a `IfValueIn`/`IfValueNotIn`, que son las únicas con las que tiene sentido: quien abra ese fichero para escribir una rama se la encuentra al lado.
- **MUST** llevar un KDoc que declare su restricción de uso: *se usa solo dentro de una rama `ifValueIn`/`ifValueNotIn`; la rama dice cuándo aplica y esta regla dice con qué mensaje se rechaza*. Suelta rechazaría cualquier valor.
- Es **aditiva**: es una clase nueva, ninguna llamada existente la ve.
- El porqué, con las alternativas descartadas, está en `decisiones.md` **D7** y en §14 nota 21.

**`Required` — qué mensaje sustituye exactamente.** La regla actual devuelve **tres** mensajes distintos según el valor: «Es requerido» (valor `null`, `String` en blanco o `MetaFile` sin `fileName`), «No puede estar vacío» (`MetaFile` de tamaño 0) y «No puede ser cero» (`Number` a 0). Pasa a `data class Required(val mensaje: String = "Es requerido")` y el parámetro sustituye **ÚNICAMENTE** el mensaje de la rama de **valor ausente** (las tres condiciones que hoy devuelven «Es requerido»).

- **MUST** quedar **intactos** los literales «No puede estar vacío» y «No puede ser cero»: dicen otra cosa (el dato está, pero vacío o a cero) y taparlos con el texto de la spec sería una regresión de comportamiento en llamadas existentes.
- **MUST NOT** darse un parámetro por rama: ninguna llamada de este trámite lo necesita y multiplicaría la API por tres.
- Con el valor por defecto, un `Required()` existente sigue devolviendo exactamente los tres mensajes de hoy.

**`Pattern`, `MinLength` y `MaxLength` — dos formas distintas, porque su mensaje actual no es el mismo tipo de cosa.**

- `Pattern` pasa a `data class Pattern(val regex: String, val mensaje: String = "El valor no cumple con el patrón especificado")`: su mensaje actual es un literal **constante**, así que sí es expresable como valor por defecto.
- `MinLength` y `MaxLength` pasan a `data class MinLength(val min: Int, val mensaje: String? = null)` y `data class MaxLength(val max: Int, val mensaje: String? = null)`. Su mensaje actual **se construye en tiempo de validación con el valor** («Debe tener como mínimo una longitud de $min pero tiene ${value.length}»), así que **MUST NOT** intentar ponerse como valor por defecto del parámetro: no es una constante. Con `mensaje == null` la regla devuelve el mensaje **calculado** de hoy, y solo cuando no es nulo devuelve el literal recibido. Es el mismo patrón que `FirmaPdf` en este mismo paso.

`FirmaPdf` pasa a ser `data class FirmaPdf(val documentoOriginalField: KCallable<*>, val mensajeFirmaInvalida: String? = null)`:

- Cuando `mensajeFirmaInvalida` **no** es nulo sustituye **solo** el mensaje de la rama de **firma inválida** — el que hoy devuelve `errorMessage.get()` de `DocumentoPdfUtil.validateFirmaPdf` (firma ausente, certificado no confiable, texto alterado, DNI distinto…) — por ese texto.
- **MUST** quedar **intacto** el mensaje de la otra rama, la de «No es posible comprobar la firma porque su usuario no tiene un documento de identidad válido…»: es otra cosa (un problema de la cuenta, no de la firma) y **MUST NOT** taparse con el literal de la spec.
- Con `null` por defecto ninguna llamada existente cambia: la de `justificacion_falta_profesorado` sigue compilando y comportándose igual.

**Verificación:** compila y los trámites existentes siguen compilando **sin tocarlos** (`Required()`, `Pattern("…")`, `MinLength(5)`, `MaxLength(10)`, `FirmaPdf(model::getX)` siguen siendo llamadas válidas). **En castellano** el texto es el de hoy, literal a literal: un `Required()` sobre un `MetaFile` de tamaño 0 sigue diciendo «No puede estar vacío»; sobre un número a 0, «No puede ser cero»; un `MinLength(5)` sobre un texto de 3 sigue diciendo «Debe tener como mínimo una longitud de 5 pero tiene 3»; un `MaxLength` desbordado, «Debe tener como máximo una longitud de … pero tiene …»; un `Pattern` incumplido, «El valor no cumple con el patrón especificado»; y la llamada `FirmaPdf(model::getX)` de `justificacion_falta_profesorado` sigue devolviendo el motivo concreto que calcula `DocumentoPdfUtil.validateFirmaPdf`. **En valenciano** esos mismos mensajes salen traducidos: es el cambio de comportamiento declarado arriba, y **MUST** comprobarse que no falta ninguna traducción (si `apertium` no supiera traducir una palabra, el build falla, §14 nota 7). Y `NoAdmitido` existe en `ConditionalRules.kt`, implementa `ValidationRule`, devuelve su mensaje para **cualquier** valor (incluido `null`) y lleva el KDoc con su restricción de uso.

