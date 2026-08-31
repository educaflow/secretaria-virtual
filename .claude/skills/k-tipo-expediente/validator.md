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
// Recomendado: alias también para los enums — minimiza el diff entre versiones (versionado.md)
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

Las reglas (`ValidationRule`) están en `com.educaflow.base.infrastructure.validation.rules`; los constructores del DSL (`rules`, `field`, `ifValueIn`, `ifValueNotIn`) están en `com.educaflow.base.infrastructure.validation.dsl`. El esqueleto generado ya trae ambos imports.

| Regla | Uso |
|---|---|
| `Required()` | Campo obligatorio |
| `Pattern("^...$")` | Regex sobre el valor |
| `MinValue(n)` / `MaxValue(n)` | Rango numérico (admite expresiones: `MaxValue(LocalDate.now().year)`) |
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
| `ifValueIn(model::getCampo, listOf(...)) { +... }` *(DSL, paquete `...validation.dsl`)* | Reglas condicionales según el valor de otro campo; su negación es `ifValueNotIn` |
| `FirmaPdf(model::getOriginal, model::getDniFirma)` | §4 |

La tabla es un resumen de uso, no un inventario cerrado: la **fuente de verdad** es el contenido del paquete `...validation.rules` (un fichero `*Rules.kt` por familia). Antes de inventarte una regla, mira si ya existe ahí.

## 4. `FirmaPdf` — validar la firma de AutoFirma en servidor

`FirmaPdf(original, dniGetter)` sobre el campo del PDF firmado valida que lo subido es el original firmado con AutoFirma por el DNI exigido:

```kotlin
field(model::getPdfSolicitudFirmado) {
    +Required()
    +FirmaPdf(model::getPdfSolicitud, model::getDniFirmaDocumentoEntrada)
}
```

Comprueba: exactamente una firma nueva, certificado en la lista de confiables, que no es sello de tiempo, texto plano del PDF idéntico al original, y DNI del certificado coincidente. Es la tercera pieza del patrón AutoFirma (`phaseeventmanager.md` §6.5).

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
