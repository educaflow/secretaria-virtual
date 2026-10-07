# Plantilla de implementación de una feature libre («free») — guía e índice

Esta carpeta de plantillas define **todo lo específico de convertir el diseño de una feature libre en código real** del proyecto EducaFlow. El skill `sdd-implementer` aporta solo el **flujo** (localizar el diseño, descomponer en tareas, implementar cada una, compilar hasta que pase) y es **agnóstico**: lo lee todo de aquí.

Una feature libre no tiene un esquema fijo de piezas (puede tocar `base`, varios sistemas, configuración, vistas, jobs…), así que esta plantilla no presupone capas: **las piezas, su ubicación y su orden los dicta el `design.md`**. Lo que esta plantilla **sí** fija es el rigor: tareas verbatim del diseño, **TDD** (el test de cada pieza se materializa **antes** que la pieza y la pieza se implementa hasta que el test pasa), superficie cerrada, y un build limpio.

Este `README.md` es **el único fichero que el skill conoce por nombre**. **Lo leen los cuatro roles** (tabla de §2).

> **Contrato fijo (lo garantiza el skill):** la entrada es `design/design.md` (`type: design`) y la salida es la carpeta `{iniciativa}/implementation/` más el código en el árbol del proyecto (`src/main/...` y `src/test/...`). La estructura interna de `implementation/` la define esta plantilla.

---

## 1. Ficheros de esta carpeta de plantillas

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: roles, estructura de entrada/salida, contexto. | Los cuatro roles. **MUST NOT** copiarse al output. |
| `decomposition.md` | **Del diseño a las tareas**: cómo leer la tabla de ficheros y los «Pasos» del `design.md`, cómo agrupar, el **orden TDD** (test antes que pieza), los skills por tarea, las plantillas exactas de `task_NN.md` y `tasks.md`, la propagación de `test-e2e-desc.md` y el checklist. | **Descomponedor**. |
| `implementation.md` | **Materializar una tarea**: XML del diseño copiados literalmente (si los hay), Java/Kotlin delegado en `developer-code-implementer`, la regla TDD de la pareja test → pieza (ejecutar el test de la pieza con Gradle hasta verde), superficie cerrada, `Crear`/`Modificar`, tokens de bloqueo, marcar `[x]`. | **Implementador**; lo consulta el **corrector-build**. |
| `tests-code.md` | **Generar el código de los tests unitarios** desde `design/test-unit-desc.md`: ubicación, skills, la plantilla del prompt de una tarea de test y la regla de que el test se escribe **antes** que la pieza (fase red). | **Descomponedor** e **implementador**. |
| `build.md` | **El build**: comando, criterio de éxito (incluidos los warnings de Error Prone, los tests de arquitectura/vistas/orden de métodos, CPD y CRAP que `./gradlew clean build` ya ejecuta), formato JSONL de errores, qué puede tocar el corrector y el chequeo de conformidad de superficie. | **Verificador-build** y **corrector-build**. |

---

## 2. Tareas de los cuatro subagentes

Los cuatro reciben este `README.md` y la ruta de `design/`; cada uno recibe además su entrada propia y lee un subconjunto de los ficheros de esta carpeta.

> **Común a los cuatro:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** copiar bloques explicativos de la plantilla al output. **MUST NOT** usar `AskUserQuestion`. **MUST NOT** pegar en la respuesta contenido que ya está en disco.

| Rol | Escenario — qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **descomponedor** (§2.1) | **Escribe las tareas** desde el diseño | `{iniciativa}/design` | `decomposition.md` + `tests-code.md` | `implementation/` con `tasks.md`, `task_NN.md` y `test-e2e-desc.md` |
| **implementador** (§2.2) | **Materializa una tarea** | la ruta de su `task_NN.md` + `design/` | `implementation.md`; `tests-code.md` si es tarea de test | código en el árbol + `DONE`/`CONFLICT`/`BLOCKED`/`DESIGN-ERROR`/`DESIGN-ERRATA` |
| **verificador-build** (§2.3) | **Compila** y reporta | — | `build.md` | `OK-COMPILA`, `BLOCKED: …` o JSONL de errores |
| **corrector-build** (§2.4) | **Corrige** los errores reportados | las líneas JSONL | `build.md` + `implementation.md` | código corregido + `CORREGIDO`/`BLOCKED`/`DESIGN-ERROR`/`DESIGN-ERRATA` |

### 2.1 descomponedor — lee el diseño y escribe las tareas

**Tarea:** leer `design.md` íntegro (y `test-unit-desc.md`, `test-e2e-desc.md`) y escribir `implementation/`: un `task_NN.md` por tarea, el índice `tasks.md` y la copia literal de `test-e2e-desc.md`.

- **Lee de esta plantilla:** `decomposition.md` (y `tests-code.md` para las tareas de test).
- **CRITICAL — orden TDD:** la pareja (tarea de test, tarea de pieza) va **consecutiva y en ese orden** para cada pieza que `test-unit-desc.md` describe. Lo detalla `decomposition.md` §2.
- **MUST NOT** materializar nada en `src/`. **MUST NOT** terminar sin pasar el checklist de `decomposition.md` §7.

### 2.2 implementador — materializa una tarea

**Tarea:** ejecutar **una** `task_NN.md` en el árbol del proyecto según su naturaleza.

- **Lee de esta plantilla:** `implementation.md`; `tests-code.md` **solo si** la tarea es de tests.
- **MUST** cargar con `Skill` los skills de la sección `## Skills a usar` de la tarea **antes** de implementar; delegar el Java/Kotlin en `developer-code-implementer` con el texto de la tarea verbatim.
- **TDD:** la regla de cierre de una tarea de **pieza** con su test en verde la fija `implementation.md` §3.
- **MUST NOT** crear superficie que la tarea no liste; **MUST NOT** editar los XML del diseño ya colocados.

### 2.3 verificador-build — compila y reporta

**Tarea:** ejecutar **tú mismo** el comando de `build.md` §1 con `Bash` y reportar. **MUST NOT** corregir nada.

### 2.4 corrector-build — corrige los errores

**Tarea:** resolver cada línea JSONL del verificador-build tocando solo lo que `build.md` §4 permite.

- **Lee de esta plantilla:** `build.md` (§4) e `implementation.md` (XML contrato fijo, Java delegado).
- Qué hacer con un test en rojo: `build.md` §4 (`tipo: TEST`).

---

## 3. Estructura de entrada y de salida

### 3.1 Entrada — la carpeta `design/`

```
design/
├── design.md             ← cabecera con **Ubicación del código:** y **Carpeta de tests E2E:**; tabla «Ficheros a crear o modificar»; «Piezas»; «Estrategia de verificación»; «Pasos»
├── test-e2e-desc.md      ← se propaga verbatim
├── test-unit-desc.md     ← origen de las tareas de test (TDD)
├── domains/ views/ menus.xml data-init/   ← solo si el diseño los materializó
```

### 3.2 Salida — `implementation/` y el árbol del proyecto

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── implementation/
    ├── tasks.md                 ← índice con checkbox por tarea (type: implementation-tasks)
    ├── task_01.md … task_NN.md  ← una tarea por fichero (type: implementation-task)
    ├── test-e2e-desc.md         ← copia literal de design/test-e2e-desc.md (contrato hacia abajo)
    └── log_*.txt / error_design.log   ← los escribe el MOTOR

src/main/…  src/test/java/…  src/main/resources/…   ← el código, donde diga el design.md
```

---

## 4. Contexto del proyecto a cargar

- Los skills los fija **cada tarea** en su `## Skills a usar` (salen de la columna `Skill` del diseño más `k-secure-coding` y `k-code-quality` para todo Java/Kotlin). Ningún rol carga skills «por defecto».
- `CLAUDE.md` del proyecto y los `CLAUDE.md` de las carpetas que la tarea toca (`base/`, `subsystem/tramitador/`, `subsystem/expedientes/`, `tramites/util/` si aplica).
- `agent_docs/deploy.md` para los comandos de Gradle (compilar, ejecutar un test concreto).
- **MUST NOT** usar como referencia `design.md` de otras iniciativas.
