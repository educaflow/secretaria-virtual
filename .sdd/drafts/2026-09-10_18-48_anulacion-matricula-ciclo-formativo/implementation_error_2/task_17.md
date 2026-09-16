---
type: implementation-task
template: expediente
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas
- k-validaciones

## Qué hay que hacer

Escribir el test unitario del **campo calculado `Ciclo.gradoNivel`** que la tarea 04 añadió al catálogo del sistema educativo:

- `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java`

Hay **precedente directo en el árbol**: `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/GradoTest.java` hace exactamente esto con `Grado.admiteNivel`, el otro campo calculado del mismo catálogo. **SHOULD** leerse antes de escribir nada, para replicar su forma (en particular la siembra del campo de respaldo con un centinela).

### Decisión de descomposición documentada

Un tipo de expediente **no genera tests propios**: su conformidad la dan los tests ya existentes y escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`, que recorren automáticamente todos los tipos del árbol. La **única excepción** del contrato es que el `test-unit-desc.md` del diseño describa **clases auxiliares propias con lógica de negocio aislable**, y aquí las describe: seis ficheros de test sobre ocho clases auxiliares. Como el contrato prevé «una tarea de test por clase» y aquí hay clases en **tres paquetes distintos** (y un fichero de test que ejerce tres clases a la vez), se han agrupado en **tres tareas, una por paquete de destino**: cada una comparte skills, ubicación y especificación, y ninguna mezcla paquetes. Es la lectura más razonable del contrato para este caso; queda documentada aquí.

Reglas duras, comunes a las tres tareas de test:

- Cada test va en el **paquete espejo** de la clase que ejerce, bajo `src/test/java/...`.
- **MUST NOT** crearse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`.
- **MUST NOT** editarse, ampliarse, debilitarse ni exonerarse ningún test ya existente: los `.java` son la fuente de verdad y si uno falla, el fallo está en el trámite generado.
- **MUST NOT** escribirse tests unitarios de `InitialEventManagerImpl`, de los `PhaseEventManagerImpl` ni de los `StateEventValidatorImpl`: el propio diseño los excluye por no ser aislables.
- **MUST NOT** crearse ningún `agent_docs/*-rules.md` ni ningún skill generador para ellos.
- **La especificación del diseño es contrato fijo y la superficie es cerrada: MUST NOT crearse ningún test, método ni aserto que la especificación no liste**, ni cambiarse los literales de los mensajes esperados.
- Para el usuario autenticado, los tests mockean `SecurityUtil` (`Mockito.mockStatic`), que es exactamente para lo que el proyecto exige usar `SecurityUtil.getUser()` en vez de `AuthUtils.getUser()`.

## Fila de la tabla `## Tests nuevos a crear` del `test-unit-desc.md` (verbatim)

| Fichero | Clase que ejerce |
|---|---|
| `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java` | el campo calculado `Ciclo.gradoNivel` (Paso 4) |

## Especificación de los tests de `Ciclo.gradoNivel`, ÍNTEGRA (verbatim de `test-unit-desc.md`)

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
  - **Assert:** devuelve `Curso de especialización` (el caso de `IABD`, el único ciclo que da de alta el Paso 4).
- **`getGradoNivel_trasCambiarElNivel_recalculaEnCadaLectura`** — Tipo: borde.
  - **Arrange:** ciclo sin nivel y con grado «Curso de especialización»; se lee una primera vez y después se le asigna un nivel con `nombreCorto` «Superior».
  - **Act:** leer `getGradoNivel()` antes y después de asignar el nivel.
  - **Assert:** la primera lectura devuelve `Curso de especialización` y la segunda `Superior`; el campo es `transient` y se recalcula, no se congela.

