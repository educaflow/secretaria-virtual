---
type: implementation-task
template: expediente
---

# Tarea 19 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-tipo-expediente

Escribe el test unitario

```
src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatriculaTest.java
```

que cubre `ReglasAnulacionMatricula.esRechazo`, invocado **desde Java**, que es además la forma de llamada que exige el `@JvmStatic`.

**Por qué esta tarea existe.** Un tipo de expediente **no genera tests propios**: su conformidad la dan los tests ya existentes y escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`. La **única excepción** es la de una pieza de **lógica de negocio pura y aislable** que **no** vive en el `PhaseEventManagerImpl`, el `StateEventValidatorImpl` ni el `InitialEventManagerImpl`, y `design/test-unit-desc.md` describe exactamente eso para esta clase. El test va en el **mismo paquete** de la clase bajo `src/test/java/...`.

**CRITICAL — la descripción de `test-unit-desc.md` que va abajo es contrato fijo y la superficie es cerrada: MUST NOT escribirse ningún test que no liste, ni omitirse ninguno de los que lista, ni cambiarse su Arrange/Act/Assert.**

- **MUST NOT** editarse, ampliarse, debilitarse ni exonerarse ningún test existente de `src/test/java/com/educaflow/tiposexpedientes/` ni de `src/test/java/com/educaflow/views/`.
- **MUST NOT** añadirse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com/educaflow/views`.
- **MUST NOT** editarse ningún fichero de `src/test/e2e/**`.
- **MUST NOT** modificarse el código de producción para que el test pase: si el test no pasa, el fallo se reporta.

## `test-unit-desc.md` — «Tests nuevos a crear» (verbatim)

## Tests nuevos a crear

Siete ficheros, todos bajo `src/test/java/`, uno por clase o por fichero de reglas:

| Fichero de test | Cubre |
|---|---|
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAccesoTest.java` | `exigeSerElCreador` y `exigeMismoCentroQueElExpediente` |
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirectorTest.java` | `borrar` |
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatriculaTest.java` | `ReglasAnulacionMatricula.esRechazo` (desde Java, que es además la forma de llamada que exige el `@JvmStatic`) |
| `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java` | el campo derivado `Ciclo.gradoNivel` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt` | `Required` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt` | `MinLength`, `MaxLength` y `Pattern` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java` | `NoAdmitido` |

`StringRulesTest` cubre las tres reglas que viven en `StringRules.kt`. Los tests de las reglas de `base` van en Kotlin cuando necesitan nombrar los parámetros opcionales y la nulabilidad de la propia regla (`Required`, `MinLength`, `MaxLength`, `Pattern`) y en Java cuando solo construyen la regla y leen su mensaje (`NoAdmitido`). **MUST NOT** añadirse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`.

## Descripción del test (verbatim, íntegra)

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula`

**Responsabilidad:** ser el dueño único **en servidor** de la clasificación «la revisión rechaza la anulación si y solo si `sentidoRevision == RECHAZAR`», de la que se siguen sus dos consecuencias (la resolución desestima y el motivo del rechazo aplica). Tiene cuatro consumidores: `triggerEnviarAFirma`, la rama del validador de `ENVIAR_A_FIRMA` y las dos expresiones Groovy de `resolucion.xml` (§9.0.3).
**Por qué sí se testea:** la define el diseño (tabla §6, §9.0.3 y §10.0), no es ninguno de los tres managers, es una decisión con ramas y es aislable del todo — un predicado puro sobre la entidad, sin colaboradores.
**Colaboradores a mockear:** ninguno.

#### Método: `@JvmStatic fun esRechazo(expediente: AnulacionMatriculaCicloFormativoV1): Boolean`

- **`esRechazo_sentidoRechazar_devuelveTrue`** — Tipo: happy.
  - **Arrange:** expediente con `sentidoRevision` = `SentidoRevisionAnulacionMatriculaCicloFormativoV1.RECHAZAR`.
  - **Act:** `ReglasAnulacionMatricula.esRechazo(expediente)` — invocado como estático desde Java, que es lo que el `@JvmStatic` promete y lo que hacen el `trigger*` y (por FQCN) las dos expresiones del PDF.
  - **Assert:** devuelve `true`.
- **`esRechazo_sentidoAceptar_devuelveFalse`** — Tipo: happy.
  - **Arrange:** expediente con `sentidoRevision` = `ACEPTAR`.
  - **Act:** invocar el predicado.
  - **Assert:** devuelve `false`.
- **`esRechazo_sinSentidoElegido_devuelveFalse`** — Tipo: borde.
  - **Arrange:** expediente con `sentidoRevision` a `null` (la secretaría aún no ha decidido).
  - **Act:** invocar el predicado.
  - **Assert:** devuelve `false` y **no lanza**: es total, y `false` es lo que sus cuatro consumidores esperan.

