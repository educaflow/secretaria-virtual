# El `StateEventValidator` — validación por estado+evento (Kotlin)

Clase Kotlin (interfaz marcadora `StateEventValidator`) con un método por cada par (estado, evento) que declara las reglas de validación de ese evento. Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`); para ver uno de verdad, abre el `StateEventValidatorImpl.kt` de cualquier fase bajo `src/main/java/com/educaflow/tramites/`.

**Hay uno por fase**, en `<vN>/<fase en minúsculas>/StateEventValidatorImpl.kt`, y cada uno cubre **solo las parejas (estado, evento) de los estados de su propia fase**. En runtime lo resuelve `ExpedienteLocator` con el estado **desde el que** se dispara el evento (`SKILL.md` §1.6).

## 1. CRITICAL: doble función — validar Y whitelist de campos

La lista de campos con reglas define **qué propiedades puede enviar el cliente en ese evento**: lo que no aparece en el `rules { }`, no se copia del request (defensa de mass-assignment, ver `k-secure-coding`).

- Un evento sin datos igualmente **MUST** tener su método con `rules { }` vacío.
- Si un campo debe llegar del cliente pero no tiene restricciones, dale igualmente entrada en `rules` (aunque sea sin reglas) — si no, se ignora en silencio.
- **MUST NOT** dar reglas a campos que rellena el servidor (PDFs generados, resguardos, año…): sería abrir la puerta a que el cliente los dicte.

## 2. Anatomía y convención de nombres

```kotlin
package com.educaflow.tramites.mi_tramite.v1.recepcion

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
- **El `<Estado>` es el código del estado dentro de su fase**, sin la fase: la clase ya está en el paquete de su fase, y `Tramitador` compone el nombre del método con el `codeState`, que ya es ese código (`SKILL.md` §1.5).
- El alias `as model` del import es lo que hace funcionar `model::getX`; el esqueleto ya lo trae.
- El alias de los enums es **opcional** y solo para acortar; también vale escribir el nombre completo (`TipoPeriodoMiTramiteV1`) sin aliasar. Lo que **MUST** cumplirse es que cada enum que aparezca en el cuerpo esté importado con **ese mismo nombre**: si aliasas, aliasa el enum que vas a usar.

## 3. Catálogo de reglas del DSL

Las reglas (`ValidationRule`) están en `com.educaflow.base.infrastructure.validation.rules`; los constructores del DSL (`rules`, `field`, `ifValueIn`, `ifValueNotIn`, `ifLambda`) están en `com.educaflow.base.infrastructure.validation.dsl`. El esqueleto generado ya trae ambos imports.

**MUST** usar la regla del catálogo cuando una cubra la comprobación; `Lambda`/`ifLambda` (§3.1) solo cuando **ninguna** la cubra. Lo que suele tentar a escribir un predicado, y la genérica que ya lo hace:

| Tentación | Genérica |
|---|---|
| «no puede estar vacío» (`tieneX`) | `Required()` |
| «no posterior a hoy» / «no anterior a hoy» | `PastOrToday()` / `FutureOrToday()` |
| «fin posterior a inicio» (fechas, horas, importes…) | `GreaterThan(model::getInicio)` |
| «dentro de un rango», también de fechas | `MinValue(...)` / `MaxValue(...)` |
| «solo si el enum vale X» (`necesitaX`) | `ifValueIn(model::getEnum, listOf(...))`, salvo que la función ya exista porque la usa también un `trigger*` (`SKILL.md` §1.8) |

- El mensaje fijo de la genérica es el que ve el usuario. Querer otro texto **no** justifica una `Lambda`.
- Las reglas de comparación (`GreaterThan`…), de fecha (`Past*`/`Future*`) y de rango (`MinValue`/`MaxValue`) dan por válido un valor nulo, y también un «otro campo» nulo: la obligatoriedad la pone `Required`. **MUST NOT** anidar `ifLambda(util::tieneX)` como guarda delante de ellas.

| Regla | Uso |
|---|---|
| `Required()` | Campo obligatorio |
| `Pattern("^...$")` | Regex sobre el valor |
| `MinValue(v)` / `MaxValue(v)` | Rango sobre cualquier `Comparable` (enteros, decimales, fechas, horas); admite expresiones: `MaxValue(LocalDate.now().year)`, `MinValue(LocalDate.now(Convert.defaultZoneId).minusYears(1))` |
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

Una comprobación que solo tiene sentido en este tipo y que **ninguna regla del catálogo cubre** (consulta a BD, cálculo, cruce de campos que no sea una comparación simple —para esa ya están `GreaterThan`/`LessThan`/`EqualTo`—) **no es una regla nueva**: es una función estática `boolean` de `<Code>Util` (`SKILL.md` §1.8) que el validador declara con una de estas dos:

| | Qué hace | Cuándo |
|---|---|---|
| `+Lambda(util::funcion, "mensaje")` | Regla terminal: si la función devuelve `false`, rechaza el campo con el mensaje | La función **es** la comprobación |
| `+ifLambda(util::funcion) { +... }` | Condicional: si la función devuelve `true`, aplica las reglas anidadas; si devuelve `false`, el campo se da por válido | La función decide **si** se aplican otras reglas (la versión de `ifValueIn` para condiciones que no son «este otro campo vale X») |

Las dos pasan a la función el **expediente entero**, no el valor del campo. Kotlin infiere el tipo de la referencia al método estático Java: `import com.educaflow.tramites.mi_tramite.v1.MiTramiteV1Util as util` y `util::funcion`.

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
- La función lanza `IllegalStateException` si le falta un dato que fija el servidor: **MUST NOT** devolver `true` en silencio cuando no puede decidir (`SKILL.md` §1.8).

## 4. Firma de un documento por el usuario

Las reglas del evento que presenta un documento firmado por el usuario (`ifSituacionFirma`, `ClaveCertificadoValida`, `FirmaPdf`) están en la receta `recetas/firma.md` §1.4, junto con el resto de piezas del patrón. **MUST** seguirla entera: son dos `field(...)` con dos ramas complementarias, no una regla suelta.

## 5. Los tests que comprueban el validator

Lo comprueban los tests `src/test/java/com/educaflow/tiposexpedientes/stateeventvalidator/StateEventValidatorTest.java` (`./gradlew test`). Antes **no lo comprobaba nada**: el check del build estaba vacío porque Spoon solo parsea Java y este fichero es Kotlin, y un método que faltara solo se descubría en **runtime** al disparar el evento ("No se ha encontrado el método: getForState<Estado>InEvent<Evento>…"). Los tests leen bytecode, así que sí alcanzan a Kotlin.

Las reglas se comprueban **fase a fase**: la unidad no es el tipo de expediente, sino cada una de sus fases, y el mensaje de error la identifica como `MiTramiteV1/RECEPCION`.

1. **V0**: la clase `<paquete de la fase>.StateEventValidatorImpl` existe compilada e implementa `StateEventValidator`.
2. **V1**: por cada pareja (estado, evento) **de la fase**, **salvo las del evento `DELETE`**, exactamente un `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>(): BeanValidationRules` sin parámetros. El mensaje de fallo trae el **código del método listo para pegar**.
3. **V2**: no puede sobrar ningún método anotado cuya pareja no sea de la propia fase (si es de otra, su sitio es el validator de esa otra).

- Ojo al recuento: se cuenta por **pareja**, no por evento. Un mismo evento declarado en tres estados son **tres** métodos del validator, aunque en el PhaseEventManager sea un único `trigger` — y si esos estados están en fases distintas, los métodos se reparten entre los validators de esas fases.
- **`DELETE` es la excepción**: `Tramitador` se salta la validación cuando el evento es `DELETE` y borra sin copiar campos, así que ese método nunca se invoca y solo podría contener un `rules { }` vacío. **MUST NOT** escribirlo: el esqueleto ya no lo genera y ningún tipo lo tiene. V1 no lo exige; V2 tampoco lo da por sobrante si aparece, porque su pareja sí está declarada en el XML.
- Al añadir/quitar/renombrar estados o eventos en el `TipoExpedienteInstance.xml`, **MUST** actualizar los métodos a mano; los tests dicen exactamente cuáles y con qué código. Si **mueves un estado de fase**, sus métodos se mudan de fichero (el nombre no cambia, porque es el corto).
- Solo cuentan los métodos **declarados en la propia clase de la fase**, igual que en el PhaseEventManager (`phaseeventmanager.md` §7): `Tramitador` los resuelve con `getDeclaredMethods()` sobre la clase concreta, así que uno heredado de una superclase no se encontraría ni en los tests ni en runtime.

## 6. Anti-patrones

- **MUST NOT** poner la lógica de negocio aquí (transiciones, generación de PDF…): eso es del PhaseEventManager. Aquí solo restricciones sobre los datos de entrada.
- **MUST NOT** dar reglas a campos que rellena el servidor (§1).
- **MUST NOT** confiar en `readonly`/`showIf`/`hidden` de la vista como defensa: la única frontera real es esta whitelist (`k-secure-coding`).
- **MUST NOT** factorizar los `getForState<Estado>InEvent<Evento>` comunes a una superclase compartida entre fases o versiones: solo se ven los declarados en la clase de la fase (§5).
- **MUST NOT** crear una `ValidationRule` en la carpeta de la versión: función `boolean` en `<Code>Util` + `Lambda`/`ifLambda` (§3.1). Solo si la comparten varios tipos es una regla, y entonces vive en `tramites/util/` o en el catálogo base.
- **MUST NOT** usar `Lambda` para lo que ya hace una genérica (`Required`, `PastOrToday`, `GreaterThan`, `MinValue`…): ver la tabla de tentaciones de §3.
