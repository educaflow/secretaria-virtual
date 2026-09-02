# Plantilla de implementación de un tipo de expediente — guía e índice

Esta carpeta de plantillas define **todo lo específico de convertir un DISEÑO (`design.md`) de un trámite y su tipo de expediente en código real** dentro del árbol del proyecto. El skill `sdd-implementer` aporta solo el **flujo** (localizar el diseño, descomponer, implementar tarea a tarea, verificar/corregir el build, cerrar) y es **agnóstico**: no sabe nada de cómo se descompone ni se materializa. Lo lee todo de aquí.

Este `README.md` es **el único fichero que el skill conoce por nombre**. **Lo leen los cuatro subagentes** y, según su rol, cada uno hace una tarea distinta:

- **descomponedor** — lee el diseño y **escribe la lista de tareas** en `implementation/` (§3.1).
- **implementador** — coge **una** tarea y la **materializa** en el árbol del proyecto (§3.2).
- **verificador-build** — **compila** el proyecto y reporta si pasa o los errores (§3.3).
- **corrector-build** — **corrige** los errores que el verificador-build reportó (§3.4).

A través de este README cada subagente descubre y lee **solo los ficheros de esta carpeta que su tarea necesita** (§2). **MUST NOT** copiarse ningún bloque explicativo de esta plantilla a los ficheros de salida.

> **Contrato fijo (lo garantiza el skill, no lo cambia esta plantilla):** la entrada es `design.md` (`type: design`) y la salida vive en `implementation/` (dentro de la carpeta de la iniciativa) y en el árbol del proyecto (`src/main/...`). Todo lo demás (cómo se descompone, qué tareas se escriben, cómo se materializa cada una, con qué comando se compila) lo define esta plantilla.

---

## 0. REGLA DE GENERALIDAD — léela antes que nada

**CRITICAL.** Esta plantilla describe **el patrón**, nunca un trámite concreto.

- **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, evento, campo, enum, perfil ni panel reales. Se usan **placeholders**.
- **MUST NOT** escribirse ninguna regla que solo valga para un número fijo de fases, estados, eventos, documentos PDF o perfiles. El patrón **MUST** funcionar con **1, 2, 3 o N fases**; con **0, 1 o N** documentos PDF; con firma **en cliente, en servidor, en ambas o en ninguna**; con **0, 1 o N** registros de entrada/salida; con estados **con y sin** `profile`.
- Todo ejemplo va encerrado en un bloque que empieza por `> **Ejemplo** (ilustrativo, NO normativo):`, con nombres inventados.

### 0.1 Placeholders

| Placeholder | Significado | Forma |
|---|---|---|
| `<tramite>` | carpeta del trámite bajo `com/educaflow/tramites/` (puede llevar segmentos de agrupación) | `snake_case` |
| `<Code>` | `<code>` del `TramiteInstance.xml` | `UpperCamelCase`, sin guiones ni underscores |
| `<vN>` | carpeta de versión (`v1`, `v2`, …), posiblemente bajo segmentos de agrupación | `v` + número |
| `<VN>` | la versión en UpperCamel (`V1`, `V2`, …) | |
| `<Entidad>` | entidad JPA y **code del tipo de expediente** | `<Code><VN>` |
| `<basePackageName>` | paquete de la carpeta de versión | lo que sigue a `/java/`, con `/`→`.` |
| `<FASE>` / `<fase>` | `name` de una fase / su carpeta y paquete | `UPPER_SNAKE_CASE` / `toLowerCase` |
| `<ESTADO>` / `<Estado>` | `name` de un estado / en UpperCamel | `UPPER_SNAKE_CASE` / `UpperCamel` |
| `<EVENTO>` / `<Evento>` | `name` de un evento / en UpperCamel | `UPPER_SNAKE_CASE` / `UpperCamel` |
| `<PERFIL>` | valor del enum `Profile`: `CREADOR`, `RESPONSABLE`, `SECRETARIO`, `DIRECTOR`, `AUDITOR` | `UPPER_SNAKE_CASE` |
| `<Campo>` | campo de la entidad, en UpperCamel para el getter | |
| `<doc>` | nombre base de un documento de `documentospdf/` | `camelCase` |
| `<carpeta de versión>` | `src/main/java/com/educaflow/tramites/<tramite>/<…segmentos…>/<vN>` | ruta desde la raíz del proyecto |

---

## 1. Ficheros de esta carpeta de plantillas

El skill abre `README.md` y, a través de él, cada subagente usa los demás.

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: contrato fijo, estructura de entrada/salida, contexto del proyecto a cargar y los principios comunes a todos los roles. | Los **cuatro** subagentes (es el contrato que el skill nombra). **MUST NOT** copiarse al output. |
| `decomposition.md` | **Cómo descomponer el diseño en tareas**: el **orden obligatorio** de las tareas, qué texto del `design.md` se copia verbatim en cada una y de qué secciones sale, los skills por tarea, la propagación de `test-e2e-desc.md`, las **plantillas exactas** de `task_NN.md` / `tasks.md` y el checklist. | El **descomponedor** (§3.1). |
| `implementation.md` | **Cómo materializar una tarea**: qué XML se copian verbatim y a qué ruta destino, cómo se rellenan los `.java`/`.kt` **sobre el esqueleto** que dejó `CreateFilesTask`, cómo se fusiona `permisos-demo.xml`, las prohibiciones duras y el manejo de `CONFLICT`/`BLOCKED`/`DESIGN-ERROR`. | El **implementador** (§3.2); el **corrector-build** (§3.4) lo consulta para saber qué puede tocar. |
| `build.md` | **Cómo verificar y corregir el build**: el comando, la nota de entorno del *sandbox*, el criterio de éxito, el formato JSONL, el chequeo de conformidad de superficie, qué puede y qué **NO** puede tocar el corrector y el catálogo de errores típicos de este artefacto. | El **verificador-build** (§3.3); el **corrector-build** (§3.4). |
| `tests-code.md` | **Qué tests se generan**: **ninguno propio**, salvo la excepción de su §4. La conformidad la dan los tests ya existentes y escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`; aquí se enumera **qué exigen**, y cómo se tratan `test-e2e-desc.md` y `test-unit-desc.md`. | El **descomponedor** (§3.1) y el **implementador** (§3.2). |

---

## 2. Estructura de entrada y de salida

### 2.1 Entrada — la carpeta `design/`

El diseñador (`/sdd-designer`) dejó en `{iniciativa}/design/` **exactamente** esta estructura:

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── design/
    ├── design.md                          ← índice (frontmatter type: design + template: heredada de la spec)
    ├── TramiteInstance.xml                ← XML materializado, listo para copiar
    ├── TipoExpedienteInstance.xml         ← XML materializado
    ├── domains.xml                        ← XML materializado
    ├── views.xml                          ← XML materializado (form plantilla de la raíz de la versión)
    ├── estados.puml                       ← materializado
    ├── fases/<fase>/views.xml             ← XML materializado, uno por CADA fase declarada
    ├── documentospdf/<doc>.xml            ← XML materializado, uno por documento (0..N)
    ├── documentospdf/_<fragmento>.xml     ← XML materializado, 0..N fragmentos reutilizables
    ├── permisos.xml                       ← fragmento a FUSIONAR en permisos-demo.xml
    ├── test-e2e-desc.md                   ← tests E2E en Given/When/Then
    └── test-unit-desc.md                  ← declaración de cobertura de los tests ya existentes
```

- **MUST** haber un `fases/<fase>/views.xml` por **cada** fase declarada en el `TipoExpedienteInstance.xml`, con la carpeta en **minúsculas**, y **ninguno** vacío.
  El diseño lo garantiza: toda fase tiene al menos un estado y todo estado lleva su form genérico, así que el fichero de toda fase existe y tiene contenido.
  Que falte el de una fase, o que llegue con el `<object-views>` sin ningún hijo, es un **DESIGN-ERROR** (un `<object-views>` vacío tumba el arranque y el build no lo detecta: `build.md` §6). El resto de la estructura es fija.
- **MUST NOT** existir ningún `.java` ni `.kt` en `design/`: el código Java/Kotlin **no** se materializa en el diseño, se **describe** en `design.md` §8, §9 y §10, y se escribe aquí.
- Los ficheros `log_best.txt`, `log_revision.txt` y `log_revision_unit-test.txt` son **logs del motor del designer**: **MUST NOT** tratarse como contenido de diseño ni generar tareas.

El `design.md` tiene **exactamente** estas 15 secciones, con estos títulos y en este orden (el descomponedor las usa para repartir el texto verbatim):

| # | Sección |
|---|---|
| 1 | Objetivo |
| 2 | Identidad del trámite y del tipo |
| 3 | Máquina de estados |
| 4 | Modelo |
| 5 | Documentos PDF |
| 6 | Ficheros a crear o modificar |
| 7 | Pasos |
| 8 | **Especificación del InitialEventManagerImpl** |
| 9 | **Especificación de los PhaseEventManagerImpl** |
| 10 | **Especificación de los StateEventValidatorImpl** |
| 11 | Reparto de reglas |
| 12 | Asignación de perfiles |
| 13 | Tests |
| 14 | Notas y supuestos |
| 15 | Checklist del diseñador |

**CRITICAL** — §8, §9 y §10 son la **especificación quirúrgica** del código Java/Kotlin: sin ellas no se puede escribir ni una clase. Toda tarea que materialice un `.java` o un `.kt` **MUST** llevarlas (la parte que le corresponde) copiadas **verbatim** dentro de sí (`decomposition.md` §4).

### 2.2 Salida — `implementation/` y el árbol del proyecto

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── implementation/
    ├── tasks.md                        ← índice de tareas con checkboxes (type: implementation-tasks)
    ├── task_01.md … task_NN.md          ← una tarea por fichero/componente (type: implementation-task)
    └── test-e2e-desc.md                 ← copia literal de design/test-e2e-desc.md

src/main/java/com/educaflow/tramites/<tramite>/
├── TramiteInstance.xml
└── <…segmentos…>/<vN>/
    ├── TipoExpedienteInstance.xml
    ├── domains.xml
    ├── views.xml
    ├── estados.puml
    ├── estados.png                      ← lo genera GenerateDocs; MUST NOT escribirse a mano
    ├── InitialEventManagerImpl.java
    ├── <fase>/PhaseEventManagerImpl.java
    ├── <fase>/StateEventValidatorImpl.kt
    ├── <fase>/views.xml
    └── documentospdf/<doc>.xml, documentospdf/_<fragmento>.xml

src/main/resources/data-demo/input/permisos-demo.xml   ← se FUSIONA (no se sobrescribe)
```

- **MUST NOT** escribirse ningún fichero bajo `src/test/...`, **salvo la excepción de `tests-code.md` §4** (una clase auxiliar propia con lógica de negocio aislable): este artefacto **no genera tests propios** (`tests-code.md`).
- **MUST NOT** crearse **jamás** un `i18n_es.csv` ni un `i18n_ca.csv`. Los genera el build; escribirlos a mano está **prohibido** y es un fallo bloqueante.
- **MUST NOT** escribirse `States.java`, `estados.png`, el data-init de trámites/tipos ni nada bajo `build/`: todos son **generados**.
- `implementation/test-e2e-desc.md` es **contrato fijo hacia abajo**: lo ejecuta `/sdd-debug-with-test-e2e-desc`. **MUST NOT** modificarlo, resumirlo ni renumerarlo.

---

## 3. Tareas de los cuatro subagentes

Todos reciben las **mismas rutas de entrada** (este `README.md` y la carpeta `{iniciativa}/design`; el **implementador** recibe además la ruta de su tarea, y el **corrector-build** las líneas JSONL de errores), pero **cada rol hace una tarea distinta y lee un subconjunto distinto** de los ficheros de esta carpeta.

> **Común a los cuatro:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** copiar ningún bloque explicativo de la plantilla a los ficheros de salida. **MUST NOT** usar `AskUserQuestion` (reportan el bloqueo con el token de su rol; el motor lleva la decisión al usuario).

**Resumen por rol** (el detalle en §3.1–§3.4):

| Rol | Escenario — qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **descomponedor** (§3.1) | **Lee el diseño y escribe las tareas**, en el orden obligatorio | la carpeta `{iniciativa}/design` | `decomposition.md`; `tests-code.md` | `{iniciativa}/implementation/` con `task_NN.md`, `tasks.md` y `test-e2e-desc.md` |
| **implementador** (§3.2) | **Materializa una tarea** en el árbol | la carpeta `design` + la ruta de **una** tarea | `implementation.md`; `tests-code.md` si la tarea lo referencia | la tarea materializada bajo `src/main/...` |
| **verificador-build** (§3.3) | **Compila y reporta** | el árbol del proyecto | `build.md` | `OK-COMPILA` o el JSONL de errores (no corrige) |
| **corrector-build** (§3.4) | **Corrige** los errores reportados | el árbol + el JSONL de errores | `build.md` + `implementation.md` | el árbol corregido en sitio |

### 3.1 descomponedor — lee el diseño y escribe las tareas

**Tarea:** leer **entera** la carpeta `{iniciativa}/design` y escribir la lista de tareas de implementación en `{iniciativa}/implementation/`, en el **orden obligatorio** que fija `decomposition.md` §2, cada una con sus skills y con el texto del `design.md` que la especifica copiado **verbatim**; más el índice `tasks.md` y la copia literal de `test-e2e-desc.md`.

- **Lee de esta plantilla:** `decomposition.md` (el orden obligatorio, el reparto del texto verbatim por secciones del `design.md`, los skills por tarea, las plantillas de `task_NN.md` / `tasks.md`, el token de salida y el **checklist**); y `tests-code.md` (por qué **no** se crea ninguna tarea de tests —salvo la excepción de su §4— y cómo se propaga `test-e2e-desc.md`).
- **Entrada propia:** la carpeta `{iniciativa}/design` — el `design.md` **íntegro** (la tabla de §6, los pasos de §7 y, sobre todo, las especificaciones de §8, §9 y §10) y los ficheros materializados.
- **CRITICAL** — el número de tareas **depende del número de fases y de documentos del tipo**: hay **una tarea por fase**. **MUST NOT** fijarse un número de tareas a priori.
- **MUST NOT** materializar código en `src/...`: solo escribe los ficheros de `implementation/`. **MUST NOT** dar la descomposición por terminada sin pasar el checklist de `decomposition.md` §7.

### 3.2 implementador — materializa una tarea

**Tarea:** dada **una** tarea de `{iniciativa}/implementation/`, **materializarla en el árbol del proyecto**: copiar verbatim el XML o el `.puml` que le toque, **ejecutar `CreateFilesTask`** si esa es su tarea, rellenar el `.java`/`.kt` **sobre el esqueleto** delegando en `developer-code-implementer`, o fusionar el fragmento de permisos.

- **Lee de esta plantilla:** `implementation.md` (el mapeo origen→destino de cada XML, la consecuencia práctica de que `CreateFilesTask` sea idempotente, cómo se rellena cada clase, la fusión de `permisos-demo.xml`, las prohibiciones duras y el manejo de bloqueos); y `tests-code.md` si la tarea lo referencia.
- **Entrada propia:** la ruta de **su** tarea (`task_NN.md`) y la carpeta `{iniciativa}/design` (los XML materializados son **contrato fijo**: se copian tal cual, **NO** se regeneran).
- **OBLIGATORIO:** carga primero, con la herramienta `Skill`, los skills que la tarea lista; y solo entonces, si la tarea es de código, **invoca `developer-code-implementer`** pasándole el `<texto del prompt>` de la tarea **verbatim**.
- **MUST NOT** adivinar ante un bloqueo: lo reporta con su token (`CONFLICT` / `BLOCKED` / `DESIGN-ERROR`), según el criterio de `implementation.md` §7.

### 3.3 verificador-build — compila y reporta

**Tarea:** **compilar el proyecto** con el comando que prescribe `build.md` y **reportar** si pasa o los errores, sin corregir nada.

- **Lee de esta plantilla:** `build.md` — el comando, la **nota de entorno del sandbox** (§1.1, CRITICAL), el criterio de éxito (incluye que pasen los tests de `com/educaflow/tiposexpedientes` y de `com/educaflow/views`), el **formato JSONL** y el **chequeo de conformidad de superficie**.
- **Ejecuta tú mismo** (con `Bash`) el comando de compilación. El motor **NUNCA** lo ejecuta.
- **MUST NOT** corregir nada: solo compila, aplica el chequeo de conformidad y reporta.

### 3.4 corrector-build — corrige los errores

**Tarea:** dada la lista JSONL de errores del verificador-build, **corregirlos en sitio**, sin tocar lo que el contrato prohíbe.

- **Lee de esta plantilla:** `build.md` (qué errores resolver, qué puede y qué **NO** puede tocar, y el catálogo de errores típicos de este artefacto con su corrección) e `implementation.md` (los XML del diseño ya colocados son contrato fijo).
- **Fuente de verdad:** los XML materializados del diseño y las especificaciones de `design.md` §8/§9/§10. **MUST NOT** editarlos para que cuadre el Java: corrige el Java para que cuadre con ellos.
- **MUST NOT** editar ni «arreglar» los tests de `src/test/java/com/educaflow/tiposexpedientes/` ni de `src/test/java/com/educaflow/views/`: son fuente de verdad escrita a mano. Si uno falla, el fallo está en el trámite generado.
- Si el error **solo** se puede resolver cambiando el diseño, responde en la **primera línea** `DESIGN-ERROR: {motivo detallado}` y termina.

---

## 4. Contexto del proyecto a cargar

Los skills técnicos que necesita el **implementador** van **por tarea**, listados dentro de cada `task_NN.md` (los puso el descomponedor según `decomposition.md` §3). El implementador los carga con `Skill` **antes** de implementar nada.

| Skill | Qué aporta | Cuándo |
|---|---|---|
| `k-tramite` | La carpeta `<tramite>/`, el `TramiteInstance.xml`, la i18n del nombre y los permisos para poder crear expedientes del trámite | Tarea del `TramiteInstance.xml` |
| `k-tipo-expediente` (`SKILL.md`) | Todo lo que hay dentro de una carpeta de versión: el `TipoExpedienteInstance.xml`, las fases, la máquina de estados, `CreateFilesTask` y la clase `States` generada | **Todas** las tareas bajo la carpeta de versión |
| `k-tipo-expediente` → `modelo.md` | El `domains.xml`: entidad, `extends="Expediente"`, enums sufijados, `<extra-code-model>` | Tarea del `domains.xml` |
| `k-tipo-expediente` → `phaseeventmanager.md` | `InitialEventManagerImpl` y `PhaseEventManagerImpl`: `@WhenEvent`, `@OnEnterState`, `EventContext`, PDFs, firmas, registros | Tareas de `.java` |
| `k-tipo-expediente` → `validator.md` | `StateEventValidatorImpl`: `@BeanValidationRulesForStateAndEvent`, el DSL `rules { }` y la frontera de confianza | Tareas de `.kt` |
| `k-tipo-expediente` → `vistas.md` | El `views.xml` de la raíz (form plantilla) y los `views.xml` de fase (`<include-panels>`, `<footer>`) | Tareas de vistas |
| `k-tipo-expediente` → `documentos.md` | El formato XML de `documentospdf/`, fragmentos, `colspan`, castellano/valenciano | Tarea de `documentospdf/` |
| `k-tipo-expediente` → `versionado.md` | Cómo duplicar un tipo para crear una versión nueva y las trampas del `import` de `States` | Tarea de una versión `<vN>` con `N > 1` |
| `k-validaciones` | En qué capa vive cada `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` | Tareas de `domains.xml` y de `.kt` |
| `k-secure-coding` | Mass-assignment, `AllowProperties` por evento, campos que solo dicta el servidor, multi-centro/IDOR | **CRITICAL** — toda tarea de `.java`, `.kt` o `domains.xml` |
| `k-i18n` | `I18n.get(...)`, el sufijo `__!!`, cómo se traducen títulos y mensajes | Tareas con mensajes visibles al usuario |
| `k-code-quality` | Calidad técnica del Java/Kotlin del proyecto (métodos, clases, idiomas, logger slf4j) | Toda tarea de `.java` o `.kt` |
| `k-datainit` | Las carpetas `data-init`, el formato de los ficheros de datos y de permisos | Tarea de `permisos-demo.xml` |

### 4.1 CRITICAL — aquí el código real de `tramites/` SÍ es referencia legítima

A diferencia de otros artefactos del proyecto, donde el código de `expedientes`/`tramites` está **excluido** como referencia por seguir otra arquitectura, **aquí es exactamente la misma arquitectura**: un tipo de expediente nuevo se escribe igual que los que ya existen.

- **Es legítimo** leer los trámites ya escritos bajo `src/main/java/com/educaflow/tramites/` para ver cómo se resuelve en la práctica un patrón: la forma de un `PhaseEventManagerImpl`, la sintaxis real del DSL de un `StateEventValidatorImpl`, la estructura de un `views.xml` de fase, el formato de un documento de `documentospdf/`, o el bloque `<extra-code-model>` de un `domains.xml`.
- **SHOULD** consultarse cuando el diseño describe una acción cuya sintaxis exacta no está en la propia especificación (una firma en servidor, un visor de PDF embebido, un maestro-detalle).
- **CRITICAL — la fuente de verdad sigue siendo el diseño, no otro trámite.** Otro trámite enseña **cómo se escribe**, nunca **qué hay que escribir**. **MUST NOT** copiarse de él un campo, un estado, un evento, un panel, un botón, un documento ni una regla de validación que el `design.md` de esta iniciativa no declare: eso es superficie inventada y el chequeo de conformidad de `build.md` §5 la reporta.
- **MUST NOT** usarse como referencia el `design.md` ni los XML de **otras iniciativas** de `.sdd/`.
- **MUST NOT** leerse la carpeta `.sdd/` (specs, designs, drafts, archive) de otras iniciativas para entender qué hace el código: es material de trabajo del pipeline y puede estar desactualizado.

- ✅ CORRECTO: mirar un `PhaseEventManagerImpl` existente para ver cómo se escribe una llamada a `almacenClaveResolver` y replicar **la forma**, con el cargo, el rectángulo y el documento que dice **este** diseño.
- ❌ INCORRECTO: copiar de él un `trigger<Evento>` entero, o añadir un campo o un botón «porque el otro trámite lo tiene» sin que este diseño lo declare.
