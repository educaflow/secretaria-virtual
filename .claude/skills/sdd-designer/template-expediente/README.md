# Plantilla de diseño de un tipo de expediente — guía e índice

Esta carpeta de plantillas define **todo lo específico de convertir una especificación funcional en el DISEÑO de un trámite y de su tipo de expediente** (la carpeta `tramites/<tramite>/` y su carpeta de versión `<vN>/`). El skill `sdd-designer` aporta solo el **flujo** (localizar la spec, decidir modo, lanzar los diseñadores en paralelo, elegir el mejor con un juez, enriquecer, verificar/corregir, describir y verificar los tests unitarios, cerrar) y es **agnóstico**: no sabe nada de qué se diseña. Lo lee todo de aquí.

Este `README.md` es **el único fichero que el motor conoce por nombre**. **Lo leen los ocho subagentes**, y cada uno hace una tarea distinta sobre el mismo diseño:

- **diseñador** — **crea** un diseño completo desde cero (§2.1).
- **juez** — **elige** entre dos diseños, detallando las ventajas **y los defectos** de cada uno (§2.2).
- **verificador** — **busca problemas** en el diseño ganador y los reporta (§2.3).
- **corrector** — **corrige** los problemas reportados e **incorpora** las mejoras del enriquecedor (§2.4).
- **enriquecedor** — **detecta** qué ventajas de los descartados incorporar y qué **defectos del propio ganador** siguen presentes (§2.5).
- **test-unitarios** — **escribe** `design/test-unit-desc.md` (§2.6).
- **verificador-test-unitarios** — **comprueba** que `test-unit-desc.md` es coherente con el diseño (§2.7).
- **corrector-test-unitarios** — **corrige** `test-unit-desc.md` (§2.8).

A través de este README cada subagente descubre y lee **solo los ficheros de esta carpeta que su tarea necesita** (§1, §2). **MUST NOT** copiarse ningún bloque explicativo de esta plantilla al `design.md` de salida.

> **Contrato fijo (lo garantiza el motor, no lo cambia esta plantilla):** la entrada es `specification.md` (frontmatter `type: specification`) y la salida es una **carpeta `design/`** dentro de la carpeta de la iniciativa, cuyo índice es `design.md` con frontmatter `type: design` más la clave `template:` copiada del `specification.md` de la iniciativa. Todo lo demás (el resto de la estructura de `design/`, la conversión spec→diseño, la validación, los tests) lo define esta plantilla.

---

## 0. REGLA DE GENERALIDAD — léela antes que nada

**CRITICAL.** Esta plantilla describe **el patrón**, nunca un trámite concreto.

- **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, evento, campo, enum, perfil, panel ni documento reales. Se usan los **placeholders** de `design-contract.md` §0.1 (`<tramite>`, `<Code>`, `<vN>`, `<VN>`, `<Entidad>`, `<FASE>`/`<fase>`, `<ESTADO>`, `<EVENTO>`, `<PERFIL>`, `<campo>`, `<panel>`, `<doc>`).
- **MUST NOT** escribirse ninguna regla que solo valga para un número fijo de fases, estados, eventos, documentos PDF, perfiles o registros. El patrón **MUST** funcionar con **1, 2, 3 o N fases**; con **0, 1 o N** documentos PDF; con firma **en cliente, en servidor, en ambas o en ninguna**; con **0, 1 o N** registros de entrada/salida; con estados **con y sin** `profile`; con eventos **con y sin** guarda.
- Todo ejemplo **MUST** ir encerrado en un bloque que empiece por `> **Ejemplo** (ilustrativo, NO normativo):`, y con nombres **inventados**.
- El **diseño producido**, en cambio, es concreto: usa los nombres reales que la especificación fija. La regla de generalidad aplica a la **plantilla**, no a su salida.

---

## 1. Ficheros de esta carpeta de plantillas

El motor abre `README.md` y, a través de él, los subagentes usan los demás. **MUST NOT** copiarse ninguno al output.

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: contrato fijo, reparto por rol, estructura de salida, contexto del proyecto a cargar y las partes condicionales del diseño. | Los **ocho** subagentes (es el contrato que el motor nombra). |
| `design-contract.md` | **Qué produce el diseño y cómo**: la estructura de `design/` (§1), las 15 secciones obligatorias del `design.md` (§2), qué se materializa verbatim y qué se describe, la máquina de estados y el `.puml`, el modelo, los documentos PDF, la tabla de ficheros, el orden de los pasos, la especificación quirúrgica del `InitialEventManagerImpl` y de los `PhaseEventManagerImpl` (con la **notación de acciones** de los `trigger*`), el **DSL del `StateEventValidatorImpl`**, el reparto de reglas por capa, la asignación de perfiles y el **checklist del diseñador**. | El **diseñador** (para producir); el **juez** y el **enriquecedor** (como criterios); el **verificador** (para saber qué *debería* existir); el **corrector** (la regla a la que ajustar cada corrección); los tres roles de tests unitarios (§15.2). |
| `vistas.md` | **Parte del diseño — las vistas**: el formato **preprocesado** de las vistas de un tipo de expediente (form plantilla `exp-<Entidad>-Templates` de la raíz de la versión, `<form state= profile=>` por estado en cada fase, `<include-panels>` y el prefijo `-`, `<footer>` y botones, cabecera `header`), las reglas duras verificadas por test (X1–X3, Y1–Y3), los patrones (visor de PDF, firma en cliente) y su checklist (§8). | El **diseñador** (materializa cada `views.xml` y pasa §8); el **juez** y el **enriquecedor** (criterios sobre vistas); el **verificador** (reaplica §4 y §8); el **corrector** (si un fallo afecta a una vista); los roles de tests (por qué botón se dispara cada evento). |
| `validacion.md` | **Reglas de verificación**: la validación **mecánica** de cada XML materializado (qué se valida contra XSD y qué solo se comprueba bien formado) y la lista numerada de **comprobaciones semánticas**, cada una con qué se mira, qué es fallo y cuál es la corrección esperada. | El **verificador** (la aplica entera, ejecutando él mismo los comandos con `Bash`); el **corrector** (para entender por qué cada cosa es un fallo y cuál es la corrección esperada). |
| `tests-e2e.md` | **Parte del diseño — tests E2E**: el contrato de `design/test-e2e-desc.md` (un escenario por transición y por perfil, datos de demo, plantilla exacta, trazabilidad `ESC-NNN → test`, checklist). Es **contrato fijo hacia abajo**: lo consume `/sdd-debug-with-test-e2e-desc`. | El **diseñador** (lo produce); el **verificador** (comprueba cobertura y formato); el **corrector** (si un fallo afecta a `test-e2e-desc.md`). |
| `tests-unitarios.md` | **Parte del diseño — tests unitarios**: el contrato de `design/test-unit-desc.md`, que en un tipo de expediente es una **declaración explícita de que no procede** (con su motivo, su plantilla literal y el criterio de excepción para clases auxiliares propias), más las **comprobaciones de coherencia** que aplica su verificador. | El **test-unitarios** (lo produce); el **verificador-test-unitarios** (aplica §4); el **corrector-test-unitarios** (la plantilla a la que ajustar la corrección). |

**MUST NOT** existir en esta carpeta ningún script de validación: la validación mecánica son comandos que `validacion.md` §1 escribe y que **ejecuta el verificador**.

---

## 2. Tareas de los ocho roles

El motor lanza ocho roles. Los ocho reciben las **mismas rutas de entrada** (este `README.md`, el `specification.md` y, si existe, `design-guidelines.md`), más su **entrada propia**; pero cada uno hace una tarea distinta y lee un subconjunto distinto de esta carpeta.

> **Común a los ocho:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** usar `AskUserQuestion`. **MUST NOT** escribir, editar ni borrar nada fuera de su carpeta de trabajo (`design_<n>/` o `design/`): el diseño es un **plan**, y todo cambio en el árbol real se **describe**, nunca se aplica. En particular **MUST NOT** tocarse `src/**`, `build.gradle` ni `axelor-config.properties`.

**Resumen por rol** (detalle en §2.1–§2.8):

| Rol | Qué hace | Entrada propia | Lee de esta plantilla | Produce | Token de salida |
|---|---|---|---|---|---|
| **diseñador** (§2.1) | **Crea** un diseño completo | spec (+ guías) + el contexto del proyecto de §4 | `design-contract.md`, `vistas.md`, `tests-e2e.md`; `tests-unitarios.md` **no** le aplica | la carpeta `design_<n>/` entera | `ESCRITO: design_<n>` |
| **juez** (§2.2) | **Elige** entre dos diseños | las dos carpetas `design_<n>/` | `design-contract.md` + `vistas.md` (como rasero) | el ganador + ventajas y defectos de **ambos** | `GANADOR: design_<n>` + los cinco bloques `=== … ===` |
| **verificador** (§2.3) | **Busca problemas** | la carpeta `design/` | `validacion.md` (entera) + `vistas.md` §4/§8; consulta `design-contract.md` y `tests-e2e.md` | la lista de fallos, o conforme | `OK-CORRECTO` o líneas JSONL `P-NNN` |
| **corrector** (§2.4) | **Corrige / incorpora** | `design/` + las líneas JSONL | `validacion.md` + `design-contract.md`; `vistas.md` / `tests-e2e.md` solo si el fallo les afecta | `design/` corregida en sitio | informe de lo corregido |
| **enriquecedor** (§2.5) | **Detecta** ventajas a incorporar y defectos del ganador que persisten | `design/` + `design/log_best.txt` | `design-contract.md` (+ `vistas.md` si aplica) | la lista de mejoras (no las aplica) | `OK-SIN-MEJORAS` o líneas JSONL `M-NNN` |
| **test-unitarios** (§2.6) | **Escribe** la declaración de tests unitarios | `design/` (sobre todo `design.md`) | `tests-unitarios.md` | `design/test-unit-desc.md` | `ESCRITO: test-unit-desc.md` |
| **verificador-test-unitarios** (§2.7) | **Comprueba** la coherencia de `test-unit-desc.md` | `design/test-unit-desc.md` + `design.md` | `tests-unitarios.md` §4 | la lista de incoherencias, o conforme | `OK-CORRECTO` o líneas JSONL `P-NNN` |
| **corrector-test-unitarios** (§2.8) | **Corrige** `test-unit-desc.md` | `design/test-unit-desc.md` + las líneas JSONL | `tests-unitarios.md` (plantilla §3) | `test-unit-desc.md` corregido en sitio | informe de lo corregido |

Solo el **diseñador** carga el contexto del proyecto de §4; los otros siete trabajan sobre lo que ya está en disco, salvo las lecturas puntuales del **verificador** que §4.3 autoriza expresamente.

### 2.1 Diseñador — crea el diseño

**Tarea:** producir un diseño **completo y autosuficiente** del trámite y de su tipo de expediente en su carpeta `design_<n>/`, a partir del `specification.md` (y de las guías, si existen), siguiendo el contrato al pie de la letra.

- **Lee de esta plantilla:** `design-contract.md` (**siempre y entero**: qué producir, la estructura de `design/`, las 15 secciones del `design.md`, la notación de acciones, el DSL del validador, el orden de los pasos y el **checklist §17**); `vistas.md` (**siempre**: todo tipo de expediente tiene vistas, incluido su checklist §8); `tests-e2e.md` (**siempre**: `test-e2e-desc.md` lo escribe él).
- **Carga además** el contexto del proyecto de §4 (skills + código real). Es el único rol que lo necesita para producir.
- **Produce:** todos los ficheros de `design-contract.md` §1 — el índice `design.md`, los XML materializados verbatim, el `estados.puml`, un `fases/<fase>/views.xml` por cada fase, `permisos.xml` y `test-e2e-desc.md` — más las partes condicionales de §5 cuando apliquen.
- **MUST NOT** materializar ningún `.java` ni `.kt`: las clases se **describen** con precisión quirúrgica en el `design.md` (`design-contract.md` §10, §11, §12). La **única excepción** es el DSL del validador, que sí se escribe con su sintaxis literal.
- **MUST NOT** escribir `design/test-unit-desc.md`: lo produce el rol **test-unitarios** en una fase posterior del motor.
- **MUST NOT** inventar fases, estados, eventos, campos, perfiles ni documentos que la especificación no pida.
- **MUST NOT** dar el diseño por terminado sin pasar el checklist de `design-contract.md` §17 y el de `vistas.md` §8 (**LIMIT**: 5 pasadas de autocorrección; lo que quede sin cumplir se anota en «Notas y supuestos»).

### 2.2 Juez — elige entre dos diseños

**Tarea:** dadas **dos** carpetas de diseño completas, decidir cuál cumple mejor la especificación, las guías y las reglas de esta plantilla, detallando las **ventajas y los defectos concretos de CADA uno** (el formato exacto de la respuesta lo fija el motor).

- **Lee de esta plantilla:** `design-contract.md` y `vistas.md`. Son el **rasero**: cobertura de la máquina de estados, coherencia XML ↔ `.puml` ↔ vistas ↔ métodos descritos, clasificación `usuario`/`servidor` de los campos, frontera de confianza del validador, calidad de las vistas y de los pasos.
- **Detalla los defectos de los DOS**, no solo del perdedor: los del diseño que acabe ganando se auditan después (§2.5). Sé concreto — qué punto del spec/guías/reglas incumple, qué estado o evento se queda sin método, qué transición no cuadra con el `.puml`.
- **MUST NOT** modificar, completar ni corregir ninguno de los dos. **MUST NOT** ejecutar la validación de `validacion.md` (es del verificador) ni cargar el contexto de §4. Si ambos son deficientes, elige el menos malo: el enriquecedor y el bucle de verificación corrigen después al ganador.

### 2.3 Verificador — busca problemas en el diseño

**Tarea:** revisar la carpeta `design/` y **reportar todos los fallos**; si no hay nada que corregir, declararlo conforme.

- **Lee de esta plantilla:** `validacion.md` — es **la lista de qué cuenta como fallo**. **Ejecuta él mismo, con `Bash`**, los comandos de validación mecánica de `validacion.md` §1 (el motor **NUNCA** los ejecuta) y aplica después **todas** las comprobaciones semánticas de `validacion.md` §2. Las de vistas están delegadas en `vistas.md` §4 y §8. Para saber qué *debería* existir consulta `design-contract.md` y, para los tests E2E, `tests-e2e.md`.
- **Comprueba** que las partes que **deben** existir (§5) existen y que las condicionales existen si y solo si su condición se cumple.
- **Puede leer** (nunca escribir) los ficheros reales del árbol que `validacion.md` le indique expresamente (§4.3).
- **MUST NOT** corregir nada: solo **detecta y reporta**. **MUST NOT** compilar ni arrancar la aplicación: el diseño es un plan, no hay código todavía.

### 2.4 Corrector — corrige el diseño

**Tarea:** dada la lista JSONL de fallos del verificador (§2.3) o de mejoras del enriquecedor (§2.5), **aplicarla en sitio** sobre `design/` (`Edit`/`Write`), sin regenerar el diseño.

- **Lee de esta plantilla:** `validacion.md` (por qué cada cosa es un fallo y **cuál es la corrección esperada** — cada comprobación la declara) y `design-contract.md` (la regla a la que ajustar el cambio); además `vistas.md` o `tests-e2e.md` **solo si** el fallo afecta a un `views.xml` o a `test-e2e-desc.md`.
- **Mantiene la coherencia transversal:** un cambio en la máquina de estados arrastra la tabla de transiciones, el `TipoExpedienteInstance.xml`, el `estados.puml`, los `trigger*`/`onEnter*` descritos, los métodos del validador, las vistas de la fase y `test-e2e-desc.md`. Corregir solo uno de esos sitios deja el diseño incoherente y el verificador lo volverá a reportar.
- **MUST NOT** renombrar ni mover la carpeta `design/`. **MUST NOT** regenerar el diseño entero ni reconstruirlo desde el spec.

### 2.5 Enriquecedor — incorpora ventajas y sanea defectos del ganador

**Tarea:** dado el ganador y `design/log_best.txt` (ventajas **y** defectos que el juez atribuyó a cada diseño), decidir **(a)** qué ventajas de los **descartados** conviene incorporar y **(b)** qué **defectos que el juez atribuyó al propio ganador siguen presentes**, y **reportarlos** (los aplica el corrector).

- **Lee de esta plantilla:** `design-contract.md` como criterio para juzgar si una ventaja ya está cubierta y si procede aplicarla, y si un defecto del ganador es real y persiste; `vistas.md` cuando la ventaja o el defecto afecta a una vista.
- **Ignora** los defectos de los diseños **descartados** (murieron con su diseño) y los marcados `Ninguno detectado`.
- **MUST NOT** modificar el diseño ni cargar el contexto de §4: solo **detecta y reporta** sobre lo que hay en disco.

### 2.6 test-unitarios — escribe `test-unit-desc.md`

**Tarea:** escribir `design/test-unit-desc.md` siguiendo el contrato de `tests-unitarios.md`.

- **CRITICAL:** en un tipo de expediente **no se describen tests unitarios de clases**. `tests-unitarios.md` explica el motivo y da la **plantilla literal** del fichero a producir: una declaración explícita y breve de que no procede, con su motivo y la remisión a los tests ya existentes y a `test-e2e-desc.md`.
- **Lee de esta plantilla:** `tests-unitarios.md` (**entero**). Puede consultar `design.md` para nombrar correctamente el tipo y sus fases, y para detectar la **excepción** de §2 de ese fichero (una clase auxiliar propia con lógica de negocio aislable, que sí se describe).
- **MUST NOT** escribir código Java. **MUST NOT** inventar clases ni métodos. **MUST NOT** proponer tests bajo `src/test/java/com/educaflow/tiposexpedientes/` ni tocarlos.
- **MUST NOT** modificar ningún otro fichero del diseño.

### 2.7 verificador-test-unitarios — comprueba la coherencia

**Tarea:** revisar `design/test-unit-desc.md` y **reportar toda incoherencia** respecto al diseño y al contrato; si todo cuadra, declararlo conforme.

- **Lee de esta plantilla:** `tests-unitarios.md` §4 (**las comprobaciones de coherencia**): que el fichero existe, que declara «no aplica» con su motivo, que **no** contiene código Java, que **no** inventa clases ni métodos, que **no** propone testear ni tocar los tests de `tiposexpedientes`, y que la excepción de §2 —si se usó— corresponde a una clase que el diseño realmente define.
- **Fuente de verdad:** `design.md`. **MUST NOT** modificar nada, ni el diseño ni los tests: solo detecta y reporta.

### 2.8 corrector-test-unitarios — corrige `test-unit-desc.md`

**Tarea:** aplicar **en sitio** sobre `design/test-unit-desc.md` cada incoherencia reportada, ajustándose a la plantilla de `tests-unitarios.md` §3.

- **Fuente de verdad:** `design.md`. **MUST NOT** modificar el diseño para que cuadre con los tests: corrige los tests. **MUST NOT** tocar ningún otro fichero del diseño.

---

## 3. Estructura de salida `design/`

La salida es una **carpeta** `design/` dentro de la carpeta de la iniciativa. **La estructura exacta, fichero a fichero, la define `design-contract.md` §1**, y no se repite aquí: el diseñador la produce tal cual y el verificador comprueba que no falta ni sobra nada.

Lo único que este README fija, porque es contrato con el motor y con los skills de aguas abajo:

- El **índice** se llama `design.md` y lleva frontmatter `type: design` más la clave `template:` copiada de la spec. Es lo que el motor usa para localizar y validar el diseño, y lo que consume `/sdd-implementer`.
- `test-e2e-desc.md` lo escribe el **diseñador**; `test-unit-desc.md` lo escribe el rol **test-unitarios** en una fase posterior. **MUST NOT** escribir el diseñador el segundo.
- Los ficheros `log_best.txt`, `log_revision.txt` y `log_revision_unit-test.txt` son **logs de orquestación del motor**: no son contenido de diseño, no los declara esta plantilla y el verificador **MUST** ignorarlos.

---

## 4. Contexto del proyecto a cargar

Lo carga el **diseñador** (§2.1) antes de generar. Es el único rol que lo necesita para producir; los demás trabajan sobre lo que hay en disco.

### 4.1 Skills técnicos

- **Siempre** `k-tipo-expediente` — **el skill central**: la carpeta de versión, el `TipoExpedienteInstance.xml` con sus fases y la máquina de estados, y **todos sus ficheros**: `modelo.md` (el `domains.xml`), `phaseeventmanager.md` (los `trigger*`/`onEnter*` y el `InitialEventManager`), `validator.md` (el DSL del `StateEventValidatorImpl`), `vistas.md` (el formato preprocesado), `documentos.md` (los `documentospdf/`) y `versionado.md` (duplicar un tipo para crear una versión nueva).
- **Siempre** `k-tramite` — el alta del trámite: la carpeta `tramites/<tramite>/`, el `TramiteInstance.xml`, la i18n del nombre y los permisos necesarios para poder crear expedientes.
- **Siempre** `k-validaciones` — en qué capa vive cada tipo de regla (`VAL-`, `RN-`, `RUI-`, `CC-`) que la spec ya clasificó. Es la referencia del **reparto de reglas** (`design-contract.md` §13). En un tipo de expediente **no existe** el prefijo `RES-`: el expediente vive guardado desde que nace y cada dato se exige **solo** en la pareja (estado, acción) en que se pide, así que toda obligatoriedad llega de la spec como `VAL-`. **MUST NOT** buscarse ni inventarse restricciones de entidad.
- **Siempre** `k-secure-coding` — el modelo de confianza cliente↔servidor. **Determina** la columna «quién lo rellena» de la tabla de campos y, con ella, qué campos pueden aparecer en un `field(...)` del validador (`design-contract.md` §6.1 y §12.3). Incluye la advertencia sobre el endpoint REST automático `POST /ws/rest/<FQN>`, que **no** pasa por el `Tramitador`.
- **Siempre** `k-datainit` — cómo se cargan los datos maestros y de permisos (`input-config.xml` + `input/`), para entender qué es `permisos-demo.xml` y qué es una fusión.
- **Siempre** `k-i18n` — cómo se traducen `title`, `help` y `name`, el marcador `__!!` y por qué **MUST NOT** escribirse ningún `i18n_*.csv`.
- **Siempre** `k-code-quality` — reglas de calidad del Java/Kotlin que el diseño especifica (descomposición, responsabilidad única, nombrado, idiomas modernos, convenciones Axelor/Guice/JPA).
- **Condicional** `k-guice` — cargar **solo si** el diseño necesita cablear DI no trivial: un servicio nuevo del trámite cuya construcción no es un simple `@Inject` de otro bean (dependencias que vienen de configuración o de runtime, `Provider`, binding explícito). Para inyectar el `<Entidad>Repository`, el `AlmacenClaveResolver` o el `ModelServiceFactory` en un `PhaseEventManagerImpl` **NO** hace falta.
- **CRITICAL — MUST NOT** cargar `k-seguridad`: está marcado **OBSOLETO** y su modelo de dominio no coincide con las clases reales. Para roles y permisos, leer el código real de `src/main/java/com/educaflow/subsystem/security/` y apoyarse en `k-secure-coding`.

Los skills son la fuente de verdad sobre **qué piezas existen y cómo se llaman**, no sobre el código exacto que las implementa.

### 4.2 Código existente a explorar

- `CLAUDE.md` del proyecto — convenciones, tipos de usuario y cargos, multicentro, y el apartado **PENDIENTE** sobre el endpoint REST automático.
- **CRITICAL — a diferencia de otros artefactos del pipeline, aquí el código real de `src/main/java/com/educaflow/tramites/` SÍ es referencia legítima.** Los trámites ya existentes en el árbol siguen **exactamente esta misma arquitectura**, así que el diseñador **SHOULD** leer uno o varios como referencia de forma (cómo se escribe un `TipoExpedienteInstance.xml`, cómo se reparten los `trigger*`, cómo se materializa un `views.xml` de fase, cómo se define un `documentospdf/<doc>.xml`). **MUST NOT**, en cambio, copiar sus nombres, sus estados, sus campos ni sus documentos: el diseño lo dicta la especificación.
- **CRITICAL — iniciativa de MODIFICACIÓN de una versión existente** (la línea «Versión» de la spec declara una modificación; la spec es un **delta**): la carpeta de versión modificada es el **as-is** y **MUST** leerse entera. El diseño es «as-is + delta»: cada fichero tocado se materializa **completo** en `design/` partiendo del fichero real, y los ficheros **no afectados** por el delta **MUST NOT** regenerarse ni aparecer en el diseño. **MUST NOT** crearse una versión nueva ni duplicarse la carpeta de versión: se modifica en su sitio.
- **Iniciativa de MODIFICACIÓN — los tests E2E ya persistidos de esa versión.** La carpeta espejo `src/test/e2e/tramites/…/<vN>/` (misma ruta que la carpeta de versión, con `src/main/java/com/educaflow/` sustituido por `src/test/e2e/`) contiene los `.desc.md` y `.spec.ts` que iniciativas anteriores dejaron sobre esta misma versión. El diseñador **MUST** leerlos (nunca escribirlos) para dos cosas: numerar sus `T-NNN` desde el primer libre (`tests-e2e.md` §2) y declarar los que el delta invalida a propósito (`design-contract.md` §15.3). En una iniciativa que **no** es de modificación esa carpeta no existe o es de otra versión: no se toca.
- `src/main/java/com/educaflow/subsystem/expedientes/` — el subsistema que tramita: `Tramitador`, `EventContext`, `ExpedienteLocator`, `ExpedienteController`, `FirmaController`, `ExpedienteUtil`, las acciones globales de `controllers/actions-expedientes.xml` y los paneles globales de `tramites/shared/`. Es donde se comprueba qué API existe realmente.
- `src/main/java/com/educaflow/base/infrastructure/` — `DocumentoPdf`, `MetaFileHelper`, `AlmacenClaveResolver`, `CampoFirma`, `Rectangulo` y las reglas del DSL de validación (`validation/rules/`).
- `src/main/resources/data-demo/input/permisos-demo.xml` — **MUST** leerse para saber qué `<perfil>` ya existen y **no duplicarlos** en `design/permisos.xml`.
- `src/main/resources/data-demo/input/usuarios-demo.xml` y `centros-demo.xml` — **MUST** leerse para los actores y credenciales de `test-e2e-desc.md` (ver `tests-e2e.md` §2).
- `src/main/java/com/educaflow/subsystem/expedientes/data-init/input/TipoTramites.xml` — **MUST** leerse para comprobar que el `<tipoTramite>` elegido existe.
- `src/test/java/com/educaflow/tiposexpedientes/` — los tests que verifican la forma de todo tipo de expediente. **MUST** leerse para saber qué se va a exigir; **MUST NOT** proponerse tocarlos ni ampliarlos (ver `tests-unitarios.md`).

### 4.3 Qué NO aplica a este artefacto

- **CRITICAL — `agent_docs/view-rules.md` y los tests de `com.educaflow.views` NO aplican a estas vistas.** Las vistas de un tipo de expediente están **explícitamente excluidas** de ambos: usan un formato propio preprocesado con tags custom (`<form state=…>`, `<include-panels>`, `<footer>`) que no es XML de vistas Axelor válido por sí solo. **MUST NOT** aplicárseles ninguna regla `VAR-`, ni las convenciones de `k-vistas`, ni el patrón `buttons-panel`/`btnSave`/`btnDelete`, ni las PI `sv-*`, ni la validación remota `remote-validation*`. Lo que sí aplica es `vistas.md` de esta plantilla.
- **MUST NOT** proponerse crear un `agent_docs/*-rules.md` ni un skill generador para los tests de `tiposexpedientes`: se escriben a mano y los `.java` son la fuente de verdad.
- **MUST NOT** usarse `agent_docs/architecture-rules.md` ni `/developer-create-arch-tests` para este artefacto.
- **MUST NOT** usarse ningún `design.md` ni XML de diseños previos de `.sdd/` como plantilla de estructura — **salvo lectura** de las iniciativas archivadas que `design-guidelines.md` cite explícitamente, y solo para respetar sus decisiones.

El **verificador** puede leer (nunca escribir) los ficheros reales que `validacion.md` le indica expresamente: `permisos-demo.xml`, `TipoTramites.xml`, los paneles globales de `tramites/shared/` y el árbol de `tramites/` para comprobar las acciones `Crear`/`Modificar`.

---

## 5. Partes del diseño: siempre vs. condicionales

El detalle de cada fichero está en `design-contract.md` §1. Aquí se fija **cuándo** existe cada parte. El verificador comprueba las dos direcciones: que lo obligatorio está, y que lo condicional está **si y solo si** su condición se cumple.

**Iniciativa de MODIFICACIÓN de una versión existente** (§4.2): la tabla §5.1 **NO aplica tal cual**. Lo obligatorio pasa a ser: `design.md` (con sus 15 secciones, escribiendo `*(sin cambios)*` donde el delta no toque — **salvo «Identidad del trámite y del tipo», «Ficheros a crear o modificar» y «Pasos», que van SIEMPRE completas**, `design-contract.md` §8), `test-e2e-desc.md`, `test-unit-desc.md`, y **exactamente** los ficheros que el delta toca (los de la tabla §6 del `design.md`); `permisos.xml` solo si hay perfiles o asignaciones nuevas. Un fichero de §5.1 ausente porque el delta no lo toca **no es un fallo**.
Dentro del `design.md`, este modo añade además la subsección `### Tests E2E supersedidos` de `## 13. Tests`, presente **si y solo si** el delta invalida a propósito algún test E2E ya persistido de esa versión (`design-contract.md` §15.3).

### 5.1 Siempre

| Parte | Nota |
|---|---|
| `design.md` | El índice, con las 15 secciones de `design-contract.md` §2 |
| `TramiteInstance.xml` | Uno por trámite |
| `TipoExpedienteInstance.xml` | Uno por versión, con **todas** sus fases |
| `domains.xml` | Con la entidad del tipo como **primera** `<entity>` |
| `views.xml` (raíz de la versión) | El form plantilla `exp-<Entidad>-Templates` |
| `estados.puml` | La proyección de la máquina de estados |
| `fases/<fase>/views.xml` | **Uno por CADA fase declarada**, con la fase en minúsculas |
| `permisos.xml` | **Siempre**: un tipo de expediente sin perfiles asignados es inalcanzable |
| `test-e2e-desc.md` | Siempre; el contrato está en `tests-e2e.md` |
| `test-unit-desc.md` | Siempre, pero lo escribe el rol **test-unitarios**, no el diseñador |

### 5.2 Condicionales

| Parte | Se incluye **solo si** | Si no aplica |
|---|---|---|
| `documentospdf/<doc>.xml` | el trámite **genera** al menos un documento PDF | no existe la carpeta `documentospdf/`, y la sección «Documentos PDF» del `design.md` lo dice en una frase, sin tabla |
| `documentospdf/_<fragmento>.xml` | hay un **fragmento compartido** entre dos o más documentos | no se declara ningún fichero con prefijo `_` |
| El bloque `<extra-code-model>` del `domains.xml` | **si y solo si** hay al menos un documento PDF | **MUST NOT** aparecer el bloque |
| La pieza de **firma en cliente** (par de campos + `<action-method>` con botón `serial:` + `FirmaPdf` en el validador) | algún documento se firma con AutoFirma | las tres piezas no existen; el `triggerInitialEvent` **MUST** declarar explícitamente que no hace falta `dniFirmaDocumentoEntrada` |
| Las acciones `FIRMAR_SERVIDOR` y sus constantes `Rectangulo` | algún documento se firma en servidor por cargo | no se declaran |
| Las acciones `REGISTRO_ENTRADA` / `REGISTRO_SALIDA` | el trámite registra entrada o salida | el `triggerInitialEvent` **MUST** declarar explícitamente que no hace falta rellenar `personaSolicitante`/`personaInteresada` |
| Paneles gemelos `<panel>-view` | la vista de lectura necesita **otro layout**; con el prefijo `-` basta en el caso normal | se usa solo el prefijo `-` |
| `<action-method>` propias de una fase | esa fase encadena una acción antes de un evento | no se declaran |
| Grids y forms auxiliares de entidades hija | el `domains.xml` declara entidades hija (`one-to-many`) | no se declaran |
| Filas de la tabla de ficheros por fase | **una vez por fase** (`PhaseEventManagerImpl`, `StateEventValidatorImpl`, `views.xml`) | — |

**MUST NOT** existir nunca en `design/`: ningún `.java`, ningún `.kt`, ningún `i18n_es.csv` ni `i18n_ca.csv`, ningún `estados.png`, ningún `States.java`, ningún data-init generado, ninguna carpeta `documentospdf/originales/` ni ningún `.pdf` binario.
