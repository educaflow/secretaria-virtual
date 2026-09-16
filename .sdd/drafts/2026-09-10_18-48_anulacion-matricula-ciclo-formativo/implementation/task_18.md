---
type: implementation-task
template: expediente
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-tipo-expediente

Escribe el test unitario

```
src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirectorTest.java
```

que cubre `DevolucionDelDirector.borrar`.

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

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.DevolucionDelDirector`

**Responsabilidad:** ser el dueño único de qué campos forman «la devolución del director» —`motivoDevolucion`, `fechaDevolucion` y `devueltoPor`— y dejarlos limpios. La llaman `triggerPresentar`, `triggerEnviarAFirma`, `triggerSubsanar` y `triggerFirmar` (§9.0.2).
**Por qué sí se testea:** la define el diseño (tabla §6 y §9.0.2), no es ninguno de los tres managers, transforma el estado de la entidad (es el motivo de que exista: que la lista de los tres campos esté en un solo sitio) y es aislable por completo — solo setters sobre la entidad, sin `EventContext`, sin repositorio y sin PDF.
**Colaboradores a mockear:** ninguno.

#### Método: `public static void borrar(AnulacionMatriculaCicloFormativoV1 expediente)`

- **`borrar_expedienteQueVieneDeUnaDevolucion_dejaLosTresCamposANull`** — Tipo: happy.
  - **Arrange:** expediente con `motivoDevolucion` = `"Falta el NIA"`, `fechaDevolucion` = una fecha cualquiera y `devueltoPor` = un `User`.
  - **Act:** `DevolucionDelDirector.borrar(expediente)`.
  - **Assert:** los **tres** campos quedan a `null`. Es el test que destapa que alguien añada un cuarto dato a la devolución y se olvide de limpiarlo.
- **`borrar_expedienteSinDevolucionPrevia_esIdempotenteYNoLanza`** — Tipo: borde.
  - **Arrange:** expediente recién construido, con los tres campos ya a `null`.
  - **Act:** invocar `borrar` dos veces seguidas.
  - **Assert:** no lanza y los tres campos siguen a `null`; ningún llamante necesita preguntarse si le toca.
- **`borrar_noTocaNingunOtroCampoDelExpediente`** — Tipo: borde.
  - **Arrange:** expediente con la devolución rellena **y además** `sentidoRevision`, `motivoRechazo`, `fechaRevision` y `revisadoPor` con valor.
  - **Act:** `DevolucionDelDirector.borrar(expediente)`.
  - **Assert:** esos cuatro campos conservan su valor — la clase es dueña de la devolución del director y **solo** de ella; limpiar la decisión de secretaría no es cosa suya.

