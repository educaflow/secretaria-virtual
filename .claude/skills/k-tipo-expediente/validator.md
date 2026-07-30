# El `StateEventValidator` — validación por estado+evento (Kotlin)

Clase Kotlin (interfaz marcadora `StateEventValidator`) con un método por cada par (estado, evento) que declara las reglas de validación de ese evento. Ejemplo real: `tramites/justificacion_falta_profesorado/v1/StateEventValidatorImpl.kt`.

## 1. CRITICAL: doble función — validar Y whitelist de campos

La lista de campos con reglas define **qué propiedades puede enviar el cliente en ese evento**: lo que no aparece en el `rules { }`, no se copia del request (defensa de mass-assignment, ver `k-secure-coding`).

- Un evento sin datos igualmente **MUST** tener su método con `rules { }` vacío.
- Si un campo debe llegar del cliente pero no tiene restricciones, dale igualmente entrada en `rules` (aunque sea sin reglas) — si no, se ignora en silencio.
- **MUST NOT** dar reglas a campos que rellena el servidor (PDFs generados, resguardos, año…): sería abrir la puerta a que el cliente los dicte.

## 2. Anatomía y convención de nombres

```kotlin
package com.educaflow.tramites.justificacion_falta_profesorado.v1

import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model
// Recomendado: alias también para los enums — minimiza el diff entre versiones (versionado.md)
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesoradoV1 as TipoResolucion

class StateEventValidatorImpl : StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules = rules {
        field(model::getDias) {
            +Required()
            +Pattern("^...$")
        }
        field(model::getHoraFin) {
            +ifValueIn(model::getTipoJornadaFalta, listOf(TipoJornadaFalta.JORNADA_PARCIAL)) {
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
- El alias `as model` del import es lo que hace funcionar `model::getX`; el esqueleto ya lo trae.

## 3. Catálogo de reglas del DSL

Paquete `com.educaflow.base.infrastructure.validation.rules`:

| Regla | Uso |
|---|---|
| `Required()` | Campo obligatorio |
| `Pattern("^...$")` | Regex sobre el valor |
| `MinValue(n)` / `MaxValue(n)` | Rango numérico (admite expresiones: `MaxValue(LocalDate.now().year)`) |
| `MinLength(n)` / `MaxLength(n)` | Longitud de texto |
| `GreaterThan(model::getOtroCampo)` | Comparación entre campos |
| `NoAllUpperCase()` | Rechaza texto todo en mayúsculas |
| `FileType(listOf("application/pdf", ...))` | MIME types admitidos de un `MetaFile` |
| `FileMaxSize(n, SizeUnit.MB)` | Tamaño máximo de un `MetaFile` |
| `ifValueIn(model::getCampo, listOf(...)) { +... }` | Reglas condicionales según el valor de otro campo |
| `FirmaPdf(model::getOriginal, model::getDniFirma)` | §4 |

## 4. `FirmaPdf` — validar la firma de AutoFirma en servidor

`FirmaPdf(original, dniGetter)` sobre el campo del PDF firmado valida que lo subido es el original firmado con AutoFirma por el DNI exigido:

```kotlin
field(model::getPdfSolicitudFirmado) {
    +Required()
    +FirmaPdf(model::getPdfSolicitud, model::getDniFirmaDocumentoEntrada)
}
```

Comprueba: exactamente una firma nueva, certificado en la lista de confiables, que no es sello de tiempo, texto plano del PDF idéntico al original, y DNI del certificado coincidente. Es la tercera pieza del patrón AutoFirma (`eventmanager.md` §6.5).

## 5. Mantenimiento manual — el build NO lo comprueba

- El check de build del validator está **vacío**: un método que falte (o con nombre mal escrito) solo se detecta en **runtime** al disparar el evento ("No se ha encontrado el método: getForState<Estado>InEvent<Evento>…").
- Al añadir/quitar/renombrar estados o eventos en el `TipoExpedienteInstance.xml`, **MUST** actualizar los métodos a mano y probar los eventos en runtime.
- El esqueleto genera un método por **cada** evento declarado, incluido `DELETE`; el runtime nunca invoca el de `DELETE` (sobra pero es inofensivo).

## 6. Anti-patrones

- **MUST NOT** poner la lógica de negocio aquí (transiciones, generación de PDF…): eso es del EventManager. Aquí solo restricciones sobre los datos de entrada.
- **MUST NOT** dar reglas a campos que rellena el servidor (§1).
- **MUST NOT** confiar en `readonly`/`showIf`/`hidden` de la vista como defensa: la única frontera real es esta whitelist (`k-secure-coding`).
