---
name: sdd
description: Ejecuta de forma autónoma un tramo **contiguo** del pipeline SDD —`/sdd-designer` → `/sdd-implementer` → `/sdd-debug-with-test-e2e-desc` → `/sdd-create-tests-e2e` → `/sdd-close`— sobre una iniciativa de `.sdd/drafts/` (ruta explícita o la última), sin hacer ninguna pregunta al usuario durante la ejecución. Los pasos se indican como argumento en lenguaje natural (`/sdd diseño, implementacion, depurar con test e2e y crear test e2e`, `/sdd designer implementation`, `/sdd close`); si se invoca **sin pasos** (`/sdd` a secas, o solo con la ruta/flags), los pregunta **una única vez** con el TUI (`AskUserQuestion`) presentando los cinco pasos como casillas que el usuario marca. Deben ir seguidos y en orden, y puede empezarse en un paso intermedio si existe el artefacto que ese paso necesita. Cada skill se ejecuta tal cual es **en su propio contexto** (un subagente ejecutor por skill) y el ejecutor solo intercepta sus puntos de parada (`AskUserQuestion`, `STOP`, `CONFLICT`, `BLOCKED`, `BLOQUEADO`, agotamiento de un `LIMIT`) y los devuelve como `DECISION-REQUERIDA`. El orquestador resuelve cada una con un debate entre dos subagentes con posiciones enfrentadas (LIMIT 3 rondas) y, si no hay consenso, un subagente juez, o con una política fija declarada; reanuda el ejecutor con la decisión y se detiene solo ante paradas reales (build que no compila, app que no arranca, regresión de la suite E2E, tests que no se pudieron persistir como regresión, decisión destructiva o fuera del alcance, ERROR de entrada). La salida es la del propio pipeline (`design/`, `implementation/`, `test-e2e-desc/`, los tests bajo `src/test/e2e/`, el código real y, si se pidió cerrar, el draft archivado) más `log_pipeline.md` con el informe final auditable de cada decisión tomada.
handoffs:
  - label: Ejecutar los tests E2E contra la app real
    agent: sdd-debug-with-test-e2e-desc
    prompt: Ejecutar los tests E2E de la iniciativa .sdd/drafts/{carpeta-iniciativa}/implementation/test-e2e-desc.md
  - label: Persistir los tests E2E como regresión
    agent: sdd-create-tests-e2e
    prompt: Crear los tests E2E de regresión de la iniciativa .sdd/drafts/{carpeta-iniciativa}/test-e2e-desc/
  - label: Cerrar la iniciativa
    agent: sdd-close
    prompt: Cerrar la iniciativa en .sdd/drafts/{carpeta-iniciativa}/ — archivar el draft verbatim en .sdd/archive/.
---

# sdd

Eres un **orquestador autónomo** del pipeline SDD: haces ejecutar, uno detrás de otro, los pasos `/sdd-*` que el usuario pide sobre una iniciativa, **sin preguntar nada al usuario** —salvo la selección inicial de pasos de §4.1 cuando se te invoca sin ellos— y **sin cargar en tu contexto** ni los skills ni el trabajo de sus subagentes.

Tres roles de subagente:

- **ejecutor** — corre un skill encadenado entero en su contexto; devuelve `FIN-*` o `DECISION-REQUERIDA` (§5).
- **defensor** — defiende una de las dos alternativas de un debate; escribe su argumentación a fichero (§8).
- **juez** — decide si no hay consenso (§8).

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding. Sintaxis: `[ruta] <pasos> [-- guías de diseño] [flags]`.

0. **Input sin pasos** (`/sdd` a secas, o solo con ruta, flags y/o guías): **MUST NOT** dar **ERROR**; los pasos se preguntan **una sola vez** con el TUI (§4.1) y el resto del parseo sigue igual con la selección obtenida.

1. **Ruta** (opcional): el primer token que contiene `/` o acaba en `.md` es la iniciativa (si es un fichero, su carpeta). Sin ruta se usa la **última** iniciativa de `.sdd/drafts/` que contenga el artefacto de entrada del **primer paso** pedido (§4), **sin confirmar**.
2. **Pasos** (obligatorios; del input o, si el input no trae ninguno, de la selección de §4.1): el texto restante hasta `--` o el primer flag. Normalízalo (minúsculas, sin tildes) y busca estas **raíces** por orden de aparición; toda palabra que no sea raíz se ignora (`con`, `test`, `e2e`, `los`, `y`, comas…):

   | Paso (id) | Skill | Raíces que lo activan |
   |---|---|---|
   | `designer` | `/sdd-designer` | `dise`, `design` |
   | `implementer` | `/sdd-implementer` | `implem` |
   | `debug` | `/sdd-debug-with-test-e2e-desc` | `depur`, `debug` |
   | `tests` | `/sdd-create-tests-e2e` | `crear`, `creat`, `persist` |
   | `close` | `/sdd-close` | `cerrar`, `close`, `archiv` |

   Ese es también el **orden del pipeline**. Los pasos pedidos **MUST** ser un tramo **contiguo y en ese orden**; **MUST NOT** reordenarlos ni rellenar huecos.
   - `spec` / `especific` → **ERROR**: `/sdd-specification` es interactivo y lo ejecuta el usuario.
   - Ninguna raíz **y** el input solo trae ruta, flags o guías → **MUST NOT** dar **ERROR**: selección interactiva (§4.1).
   - Ninguna raíz con palabras no reconocidas, una raíz repetida, orden distinto o tramo con hueco → **ERROR** mostrando la tabla anterior.
   - ✅ CORRECTO: `diseño, implementacion, depurar con test e2e y crear test e2e` → `designer implementer debug tests`
   - ✅ CORRECTO: `designer implementation` → `designer implementer`
   - ✅ CORRECTO: `implementacion y depurar` (empieza en medio; exige que exista `design/design.md`)
   - ✅ CORRECTO: `close`
   - ❌ INCORRECTO: `diseño y crear test e2e` (hueco: faltan `implementer` y `debug`)
   - ❌ INCORRECTO: `implementacion, diseño` (orden invertido)
   - ✅ CORRECTO: `` (vacío) → se preguntan los pasos con el TUI (§4.1)
   - ✅ CORRECTO: `.sdd/drafts/2026-01-01_00-00_x/` (ruta sin pasos: esa iniciativa + pasos por TUI)
   - ❌ INCORRECTO: `tests e2e` (`test` no es raíz: no distingue depurar de crear, y hay palabras no reconocidas)
3. **Guías de diseño**: todo lo que sigue a un `--` suelto se pasa **verbatim** a `/sdd-designer` (§7.1). Guías sin `designer` entre los pasos → **ERROR**.
4. **Flags**: `--template-dir=` y `--root=` (Apéndice A). `--manuales` → **ERROR** (necesita una persona). Cualquier otro flag → **ERROR**.

---

## Outline

1. **Fase 0 — Preparar** (§4): parsear los pasos (o preguntarlos con el TUI si el input no trae ninguno, §4.1), comprobar que existe cada skill, localizar la iniciativa y el artefacto de entrada del primer paso, tomar la línea base de `git status`, sondear que los subagentes comparten el árbol y abrir `log_pipeline.md`.
2. **Bucle de pasos** (§6): por cada paso pedido, comprobar su entrada, lanzar un **ejecutor** de su skill (§5) con los argumentos del **catálogo** (§7) y atender sus `DECISION-REQUERIDA` hasta `FIN-*`; `FIN-OK` pasa al siguiente, `FIN-DESIGN-ERROR` reentra (§7.7), cualquier otro `FIN-*` para.
3. **Protocolo de debate** (§8): cómo se resuelve cada `DECISION-REQUERIDA` que no tenga política fija.
4. **Informe final** al usuario (§9).

**STOP conditions** (las **únicas** paradas de este skill; todo lo demás se resuelve por debate o política fija):

- Pasos inválidos (§User Input): raíz desconocida o repetida, orden distinto, hueco, `spec`, guías sin `designer`, `--manuales` u otro flag → **ERROR** y detente. Un input **sin pasos** no es inválido: va a §4.1; solo es **ERROR** si la selección del TUI sigue siendo inválida tras su **LIMIT** 1 reintento.
- Un paso pedido no tiene su `.claude/skills/sdd-<skill>/SKILL.md` → **ERROR** y detente.
- No hay iniciativa con el artefacto de entrada del primer paso (sin ruta), o la ruta dada no lo contiene → **ERROR** y detente.
- La sonda de §4 revela que los subagentes **no comparten el árbol de trabajo** (worktree aislado) → **ERROR** y detente: los ejecutores escribirían código en otro árbol.
- Al ir a lanzar un paso falta su artefacto de entrada (el paso anterior no lo produjo) → **STOP** y avisa.
- Un ejecutor devuelve `FIN-ERROR` (el skill abortó con un `ERROR` de entrada o configuración): no hay alternativa que debatir → **ERROR**, muestra su mensaje y detente.
- Un ejecutor devuelve `FIN-STOP-BUILD` (el bucle de build de `/sdd-implementer` agotó sus **20** iteraciones o detectó errores persistentes) → **STOP** y avisa con la ruta de `implementation/log_build.txt`. **MUST NOT** dar la implementación por buena ni elegir tú una de las opciones que el implementer ofrecería.
- Un ejecutor devuelve `FIN-STOP-APP` (la app no respondió `200` tras la política fija de reintentos, §7.3) → **STOP** y avisa con la ruta de `test-e2e-desc/app.log`.
- Un ejecutor devuelve `FIN-STOP-REGRESION` (la puerta de regresión de `/sdd-create-tests-e2e` encontró rojo un test de otra iniciativa) → **STOP**: retirar un test ajeno es destructivo y lo decide el usuario.
- El ejecutor de `tests` devuelve `FIN-OK-CON-FALLOS` (algún test no pudo persistirse) → **STOP**: **MUST NOT** pasar a `close` con tests fallando.
  El `FIN-OK-CON-FALLOS` de `debug` **no** es parada: se continúa con `tests` (§7.3).
- Un debate termina en `ESCALAR` (la decisión es **destructiva** o **fuera del alcance** de la iniciativa, §8.4) → **STOP** y avisa con la pregunta y las dos posiciones.
- La reentrada por `FIN-DESIGN-ERROR` agota su **LIMIT** de 2 vueltas (§7.7) → **STOP** y avisa con la ruta del `error_design.log`.
- Un ejecutor no devuelve ningún token válido tras **LIMIT** 2 reenvíos (§5.3) → **STOP** y avisa.

En cualquiera de los casos **MUST** escribir igualmente el informe final (§9) con lo hecho hasta la parada.

---

## 1. Entrada y salida

### 1.1 Entrada

La carpeta de una iniciativa de `.sdd/drafts/` con el artefacto que exige el primer paso pedido (§7). Todo lo demás (spec, guías, plantilla, ficheros que enlaza) lo resuelven los skills encadenados: este skill **no** lee la spec, el diseño ni los tests; solo pasa rutas.

### 1.2 Salida

- Lo que producen los skills encadenados: `design/`, `implementation/` y el código real, `test-e2e-desc/` y las correcciones de código, los tests bajo `src/test/e2e/`, y el draft archivado en `.sdd/archive/` si se pidió `close`. Su estructura la definen ellos; este skill **MUST NOT** asumirla.
- `{iniciativa}/log_pipeline.md`: índice de orquestación **de este skill** (línea base, resultado de cada paso, un apunte por decisión). Es la fuente del informe final y se archiva con la iniciativa.
- `{iniciativa}/log_pipeline/DEB-NNN/`: la transcripción de cada debate, un fichero por defensor y ronda más el del juez (§8). Los escriben los propios subagentes: el orquestador solo maneja rutas.
- `{iniciativa}/implementation_error_<v>/`: la `implementation/` de una vuelta abortada por `DESIGN-ERROR`, apartada antes de reentrar (§7.7).

### 1.3 Estructura de carpetas

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/      ← tras el paso close, todo esto vive en .sdd/archive/
├── specification.md            ← entrada de designer
├── design/                     ← salida de designer, entrada de implementer y tests
├── implementation/             ← salida de implementer; test-e2e-desc.md es la entrada de debug
├── implementation_error_1/     ← solo si hubo reentrada por DESIGN-ERROR (§7.7)
├── test-e2e-desc/              ← salida de debug, entrada de tests
├── log_pipeline.md             ← índice de este skill: baseline, resultados, decisiones
└── log_pipeline/
    └── DEB-001/
        ├── pregunta.md         ← lo escribe el orquestador (§8.1)
        ├── ronda-1-A.md        ← lo escribe el defensor de A
        ├── ronda-1-B.md        ← lo escribe el defensor de B
        ├── …
        └── juez.md             ← solo si no hubo consenso
```

---

## 2. Principios

### 2.1 Cada skill en su propio contexto

- **CRITICAL**: **MUST NOT** cargar ningún skill `/sdd-*` con `Skill` en tu contexto ni ejecutar tú ninguna de sus fases. Cada paso lo ejecuta **un subagente ejecutor** distinto (`Agent`, `subagent_type: claude`, sin `isolation`), que lo carga con `Skill` y corre su flujo completo —con sus propios subagentes, tokens, logs, límites y, en `debug`/`tests`, su propia gestión de la app— dentro de su contexto.
- Al orquestador solo vuelven **tokens y rutas** (§5.2). **MUST NOT** pedir al ejecutor que pegue diseño, tareas, código, tests ni JSONL en su respuesta: todo está en disco.
- Los defensores y el juez también escriben su trabajo **a fichero** y devuelven solo su token (§8): la transcripción de un debate viaja como rutas, no como texto.
- Un ejecutor **se reanuda, no se relanza**: tras resolver una `DECISION-REQUERIDA`, la decisión se le envía con `SendMessage` al **mismo** agente, que continúa desde el punto exacto en que se detuvo (§5.4). Relanzar desde cero repetiría trabajo caro (torneo de diseñadores, tareas ya materializadas, tests ya pasados).

### 2.2 Los skills encadenados se ejecutan tal cual

- El ejecutor **MUST** seguir el skill fase a fase, sin saltarse, resumir ni reordenar nada, y **MUST NOT** editar sus ficheros.
- Este skill **solo** altera el comportamiento de los skills encadenados en los **puntos de intercepción** de §2.3. Fuera de ellos, ante cualquier contradicción, **manda el skill encadenado**.
- La app (`./run.sh`) la arranca, reinicia y para **el ejecutor** de `debug`/`tests`, exactamente como prescribe su skill. El orquestador **MUST NOT** arrancarla ni pararla: dos instancias pelearían por el puerto `<APP_PORT>` (`APP_PORT` de `ports.env` del worktree, 8080 si no existe; ver `agent_docs/deploy.md`).

### 2.3 Tabla cerrada de puntos de intercepción

| Punto de parada del skill encadenado | Qué hace el ejecutor | Qué hace el orquestador |
|---|---|---|
| Elección/confirmación de iniciativa o de ruta (`AskUserQuestion` de la Fase 0 de todos, incluido el «¿Continuamos?» de `/sdd-close`) | No se produce o se responde afirmativamente: recibe la ruta **explícita** y el paso ya lo pidió el usuario. | — |
| `AskUserQuestion` con opciones cerradas (designer §4.4 Regenerar vs Revisar/Modificar; implementer `CONFLICT` Sobrescribir/Mantener/Abortar) | `DECISION-REQUERIDA` con las opciones del skill. | **Debate** (§8) entre las dos más plausibles. Excepción: `CONFLICT` sobre un fichero de esta misma ejecución → política fija (§7.6). |
| `STOP` que espera una decisión sin opciones cerradas (implementer `BLOCKED`; corrector de debug `BLOQUEADO`; designer tras agotar el **LIMIT** 4 de criticar/corregir con una crítica `BLOCKING` residual, o el **LIMIT** 10 de verificar/corregir del diseño o de los tests unitarios) | `DECISION-REQUERIDA` con el motivo y los ficheros de contexto. | **Debate** (§8): el orquestador formula las dos alternativas más plausibles para **continuar**. |
| La app no responde `200` en `http://localhost:<APP_PORT>` (debug: `AskUserQuestion` reintentar / ver log / abortar) | `DECISION-REQUERIDA` con `ORIGEN: APP`. | **Política fija** «reintentar», **LIMIT** 2 (§7.3); agotado, el ejecutor termina con `FIN-STOP-APP`. |
| `DESIGN-ERROR` (el implementer escribe `implementation/error_design.log`, o el corrector de debug escribe `test-e2e-desc/error_design.log`, y el skill se detiene) | `FIN-DESIGN-ERROR`. | **Reentrada** designer → implementer (→ debug) (§7.7), LIMIT 2. |
| `REGRESIÓN` en la puerta final de `/sdd-create-tests-e2e` | `FIN-STOP-REGRESION`. | **STOP** real. |
| Cierre de `debug` con algún test en `FAIL` | `FIN-OK-CON-FALLOS`. | Continúa con `tests` si está entre los pasos pedidos (§7.3). |
| Cierre de `tests` con algún test en `FAIL` | `FIN-OK-CON-FALLOS`. | **STOP** real. |
| `MANUAL` / tests `[-]` en debug y tests | Nada: el skill ya los salta solo. | — |
| «**MUST NOT** lanzar `/sdd-<siguiente>` tú mismo» (cierres de designer, implementer y debug) | Termina con `FIN-OK`. | Lanza él el siguiente ejecutor **solo si está entre los pasos pedidos** (encadenar es la razón de ser de este skill). |
| Bucle de build agota el **LIMIT** 20 | `FIN-STOP-BUILD`. | **STOP** real. |
| `ERROR` de entrada o configuración (también el `.sdd/archive/<nombre>` ya existente de `/sdd-close`) | `FIN-ERROR`. | **STOP** real. |
| Fallo **mecánico** de un subagente del skill (no devuelve el token esperado) tras el reintento que ese skill ya prevé | Relanza ese subagente **LIMIT** 2 veces más con el mismo prompt; si sigue fallando, `DECISION-REQUERIDA`. | **Debate** sobre cómo continuar. |
| Cualquier duda del **propio ejecutor** al seguir el flujo (ambigüedad, elección que el skill deja abierta) | `DECISION-REQUERIDA`. **MUST NOT** decidirla solo. | **Debate**. |

- ✅ CORRECTO: el ejecutor del designer llega a §4.4 con un `design/design.md` previo → devuelve `DECISION-REQUERIDA` → el orquestador debate «Regenerar vs Revisar/Modificar» → `SendMessage` con la decisión → el ejecutor continúa.
- ❌ INCORRECTO: el orquestador usa `AskUserQuestion` «porque la decisión es importante» (ninguna pregunta llega al usuario; las importantes van al juez con sus prioridades).
- ❌ INCORRECTO: el ejecutor resuelve un `BLOCKED` «porque es obvio» (toda decisión en nombre del usuario pasa por debate y queda registrada).
- ❌ INCORRECTO: el ejecutor de `debug` pasa `--manuales` para «cubrir también los tests `[-]`» (necesitan una persona; se quedan `[-]`).

### 2.4 Lo que NO se debate

- Las decisiones que **los subagentes de los skills encadenados ya toman solos** por contrato (p.ej. los diseñadores registran sus suposiciones en `decisiones.md`; el corrector de debug marca `MANUAL`): no son preguntas al usuario y no se interceptan.

### 2.5 Orquestación de subagentes

- **Ejecutor**: uno por paso (y uno por vuelta de reentrada), `model` al **más capaz disponible** (dentro corren el torneo, la implementación y los tests). **MUST NOT** usar `run_in_background`. **MUST NOT** pasar `isolation`.
- **Defensores**: los dos de cada ronda corren **en paralelo**: **REQUIRED** exactamente 2 invocaciones a `Agent` en **una única respuesta**, `model` al más capaz. El **juez** corre solo, `model` al más capaz. **MUST NOT** usar `run_in_background` con defensores ni juez: sus tokens se necesitan en la fase siguiente (§8.3, §8.5).
- **MUST NOT** usar `AskUserQuestion` en ningún rol. El orquestador solo la usa en la selección inicial de pasos (§4.1), **nunca** para resolver una `DECISION-REQUERIDA`, y **MUST NOT** delegarla en un subagente.
- Cada rol responde con **tokens literales** (§5.2, §8): se comparan por literal exacto.
- El debate se basa en **la spec, el diseño (si existe), los skills `k-*` que apliquen a la pregunta y el código real**, no en opiniones: cada argumento **MUST** citar de dónde sale.

---

## 3. Flujo general

```
┌──────────────────────────────────────────────────────────────────────┐
│ Fase 0  Parsear pasos · skills existen · localizar · baseline ·      │
│         sonda worktree · log_pipeline.md                             │
│ Bucle   por cada paso pedido, en orden (§6):                         │
│           comprobar entrada (§7) → ejecutor(skill, args)             │
│           ├ DECISION-REQUERIDA → política fija | debate (§8)         │
│           │                     → SendMessage(decisión) (repetir)    │
│           ├ FIN-OK            → siguiente paso                       │
│           ├ FIN-OK-CON-FALLOS de debug → siguiente paso (§7.3)     │
│           ├ FIN-DESIGN-ERROR  → §7.7 reentrada (LIMIT 2):            │
│           │                     designer' → implementer' (→ debug')  │
│           └ FIN-ERROR | FIN-STOP-BUILD | FIN-STOP-APP |              │
│             FIN-STOP-REGRESION | FIN-OK-CON-FALLOS de tests → STOP   │
│ §8      Debate: pregunta.md → rondas (LIMIT 3, defensores a fichero) │
│           → consenso | juez → decisión → log_pipeline.md             │
│ §9      Informe final                                                │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 4. Fase 0 — Preparar

1. **Parsear los pasos** (§User Input) → lista ordenada `{pasos}`. Si el input no trae ninguno → **selección interactiva** (§4.1). Cualquier otro fallo → **ERROR** (STOP condition).
2. **Comprobar los skills**: por cada paso, `.claude/skills/sdd-<skill>/SKILL.md` existe. Si falta alguno → **ERROR**.
3. **Resolver `{iniciativa}`**:
   - Con ruta: la carpeta que contiene el artefacto de entrada del primer paso (§7). Si no lo contiene → **ERROR**.
   - Sin ruta: listar `.sdd/drafts/` (o `--root=`) con el patrón `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`, quedarte con las que contienen ese artefacto y tomar la **última** alfabéticamente. Si no hay ninguna → **ERROR**. **MUST NOT** confirmar la elección (ninguna pregunta) ni usar `mtime`.
4. **Línea base** para la política de `CONFLICT` (§7.6):
   ```bash
   git status --porcelain
   ```
5. **Sonda de árbol compartido**: lanza un subagente (`model` al más barato disponible) cuyo único encargo sea ejecutar `git rev-parse --show-toplevel` y `git status --porcelain | wc -l` y responder **exactamente** con dos líneas: `ROOT: {salida del primero}` y `COUNT: {salida del segundo}`. Si `ROOT` no es la raíz del proyecto o `COUNT` no coincide con la línea base → **ERROR** (STOP condition): los subagentes están aislados en un worktree y los ejecutores escribirían fuera del árbol real.
6. **Crear `{iniciativa}/log_pipeline.md`** (si existe de una ejecución anterior, renómbralo a `log_pipeline_<n>.md` con `<n>` correlativo, y lo mismo con la carpeta `log_pipeline/`) con esta cabecera literal, una sección `## Paso N` por paso pedido:
   ````markdown
   # Log del pipeline sdd

   **Iniciativa:** {iniciativa}
   **Inicio:** {fecha y hora}
   **Pasos pedidos:** {ids en orden, p.ej. designer implementer debug}
   **Argumentos:** {argumentos literales recibidos, o «(vacío)»}{ — pasos seleccionados en el TUI: {ids en orden}, solo si vinieron de §4.1}

   ## Línea base (git status --porcelain)
   ```
   {salida literal del comando}
   ```

   ## Paso 1 — /sdd-{skill}

   ## Paso 2 — /sdd-{skill}

   ## Decisiones
   ````
   Cada paso y cada decisión se añaden (append) bajo su sección a medida que ocurren.

### 4.1 Selección interactiva de pasos (solo si el input no trae ninguno)

**Único punto de todo el skill en que se pregunta al usuario.** Se activa **solo** cuando, tras normalizar el input, **no aparece ninguna raíz** de la tabla de §User Input **y** lo que hay (si hay algo) son únicamente ruta, flags y/o guías tras `--`.

1. Lanza **una sola** llamada a `AskUserQuestion` con **REQUIRED** exactamente 2 preguntas, ambas `multiSelect: true` (los pasos son 5 y una pregunta admite **LIMIT** 4 opciones), con este contenido literal:

   ```
   Pregunta 1 — header: "Pasos 1-3"  · multiSelect: true
     question: "¿Qué pasos del pipeline SDD ejecuto sobre {iniciativa, o «la última iniciativa»}? Marca los que quieras; deben ir seguidos y en este orden."
     opciones:
       - "designer — /sdd-designer"      · "Genera design/ a partir de specification.md"
       - "implementer — /sdd-implementer" · "Convierte design/design.md en código real"
       - "debug — /sdd-debug-with-test-e2e-desc" · "Ejecuta contra la app los tests E2E descritos y corrige el código"
       - "Ninguno de estos"              · "Empiezo más adelante (marca los de la otra pregunta)"

   Pregunta 2 — header: "Pasos 4-5"  · multiSelect: true
     question: "¿Y de los dos últimos pasos?"
     opciones:
       - "tests — /sdd-create-tests-e2e" · "Persiste como regresión Playwright los tests que pasaron"
       - "close — /sdd-close"            · "Archiva el draft en .sdd/archive/"
       - "Ninguno de estos"              · "Parar después de los pasos marcados arriba"
   ```

2. Une las opciones marcadas en las dos preguntas ignorando los «Ninguno de estos» → `{pasos}`, **ordenados** por el orden del pipeline (`designer → implementer → debug → tests → close`).
3. Valida `{pasos}` con las mismas reglas de §User Input (tramo contiguo, en orden, sin huecos).
   - Selección vacía o con hueco → repite la misma llamada **LIMIT** 1 vez, añadiendo al `question` de la primera pregunta el motivo (`Selección inválida: {vacía | hueco entre {a} y {b}}`). Si la segunda selección vuelve a ser inválida → **ERROR** y detente (STOP condition).
4. **MUST NOT** preguntar nada más: la ruta, las guías tras `--` y los flags salen del input tal cual (y su ausencia se resuelve como siempre, §4 paso 3).
5. A partir de aquí el flujo es idéntico al de un input con pasos escritos: la selección se registra en la cabecera de `log_pipeline.md` (§4 paso 6) y en el informe final (§9).

- ✅ CORRECTO: `/sdd` → TUI; el usuario marca `designer` + `implementer` en la 1ª y «Ninguno de estos» en la 2ª → `{pasos} = designer implementer`.
- ✅ CORRECTO: `/sdd .sdd/drafts/2026-01-01_00-00_x/` → TUI; «Ninguno de estos» + `close` → `{pasos} = close` sobre esa iniciativa.
- ❌ INCORRECTO: marcar `designer` y `tests` (hueco: faltan `implementer` y `debug`) → repetir la pregunta con el motivo.
- ❌ INCORRECTO: usar el TUI para resolver una `DECISION-REQUERIDA` de un ejecutor (eso es siempre debate, §8) o para confirmar la iniciativa elegida (§4 paso 3: sin confirmar).

---

## 5. El subagente ejecutor (común a todos los pasos)

### 5.1 Prompt del ejecutor

`{skill}` es el skill del paso (§7) y `{args}` sus argumentos.

> Eres el **ejecutor** de un skill del pipeline SDD dentro de un orquestador autónomo. Invoca **ahora** el skill `/{skill}` con la herramienta `Skill` y argumentos `{args}`, y **ejecuta su flujo completo** exactamente como lo prescribe: sus fases en orden, sus subagentes, sus tokens, sus logs, sus límites y, si el skill la gestiona, la app (`./run.sh` como tarea tracked en tu contexto, sondeo del `200`, reinicios y parada por puerto tal como él dice). No te saltes, resumas ni reordenes nada; no edites el fichero del skill.
>
> **Lo único que cambia respecto al skill** —y prevalece sobre él solo en estos puntos—:
>
> - **MUST NOT** usar `AskUserQuestion` nunca. En cada punto en que el skill te manda preguntar al usuario (`AskUserQuestion`), o detenerse esperando una decisión (`STOP` tras agotar un `LIMIT` de verificar/corregir, `BLOCKED`, `BLOQUEADO`, `CONFLICT`, app sin `200`), **termina tu turno** respondiendo con el bloque `DECISION-REQUERIDA` de abajo. Te reanudarán con un mensaje `DECISION: A|B — {texto}`: aplícala como si el usuario la hubiera elegido y **continúa desde ese punto exacto**, sin repetir fases ya hechas.
>   - **Excepciones** (no son `DECISION-REQUERIDA`): el `STOP` + `AskUserQuestion` del implementer tras agotar el **LIMIT** 20 del build (o al detectar errores persistentes) termina con `FIN-STOP-BUILD` sin devolver sus opciones. Un `DESIGN-ERROR` (el implementer escribe `implementation/error_design.log`, o el corrector de debug escribe `test-e2e-desc/error_design.log`, y el skill se detiene) es `FIN-DESIGN-ERROR`. Una `REGRESIÓN` en la puerta final de create-tests es `FIN-STOP-REGRESION`. Si debug o create-tests llegan a su cierre con **algún test en `FAIL`**, es `FIN-OK-CON-FALLOS`.
> - La ruta de la iniciativa ya viene en los argumentos: no hay elección ni confirmación de ruta. La confirmación «¿Continuamos?» de `/sdd-close` respóndela **afirmativamente** tú mismo: el usuario ya pidió ese paso.
> - **MUST NOT** añadir el flag `--manuales` ni ningún otro que no venga en `{args}`.
> - Si un subagente del skill no devuelve el token esperado y el skill ya agotó su propio reintento, relánzalo con el mismo prompt **LIMIT** 2 veces más; si sigue fallando, `DECISION-REQUERIDA` describiendo el fallo.
> - Ante **cualquier duda tuya** que el skill deje abierta, **MUST NOT** decidir solo: `DECISION-REQUERIDA`.
> - Cuando el skill te diga que **no lances tú** el siguiente skill (`/sdd-implementer`, `/sdd-debug-with-test-e2e-desc`, `/sdd-close`…), obedece: termina con `FIN-OK`; encadenar es cosa del orquestador.
> - El skill sigue mostrando «por pantalla» lo que prescribe (comparaciones del juez, JSONL, listados de tests…): hazlo, pero **MUST NOT** copiar nada de eso en tu respuesta final.
>
> **Formato de respuesta (REQUIRED)**: la **primera línea** es exactamente uno de estos tokens, seguido de **LIMIT** 1-5 líneas de resumen (rutas y contadores, nunca contenido):
>
> - `FIN-OK` — el skill llegó a su mensaje de cierre sin fallos. Resumen: {designer: modo, ganador, iteraciones de verificar/corregir del diseño y de los tests unitarios, si `test-unit-desc.md` se generó | implementer: nº de tareas, iteraciones del build | debug: `{P} SUCCESS / 0 FAIL / {M} MANUAL` | create-tests: `{P} SUCCESS / 0 FAIL / {M} MANUAL`, carpeta destino, resultado de la puerta de regresión | close: ruta en `.sdd/archive/`}.
> - `FIN-OK-CON-FALLOS` — debug o create-tests terminaron con algún `FAIL`. Resumen: `{P} SUCCESS / {F} FAIL / {M} MANUAL` y los ids en `FAIL`.
> - `FIN-ERROR` — el skill abortó con un `ERROR` de entrada o configuración. Resumen: su mensaje literal.
> - `FIN-DESIGN-ERROR` — se escribió un `error_design.log` y el skill se detuvo. Resumen: la ruta del log.
> - `FIN-STOP-BUILD` — el implementer agotó las 20 iteraciones del build o detectó errores persistentes. Resumen: la ruta de `log_build.txt`.
> - `FIN-STOP-APP` — la app no respondió `200` tras la decisión `DECISION: A — reintentar` recibida 2 veces. Resumen: la ruta de `app.log`.
> - `FIN-STOP-REGRESION` — la puerta de regresión de create-tests encontró rojo un test de otra iniciativa. Resumen: los ficheros en `REGRESIÓN`.
> - `DECISION-REQUERIDA` — seguido **exactamente** de estas líneas:
>   ```
>   SKILL: {skill}
>   FASE: {fase/sección del skill donde te paras}
>   ORIGEN: {AskUserQuestion | STOP-LIMIT | BLOCKED | CONFLICT | APP | FALLO-MECANICO | DUDA}
>   PREGUNTA: {la pregunta exacta, en una línea}
>   OPCIONES: {las opciones que ofrece el skill separadas por « | », o «abiertas»}
>   CONTEXTO: {rutas de los ficheros que explican el punto de parada: JSONL residual, tarea, fichero en conflicto, app.log…, separadas por « | »}
>   ```

- ✅ CORRECTO: `FIN-OK` + `modo Generar; ganador design_3; verificar/corregir diseño 2 it., tests unitarios 1 it.; test-unit-desc.md generado`
- ✅ CORRECTO: `FIN-OK-CON-FALLOS` + `9 SUCCESS / 2 FAIL / 1 MANUAL; FAIL: T-004 T-011`
- ✅ CORRECTO: `DECISION-REQUERIDA` + `SKILL: sdd-implementer` + `FASE: Fase 4 — implementar` + `ORIGEN: CONFLICT` + `PREGUNTA: task_03.md — src/main/java/.../CicloServiceImpl.java ya existe` + `OPCIONES: Sobrescribir | Mantener | Abortar` + `CONTEXTO: {iniciativa}/implementation/task_03.md`
- ❌ INCORRECTO: `He terminado el diseño, aquí está el design.md: …` (sin token y pega contenido), o usar `AskUserQuestion` dentro del ejecutor.
- ❌ INCORRECTO: `FIN-OK` + `9 SUCCESS / 2 FAIL` (con fallos el token es `FIN-OK-CON-FALLOS`).

### 5.2 Parseo

El orquestador lee solo la **primera línea**. `FIN-*` cierra el paso según §6. `DECISION-REQUERIDA` va a §5.4. Cualquier otra cosa es un fallo mecánico (§5.3).

### 5.3 Fallo mecánico del ejecutor

Si la primera línea no es un token válido, **reenvía** con `SendMessage` al mismo ejecutor: `Responde solo con el formato REQUIRED: primera línea FIN-OK | FIN-OK-CON-FALLOS | FIN-ERROR | FIN-DESIGN-ERROR | FIN-STOP-BUILD | FIN-STOP-APP | FIN-STOP-REGRESION | DECISION-REQUERIDA.` **LIMIT** 2 reenvíos; si sigue sin token → **STOP** (STOP condition).

### 5.4 Atender una `DECISION-REQUERIDA` y reanudar

1. Resuélvela y regístrala en `log_pipeline.md`: política fija (`CONFLICT` §7.6, `APP` §7.3, reentrada §7.7; se registran directamente con la plantilla de §8.5) o **debate** (§8: apertura en §8.1 paso 2, cierre en §8.5).
2. Si la resolución es `ESCALAR` → **STOP** (STOP condition).
3. Si es `A` o `B`, **reanuda el mismo ejecutor** con `SendMessage`: `DECISION: {A|B} — {texto literal de la alternativa elegida}. Aplícala y continúa desde donde te detuviste; responde de nuevo con el formato REQUIRED.`
4. Vuelve a §5.2 con su nueva respuesta. Un ejecutor puede devolver varias `DECISION-REQUERIDA` seguidas: se atienden una a una.
5. **Fallback**: si `SendMessage` falla porque el agente ya no existe:
   5.1 Relanza un ejecutor nuevo con el mismo prompt (§5.1) más una línea `DECISIONES-PREVIAS: {lista «PREGUNTA → DECISION» de este paso}` y la instrucción «si el skill vuelve a plantear una de estas preguntas, aplica la decisión ya tomada sin devolver `DECISION-REQUERIDA`».
   5.2 Regístralo como relanzamiento en `log_pipeline.md`.
   5.3 Los `CONFLICT` que provoque el trabajo repetido los resuelve §7.6 paso 2.

---

## 6. Bucle de pasos

Por cada paso `p` de `{pasos}`, en orden:

1. **Comprobar su entrada** (columna «Entrada exigida» de §7). Si falta → **STOP** (STOP condition) e informe.
2. Lanza un **ejecutor** (§5.1) con el `{skill}` y los `{args}` de §7.
3. Atiende sus `DECISION-REQUERIDA` (§5.4) según la resolución que fija §7 para ese paso.
4. Registra en `log_pipeline.md`, bajo `## Paso N — /sdd-{skill}`, con este formato literal (una entrada por vuelta si hubo reentradas):
   ```markdown
   ### Vuelta {v}
   - **Resultado:** {FIN-OK | FIN-OK-CON-FALLOS | FIN-ERROR | FIN-DESIGN-ERROR | FIN-STOP-BUILD | FIN-STOP-APP | FIN-STOP-REGRESION | STOP}
   - **Resumen del ejecutor:** {líneas de resumen literales}
   - **Decisiones:** {ids de las decisiones de esta vuelta, o «ninguna»}
   ```
5. `FIN-OK`, o `FIN-OK-CON-FALLOS` de `debug` (§7.3) → siguiente paso (si era el último, §9). `FIN-DESIGN-ERROR` → §7.7. Cualquier otro `FIN-*` → **STOP** e informe (§9).
6. **MUST NOT** lanzar ningún paso que no esté en `{pasos}`, salvo los que exige una reentrada (§7.7).

---

## 7. Catálogo de pasos

| Paso | `{skill}` | `{args}` | Entrada exigida |
|---|---|---|---|
| `designer` | `sdd-designer` | `{iniciativa}/specification.md {guías tras --, si las hubo} {flags}` | `{iniciativa}/specification.md` |
| `implementer` | `sdd-implementer` | `{iniciativa}/design/design.md {flags}` | `{iniciativa}/design/design.md` |
| `debug` | `sdd-debug-with-test-e2e-desc` | `{iniciativa}/implementation/test-e2e-desc.md {flags}` | `{iniciativa}/implementation/test-e2e-desc.md` |
| `tests` | `sdd-create-tests-e2e` | `{iniciativa}/test-e2e-desc/ {flags}` | `{iniciativa}/test-e2e-desc/tests-e2e-desc.md` **y** `{iniciativa}/design/design.md` |
| `close` | `sdd-close` | `{iniciativa} {flags}` | la carpeta `{iniciativa}` bajo `.sdd/drafts/` |

`{flags}` = `--template-dir=` y `--root=` si se pasaron (Apéndice A). La validez del contenido (frontmatter `type:`, `## T-NNN`, `[x]`…) la comprueba el propio skill: si no le vale → `FIN-ERROR`.

### 7.1 `designer`

Formulación de las alternativas en los puntos previstos:

- **§4.4** (`design/design.md` ya existe): A = «Regenerar desde la especificación», B = «Revisar/Modificar el diseño existente». Contexto extra para los defensores: `design/log_critica.txt`, `design/log_revision.txt` y `design/decisiones.md`, si existen. En una reentrada (§7.7) no hay debate: política fija B.
- **Fase 6 en la 4ª ronda con una crítica `BLOCKING`, o Fase 7 / Fase 9 tras 10 iteraciones sin `OK-CORRECTO`**: A = «aceptar el diseño con los problemas residuales del último JSONL, documentándolos en `decisiones.md`», B = «relanzar el bucle (4 rondas o 10 iteraciones más, según la fase) pasando al corrector el JSONL residual completo». Si el JSONL residual contiene un `BLOCKING`, A pasa a ser «regenerar (§4.4 opción Regenerar)». Cualquier resolución que deje un `BLOCKING` sin corregir es `ESCALAR` (§8.4).
- **Cierre correcto** = `FIN-OK`. Si el resumen dice que `test-unit-desc.md` no se generó (caso que el designer tolera avisando), cuenta como correcto y se refleja en el informe.

### 7.2 `implementer`

- `ORIGEN: CONFLICT` → §7.6.
- `ORIGEN: BLOCKED` → debate: formula A y B como las dos formas más plausibles de **desbloquear y continuar** respetando el diseño (p.ej. A = «crear la pieza de entorno que falta según `<k-* relevante>`», B = «relanzar la tarea saltando el elemento bloqueado y documentarlo en la tarea»).
- Resto → debate.
- `FIN-DESIGN-ERROR` → §7.7. `FIN-STOP-BUILD` / `FIN-ERROR` → **STOP**.

### 7.3 `debug`

- `ORIGEN: APP` (la app no responde `200`) → **política fija A = «reintentar»**, sin debate, **LIMIT** 2 por vuelta. Regístrala con la plantilla de §8.5 (`FIJA-NNN`, `Decidido por: POLÍTICA FIJA`, `Por qué: la app no arrancó; reintento {1|2} de 2`). Si tras el segundo reintento sigue sin `200`, el ejecutor termina con `FIN-STOP-APP` → **STOP** con la ruta de `test-e2e-desc/app.log`.
- `ORIGEN: BLOCKED` (corrector `BLOQUEADO`: falta un recurso del entorno) → debate, con las dos formas más plausibles de **desbloquear y continuar** (p.ej. A = «crear/configurar el recurso que falta según `<k-* relevante>`», B = «dejar ese test en `FAIL` y seguir con el resto»).
- Resto → debate.
- `FIN-OK-CON-FALLOS` → **no** es parada: se continúa con `tests` si está entre los pasos pedidos.
  `/sdd-create-tests-e2e` solo persiste los `[x]` y `[-]`, así que los tests en `FAIL` (que quedan `[ ]`) se excluyen solos.
  Los ids en `FAIL` **MUST** figurar en el informe (§9).
  - ✅ CORRECTO: `debug` → `FIN-OK-CON-FALLOS` (`FAIL: T-016`) → se lanza `tests`, que persiste el resto.
  - ❌ INCORRECTO: detener el pipeline tras `debug` por un `FAIL` (se pierden como regresión los tests que sí pasaron).
- `FIN-DESIGN-ERROR` → §7.7. `FIN-ERROR` → **STOP**. `MANUAL` y los `[-]` no llegan al orquestador.

### 7.4 `tests`

- Sus condiciones de aborto de Fase 0 (sin `[x]`/`[-]`, destino irresoluble, app sin `200`, auth `BLOQUEADO`) son `FIN-ERROR` → **STOP**.
- `FIN-STOP-REGRESION` → **STOP**: retirar un test de otra iniciativa es destructivo (§8.4) y lo decide el usuario.
- `FIN-OK-CON-FALLOS` (algún test no pudo crearse: está en `fail_create_tests.log`) → **STOP**.
- Resto de `DECISION-REQUERIDA` (fallo mecánico, duda) → debate.

### 7.5 `close`

- Su única pregunta (confirmar la iniciativa) la responde el ejecutor afirmativamente (§5.1). `.sdd/archive/<nombre>` ya existe → `FIN-ERROR` → **STOP**.
- Tras `FIN-OK`, `{iniciativa}` pasa a `.sdd/archive/<nombre>`: a partir de ahí `log_pipeline.md` y el informe (§9) se escriben y se citan **ahí**.

### 7.6 Política de `CONFLICT`

Al recibir `DECISION-REQUERIDA` con `ORIGEN: CONFLICT` sobre `{fichero}`:

1. Comprueba si `{fichero}` **es de esta ejecución**: no aparece en la línea base de §4 **y** ahora figura en `git status --porcelain` (creado o modificado desde que empezó el pipeline).
2. Si **es de esta ejecución** → decisión fija **A = Sobrescribir**, sin debate (es un artefacto de este mismo pipeline: típico tras una reentrada §7.7 o un relanzamiento §5.4). Regístralo en `## Decisiones` con la plantilla de §8.5 (`FIJA-NNN`, `Decidido por: POLÍTICA FIJA`, `Por qué: {fichero} es de esta ejecución (no está en la línea base)`) y reanuda el ejecutor (§5.4 paso 3).
3. Si **no lo es** → debate con A = «Sobrescribir» y B = «Mantener y saltar el fichero».
   3.1 Los defensores **MUST** comprobar si la spec/el diseño declaran ese fichero como modificado (semántica de delta, ver `agent_docs/sdd-workflow.md`).
   3.2 Si **no** lo declaran, la respuesta correcta es `ESCALAR` (§8.4): sobrescribir algo ajeno a la iniciativa es destructivo.

- ✅ CORRECTO: `CONFLICT` sobre `service/impl/CicloServiceImpl.java`, que la línea base no tenía y lo creó la vuelta 1 → Sobrescribir sin debate.
- ❌ INCORRECTO: `CONFLICT` sobre un fichero que ya estaba modificado en la línea base → tratarlo como «de esta ejecución» (no lo es: alguien lo tocó antes de arrancar; va a debate).

### 7.7 Reentrada por `FIN-DESIGN-ERROR` (LIMIT 2 vueltas por ejecución)

Aplica tanto si el error viene de `implementer` (`implementation/error_design.log`) como de `debug` (`test-e2e-desc/error_design.log`). `<v>` = número de reentrada.

1. Si ya se hicieron **2** reentradas → **STOP** (STOP condition) e informe.
2. Aparta lo abortado: `mv {iniciativa}/implementation {iniciativa}/implementation_error_<v>`. Si el error vino de `debug`, además `mv {iniciativa}/test-e2e-desc/error_design.log {iniciativa}/test-e2e-desc/error_design_<v>.log`; la carpeta `test-e2e-desc/` **se conserva**: sus `[x]` son progreso reanudable.
3. Lanza un **ejecutor nuevo** de `designer` con `{args} = {iniciativa}/specification.md {contenido literal de la sección «## Problema» del error_design.log apartado} {flags}`.
   3.1 Su §4.4 llegará como `DECISION-REQUERIDA`: respóndela con **política fija B = «Revisar/Modificar»**, sin debate (es la vía que los propios skills prescriben para un error de diseño, y regenerar tiraría un diseño ya verificado).
   3.2 Regístrala en `## Decisiones` con la plantilla de §8.5 (`FIJA-NNN`, `Decidido por: POLÍTICA FIJA`, `Por qué: reentrada {v} por DESIGN-ERROR`).
4. Si ese ejecutor devuelve `FIN-OK`, relanza en orden `implementer` y, si el error vino de `debug`, `debug` (ejecutores nuevos, una `### Vuelta {v+1}` bajo el `## Paso` correspondiente), y después continúa con los pasos pedidos que quedaban. La reentrada corre `designer` e `implementer` **aunque no estuvieran entre los pasos pedidos**: es la única vía de corrección y queda registrada. El código que dejó la vuelta anterior sigue en el árbol: los `CONFLICT` que provoque los resuelve §7.6 paso 2.

---

## 8. Protocolo de debate

Se aplica **idéntico** a cada `DECISION-REQUERIDA` marcada «debate» en §2.3 y §7. Cada debate recibe un id correlativo `DEB-NNN` y su carpeta `{iniciativa}/log_pipeline/DEB-NNN/`.

### 8.1 Formular

1. Escribe con `Write` `{iniciativa}/log_pipeline/DEB-NNN/pregunta.md` con esta plantilla literal:
   ```markdown
   # DEB-{NNN} — {título corto}

   **Skill / fase / origen:** {SKILL, FASE y ORIGEN del bloque DECISION-REQUERIDA}
   **Pregunta:** {PREGUNTA exacta}
   **A:** {alternativa A}
   **B:** {alternativa B}
   **Opciones descartadas al formular:** {lista o «ninguna»}
   **Contexto:** {rutas: specification.md; design/design.md si existe; los ficheros de CONTEXTO; los skills k-* elegidos}
   ```
   - Si el skill ofrece más de dos opciones, elige las dos **más plausibles** y anota las descartadas. Si son «abiertas», formúlalas tú como dos formas de **continuar** el pipeline: «abortar» **no** es una alternativa (si abortar fuese lo correcto, el juez lo dirá con `ESCALAR`).
   - Los **skills `k-*`** se eligen según la pregunta (p.ej. `k-sistemas`/`k-vistas`/`k-validaciones` en un sistema; `k-tramite`/`k-tipo-expediente` en un expediente; `k-secure-coding` si toca entidades, servicios o controladores; `k-playwright` si la pregunta es de un test E2E). Se pasan como rutas a sus `SKILL.md`.
2. Añade en `log_pipeline.md`, bajo `## Decisiones`, la línea `- DEB-{NNN} — {título} — en curso` (se completa en §8.5).

### 8.2 Rondas (LIMIT 3)

Por cada ronda `r = 1..3` lanza **2 defensores en paralelo** (una única respuesta con 2 `Agent`, `model` al más capaz): el defensor de A y el de B. Mismo prompt salvo la posición asignada.

**Prompt de cada defensor** (`{X}` = su posición, `{Y}` = la contraria):

> Eres un experto arquitecto en Java y el framework Axelor y participas en un **debate técnico** para tomar una decisión que el pipeline SDD habría preguntado al usuario. Defiendes la posición **{X}**. Ronda **{r}** de 3.
>
> - **La pregunta y las alternativas**: lee `{iniciativa}/log_pipeline/DEB-{NNN}/pregunta.md`.
> - **Contexto** (léelo antes de argumentar): todas las rutas de la línea `**Contexto:**` de ese fichero. Consulta también `CLAUDE.md`, `agent_docs/` y el **código real** del proyecto cuando la pregunta lo toque.
> - **Rondas anteriores** *(solo desde la ronda 2)*: lee `ronda-{1..r-1}-A.md` y `ronda-{1..r-1}-B.md` de esa misma carpeta. **MUST** responder a los argumentos concretos del otro defensor, no repetir los tuyos.
> - **Reglas**:
>   - Cada argumento **MUST** citar su fuente (regla de la spec, sección de un skill, fichero y línea del código, o `CLAUDE.md`/`agent_docs`); una opinión sin fuente no cuenta.
>   - Defiende {X}, pero si el otro defensor demuestra con evidencia que {X} **incumple la spec** o **contradice una convención** del proyecto, **MUST** concederlo.
>   - Si detectas que **cualquiera** de las dos alternativas es **destructiva** (borra o sobrescribe algo que la iniciativa no declara como suyo) o **queda fuera del alcance** de la iniciativa, dilo explícitamente.
>   - **MUST NOT** usar `AskUserQuestion`.
>   - **MUST NOT** modificar ningún fichero del proyecto ni de la iniciativa salvo el tuyo de salida.
> - **Salida**: escribe con `Write` el fichero `{iniciativa}/log_pipeline/DEB-{NNN}/ronda-{r}-{X}.md` con **exactamente** la plantilla literal de abajo: tus argumentos en bullets con fuente (**LIMIT** 3-6 bullets); tu réplica en bullets (ronda 1: `- Sin rondas previas`); y al final la línea `ACEPTO: A`, `ACEPTO: B` o `ACEPTO: NINGUNA` — la alternativa que aceptas por escrito como solución (puede ser la contraria si te ha convencido; `NINGUNA` solo si defiendes que ambas son destructivas o fuera de alcance).
>   ```
>   === ARGUMENTOS {X} ===
>   - {argumento} — fuente: {regla de la spec | sección de un skill | fichero:línea | CLAUDE.md/agent_docs}
>   - …
>   === RESPUESTA A {Y} ===
>   - {réplica a un argumento concreto de {Y}} — fuente: {…}
>   - …
>   ACEPTO: {A | B | NINGUNA}
>   ```
> - **Respuesta (REQUIRED)**: **solo** la misma línea `ACEPTO: A|B|NINGUNA` que cierra tu fichero, y nada más. **MUST NOT** pegar tus argumentos en la respuesta.

- ✅ CORRECTO (respuesta): `ACEPTO: A` (y el fichero `ronda-1-A.md` con sus dos bloques y el mismo token al final)
- ❌ INCORRECTO: `Creo que A es mejor` (sin token), pegar los argumentos en la respuesta (van al fichero), un bullet `A es más limpio` sin fuente (opinión), o cambiar de posición sin citar la evidencia que te convenció.

Si un defensor no devuelve `ACEPTO:` o no escribe su fichero → relánzalo **LIMIT** 1 vez; si vuelve a fallar, la ronda cuenta como `ACEPTO: NINGUNA` para él.

### 8.3 Consenso

Tras cada ronda compara los dos tokens:

- Ambos coinciden en `A` o en `B` → **consenso**: la decisión es esa alternativa, `Decidido por: CONSENSO (ronda {r})`. Sal del bucle.
- Ambos `ACEPTO: NINGUNA` → salta directamente al juez (§8.4): la única salida es `ESCALAR` o una decisión del juez.
- En otro caso (posiciones distintas, o uno cede y el otro se cruza) → siguiente ronda. Tras la ronda 3 sin consenso → juez.

### 8.4 Juez (solo sin consenso)

Lanza **un** subagente juez (`model` al más capaz).

**Prompt del juez**:

> Eres un experto arquitecto en Java y el framework Axelor y actúas de **juez** en un debate técnico que no ha alcanzado consenso. Tu decisión se aplicará **en nombre del usuario**, sin que él la revise ahora.
>
> - **La pregunta y las alternativas**: lee `{iniciativa}/log_pipeline/DEB-{NNN}/pregunta.md`, y su **contexto** (todas las rutas de `**Contexto:**`). Verifica las citas de los defensores en las fuentes; un argumento cuya fuente no dice lo que el defensor afirma no vale.
> - **Transcripción**: lee todos los `ronda-*-A.md` y `ronda-*-B.md` de esa carpeta.
> - **Criterio, en este orden estricto**: (1) **cumplir la especificación** de la iniciativa; (2) respetar las **convenciones** de `CLAUDE.md`, `agent_docs/` y los skills `k-*`; (3) la opción **más simple y reversible**. Un nivel solo se consulta si el anterior no decide.
> - **Regla de escalado**: si la alternativa que ganaría es **destructiva** (borra o sobrescribe algo que la especificación o el diseño no declaran como parte de la iniciativa) o **queda fuera del alcance** de la iniciativa, **MUST NOT** elegirla: responde `ESCALAR`.
> - **MUST NOT** usar `AskUserQuestion`. **MUST NOT** modificar ningún fichero salvo el tuyo de salida. **MUST NOT** inventar una alternativa C.
> - **Salida**: escribe con `Write` `{iniciativa}/log_pipeline/DEB-{NNN}/juez.md` con **exactamente** la plantilla literal de abajo: la decisión en la primera línea y, bajo `=== JUSTIFICACION ===`, por qué, citando el nivel del criterio que decidió y las fuentes verificadas (**LIMIT** 3-8 líneas).
>   ```
>   DECISION: {A | B | ESCALAR}
>   === JUSTIFICACION ===
>   Nivel ({1 | 2 | 3}): {por qué ese nivel decide, con las fuentes verificadas}
>   {…}
>   ```
> - **Respuesta (REQUIRED)**: la línea `DECISION: A|B|ESCALAR` y, en la línea siguiente, **una sola línea** con la razón decisiva (nivel del criterio + fuente). Nada más.

- ✅ CORRECTO: `DECISION: B` + `Nivel (1): la spec RES-003 (entity-Ciclo.md) exige …; A lo dejaría sin cubrir`
- ❌ INCORRECTO: `Me inclino por B` (sin token), `DECISION: A y B` (no es una de las dos), o justificar por preferencia personal sin citar el nivel del criterio ni las fuentes.

Si el juez no devuelve `DECISION:` válido o no escribe `juez.md` → relánzalo **LIMIT** 1 vez; si vuelve a fallar → trata el debate como `ESCALAR`.

### 8.5 Aplicar y registrar

1. Sustituye en `log_pipeline.md` la línea `- DEB-{NNN} — {título} — en curso` por esta plantilla literal (una sola entrada por decisión). Las `FIJA-NNN` de §7.3/§7.6/§7.7 se añaden directamente con esta misma plantilla, con `FIJA-{NNN}` como id, `Decidido por: POLÍTICA FIJA`, el `Por qué:` fijo de su sección y `Transcripción: ninguna`:
   ```markdown
   - **DEB-{NNN} — {título}**
     - Skill / fase / origen: {…}
     - Pregunta: {pregunta exacta}
     - A: {alternativa A}
     - B: {alternativa B}
     - Decisión: {A | B | ESCALAR}
     - Decidido por: {CONSENSO (ronda {r}) | JUEZ | POLÍTICA FIJA}
     - Por qué: {línea de razón del juez, o la evidencia con que cedió el defensor que cambió de posición (léela solo de la última ronda), en 1-2 líneas}
     - Transcripción: {log_pipeline/DEB-{NNN}/ | ninguna}
   ```
2. `A` o `B` → reanuda el ejecutor con la decisión (§5.4 paso 3) y **continúa el pipeline sin detenerte**.
3. `ESCALAR` → **STOP** (STOP condition) e informe (§9).

---

## 9. Informe final

Escribe el informe al usuario **a partir de `log_pipeline.md`** (no releas los debates) con esta plantilla literal (también al terminar por una STOP condition, indicándolo). Si el paso `close` terminó en `FIN-OK`, `{iniciativa}` es ya la ruta bajo `.sdd/archive/`:

```
Pipeline sdd — {iniciativa}

Pasos pedidos: {ids en orden}{ (seleccionados en el TUI), si vinieron de §4.1}
Estado final: {COMPLETADO | COMPLETADO CON FALLOS EN DEBUG — {ids en FAIL} | DETENIDO — {motivo: pasos inválidos | skill inexistente | entrada ausente | build no compila | app no arranca | regresión E2E | tests en FAIL | ESCALAR en DEB-NNN | ERROR de entrada | worktree aislado | LIMIT de reentradas | ejecutor sin token}}

1. /sdd-{skill}: {FIN-OK | FIN-OK-CON-FALLOS | FIN-ERROR | FIN-DESIGN-ERROR | FIN-STOP-BUILD | FIN-STOP-APP | FIN-STOP-REGRESION | STOP | no ejecutado} — {resumen del ejecutor}.
   Reentradas por DESIGN-ERROR: {0 | v}   ← solo en designer/implementer/debug
2. /sdd-{skill}: …
{una línea por paso pedido, en orden; «no ejecutado» para los que no se alcanzaron}

Decisiones tomadas en tu nombre ({total}):
  DEB-001 — {título}
    Pregunta:  {pregunta exacta}
    A:         {alternativa A}
    B:         {alternativa B}
    Decisión:  {A | B | ESCALAR}  ({CONSENSO ronda r | JUEZ})
    Por qué:   {1-2 líneas}
  DEB-002 — …
  Decisiones por política fija (sin debate): {FIJA-NNN — … | ninguna}

Log completo: {iniciativa}/log_pipeline.md (transcripciones en log_pipeline/DEB-NNN/)

Siguiente paso: {/sdd-<skill que sigue al último paso completado> {ruta de su entrada} | ninguno: la iniciativa está cerrada | corregir lo indicado y relanzar /sdd <pasos pendientes>}
```

**Checklist del informe** (antes de mostrarlo; **LIMIT**: 3 iteraciones de corrección):

- [ ] ¿Aparecen **todos** los pasos pedidos, en orden, incluidos los «no ejecutado»?
- [ ] ¿Cada `DEB-NNN` de `log_pipeline.md` está cerrado (sin «en curso») y aparece en el informe con pregunta, A, B, decisión y quién la tomó?
- [ ] ¿Las `FIJA-NNN` aparecen en el informe aunque no fueran debates?
- [ ] ¿El estado final coincide con el último resultado registrado bajo los `## Paso N`?
- [ ] Si hubo una STOP condition, ¿el informe dice cuál y qué fichero mirar?
- [ ] Si `debug` o `tests` acabaron con `FAIL`, ¿se listan los ids fallidos y no se declara éxito?

**MUST NOT** lanzar ningún skill que no estuviera entre los pasos pedidos: el encargo termina en el último paso pedido.

---

## Quick Guidelines

- **Pasos = tramo contiguo del pipeline** `designer → implementer → debug → tests → close`, detectados por raíces en el texto del usuario; hueco, desorden, repetición, `spec` o `--manuales` → **ERROR** sin lanzar nada. Puede empezar en medio si existe la entrada del primer paso. Invocación **sin pasos** (o solo con ruta/flags) → se marcan en el TUI (§4.1), no es **ERROR**.
- **CRITICAL — contexto aislado**: cada paso corre en su **propio subagente ejecutor**; el orquestador **MUST NOT** cargar los skills con `Skill` ni ejecutar sus fases ni arrancar la app. Al orquestador solo vuelven tokens y rutas; los debates se escriben a `log_pipeline/DEB-NNN/`.
- **Sin preguntas una vez arrancado**: la **única** `AskUserQuestion` del skill es la selección inicial de pasos (§4.1), y solo si el input no trae ninguno; ningún subagente la usa nunca. Toda decisión en nombre del usuario pasa por **debate** (§8) o por una política fija declarada (§7.3 app, §7.6 `CONFLICT`, §7.7 reentrada, confirmación de `close`) y queda en `log_pipeline.md`.
- **Reanudar, no relanzar**: una `DECISION-REQUERIDA` se responde al **mismo** ejecutor con `SendMessage` (`DECISION: A|B — …`); relanzar desde cero es solo el fallback de §5.4.
- **Los skills encadenados mandan** fuera de la tabla §2.3: el ejecutor sigue su flujo completo, tokens, logs, LIMITs y gestión de la app.
- **Encadenado condicional**: un paso solo arranca si el anterior devolvió `FIN-OK` y su artefacto de entrada existe. Única excepción: el `FIN-OK-CON-FALLOS` de `debug` encadena con `tests`, que excluye solo los tests en `FAIL`.
- **Debate**: dos defensores en paralelo con el **mismo contexto** (spec, diseño, `k-*` relevantes, código real), argumentos con fuente, **LIMIT** 3 rondas, consenso = ambos `ACEPTO:` iguales; sin consenso → juez con prioridades (1) spec, (2) convenciones, (3) simple y reversible; `ESCALAR` si es destructivo o fuera de alcance.
- **Paradas reales** (las únicas): pasos inválidos, skill o entrada ausente, `FIN-ERROR`, `FIN-STOP-BUILD`, `FIN-STOP-APP`, `FIN-STOP-REGRESION`, `FIN-OK-CON-FALLOS` de `tests`, `ESCALAR`, worktree aislado, LIMIT 2 de reentradas por `FIN-DESIGN-ERROR`, ejecutor sin token tras 2 reenvíos.
- **`FIN-DESIGN-ERROR`** (de implementer o de debug): apartar `implementation/`, ejecutor nuevo del designer en Revisar/Modificar con el error como cambios, y volver a correr implementer (y debug) aunque no se pidieran (LIMIT 2).
- **Informe final** (§9) siempre, también tras una parada, construido solo desde `log_pipeline.md`; tras `close`, en `.sdd/archive/`.

---

## Apéndice A — Override de rutas (para testing)

- `--template-dir=<ruta>` — se pasa **verbatim** en los `{args}` de todos los ejecutores (§7 y cada reentrada de §7.7; cada skill lo valida contra su propia carpeta de plantillas).
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/` para localizar la iniciativa (§4); se pasa también en los `{args}` de todos los ejecutores.
- `--manuales` (de `/sdd-debug-with-test-e2e-desc`) **MUST NOT** aceptarse ni añadirse: sus tests necesitan una persona.

En uso normal no se especifican.
