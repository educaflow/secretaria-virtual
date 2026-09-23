---
type: implementation-task
template: expediente
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding
- k-code-quality
- k-i18n

La especificación del diseño es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ningún método, clase, campo ni acción que la especificación no liste.

Esta es la tarea de la fase **RECEPCION** (primera fase declarada en `actual/v1/TipoExpedienteInstance.xml`).

## Ficheros que cubre esta tarea (filas de la sección 6 del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/PhaseEventManagerImpl.java` | Modificar | `k-tipo-expediente` | Especificado en `## 9. Especificación de los PhaseEventManagerImpl`, fase `RECEPCION` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/StateEventValidatorImpl.kt` | Modificar | `k-tipo-expediente`, `k-secure-coding` | Especificado en `## 10. Especificación de los StateEventValidatorImpl`, fase `RECEPCION` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/views.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/fases/recepcion/views.xml` |

## Cómo materializar el `views.xml` de la fase

- **Origen:** `.sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/design/fases/recepcion/views.xml`.
- **Destino resuelto:** `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/views.xml`.
- **Cópialo literalmente**, **sobrescribiendo** el fichero actual del árbol (y el esqueleto que hubiera dejado `CreateFilesTask`), **sin regenerarlo** ni reescribir ni una línea.

## Pasos del diseño (verbatim)

### Paso 5 — `recepcion/PhaseEventManagerImpl.java`

Fichero destino: `…/actual/v1/recepcion/PhaseEventManagerImpl.java`.
FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.PhaseEventManagerImpl`.
Especificación: `## 9. Especificación de los PhaseEventManagerImpl`, subsección `### Fase RECEPCION`.
Supertipo: `extends PhaseEventManager<JustificacionFaltaProfesoradoV1> implements TareaFirmaNotifier` (sin cambios).

Verificación: compila; hay un `trigger*` por cada evento de la lista de cobertura de la fase y un `onEnter*` por cada estado, ninguno de más; la limpieza del periodo (`fechaFin`/`horaInicio`/`horaFin` según `JustificacionFaltaProfesoradoV1Util.necesitaFechaFin`/`necesitaHoraInicio`/`necesitaHoraFin`) va **inline** al principio de `triggerGuardarDatos`, **antes** de generar el PDF; el fichero no declara ningún método privado nuevo.

### Paso 6 — `recepcion/StateEventValidatorImpl.kt`

Fichero destino: `…/actual/v1/recepcion/StateEventValidatorImpl.kt`.
FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.StateEventValidatorImpl`.
Especificación: `## 10. Especificación de los StateEventValidatorImpl`, subsección `### Fase RECEPCION`.
Supertipo: `: StateEventValidator`.

Verificación: compila; existe un método por cada fila de la tabla de cobertura de la fase y ninguno de más; el fichero ya no importa `TipoJornadaFaltaJustificacionFaltaProfesoradoV1`, `MesJustificacionFaltaProfesoradoV1` ni `com.educaflow.base.infrastructure.validation.rules.Pattern`, y no menciona `getDias`, `getMes` ni `getAnyo`.

### Paso 7 — `recepcion/views.xml`

El fichero está materializado en `design/fases/recepcion/views.xml`. **Cópialo literalmente** a `…/actual/v1/recepcion/views.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Resumen estructural de la fase. Respecto al actual cambian tres cosas: la `<action-record>` nueva; el panel `avisoEstadoExpediente`, que faltaba en los dos forms genéricos de solo `EXIT`; y que el form genérico de `ENTRADA_DATOS` pasa de `-datos-falta` a `-datos-falta-view` (`decisiones.md` D4):

| estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|
| `ENTRADA_DATOS` | `CREADOR` | `-datos-profesor`, `-disconformidad-view`, `datos-falta`, `justificante-upload` | `DELETE` / `GUARDAR_DATOS` |
| `ENTRADA_DATOS` | — | `-datos-profesor`, `-disconformidad-view`, `-datos-falta-view`, `-justificante-view` + `avisoEstadoExpediente` | — / `EXIT` |
| `PENDIENTE_PRESENTACION` | `CREADOR` | `-pdfSolicitud` + paneles propios de firma | `BACK` / `PRESENTAR` (×2, excluyentes por `showIf`) |
| `PENDIENTE_PRESENTACION` | — | `-pdfSolicitud` + `avisoEstadoExpediente` | — / `EXIT` |

Verificación: el fichero existe; `diff` con el del diseño vacío; ningún `<button name="">`; existe la `<action-record>` `exp-JustificacionFaltaProfesoradoV1-onChange-tipoJornadaFalta-action`; el form genérico de `ENTRADA_DATOS` incluye `-datos-falta-view` y no `-datos-falta`; los dos forms genéricos de solo `EXIT` llevan su `<panel name="avisoEstadoExpediente">` (`vistas.md` §6.1).

## Máquina de estados (sección 3 del diseño, verbatim)

## 3. Máquina de estados

*(sin cambios)*

El ciclo de vida completo —las dos fases `RECEPCION` y `TRAMITACION`, sus cinco estados, sus eventos y todas sus transiciones— se conserva tal cual está hoy en `actual/v1/TipoExpedienteInstance.xml` y en `actual/v1/estados.puml`. Esta iniciativa no toca ninguno de los dos ficheros, así que **no** se materializan en el diseño (`design-contract.md` §8, modo modificación). Lo único que cambia dentro del estado `RECEPCION` / `ENTRADA_DATOS` es **qué datos pide** la acción `GUARDAR_DATOS` y **qué comprueba** — ver secciones 9, 10 y 11.

> Por tanto **no hay tabla de transiciones en el diseño** que filtrar para esta fase: la referencia normativa de las transiciones de `RECEPCION` es el `TipoExpedienteInstance.xml` y el `estados.puml` del as-is, que esta tarea **MUST NOT** modificar. Los `UPDATE_STATE` de los `trigger*` son los que fija la sección 9, más abajo.

## 9. Especificación de los PhaseEventManagerImpl — `### Fase RECEPCION` (verbatim, íntegra)

### Fase RECEPCION

FQCN: `…actual.v1.recepcion.PhaseEventManagerImpl`.
Supertipo: `extends PhaseEventManager<JustificacionFaltaProfesoradoV1> implements TareaFirmaNotifier` — *(sin cambios)*.
Constructor `@Inject` con `JustificacionFaltaProfesoradoV1Repository` y `super(JustificacionFaltaProfesoradoV1.class)` — *(sin cambios)*.
Dependencias inyectadas (`RegistroEntradaRepository`, `ModelServiceFactory`, `FirmaServidorHelper`), constantes (`POSICION_FIRMA_SOLICITUD`, `PAGINA_FIRMA_SOLICITUD`) e `import` de `States` de la propia versión — *(sin cambios)*.

#### `triggerGuardarDatos`

Lista **ordenada** de acciones (la numeración es normativa):

1. `LIMPIAR(fechaFin)` si no `necesitaFechaFin`; `LIMPIAR(horaInicio)` si no `necesitaHoraInicio`; `LIMPIAR(horaFin)` si no `necesitaHoraFin` (predicados de `JustificacionFaltaProfesoradoV1Util`) — **inline, al principio de `triggerGuardarDatos`**: solo se limpian los que el `tipoJornadaFalta` vigente no necesita. Es **RN-001**, y va **la primera**: el PDF de la solicitud se genera en el paso siguiente y no puede llevar un valor de una elección de tipo anterior. No va a `Util` porque su único llamante es este `trigger*` (`k-tipo-expediente` §1.8 — una utilidad de un solo llamante no se saca a `<Code>Util`); los tres `necesita*`, que sí tienen un segundo llamante (el validador), se quedan en `Util`. **MUST NOT** extraerse a un método privado: lo que se hace en un solo evento se queda inline (`k-tipo-expediente/phaseeventmanager.md` §2.2).
2. `GENERAR_PDF(SOLICITUD)`
3. `CREAR_METAFILE → pdfSolicitud`
4. *(sin cambios)* — la creación de la `TareaFirma` de prueba a través del `ModelServiceFactory`, con su comentario de que es temporal.
5. `UPDATE_STATE(States.Recepcion.PENDIENTE_PRESENTACION)`

**Delta:** solo se añade la acción 1. Las 2-5 son exactamente las que ya hay.

#### Resto de la fase

- `triggerDelete` — *(sin cambios)*: cuerpo vacío, **MUST NOT** llamar a `UPDATE_STATE`.
- `triggerBack` — *(sin cambios)*: pone `claveCertificado` a `null` y ramifica sobre el estado actual con `States.INSTANCE.getState(codePhase, codeState)`, con `default → error`. **MUST NOT** limpiar los campos del periodo: ESC-014 exige que al volver atrás el profesor encuentre lo que había escrito.
- `triggerPresentar` — *(sin cambios)*.
- `onEnterEntradaDatos`, `onEnterPendientePresentacion` — *(sin cambios)*: vacíos.
- `notify(TareaFirma, Object)` de `TareaFirmaNotifier` — *(sin cambios)*.

#### Lista de cobertura de la fase

- Eventos de la fase (unión de los `events` de `ENTRADA_DATOS` y `PENDIENTE_PRESENTACION`): `DELETE` → `triggerDelete`; `GUARDAR_DATOS` → `triggerGuardarDatos`; `BACK` → `triggerBack`; `PRESENTAR` → `triggerPresentar`.
- Estados de la fase: `ENTRADA_DATOS` → `onEnterEntradaDatos`; `PENDIENTE_PRESENTACION` → `onEnterPendientePresentacion`.

## 10. Especificación de los StateEventValidatorImpl — `### Fase RECEPCION` (verbatim, íntegra)

### Fase RECEPCION

```kotlin
package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion

import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.JustificacionFaltaProfesoradoV1Util as util
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model
```

`import` que **se añaden**: `com.educaflow.base.infrastructure.validation.dsl.ifLambda`, `com.educaflow.base.infrastructure.validation.rules.Lambda` y el alias `util` de arriba.
`import` que **se quitan** (se quedan sin uso): `TipoJornadaFaltaJustificacionFaltaProfesoradoV1`, `GreaterThan`, `MaxValue`, `MinValue`, `java.time.LocalDate`, `Pattern` (`com.educaflow.base.infrastructure.validation.rules.Pattern`) — su único uso en el as-is era `field(model::getDias) { +Pattern(...) }`, y `dias` desaparece por completo del modelo; ningún `field(...)` de esta sección 10, en ninguno de los dos estados de la fase, vuelve a llamar a `Pattern`.
Los demás (`MotivoFaltaJustificacionFaltaProfesoradoV1`, `ifValueIn`, `Required`, `MinLength`, `MaxLength`, `NoAllUpperCase`, `FileType`, `FileMaxSize`, `SizeUnit`, `FirmaPdf`, `ClaveCertificadoValida`, `ifSituacionFirma`) siguen en uso.

#### Tabla de cobertura

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `ENTRADA_DATOS` | `GUARDAR_DATOS` | `getForStateEntradaDatosInEventGuardarDatos` | sí |
| `ENTRADA_DATOS` | `DELETE` | **ninguno** (exento) | — |
| `PENDIENTE_PRESENTACION` | `BACK` | `getForStatePendientePresentacionInEventBack` | `rules { }` vacío |
| `PENDIENTE_PRESENTACION` | `PRESENTAR` | `getForStatePendientePresentacionInEventPresentar` | sí |

#### `getForStateEntradaDatosInEventGuardarDatos` — **cambia entero el bloque del periodo**

```kotlin
return rules {
    field(model::getTipoJornadaFalta) {
        +Lambda(util::tieneTipoJornadaFalta, "Debe indicar el tipo de jornada faltada")
    }
    field(model::getFechaInicio) {
        +Lambda(util::tieneFechaInicio, "Debe indicar la fecha")
        +Lambda(util::fechaInicioNoAnteriorAHaceUnAnyo, "La fecha debe ser de los últimos 12 meses")
        +Lambda(util::fechaInicioNoPosteriorAHoy, "La fecha no puede ser posterior a hoy")
    }
    field(model::getFechaFin) {
        +ifLambda(util::necesitaFechaFin) {
            +Lambda(util::tieneFechaFin, "Debe indicar la fecha de fin")
            +ifLambda(util::tieneFechaFin) {
                +ifLambda(util::tieneFechaInicio) {
                    +Lambda(util::fechaFinPosteriorAFechaInicio, "La fecha de fin debe ser posterior a la fecha de inicio")
                }
            }
            +Lambda(util::fechaFinNoPosteriorAHoy, "La fecha de fin no puede ser posterior a hoy")
        }
    }
    field(model::getHoraInicio) {
        +ifLambda(util::necesitaHoraInicio) {
            +Lambda(util::tieneHoraInicio, "Debe indicar la hora de inicio")
        }
    }
    field(model::getHoraFin) {
        +ifLambda(util::necesitaHoraFin) {
            +Lambda(util::tieneHoraFin, "Debe indicar la hora de fin")
            +ifLambda(util::tieneHoraFin) {
                +ifLambda(util::tieneHoraInicio) {
                    +Lambda(util::horaFinPosteriorAHoraInicio, "La hora de fin debe ser posterior a la hora de inicio")
                }
            }
        }
    }
    field(model::getMotivoFalta) {
        +Required()
    }
    field(model::getOtroMotivo) {
        +ifValueIn(model::getMotivoFalta, listOf(MotivoFaltaJustificacionFaltaProfesoradoV1.OTROS)) {
            +Required()
            +NoAllUpperCase()
            +MinLength(5)
            +MaxLength(100)
        }
    }
    field(model::getJustificante) {
        +Required()
        +FileType(listOf("image/png","image/jpeg","image/gif","application/pdf"))
        +FileMaxSize(5, SizeUnit.MB)
    }
}
```

**Delta:** desaparecen los `field(model::getDias)`, `field(model::getMes)` y `field(model::getAnyo)` enteros; el bloque de `tipoJornadaFalta`, `horaInicio` y `horaFin` se reescribe; se añaden `fechaInicio` y `fechaFin`. Los tres últimos `field(...)` (`motivoFalta`, `otroMotivo`, `justificante`) se conservan **literalmente** como están hoy.

Por qué así y no de otra forma:

- Los mensajes son los que fija la especificación, y las reglas del catálogo base llevan el suyo dentro (`"Es requerido"`, `"El valor debe ser mayor que …"`). `Lambda(util::…, "<mensaje>")` es el mecanismo documentado para una comprobación propia de un tipo con su texto (ver `decisiones.md` D1).
- La **condición** de cuándo aplica cada grupo se lee **en la rama** (`ifLambda(util::necesitaFechaFin)`), no dentro de las reglas: las reglas de dentro dan por hecho que están en su caso. Por eso las comparaciones `fechaFinPosteriorAFechaInicio`/`horaFinPosteriorAHoraInicio` van envueltas en `ifLambda(util::tieneFechaFin) { ifLambda(util::tieneFechaInicio) { … } }` y su equivalente de horas: la ausencia de cualquiera de los dos operandos la excluye la rama, no el predicado (el DSL no corta en el primer fallo de un `field(...)`), y no depende de que `necesitaHoraFin` implique `necesitaHoraInicio`.
- Las ramas son complementarias por construcción: `necesitaX` / `!necesitaX`.

#### Los otros dos métodos

`getForStatePendientePresentacionInEventBack` (`rules { }` vacío) y `getForStatePendientePresentacionInEventPresentar` — *(sin cambios)*.

#### Frontera de confianza (CRITICAL)

- Los cinco campos del periodo (`tipoJornadaFalta`, `fechaInicio`, `fechaFin`, `horaInicio`, `horaFin`) son `usuario` y **MUST** estar los cinco en un `field(...)` de `GUARDAR_DATOS`: el conjunto de `field(...)` **es** la lista de campos que el cliente puede dictar en ese evento, y un campo que no esté ahí no se copia desde la petición (el profesor escribiría y el valor se perdería en silencio). `fechaFin`, `horaInicio` y `horaFin` están aunque en algunos tipos no se pidan: quien decide si el valor sobrevive es RN-001 en el servidor, no la ausencia del `field(...)`.
- **MUST NOT** aparecer en ningún `field(...)` de este evento ningún campo `servidor` (`pdfSolicitud`, `pdfSolicitudFirmado`, `pdfJustificanteRegistroEntrada`, `pdfResolucion`), ni `codePhase`, `codeState`, `abierto`, `centro` ni `usuarioRegistrador`.
- `required="true"`, `showIf` y el prefijo `-` del `views.xml` son **UX, nunca defensa**. Que un panel esté oculto no impide enviar su campo por la petición: lo que lo impide es no estar en el `field(...)`, y lo que lo corrige es RN-001.
- **CRITICAL** — nada de esto protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1`, que **no** pasa por el `Tramitador`. Es el agujero conocido documentado en `CLAUDE.md`; este diseño **MUST NOT** intentar taparlo por su cuenta ni reintroducir un `ModelService` deny-all de expedientes.

## Filas del reparto de reglas (sección 11) que aplican a esta fase

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Obligatoriedad de un campo **en un evento** | **DSL del validador** | `+Required()`, o `+Lambda(util::tiene…, "<mensaje>")` cuando la spec fija el texto |
| Formato, rango, longitud, comparación entre campos, tipo/tamaño de fichero, firma | **DSL del validador** | `Pattern`, `MinLength`/`MaxLength`, `FileType`/`FileMaxSize`, `FirmaPdf`, `Lambda` |
| Obligatoriedad **condicional** (depende del valor de otro campo) | **DSL del validador** | `ifValueIn(...) { … }` / `ifLambda(util::necesita…) { … }` |
| Qué campos puede dictar el cliente en un evento | **DSL del validador** | el conjunto de `field(...)` de esa pareja |
| Efectos: generar PDF, firmar, registrar, transicionar, limpiar, calcular | **`trigger*`** del `PhaseEventManagerImpl` | lista de acciones de la sección 9 |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf` en el panel del modo, `required`, prefijo `-`, `<action-record>` del `onChange` — **solo UX, NUNCA defensa** |

| Regla | Capa | Cómo |
|---|---|---|
| VAL-…-001 tipo de jornada obligatorio | validador | `field(getTipoJornadaFalta) { +Lambda(util::tieneTipoJornadaFalta, "Debe indicar el tipo de jornada faltada") }` |
| VAL-…-002 fecha de inicio obligatoria | validador | `+Lambda(util::tieneFechaInicio, "Debe indicar la fecha")` |
| VAL-…-003 fecha de inicio no anterior a hace un año | validador | `+Lambda(util::fechaInicioNoAnteriorAHaceUnAnyo, "La fecha debe ser de los últimos 12 meses")` |
| VAL-…-004 fecha de inicio no posterior a hoy | validador | `+Lambda(util::fechaInicioNoPosteriorAHoy, "La fecha no puede ser posterior a hoy")` |
| VAL-…-005 fecha de fin obligatoria (condicional) | validador | dentro de `ifLambda(util::necesitaFechaFin)` |
| VAL-…-006 fecha de fin posterior a la de inicio | validador | dentro de `ifLambda(util::necesitaFechaFin)` |
| VAL-…-007 fecha de fin no posterior a hoy | validador | dentro de `ifLambda(util::necesitaFechaFin)` |
| VAL-…-008 hora de inicio obligatoria (condicional) | validador | dentro de `ifLambda(util::necesitaHoraInicio)` |
| VAL-…-009 hora de fin obligatoria (condicional) | validador | dentro de `ifLambda(util::necesitaHoraFin)` |
| VAL-…-010 hora de fin posterior a la de inicio | validador | dentro de `ifLambda(util::necesitaHoraFin)` |
| Comprobaciones de motivo, «Otros» y justificante | validador | se conservan **literalmente** como están hoy |
| RN-001 limpiar lo que el tipo no necesita | `trigger*` | la limpieza del periodo va inline al principio de `triggerGuardarDatos` (acción 1), antes de generar el PDF, usando los `necesita*` de `JustificacionFaltaProfesoradoV1Util` |
| RUI-ENTRADA_DATOS-CREADOR-001/002/003 mostrar fecha de fin / hora de inicio / hora de fin | vista | tres `<panel>` anidados con `showIf` dentro de `datos-falta`: `datos-falta-un-dia` (`UN_DIA_COMPLETO` o `UNAS_HORAS_UN_DIA`; `horaInicio`/`horaFin` con `showIf` propio a `UNAS_HORAS_UN_DIA`), `datos-falta-varios-dias-completos` (`VARIOS_DIAS_COMPLETOS` o sin tipo elegido; `fechaFin` con `showIf` propio a `VARIOS_DIAS_COMPLETOS`) y `datos-falta-varios-dias-primero-parcial` |
| RUI-ENTRADA_DATOS-CREADOR-010/011/012 limpiar al cambiar de tipo | vista | `<action-record>` `exp-JustificacionFaltaProfesoradoV1-onChange-tipoJornadaFalta-action`, en el `onChange` del selector. Coherente con la visibilidad: nunca limpia `fechaInicio`, que se ve en todos los estados (también sin tipo), y cada `if` vacía un campo exactamente cuando queda oculto, incluido el caso sin tipo (`fechaFin`, `horaInicio` y `horaFin` están ocultos y sus tres condiciones son ciertas) |
| RUI-ENTRADA_DATOS-GENERICA-001..004 | vista | los paneles anidados del gemelo de solo lectura `datos-falta-view`, con los mismos `showIf` y el mismo `title="Fecha"`; el form genérico de `ENTRADA_DATOS` incluye `-datos-falta-view` |

Reglas duras que se respetan: ningún `required="true"` en el `domains.xml`; ningún dato de usuario validado en un `trigger*` (RN-001 no valida, **normaliza**); ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.

## Nota del diseño aplicable a esta fase (sección 14, verbatim)

- **Se añade el panel `avisoEstadoExpediente` a los cinco forms genéricos de solo `EXIT`** (`ENTRADA_DATOS` y `PENDIENTE_PRESENTACION` en `recepcion/views.xml`; `PENDIENTE_RESOLUCION`, `ACEPTADO` y `RECHAZADO` en `tramitacion/views.xml`). Va **más allá del delta de la spec**, que declara esas pantallas sin cambios (y la de `PENDIENTE_PRESENTACION` ni siquiera la toca el delta). Motivo: es una regla MUST de `k-tipo-expediente/vistas.md` §6.1 que el as-is incumplía, y como los dos `views.xml` de fase se sobrescriben enteros con la copia del diseño, dejarlos sin ese panel sería materializar a sabiendas un incumplimiento. El efecto visible es solo el texto de aviso del estado del expediente; ningún dato ni botón cambia.
