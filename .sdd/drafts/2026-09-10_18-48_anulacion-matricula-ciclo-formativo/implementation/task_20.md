---
type: implementation-task
template: expediente
---

# Tarea 20 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Escribe el test unitario

```
src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java
```

que cubre el campo derivado `Ciclo.gradoNivel` del catálogo del sistema educativo.

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

### Clase: `com.educaflow.subsystem.sistemaeducativo.db.Ciclo` — campo derivado `gradoNivel`

**Responsabilidad:** dar el texto del grado o del nivel que imprimen los dos documentos («en el Ciclo Formativo de Grado ____») y que la pantalla muestra junto al ciclo (CC-014). La entidad la genera el build a partir de `subsystem/sistemaeducativo/domains/Ciclo.xml`, que el diseño modifica añadiendo ese campo `transient` con cuerpo Java (§4, «Cambios en el catálogo»).
**Por qué sí se testea:** el diseño lo define (tabla §6, fila de `domains/Ciclo.xml`, y §4), no es ninguno de los tres managers, es una decisión con ramas (nivel con nombre corto / nivel sin nombre corto / sin nivel) y es aislable: se construye un `Ciclo` con su `Grado` y su `Nivel` en memoria, sin base de datos y sin `States`.
**Colaboradores a mockear:** ninguno.

#### Método: `public String getGradoNivel()`

- **`getGradoNivel_conNivelConNombreCorto_devuelveElNombreCortoDelNivel`** — Tipo: happy.
  - **Arrange:** `Ciclo` con `grado` de `name` `"Grado Superior"` y `nivel` con `name` `"Grado Superior"` y `nombreCorto` `"Superior"`.
  - **Act:** `ciclo.getGradoNivel()`.
  - **Assert:** devuelve `"Superior"`.
- **`getGradoNivel_conNivelSinNombreCorto_devuelveElNameDelNivel`** — Tipo: borde.
  - **Arrange:** `Ciclo` con `nivel` cuyo `nombreCorto` está en blanco (cadena vacía) y cuyo `name` es `"Grado Medio"`.
  - **Act:** invocar el getter.
  - **Assert:** devuelve `"Grado Medio"` — el campo es nuevo sobre una tabla con filas, así que el nivel sin nombre corto es el caso normal mientras no se rellene.
- **`getGradoNivel_conNivelConNombreCortoNulo_devuelveElNameDelNivel`** — Tipo: borde.
  - **Arrange:** el mismo caso con `nombreCorto` a `null`.
  - **Act:** invocar el getter.
  - **Assert:** devuelve el `name` del nivel, sin `NullPointerException`.
- **`getGradoNivel_sinNivel_devuelveElNameDelGrado`** — Tipo: happy.
  - **Arrange:** `Ciclo` con `nivel` a `null` y `grado` de `name` `"Curso de especialización"` (es el caso de `IABD`, el ciclo que da de alta el Paso 4).
  - **Act:** invocar el getter.
  - **Assert:** devuelve `"Curso de especialización"`.
- **`getGradoNivel_trasCambiarElNivel_recalculaEnCadaLectura`** — Tipo: borde.
  - **Arrange:** `Ciclo` con un nivel; se lee `gradoNivel`, se le cambia el nivel por otro con distinto nombre corto y se vuelve a leer.
  - **Act:** dos lecturas del getter con un cambio de nivel en medio.
  - **Assert:** la segunda devuelve el valor del nivel nuevo — es un campo `transient` calculado en cada lectura, no un valor cacheado.

