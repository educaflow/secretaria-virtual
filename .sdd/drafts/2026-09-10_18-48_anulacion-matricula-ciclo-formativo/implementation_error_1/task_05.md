---
type: implementation-task
template: expediente
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-validaciones
- k-secure-coding
- k-code-quality
- k-i18n

## Qué hay que hacer

Amplía, de forma **aditiva**, el catálogo común de reglas de validación de `base/infrastructure/validation/rules/` tal y como especifica el `### Paso 5` del diseño. Ficheros (todos con acción `Modificar`):

- `src/main/java/com/educaflow/base/infrastructure/validation/rules/RequiredRules.kt`
- `src/main/java/com/educaflow/base/infrastructure/validation/rules/StringRules.kt`
- `src/main/java/com/educaflow/base/infrastructure/validation/rules/ConditionalRules.kt`
- `src/main/java/com/educaflow/base/infrastructure/validation/rules/PdfRules.kt`

**Ninguna llamada existente puede cambiar de comportamiento**: los parámetros nuevos llevan valor por defecto y las ramas de mensaje que el diseño declara intactas **MUST** quedar intactas. Los mensajes se devuelven con `I18n.get(...)`.

La especificación del diseño es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ninguna regla, parámetro, clase ni método que el `### Paso 5` no liste (la única regla nueva es `NoAdmitido`).

## Filas de la tabla «## 6. Ficheros a crear o modificar» del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/RequiredRules.kt` | Modificar | `k-validaciones` | `Required` pasa a `data class Required(val mensaje: String = "Es requerido")`. El parámetro sustituye **solo** el mensaje de la rama «valor ausente»; las ramas «No puede estar vacío» y «No puede ser cero» conservan el suyo (Paso 5) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/StringRules.kt` | Modificar | `k-validaciones` | Mensaje opcional en `Pattern` (valor por defecto = el literal actual) y en `MinLength`/`MaxLength` (`mensaje: String? = null`; con `null` se sigue devolviendo el mensaje **calculado** actual) (Paso 5) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/ConditionalRules.kt` | Modificar | `k-validaciones` | Regla **nueva** del catálogo común `NoAdmitido(mensaje)`: falla siempre con ese mensaje; se usa **solo** dentro de una rama `ifValueIn`/`ifValueNotIn`, junto a las cuales vive (Paso 5, `decisiones.md` D7 y §14 nota 21) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/PdfRules.kt` | Modificar | `k-validaciones` | Segundo parámetro opcional `mensajeFirmaInvalida` en `FirmaPdf`, con `null` por defecto (§10.1 y Paso 5) |

## `### Paso 5 — Reglas de validación de base` del diseño (verbatim)

### Paso 5 — Reglas de validación de `base`

Aplica los cuatro cambios de la tabla §6 sobre `base/infrastructure/validation/rules/`: mensaje opcional en `Required`, `Pattern`, `MinLength` y `MaxLength` (**sin que ninguna llamada existente cambie de comportamiento**), regla nueva `NoAdmitido(mensaje)` que falla siempre con ese mensaje, y en `PdfRules.kt` la ampliación **aditiva** de `FirmaPdf`. Los mensajes se devuelven con `I18n.get(...)`, igual que hace `ClaveCertificadoValida`.

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

**Verificación:** compila y los trámites existentes siguen compilando **sin tocarlos** (`Required()`, `Pattern("…")`, `MinLength(5)`, `MaxLength(10)`, `FirmaPdf(model::getX)` siguen siendo llamadas válidas) y **sin cambiar de comportamiento**: un `Required()` sobre un `MetaFile` de tamaño 0 sigue diciendo «No puede estar vacío»; sobre un número a 0, «No puede ser cero»; un `MinLength(5)` sobre un texto de 3 sigue diciendo «Debe tener como mínimo una longitud de 5 pero tiene 3»; un `MaxLength` desbordado, «Debe tener como máximo una longitud de … pero tiene …»; un `Pattern` incumplido, «El valor no cumple con el patrón especificado»; y la llamada `FirmaPdf(model::getX)` de `justificacion_falta_profesorado` sigue devolviendo el motivo concreto que calcula `DocumentoPdfUtil.validateFirmaPdf`. Y `NoAdmitido` existe en `ConditionalRules.kt`, implementa `ValidationRule`, devuelve su mensaje para **cualquier** valor (incluido `null`) y lleva el KDoc con su restricción de uso.

