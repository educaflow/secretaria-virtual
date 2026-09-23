---
type: implementation-task
template: expediente
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding
- k-code-quality

**Decisión de descomposición (documentada aquí porque el contrato no la cubre):** el contrato de descomposición no tiene un bloque propio para la clase auxiliar `<Code>Util` del tipo. Esta tarea se coloca **después** del `domains.xml` (necesita la entidad y el enum `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` con sus cuatro ítems) y **antes** de las clases que la consumen (`recepcion/PhaseEventManagerImpl.java` y `recepcion/StateEventValidatorImpl.kt`), que es exactamente el sitio que le da el propio diseño en su lista de pasos (Paso 2).

La especificación del diseño es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ningún método, clase, campo ni acción que la especificación no liste.

## Fichero que cubre esta tarea (fila de la sección 6 del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/JustificacionFaltaProfesoradoV1Util.java` | Crear | `k-tipo-expediente` | Especificado en `## 9. Especificación de los PhaseEventManagerImpl`, subsección «Clase auxiliar del tipo» |

## Paso del diseño (verbatim)

### Paso 2 — `JustificacionFaltaProfesoradoV1Util.java`

Fichero destino: `…/actual/v1/JustificacionFaltaProfesoradoV1Util.java`.
FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.JustificacionFaltaProfesoradoV1Util`.
Especificación: `## 9. Especificación de los PhaseEventManagerImpl`, subsección **«Clase auxiliar del tipo — `JustificacionFaltaProfesoradoV1Util`»**.
Clase `final`, sin supertipo, con constructor privado y solo métodos `static`.

Verificación: compila; existe un método por cada fila de la tabla de esa subsección, con su firma exacta.

(Ruta destino resuelta: `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/JustificacionFaltaProfesoradoV1Util.java`.)

## 9. Especificación de los PhaseEventManagerImpl — subsección «Clase auxiliar del tipo» (verbatim)

### Clase auxiliar del tipo — `JustificacionFaltaProfesoradoV1Util`

Fichero **nuevo**: `actual/v1/JustificacionFaltaProfesoradoV1Util.java` (raíz de la versión, junto al `TipoExpedienteInstance.xml`).

```java
package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

public final class JustificacionFaltaProfesoradoV1Util {
    private JustificacionFaltaProfesoradoV1Util() {
    }
}
```

Es la **única** clase auxiliar del tipo (`k-tipo-expediente` §1.8: una sola `<Code>Util`, `final`, con constructor privado y solo métodos estáticos). Aquí vive el **dueño único en servidor** de la clasificación de la sección 4.3, y de él la leen el `trigger*` (RN-001) y el validador (VAL-005..010).

| # | Método | Devuelve | Quién lo usa | Qué hace |
|---|---|---|---|---|
| 1 | `necesitaFechaFin(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | `triggerGuardarDatos` de `recepcion/PhaseEventManagerImpl` (acción 1, inline), validador (`ifLambda`) | `switch` expression exhaustivo sobre `tipoJornadaFalta`, **sin `default`**: `case null -> false`; `case VARIOS_DIAS_COMPLETOS, VARIOS_DIAS_PRIMERO_PARCIAL -> true`; `case UN_DIA_COMPLETO, UNAS_HORAS_UN_DIA -> false` |
| 2 | `necesitaHoraInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | `triggerGuardarDatos` de `recepcion/PhaseEventManagerImpl` (acción 1, inline), validador (`ifLambda`) | `switch` expression exhaustivo sobre `tipoJornadaFalta`, **sin `default`**: `case null -> false`; `case UNAS_HORAS_UN_DIA, VARIOS_DIAS_PRIMERO_PARCIAL -> true`; `case UN_DIA_COMPLETO, VARIOS_DIAS_COMPLETOS -> false` |
| 3 | `necesitaHoraFin(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | `triggerGuardarDatos` de `recepcion/PhaseEventManagerImpl` (acción 1, inline), validador (`ifLambda`) | `switch` expression exhaustivo sobre `tipoJornadaFalta`, **sin `default`**: `case null -> false`; `case UNAS_HORAS_UN_DIA -> true`; `case UN_DIA_COMPLETO, VARIOS_DIAS_COMPLETOS, VARIOS_DIAS_PRIMERO_PARCIAL -> false` |
| 4 | `tieneTipoJornadaFalta(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getTipoJornadaFalta() != null` |
| 5 | `tieneFechaInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getFechaInicio() != null` |
| 6 | `fechaInicioNoAnteriorAHaceUnAnyo(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | Cierto si `fechaInicio` es `null` o **no** anterior a `LocalDate.now(Convert.defaultZoneId).minusYears(1)` |
| 7 | `fechaInicioNoPosteriorAHoy(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | Cierto si `fechaInicio` es `null` o **no** posterior a `LocalDate.now(Convert.defaultZoneId)` |
| 8 | `tieneFechaFin(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getFechaFin() != null` |
| 9 | `fechaFinPosteriorAFechaInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `fechaFin.isAfter(fechaInicio)`. Da por hecho que los dos no son `null`: si alguno lo es, lanza `IllegalStateException` (la rama del validador lo excluye antes) |
| 10 | `fechaFinNoPosteriorAHoy(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | Cierto si `fechaFin` es `null` o **no** posterior a `LocalDate.now(Convert.defaultZoneId)` |
| 11 | `tieneHoraInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getHoraInicio() != null` |
| 12 | `tieneHoraFin(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getHoraFin() != null` |
| 13 | `horaFinPosteriorAHoraInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `horaFin.isAfter(horaInicio)`. Da por hecho que las dos no son `null`: si alguna lo es, lanza `IllegalStateException` (la rama del validador lo excluye antes) |

Reglas de la clase:

- Los métodos 1-3 son la **única** definición en servidor de la tabla 4.3. **MUST NOT** repetirse esa clasificación en ningún `trigger*` ni en ningún `ifValueIn` del validador. Su `switch` **MUST NOT** llevar `default`: así un ítem nuevo del enum que no se clasifique no compila, en vez de devolver `false` en silencio.
- Los métodos 9 y 13 son **comparaciones tontas**: dan por hecho que los dos operandos no son `null` y, si alguno lo es, lanzan `IllegalStateException`. Cuándo se pueden evaluar lo decide **la rama del validador**, no ellos: van dentro de `ifLambda(util::tieneFechaFin) { ifLambda(util::tieneFechaInicio) { … } }` / `ifLambda(util::tieneHoraFin) { ifLambda(util::tieneHoraInicio) { … } }` (§10). Las dos ramas hacen falta porque el DSL evalúa **todas** las reglas de un `field(...)` sin cortar en el primer fallo: el `+Lambda(util::tieneFechaFin, …)` que va delante da el mensaje, pero no impide que se evalúe la regla siguiente. Los métodos 6, 7 y 10 comparan un solo campo con «hoy» y devuelven `true` si ese campo es `null`, igual que `PastOrToday` del catálogo base: la ausencia de ese mismo campo la señala el `tiene…` que va delante en su mismo `field(...)`.
- «Hoy» se obtiene siempre con `LocalDate.now(Convert.defaultZoneId)` (`com.educaflow.base.util.Convert`), que es la zona horaria que ya usaba este mismo tipo.
- **`JustificacionFaltaProfesoradoV1Util` no tiene ningún método de presentación** (ni `fecha`/`hora` de formato, ni un texto por modo): el texto de cada casilla del PDF vive **en la propia sección «Declara que» de `documentospdf/solicitud.xml`**, con los campos del periodo interpolados inline (§5). Una frase de presentación construida en Java perdería la traducción al valenciano (el generador i18n protege los `${...}` de apertium): `k-i18n` regla 5.
- **MUST NOT** crearse una `ValidationRule` propia del tipo: las comprobaciones del tipo son funciones `boolean` de esta clase, declaradas en el validador con `Lambda`/`ifLambda`.

## Contrato que implementa esta clase (sección 4.3 del diseño, verbatim)

### 4.3 Tipo de jornada faltada → campos del periodo (CONTRATO)

El dueño de esta clasificación es el par `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` (los modos posibles, en `domains.xml`) + los tres métodos `necesita*` de `JustificacionFaltaProfesoradoV1Util` (qué campos pide cada modo, sección 9). Esta tabla es una **proyección** de esas dos piezas, para lectura rápida, no su fuente. Añadir un valor al enum obliga a tocar, **y exactamente**, estos sitios:

1. `domains.xml` — el nuevo ítem del enum `TipoJornadaFaltaJustificacionFaltaProfesoradoV1`.
2. `JustificacionFaltaProfesoradoV1Util` — el compilador exige clasificar el nuevo ítem en los tres `necesita*` (cada uno es un `switch` exhaustivo sobre el enum, sin `default`).
3. `views.xml` (raíz de la versión) — dentro de `datos-falta`, o bien un `<panel>` anidado nuevo con sus campos, su título y su `required`, o bien (si comparte título de `fechaInicio` con un panel existente y sus campos de más van al final de la fila) añadir el valor al `showIf` de ese panel y a los `showIf` de los campos que solo él usa, como hace `datos-falta-un-dia`.
   Y **lo mismo en `datos-falta-view`**, el gemelo de solo lectura que incluye TRAMITACION: sus paneles anidados `datos-falta-view-*` repiten los de `datos-falta`, con los campos en `readonly="true"` y sin `required` (`decisiones.md` D4).
4. `recepcion/views.xml` — añadir el nuevo valor a las condiciones `if` del `<action-record>` `exp-JustificacionFaltaProfesoradoV1-onChange-tipoJornadaFalta-action` que correspondan (los campos que el nuevo modo **sí** necesita).
5. `documentospdf/solicitud.xml` — un `<check>` nuevo, en la sección «Declara que», con su propio `<castellano>` (y `<valenciano>` si no debe traducirse solo) interpolando los campos del periodo que ese modo usa.

Nada más: el validador (`StateEventValidatorImpl.kt`) no cambia porque sus `ifLambda(util::necesita…)` son genéricos y ya delegan en el punto 2.

| tipo de jornada faltada | `fechaInicio` | `fechaFin` | `horaInicio` | `horaFin` | título de `fechaInicio` |
|---|---|---|---|---|---|
| `UN_DIA_COMPLETO` | sí | — | — | — | «Fecha» |
| `VARIOS_DIAS_COMPLETOS` | sí | sí | — | — | «Fecha de Inicio» |
| `UNAS_HORAS_UN_DIA` | sí | — | sí | sí | «Fecha» |
| `VARIOS_DIAS_PRIMERO_PARCIAL` | sí | sí | sí | — | «Fecha de Inicio» |
| *(sin elegir)* | sí | — | — | — | «Fecha de Inicio» |

La fila *(sin elegir)* es solo de pantalla (RUI-004 y RUI-006: la fecha de inicio se ve y se marca obligatoria siempre, con su título por defecto): la cubre el panel `datos-falta-varios-dias-completos`, cuyo `showIf` incluye `!tipoJornadaFalta`, con `fechaFin` oculto por su propio `showIf`. No es un modo del enum, así que no entra en los `necesita*` ni en la lista de sitios de arriba.

## Filas del reparto de reglas (sección 11) que apuntan a esta clase

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
| RN-001 limpiar lo que el tipo no necesita | `trigger*` | la limpieza del periodo va inline al principio de `triggerGuardarDatos` (acción 1), antes de generar el PDF, usando los `necesita*` de `JustificacionFaltaProfesoradoV1Util` |

Reglas duras que se respetan: ningún `required="true"` en el `domains.xml`; ningún dato de usuario validado en un `trigger*` (RN-001 no valida, **normaliza**); ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.

## Nota del diseño aplicable a esta clase (sección 14, verbatim)

- **La copia en cliente de la clasificación de la sección 4.3 es de cortesía.** Vive en los `showIf` de los tres paneles anidados de `datos-falta` y de los tres de su gemelo `datos-falta-view` (y de `horaInicio`/`horaFin` dentro de `datos-falta-un-dia`/`datos-falta-view-un-dia` y de `fechaFin` dentro de `datos-falta-varios-dias-completos`) y en las condiciones `if` del `<action-record>`; la que cuenta es el enum `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` más los tres `necesita*` de `JustificacionFaltaProfesoradoV1Util` (`decisiones.md` D3). La tabla 4.3 es solo una proyección de ese par para leer el diseño; su lista numerada enumera **exactamente** los sitios del árbol que toca añadir un tipo de jornada nuevo, y el `switch` exhaustivo de los `necesita*` hace que el compilador obligue a clasificarlo.
