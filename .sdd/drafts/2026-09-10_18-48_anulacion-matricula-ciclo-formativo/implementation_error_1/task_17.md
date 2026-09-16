---
type: implementation-task
template: expediente
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

## Qué hay que hacer

Escribe los tests unitarios de:

- `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java` — ejerce el campo calculado `Ciclo.gradoNivel` (tarea 04)

**Por qué existe esta tarea.** Un tipo de expediente **no genera tests propios**: su conformidad la dan los tests ya existentes y escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`. La **única excepción** es que el diseño describa el test de una pieza de **lógica de negocio pura y aislable** que no viva en el `PhaseEventManagerImpl`, el `StateEventValidatorImpl` ni el `InitialEventManagerImpl`, y `design/test-unit-desc.md` describe seis ficheros así. Se reparten en **tres** tareas de test, una por cada tarea de implementación que produce la lógica que ejercen (las clases auxiliares de la versión, el campo calculado del catálogo y el catálogo común de reglas de `base`), para que cada test se escriba junto al criterio de «bien hecho» de lo que prueba. Esta decisión la toma la descomposición y queda documentada aquí.

**Reglas duras:**

- Cada test va en el **paquete espejo** de la clase que ejerce, bajo `src/test/java/…`, con exactamente el nombre de fichero que la tabla «Tests nuevos a crear» declara.
- **MUST NOT** crearse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`, ni ningún `agent_docs/*-rules.md`, ni ningún skill generador.
- **MUST NOT** editarse, ampliarse, debilitarse ni exonerarse ningún test existente: los `.java` de `tiposexpedientes` y de `views` son fuente de verdad escrita a mano. Si uno falla, el fallo está en el código de esta iniciativa.
- **MUST NOT** escribirse tests de los `PhaseEventManagerImpl`, de los `StateEventValidatorImpl`, del `InitialEventManagerImpl`, de `SinOtraSolicitudEnCursoParaElMismoCiclo` ni de `FirmaPdf`: `test-unit-desc.md` los excluye con su motivo.
- La descripción de los tests es **contrato fijo** y la **superficie es cerrada**: se escriben **los tests descritos**, con su nombre, su tipo y sus asertos; **MUST NOT** inventarse otros ni omitirse ninguno.
- Para mockear el usuario autenticado se mockea `SecurityUtil` (`Mockito.mockStatic`), nunca `AuthUtils`.

## Tabla «Tests nuevos a crear» de `design/test-unit-desc.md` (verbatim)

## Tests nuevos a crear

Seis ficheros, todos bajo `src/test/java/`, en el paquete espejo de la clase que ejercen:

| Fichero | Clase que ejerce |
|---|---|
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAccesoTest.java` | `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ControlDeAcceso` |
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirectorTest.java` | `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.DevolucionDelDirector` |
| `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java` | el campo calculado `Ciclo.gradoNivel` (Paso 4) |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt` | `Required` (Paso 5) |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt` | `MinLength`, `MaxLength` y `Pattern` (Paso 5) |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java` | `NoAdmitido` (Paso 5) |

**MUST NOT** crearse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`, ni ningún `agent_docs/*-rules.md`, ni ningún skill generador.

## Descripción de los tests de esta tarea, en `design/test-unit-desc.md` (verbatim)

### Clase: `com.educaflow.subsystem.sistemaeducativo.db.Ciclo` — campo calculado `gradoNivel`

**Responsabilidad:** `gradoNivel` es el texto que se imprime en la línea «en el Ciclo Formativo de Grado ____» de los dos documentos y que la pantalla muestra junto al ciclo. Su cuerpo lo declara este diseño en `subsystem/sistemaeducativo/domains/Ciclo.xml` (§4, «Cambios en el catálogo»): si el ciclo tiene nivel devuelve el `nombreCorto` del nivel —o su `name` si el nombre corto está vacío— y, si no tiene nivel, el `name` de su grado.
**Por qué sí se testea:** la define el diseño (tabla §6, fila `domains/Ciclo.xml`, con su especificación en §4); no es ninguno de los tres managers; tiene lógica propia (tres ramas de decisión sobre datos del catálogo); y es aislable: son entidades POJO construidas en memoria, sin `Tramitador`, `EventContext`, base de datos, PDF ni `States`. Hay precedente directo en el árbol: `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/GradoTest.java` hace exactamente esto con `Grado.admiteNivel`, el otro campo calculado del mismo catálogo.
**Colaboradores a mockear:** ninguno.

> **Nota de forma (la misma que documenta `GradoTest`):** el getter generado por AOP captura la `NullPointerException` del cálculo y devuelve el último valor del campo de respaldo. Por eso cada test **siembra antes** el campo de respaldo con un valor centinela distinto del esperado: así el valor observado solo puede venir de un cálculo ejecutado con éxito, y no de que el cálculo no llegue a ejecutarse.

#### Método: `String getGradoNivel()`

- **`getGradoNivel_conNivelConNombreCorto_devuelveElNombreCortoDelNivel`** — Tipo: happy.
  - **Arrange:** ciclo con grado («Técnico Superior») y nivel con `name` «Grado Superior» y `nombreCorto` «Superior»; campo de respaldo sembrado con un centinela.
  - **Act:** leer `getGradoNivel()`.
  - **Assert:** devuelve `Superior` (el caso de «Desarrollo de Aplicaciones Web», §4).
- **`getGradoNivel_conNivelSinNombreCorto_devuelveElNameDelNivel`** — Tipo: borde.
  - **Arrange:** ciclo con nivel cuyo `nombreCorto` es la cadena vacía y cuyo `name` es «Grado Medio»; campo de respaldo sembrado con un centinela.
  - **Act:** leer `getGradoNivel()`.
  - **Assert:** devuelve `Grado Medio`; el nombre corto es opcional (el campo se añade sin `required`, sobre filas que ya existen) y su ausencia no deja el texto en blanco.
- **`getGradoNivel_conNivelConNombreCortoNulo_devuelveElNameDelNivel`** — Tipo: borde.
  - **Arrange:** igual que el anterior pero con `nombreCorto` a `null`.
  - **Act:** leer `getGradoNivel()`.
  - **Assert:** devuelve `Grado Medio`; `null` y cadena vacía se tratan igual.
- **`getGradoNivel_sinNivel_devuelveElNameDelGrado`** — Tipo: happy.
  - **Arrange:** ciclo sin nivel (`null`) y con grado de `name` «Curso de especialización»; campo de respaldo sembrado con un centinela.
  - **Act:** leer `getGradoNivel()`.
  - **Assert:** devuelve `Curso de especialización` (el caso de `IABD`, el ciclo que da de alta el Paso 4).
- **`getGradoNivel_trasCambiarElNivel_recalculaEnCadaLectura`** — Tipo: borde.
  - **Arrange:** ciclo sin nivel y con grado «Curso de especialización»; se lee una primera vez y después se le asigna un nivel con `nombreCorto` «Superior».
  - **Act:** leer `getGradoNivel()` antes y después de asignar el nivel.
  - **Assert:** la primera lectura devuelve `Curso de especialización` y la segunda `Superior`; el campo es `transient` y se recalcula, no se congela.


## `## Cobertura` de `design/test-unit-desc.md` (verbatim)

## Cobertura

- Clases auxiliares descritas: **8**, agrupadas en **6** secciones (`ControlDeAcceso`, `DevolucionDelDirector`, el campo calculado `Ciclo.gradoNivel`, `Required`, el trío `MinLength`/`MaxLength`/`Pattern` de `StringRules.kt` y `NoAdmitido`), con **35** tests descritos en total.
- Clases del tipo excluidas: `InitialEventManagerImpl`; los tres `PhaseEventManagerImpl` (`solicitud`, `revision`, `resolucion`); los tres `StateEventValidatorImpl` (`solicitud`, `revision`, `resolucion`); además de `SinOtraSolicitudEnCursoParaElMismoCiclo` y de la ampliación de `FirmaPdf`, que no son aislables.
- Tests nuevos a crear: **6 ficheros** bajo `src/test/java/…` (los de la tabla «Tests nuevos a crear»). Ninguno bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`.
