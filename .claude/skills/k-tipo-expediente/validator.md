# El `StateEventValidator` — validación por estado+evento (Kotlin)

Clase Kotlin (interfaz marcadora `StateEventValidator`) con un método por cada par (estado, evento) que declara las reglas de validación de ese evento. Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`); para ver uno de verdad, abre el `StateEventValidatorImpl.kt` de cualquier fase bajo `src/main/java/com/educaflow/tramites/`.

**Hay uno por fase**, en `<vN>/<fase en minúsculas>/StateEventValidatorImpl.kt`, y cada uno cubre **solo las parejas (estado, evento) de los estados de su propia fase**: las reglas de un evento son las del estado **desde el que** se dispara (`SKILL.md` §1.6).

## 1. CRITICAL: doble función — validar Y whitelist de campos

La lista de campos con reglas define **qué propiedades puede enviar el cliente en ese evento**: lo que no aparece en el `rules { }`, no se copia del request (defensa de mass-assignment, ver `k-secure-coding`).

- Un evento sin datos igualmente **MUST** tener su método con `rules { }` vacío.
- En un evento de sistema (`systemEvents`, `SKILL.md` §2.1) no hay cliente: el `requestData` lo monta el código del servidor que lo dispara (`phaseeventmanager.md` §5.1). Los campos que trae **MUST** llevar `+Required()`; si no trae ninguno, `rules { }` vacío.
- Si un campo debe llegar del cliente pero no tiene restricciones, dale igualmente entrada en `rules` (aunque sea sin reglas) — si no, se ignora en silencio.
- **MUST NOT** dar reglas a campos que rellena el servidor (PDFs generados, resguardos, año…): sería abrir la puerta a que el cliente los dicte.

## 2. Anatomía y convención de nombres

```kotlin
package com.educaflow.tramites.mi_tramite.v1.entrada

import com.educaflow.subsystem.expedientes.db.MiTramiteV1 as model
// Recomendado: alias también para los enums — minimiza el diff entre versiones (recetas/versionado.md)
import com.educaflow.subsystem.expedientes.db.TipoPeriodoMiTramiteV1 as TipoPeriodo

class StateEventValidatorImpl : StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules = rules {
        field(model::getDias) {
            +Required()
            +Pattern("^...$")
        }
        field(model::getHoraFin) {
            +ifValueIn(model::getTipoPeriodo, listOf(TipoPeriodo.PERIODO_PARCIAL)) {
                +Required()
                +GreaterThan(model::getHoraInicio)
            }
        }
        field(model::getJustificante) {
            +Required()
            +FileType(listOf("image/png", "image/jpeg", "application/pdf"))
            +FileMaxSize(5, SizeUnit.MB)
        }
    }
}
```

- Nombre del método: `getForState<Estado>InEvent<Evento>` en UpperCamel (`ENTRADA_DATOS`+`GUARDAR_DATOS` → `getForStateEntradaDatosInEventGuardarDatos`), anotado `@BeanValidationRulesForStateAndEvent`.
- **El `<Estado>` es el nombre del estado dentro de su fase**, sin la fase (`SKILL.md` §1.5).
- El alias `as model` del import es lo que hace funcionar `model::getX`; el esqueleto ya lo trae.
- El alias de los enums es **opcional** y solo para acortar; también vale escribir el nombre completo (`TipoPeriodoMiTramiteV1`) sin aliasar. Lo que **MUST** cumplirse es que cada enum que aparezca en el cuerpo esté importado con **ese mismo nombre**: si aliasas, aliasa el enum que vas a usar.

## 3. Catálogo de reglas del DSL

Las reglas (`ValidationRule`) están en `com.educaflow.base.infrastructure.validation.rules`; los constructores del DSL (`rules`, `field`, `ifValueIn`, `ifValueNotIn`, `ifLambda`) están en `com.educaflow.base.infrastructure.validation.dsl`. El esqueleto generado ya trae ambos imports.

**CRITICAL**: leyendo el `StateEventValidatorImpl` se tiene que saber **exactamente** qué se valida en cada evento, sin abrir otros ficheros.

- **MUST** escribir cada comprobación con las reglas que ya existen, combinándolas si hace falta (varias reglas en el mismo `field`, `ifValueIn`/`ifValueNotIn` para condicionarlas).
- **MUST NOT** crear una validación nueva —una `ValidationRule`, una función que agrupe reglas (`fun x(...): FieldValidationRules`) o una `Lambda`/`ifLambda` (§3.1)— para lo que se consiga combinando las existentes, salvo que esté **muy** justificado.
- No lo justifican: un mensaje distinto, que varios tipos repitan el mismo bloque, ni que el validador quede más corto.

Lo que suele tentar a escribir un predicado, y la genérica que ya lo hace:

| Tentación | Genérica |
|---|---|
| «no puede estar vacío» (`tieneX`) | `Required()` |
| «no posterior a hoy» / «no anterior a hoy» | `PastOrToday()` / `FutureOrToday()` |
| «fin posterior a inicio» (fechas, horas, importes…) | `GreaterThan(model::getInicio)` |
| «dentro de un rango», también de fechas | `MinValue(...)` / `MaxValue(...)` |
| «solo si el enum vale X» (`necesitaX`) | `ifValueIn(model::getEnum, listOf(...))`, salvo que la función ya exista porque la usa también un `trigger*` (`SKILL.md` §1.7) |

- El mensaje fijo de la genérica es el que ve el usuario. Querer otro texto **no** justifica una `Lambda`.
- Las reglas de comparación (`GreaterThan`…), de fecha (`Past*`/`Future*`) y de rango (`MinValue`/`MaxValue`) dan por válido un valor nulo, y también un «otro campo» nulo: la obligatoriedad la pone `Required`. **MUST NOT** anidar `ifLambda(util::tieneX)` como guarda delante de ellas.

| Regla | Uso |
|---|---|
| `Required()` | Campo obligatorio |
| `Pattern("^...$")` | Regex sobre el valor |
| `MinValue(v)` / `MaxValue(v)` | Rango sobre cualquier `Comparable` (enteros, decimales, fechas, horas); admite expresiones: `MaxValue(LocalDate.now(Convert.defaultZoneId).year)`, `MinValue(LocalDate.now(Convert.defaultZoneId).minusYears(1))` |
| `MinLength(n)` / `MaxLength(n)` | Longitud de texto |
| `GreaterThan(model::getOtroCampo)` / `GreaterThanOrEqual` / `LessThan` / `LessThanOrEqual` | Comparación con otro campo `Comparable` del modelo |
| `EqualTo(model::getOtroCampo)` / `NotEqualTo(model::getOtroCampo)` | Igualdad con otro campo del modelo |
| `Past()` / `PastOrToday()` / `Future()` / `FutureOrToday()` | Fecha respecto de hoy |
| `NoAllUpperCase()` | Rechaza texto todo en mayúsculas |
| `ListIntNumbers()` | El texto es una lista de números enteros |
| `MinListSize(n)` / `MaxListSize(n)` | Tamaño de una colección |
| `FileType(listOf("application/pdf", ...))` | MIME types admitidos de un `MetaFile` |
| `FileMaxSize(n, SizeUnit.MB)` | Tamaño máximo de un `MetaFile` |
| `FileName("^...$")` | Regex sobre el nombre de fichero de un `MetaFile` |
| `Dni()` / `Nia()` / `Nuss()` / `Phone()` / `PostalCode()` / `Iban()` | Formato de identificadores españoles, teléfono, código postal e IBAN |
| `AlwaysFail("mensaje")` / `AlwaysPass()` | Dentro de una rama condicional: rechazar siempre con ese mensaje / aceptar siempre |
| `ifValueIn(model::getCampo, listOf(...)) { +... }` *(DSL, paquete `...validation.dsl`)* | Reglas condicionales según el valor de otro campo; su negación es `ifValueNotIn` |
| `Lambda(util::funcion, "mensaje")` | Rechaza el campo con el mensaje si la función estática de `<Code>Util` devuelve `false`; §3.1 |
| `ifLambda(util::funcion) { +... }` *(DSL)* | Reglas condicionales según una función estática de `<Code>Util`; §3.1 |
| `ifSituacionFirma(...) { +... }`, `ClaveCertificadoValida()`, `FirmaPdf(model::getOriginal)` | Firma de un documento por el usuario; §4 → `recetas/firma.md` §1.4 |

La tabla es un resumen de uso, no un inventario cerrado: la **fuente de verdad** es el contenido del paquete `...validation.rules` (un fichero `*Rules.kt` por familia). Antes de inventarte una regla, mira si ya existe ahí.

### 3.1 Comprobaciones propias del tipo: `Lambda` e `ifLambda`

Una comprobación que solo tiene sentido en este tipo y que **ninguna regla del catálogo cubre** (consulta a BD, cálculo, cruce de campos que no sea una comparación simple —para esa ya están `GreaterThan`/`LessThan`/`EqualTo`—) **no es una regla nueva**: es una función estática `boolean` de `<Code>Util` (`SKILL.md` §1.7) que el validador declara con una de estas dos:

| | Qué hace | Cuándo |
|---|---|---|
| `+Lambda(util::funcion, "mensaje")` | Regla terminal: si la función devuelve `false`, rechaza el campo con el mensaje | La función **es** la comprobación |
| `+ifLambda(util::funcion) { +... }` | Condicional: si la función devuelve `true`, aplica las reglas anidadas; si devuelve `false`, el campo se da por válido | La función decide **si** se aplican otras reglas (la versión de `ifValueIn` para condiciones que no son «este otro campo vale X») |

Las dos pasan a la función el **bean sobre el que se declara el `field`**, no el valor del campo: el expediente entero, o la `Persona` si el `field` va anidado sobre una relación (`modelo.md` §2.1). Kotlin infiere el tipo de la referencia al método estático Java: `import com.educaflow.tramites.mi_tramite.v1.MiTramiteV1Util as util` y `util::funcion`.

```kotlin
field(model::getCiclo) {
    +Required()
    +Lambda(util::sinOtraSolicitudEnCurso, "Ya tiene una solicitud en curso para este ciclo")
}
field(model::getMotivoRechazo) {
    +ifLambda(util::esRechazo) {
        +Required()
        +MaxLength(500)
    }
}
```

- El validador se queda **declarativo**: **MUST NOT** contener JPQL, `JpaRepository`, `Beans.get` ni lambdas con cuerpo; solo referencias `util::funcion`.
- ✅ CORRECTO: `+Lambda(util::sinOtraSolicitudEnCurso, "...")` (consulta a BD: no hay genérica).
- ❌ INCORRECTO: `+Lambda(util::tieneFechaInicio, "Debe indicar la fecha")` (es `Required()`; el mensaje distinto no lo justifica).
- ❌ INCORRECTO: `+ifLambda(util::tieneFechaFin) { +ifLambda(util::tieneFechaInicio) { +Lambda(util::fechaFinPosteriorAFechaInicio, "...") } }` (es `+GreaterThan(model::getFechaInicio)`, que ya maneja los nulos).
- El mensaje va en el validador, no en la función: la función devuelve `boolean` y no sabe de mensajes.
- La función lanza `IllegalStateException` si le falta un dato que fija el servidor: **MUST NOT** devolver `true` en silencio cuando no puede decidir (`SKILL.md` §1.7).

### 3.2 Reglas de las fases comunes

Los campos comunes de las fases `ENTRADA` y `VERIFICACION` (`SKILL.md` §1.2) llevan las mismas reglas en todos los tipos, escritas con las reglas del catálogo de §3 **dentro del propio validador**:

| Evento | Campo | Reglas |
|---|---|---|
| `CONTINUAR` de `PENDIENTE_DOCUMENTO_ESCANEADO` | `pdfSolicitudFirmada` | `Required()` + `FileType(listOf("application/pdf"))` + `FileMaxSize(10, SizeUnit.MB)` |
| `VERIFICAR` de `PENDIENTE_VERIFICACION` | `resultadoVerificacion` | `Required()` |
| `VERIFICAR` de `PENDIENTE_VERIFICACION` | `textoSubsanacion` | Solo si `resultadoVerificacion` es `SUBSANAR`: `Required()` + `MinLength(10)` + `MaxLength(1000)` |

```kotlin
import com.educaflow.subsystem.expedientes.db.ResultadoVerificacionMiTramiteV1 as ResultadoVerificacion
...
// entrada/StateEventValidatorImpl.kt
@BeanValidationRulesForStateAndEvent
fun getForStatePendienteDocumentoEscaneadoInEventContinuar(): BeanValidationRules = rules {
    field(model::getPdfSolicitudFirmada) {
        +Required()
        +FileType(listOf("application/pdf"))
        +FileMaxSize(10, SizeUnit.MB)
    }
}

// verificacion/StateEventValidatorImpl.kt
@BeanValidationRulesForStateAndEvent
fun getForStatePendienteVerificacionInEventVerificar(): BeanValidationRules = rules {
    field(model::getResultadoVerificacion) {
        +Required()
    }
    field(model::getTextoSubsanacion) {
        +ifValueIn(model::getResultadoVerificacion, listOf(ResultadoVerificacion.SUBSANAR)) {
            +Required()
            +MinLength(10)
            +MaxLength(1000)
        }
    }
}
```

- Aunque todos los tipos repitan estos bloques, **MUST NOT** agruparlos en una función común (§3).

- ✅ CORRECTO: `field(model::getResultadoVerificacion) { +Required() }`
- ❌ INCORRECTO: `+resultadoVerificacion(model::getResultadoVerificacion)` (función que esconde un `Required()`: la regla no se ve en el validador)

## 4. Firma de un documento por el usuario

Las reglas del evento que presenta un documento firmado por el usuario (`ifSituacionFirma`, `ClaveCertificadoValida`, `FirmaPdf`) están en la receta `recetas/firma.md` §1.4, junto con el resto de piezas del patrón. **MUST** seguirla entera: son dos `field(...)` con dos ramas complementarias, no una regla suelta.

## 5. Los tests que comprueban el validator

Lo comprueban los tests (`SKILL.md` §3.3), **fase a fase**; el mensaje de fallo trae el código del método que falta, listo para pegar.

1. **V0**: existe `<paquete de la fase>.StateEventValidatorImpl` e implementa `StateEventValidator`.
2. **V1**: por cada pareja (estado, evento) **de la fase** —eventos de sistema incluidos—, **salvo las de `DELETE`**, exactamente un `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>(): BeanValidationRules` sin parámetros.
3. **V2**: no sobra ningún método cuya pareja no sea de la propia fase.

- Se cuenta por **pareja**, no por evento: un mismo evento declarado en tres estados son **tres** métodos del validador, aunque en el `PhaseEventManagerImpl` sea un único `trigger`.
- **MUST NOT** escribir el método de `DELETE`: ese evento no valida ni copia campos, así que nunca se invoca.
- Al añadir, quitar o renombrar estados o eventos en el XML, actualiza los métodos según digan los tests. Si **mueves un estado de fase**, sus métodos se mudan de fichero con el mismo nombre.
- Solo cuentan los métodos **declarados en la propia clase de la fase**: uno heredado de una superclase no lo ve ni el test ni el motor.

## 6. Anti-patrones

- **MUST NOT** poner la lógica de negocio aquí (transiciones, generación de PDF…): eso es del PhaseEventManager. Aquí solo restricciones sobre los datos de entrada.
- **MUST NOT** dar reglas a campos que rellena el servidor (§1).
- **MUST NOT** confiar en `readonly`/`showIf`/`hidden` de la vista como defensa: la única frontera real es esta whitelist (`k-secure-coding`).
- **MUST NOT** factorizar los `getForState<Estado>InEvent<Evento>` comunes a una superclase compartida entre fases o versiones: solo se ven los declarados en la clase de la fase (§5).
- **MUST NOT** crear una `ValidationRule` en la carpeta de la versión: función `boolean` en `<Code>Util` + `Lambda`/`ifLambda` (§3.1). Solo es una regla nueva si ninguna combinación de las existentes lo consigue (§3) y la comparten varios tipos, y entonces vive en `tramites/util/` (`tramites/util/CLAUDE.md`) o en el catálogo base.
- **MUST NOT** usar `Lambda` para lo que ya hace una genérica (`Required`, `PastOrToday`, `GreaterThan`, `MinValue`…): ver la tabla de tentaciones de §3.
