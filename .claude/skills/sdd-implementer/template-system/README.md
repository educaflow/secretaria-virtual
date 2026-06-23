# Plantilla de implementación de un sistema — guía e índice

Esta carpeta de plantillas define **todo lo específico de convertir un DISEÑO (`design.md`) en código real** de un sistema/subsistema del proyecto EducaFlow. El skill `sdd-implementer` aporta solo el **flujo** (localizar el diseño, descomponer, implementar tarea a tarea, verificar/corregir el build, cerrar) y es **agnóstico**: no sabe nada de cómo se descompone ni se materializa. Lo lee todo de aquí.

Este `README.md` es **el único fichero que el skill conoce por nombre**. **Lo leen los cuatro subagentes** y, según su rol, cada uno tiene una tarea distinta:

- **descomponedor** — **lee el diseño y escribe la lista de tareas** en `implementation/` (§2.1).
- **implementador** — coge **una** tarea y la **materializa** en el árbol del proyecto (§2.2).
- **verificador-build** — **compila** el proyecto y reporta si pasa o los errores (§2.3).
- **corrector-build** — **corrige** los errores de compilación que el verificador-build reportó (§2.4).

A través de este README cada subagente descubre y lee **solo los ficheros de esta carpeta que su tarea necesita** (§2). **MUST NOT** copiarse ningún bloque explicativo de esta plantilla a los ficheros de salida.

> **Contrato fijo (lo garantiza el skill, no lo cambia esta plantilla):** la entrada es `design.md` (`type: design`) y la salida vive en `implementation/` (dentro de la carpeta de la iniciativa) y en el árbol del proyecto (`src/main/...`, `src/test/...`). Todo lo demás (cómo se descompone, qué tareas se escriben, cómo se materializa cada una, con qué comando se compila) lo define esta plantilla.

---

## 1. Ficheros de esta carpeta de plantillas

El skill abre `README.md` y, a través de él, usa los demás. Cada uno se transmite al subagente que lo necesita.

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: contrato fijo, estructura de entrada/salida, contexto del proyecto, y los principios comunes a todos los roles. | Todos los subagentes (es el contrato que el skill nombra). **MUST NOT** copiarse al output. |
| `decomposition.md` | **Cómo descomponer el diseño en tareas**: qué tareas crear de cada parte del diseño (XML, Java, tests unitarios), cómo agrupar los ficheros acoplados, el orden de implementación, cómo determinar los skills de cada tarea, el texto del diseño a copiar verbatim, la propagación de los ficheros de contrato hacia abajo, las **plantillas exactas** de `task_NN.md` / `tasks.md` (índice con checkboxes de progreso) y el checklist. | El **descomponedor** (§2.1). |
| `implementation.md` | **Cómo materializar una tarea** en el árbol del proyecto: copiar literalmente los XML ya materializados, fusionar `menus.xml` y validarlo, y delegar el código Java en `code-implementer` (cargando antes los skills). Define los principios de no-regenerar-XML, no-escribir-Java-a-mano y el manejo de conflictos/bloqueos. | El **implementador** (§2.2); el **corrector-build** (§2.4) lo consulta para saber qué puede tocar al corregir. |
| `tests-code.md` | **Cómo generar el código de los tests** a partir de las descripciones del diseño: tests unitarios (JUnit 5 + Mockito) desde `design/test-unit-desc.md`, dónde se ubican (`src/test/...`), qué skills cargar y cómo delegarlos en `code-implementer`. | El **descomponedor** (§2.1, para crear las tareas de test); el **implementador** (§2.2, para materializarlas). |
| `build.md` | **Cómo verificar y corregir el build**: el comando de compilación (`./gradlew clean build`), qué cuenta como éxito, cómo reportar los errores en JSONL, la detección de fallos persistentes y qué puede/no puede tocar el corrector. | El **verificador-build** (§2.3); el **corrector-build** (§2.4). |

---

## 2. Tareas de los cuatro subagentes

El skill `sdd-implementer` lanza estos cuatro roles. Todos reciben las **mismas rutas de entrada** (este `README.md` y la carpeta `{iniciativa}/design`; el **implementador** recibe además la ruta de su tarea, y el **corrector-build** las líneas JSONL de errores), pero **cada rol hace una tarea distinta y lee un subconjunto distinto de los ficheros de esta carpeta**. Lo que sigue acota, por rol, **qué hace** y **qué ficheros de esta plantilla le aplican**. El contrato de tokens y la orquestación los fija el skill; aquí solo se delimita el trabajo de cada uno.

> **Común a los cuatro:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** copiar ningún bloque explicativo de la plantilla a los ficheros de salida. **MUST NOT** usar `AskUserQuestion` (reportan el bloqueo con el token de su rol; el motor lleva la decisión al usuario).

**Resumen por rol** (el detalle de cada uno en §2.1–§2.4):

| Rol | Escenario — qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **descomponedor** (§2.1) | **Lee el diseño y escribe las tareas** | la carpeta `{iniciativa}/design` | `decomposition.md`; `tests-code.md` (para las tareas de test) | la carpeta `{iniciativa}/implementation/` con las tareas, su índice y los ficheros propagados |
| **implementador** (§2.2) | **Materializa una tarea** en el árbol | la carpeta `design` + la ruta de **una** tarea | `implementation.md`; `tests-code.md` si la tarea es de tests | la tarea materializada en `src/main/...` o `src/test/...` |
| **verificador-build** (§2.3) | **Compila y reporta** | el árbol del proyecto | `build.md` (comando, criterio de éxito, formato de error) | `OK-COMPILA` o la lista de errores (no corrige) |
| **corrector-build** (§2.4) | **Corrige** los errores de compilación | el árbol + la lista de errores | `build.md` + `implementation.md` (qué puede tocar) | el árbol corregido en sitio |

Solo el **descomponedor** lee el diseño íntegro para planificar; el **implementador** carga, por tarea, los skills técnicos que la propia tarea indica (los puso el descomponedor desde `decomposition.md`); el **verificador-build** y el **corrector-build** trabajan sobre el código ya escrito.

### 2.1 descomponedor — lee el diseño y escribe las tareas

**Tarea:** leer la carpeta `{iniciativa}/design` y **escribir la lista de tareas de implementación** en `{iniciativa}/implementation/`, una tarea por fichero (agrupando los ficheros fuertemente acoplados), cada una con sus skills y el texto del diseño que la describe; más el índice y los ficheros de contrato propagados.

- **Lee de esta plantilla:** `decomposition.md` (qué tareas crear, cómo agrupar, el orden, los skills por tarea, el texto a copiar verbatim, la propagación, las plantillas de `task_NN.md` / `tasks.md` y el **checklist**); y `tests-code.md` para crear las tareas que materializan los tests unitarios (desde `design/test-unit-desc.md`), si el diseño los describe.
- **Entrada propia:** la carpeta `{iniciativa}/design` —sobre todo `design.md` (la tabla de ficheros y las secciones de pasos), y las descripciones de tests si existen—.
- **MUST NOT** materializar código en `src/...`: solo escribe los ficheros de `implementation/`. **MUST NOT** dar la descomposición por terminada sin pasar el checklist de `decomposition.md`.

### 2.2 implementador — materializa una tarea

**Tarea:** dada **una** tarea de `{iniciativa}/implementation/`, **materializarla en el árbol del proyecto** según el contrato (colocar los XML ya materializados / fusionar menús / delegar el Java y los tests en `code-implementer`).

- **Lee de esta plantilla:** `implementation.md` (cómo colocar los XML literalmente, cómo fusionar y validar `menus.xml`, cómo delegar el Java en `code-implementer` cargando antes los skills, y el manejo de conflictos/bloqueos); y `tests-code.md` **solo si** la tarea es de tests (cómo generar el código JUnit desde la descripción).
- **Entrada propia:** la ruta de **su** tarea (`task_NN.md`) y la carpeta `{iniciativa}/design` (los XML materializados de los que dependa son **contrato fijo**).
- **OBLIGATORIO:** carga primero, con la herramienta `Skill`, los skills que la tarea lista, y luego —si el contrato lo indica— invoca `code-implementer` con el texto de la tarea **verbatim**.
- **MUST NOT** regenerar los XML del diseño. **MUST NOT** adivinar ante un bloqueo: lo reporta con su token (`CONFLICT`/`BLOCKED`).

### 2.3 verificador-build — compila y reporta

**Tarea:** **compilar el proyecto** con el comando que prescribe la plantilla y **reportar** si pasa o los errores, sin corregir nada.

- **Lee de esta plantilla:** `build.md` — con qué comando se compila, qué cuenta como éxito (incluye que los tests unitarios del build pasen), y el **formato JSONL** exacto para reportar los errores.
- **Ejecuta tú mismo** (con `Bash`) el comando de compilación que `build.md` prescribe.
- **MUST NOT** corregir nada: solo compila y reporta. Corregir es tarea del corrector-build (§2.4).

### 2.4 corrector-build — corrige los errores de compilación

**Tarea:** dada la lista de errores del verificador-build (§2.3), **corregirlos en sitio** sobre el código del árbol del proyecto, sin tocar lo que el contrato prohíbe.

- **Lee de esta plantilla:** `build.md` (qué errores hay que resolver y qué puede/no puede tocar al corregir) e `implementation.md` (los XML del diseño son contrato fijo; el Java se corrige delegando en `code-implementer`).
- **Fuente de verdad:** los XML materializados del diseño. **MUST NOT** editarlos para que cuadre el Java: corrige el Java para que cuadre con ellos. Si un XML del diseño está mal, **detente y repórtalo**.
- **Aplica** exactamente los errores reportados, sin reescribir lo que ya funciona.

---

## 3. Estructura de entrada y de salida

### 3.1 Entrada — la carpeta `design/`

El diseñador (`/sdd-designer`) dejó en `{iniciativa}/design/` el diseño completo. Los nombres concretos los define la plantilla del designer; el descomponedor los descubre leyendo `design.md`. A título orientativo:

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── design/
    ├── design.md                      ← índice (type: design) — la tabla de ficheros y los pasos
    ├── domains/<Entidad>.xml           ← XML materializados (ya validados con xmllint por el designer)
    ├── views/<Fichero>.xml
    ├── menus.xml                        ← porción de <menuitem> a fusionar
    ├── test-e2e-desc.md                 ← tests E2E (si los hay) — se propaga tal cual
    ├── test-unit-desc.md               ← descripción de los tests unitarios (si hay clases Java)
    └── rules/R-<Entidad>-NNN.md         ← reglas complejas (si las hay) — documentación referenciada
```

Los XML **ya están validados con `xmllint`** por el diseñador: son la fuente de verdad y se **copian tal cual**, no se regeneran (`implementation.md`).

### 3.2 Salida — `implementation/` y el árbol del proyecto

El descomponedor escribe `{iniciativa}/implementation/`; los implementadores escriben el código real. La estructura interna de `implementation/` la fija `decomposition.md`; a título orientativo:

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── implementation/
    ├── tasks.md                        ← índice de tareas con checkboxes de progreso (type: implementation-tasks)
    ├── task_01.md … task_NN.md          ← una tarea por fichero/componente (type: implementation-task)
    └── test-e2e-desc.md                 ← copia literal de design/test-e2e-desc.md (si existe) — la consume /sdd-test-e2e

src/main/java/com/educaflow/…           ← XML materializados colocados/fusionados + código Java
src/test/java/com/educaflow/…           ← tests unitarios (JUnit+Mockito)
```

`implementation/test-e2e-desc.md` es **contrato fijo hacia abajo**: lo ejecuta `/sdd-test-e2e`. **MUST NOT** modificarlo, resumirlo ni renumerarlo (`decomposition.md`).

---

## 4. Contexto del proyecto a cargar

Ningún rol carga skills "por defecto" desde aquí: los skills técnicos que necesita el **implementador** van **por tarea**, listados dentro de cada `task_NN.md` (los puso el descomponedor según `decomposition.md`). El implementador los carga con `Skill` antes de implementar. El verificador-build y el corrector-build no necesitan skills de dominio salvo los que la tarea afectada implique.

- Referencia de arquitectura, capas, tipos de usuario y árbol de subsistemas: el `CLAUDE.md` del proyecto y los `CLAUDE.md` de cada carpeta. Los subagentes los consultan cuando el contrato lo pide.
- **MUST NOT** usar como referencia el código de `expedientes`/`tiposexpedientes`/`tramites` (siguen otra arquitectura) ni `design.md`/XML de diseños previos de otras iniciativas como plantilla.
