---
name: sdd-test-e2e
description: Herramienta de testing E2E del pipeline SDD. Dado el `test-e2e-desc.md` de una iniciativa (la descripción de tests Given/When/Then que produce `/sdd-designer` a partir de los escenarios `ESC-NNN`, propagada por `/sdd-implementer` a `implementation/`), descompone esos tests en una carpeta `test-e2e/` (un índice `tests-e2e-desc.md` con un checkbox por test + un `test-e2e-desc_NN.md` autocontenido por test) y luego, test a test, los ejecuta contra la aplicación real. El skill es un MOTOR genérico y agnóstico al artefacto: aporta solo el flujo (localizar la iniciativa, cargar el contrato, descomponer, y por cada test el bucle ejecutar→corregir→reejecutar) y delega TODO lo específico (qué es un test, cómo se descompone, cómo se ejecuta contra la app, cómo se corrige el código) en la guía `template-system/README.md` (configurable con `--template-dir`), que los subagentes leen como contrato. Lanza tres roles: un descomponedor que escribe `test-e2e/`; un ejecutor por test que lo pilota en el navegador y devuelve SUCCESS/FAIL; y, ante un FAIL, un corrector que analiza el problema, carga los skills necesarios y arregla el código Java (bucle ejecutar↔corregir, **LIMIT** 10 por test). Marca cada test como `[x]` en el índice al pasar (progreso reanudable). La salida son las correcciones de código en `src/main/...` y la carpeta `test-e2e/` con el estado de cada test.
handoffs:
  - label: Cerrar la iniciativa
    agent: sdd-close-spec
    prompt: Cerrar la iniciativa en .sdd/drafts/{carpeta-iniciativa}/ tras pasar los tests E2E — archivar en .sdd/specs/ y actualizar los CLAUDE.md afectados.
allowed-tools: Bash, Read, Write(.sdd/**), Edit(.sdd/**), Skill, AskUserQuestion, Agent, Monitor, mcp__intellij-index__ide_search_text, mcp__intellij-index__ide_find_class, mcp__intellij-index__ide_find_file, mcp__intellij-index__ide_find_definition, mcp__intellij-index__ide_find_references, mcp__intellij-index__ide_find_implementations, mcp__intellij-index__ide_find_super_methods, mcp__intellij-index__ide_call_hierarchy, mcp__intellij-index__ide_type_hierarchy, mcp__intellij-index__ide_diagnostics, mcp__intellij-index__ide_index_status, mcp__intellij-index__ide_sync_files
---

# sdd-test-e2e

Eres un **motor de testing E2E** del pipeline SDD: tomas la **descripción de tests** de una iniciativa (`test-e2e-desc.md`, producida por `/sdd-designer` desde los escenarios `ESC-NNN`), la **descompones** en una carpeta `test-e2e/` con un fichero autocontenido por test, y luego **ejecutas cada test contra la aplicación real**; cuando un test falla, **corriges el código** y reintentas hasta que pase o se agote el `**LIMIT**`. Marcas cada test que pasa con `[x]` en el índice. Es una herramienta de depuración independiente: modifica código en `src/main/...` y mantiene sus artefactos en `test-e2e/`.

**CRITICAL — eres agnóstico al artefacto.** Este `SKILL.md` define **solo el flujo y la orquestación de agentes**. **No sabe nada de qué es un test, cómo se descompone, cómo se ejecuta contra la app ni cómo se corrige el código**: todo eso lo declara la guía `template-system/README.md`, que los subagentes leen como contrato. **MUST NOT** asumir de memoria ningún detalle específico; **MUST NOT** nombrar ficheros, plantillas ni comandos concretos de la ejecución o la corrección en este skill, salvo el contrato fijo de entrada/salida y la gestión de la app (§2.2). Apuntar `--template-dir` a otra carpeta con un README distinto cambia por completo qué se prueba y cómo, **sin tocar este skill**.

El skill lanza **tres roles** de subagente (todos leen el mismo `README.md`, cada uno hace una tarea distinta):

- **descomponedor** — lee `test-e2e-desc.md` y escribe la carpeta `test-e2e/` (índice + un fichero autocontenido por test) (§7).
- **ejecutor** — coge **un** test, lo pilota en el navegador contra la app real y devuelve `SUCCESS`/`FAIL` (§9).
- **corrector** — ante un `FAIL`, analiza el problema, carga los skills que necesite y **corrige el código** (§9).

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- **Ruta explícita** a un `test-e2e-desc.md` (p.ej. `.sdd/drafts/2026-06-16_01-44_grupos-y-notas/implementation/test-e2e-desc.md`). El skill valida que existe y procede.
- **Identificador de test concreto** (`T-007`) o una lista (`T-001 T-007`) para depurar solo esos. Sin esto, se procesan **todos** los pendientes.
- **Sin argumentos** → auto-detección de la **última** iniciativa de `.sdd/drafts/` que contenga `implementation/test-e2e-desc.md` (§4.2).
- Flags de override `--template-dir=`, `--in=`, `--root=`, `--fresh` (Apéndice A).

---

## Outline

1. **Fase 0 — Localizar** la iniciativa y su `test-e2e-desc.md`, y **confirmar** la ruta detectada (§4).
2. **Fase 1 — Cargar** el contrato (`template-system/README.md`) y resolver las rutas de entrada (§5).
3. **Fase 2 — Descomponer**: lanzar el subagente **descomponedor**, que escribe `test-e2e/` (índice + un fichero por test); cargar el progreso del índice y descartar los tests ya `[x]` (§7).
4. **Fase 3 — Arrancar la app** (la gestiona el motor, §2.2): dejarla respondiendo `200` antes de ejecutar el primer test (§8).
5. **Fase 4 — Por cada test pendiente, en orden** (§9):
   - **ejecutor**(test) → `SUCCESS` | `FAIL` + descripción.
   - `SUCCESS` → marcar `[x]` en el índice y pasar al siguiente test.
   - `FAIL` → bucle **corrector**(problema + log) → reiniciar app → reejecutar (**LIMIT** 10 ciclos por test); al pasar, marcar `[x]`; al agotar, dejar el test sin marcar y seguir.
6. **Fase 5 — Reportar** el listado final `SUCCESS`/`FAIL` de todos los tests (§10).

**STOP conditions**:

- `--template-dir=` apunta a una carpeta que **no contiene `README.md`** → **ERROR** y detente.
- No se encuentra ningún `test-e2e-desc.md` (o no contiene ningún bloque `## T-NNN`) → **ERROR** y detente sin ejecutar nada.
- El usuario no confirma la ruta auto-detectada (Fase 0 caso 2) → **STOP** y pide la ruta.
- El **descomponedor** no devuelve el token `ESCRITO: test-e2e/` con su lista de tests tras 1 reintento → **STOP** y muestra el problema.
- La app no responde `200` en `http://localhost:8080` tras arrancarla (compila pero no levanta) → **STOP** y `AskUserQuestion` (reintentar / ver log / abortar).
- El **corrector** devuelve `BLOQUEADO` (la corrección exigiría tocar `test-e2e-desc.md`, el XML/contrato del diseño, o un recurso ausente) → **STOP** y pregunta al usuario (no es trabajo de este skill; eso vuelve a `/sdd-designer`). **MUST NOT** adivinar.
- Tras **10** ciclos de corrección un test sigue en `FAIL` → ese test queda `FAIL` (sin marcar en el índice) y el bucle continúa con el resto; al final se reporta como `FAIL`.

---

## 1. Entrada y salida

### 1.1 Entrada

Un único `test-e2e-desc.md` que vive **siempre en la subcarpeta `implementation/`** de la iniciativa (`/sdd-designer` lo produce en `design/` y `/sdd-implementer` lo propaga a `implementation/`; este skill lee **el de `implementation/`**): una **cabecera común** con la sección `## Estado inicial de la base de datos` (datos maestros + tabla de credenciales de login) seguida de bloques `## T-NNN — <nombre>` con `Precondiciones`, `Pasos` y `Resultado esperado`. El skill **no asume su estructura interna**: la conoce el descomponedor leyendo el contrato.

### 1.2 Salida

- En `.sdd/drafts/{iniciativa}/test-e2e/`: el índice `tests-e2e-desc.md` (un checkbox por test, marcado `[x]` al pasar) y un `test-e2e-desc_NN.md` autocontenido por test. **Su estructura interna la define la plantilla**, no este skill.
- En el **árbol del proyecto** (`src/main/java/com/educaflow/...`): las correcciones de código Java necesarias para que los tests pasen.
- En la conversación: el listado final `SUCCESS`/`FAIL` por test (§10).

**Logs de orquestación del motor** (en `test-e2e/`, no son contenido de la plantilla; los subagentes los ignoran):

- `app.log` — la salida de la app arrancada por el motor (§2.2, §8); el motor la lee al diagnosticar un `FAIL` y pasa el extracto relevante al corrector.

**MUST NOT** modificar `test-e2e-desc.md` ni ningún artefacto de `design/` o `implementation/` para que un test pase. Este skill solo lee la descripción de tests, escribe `test-e2e/` y corrige **código Java**.

### 1.3 Estructura de carpetas

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
├── implementation/
│   └── test-e2e-desc.md        ← entrada (SIEMPRE aquí; la propaga /sdd-implementer; NO se modifica)
└── test-e2e/                   ← salida del descomponedor (Fase 2)
    ├── tests-e2e-desc.md        ← índice con un checkbox por test (type: test-e2e-index)
    ├── test-e2e-desc_01.md … test-e2e-desc_NN.md   ← un fichero autocontenido por test (type: test-e2e)
    └── app.log                  ← log del motor: salida de la app arrancada en la Fase 3

src/main/java/com/educaflow/...  ← código Java que se corrige
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 El README es el contrato único

Todo lo específico (qué es un test, cómo se descompone `test-e2e-desc.md`, cómo se ejecuta un test contra la app, cómo se corrige el código y qué **MUST NOT** tocarse) lo define `template-system/README.md` y los ficheros que él referencie. Los subagentes los **leen de disco**; el skill **MUST NOT** asumirlos, restatarlos ni hardcodearlos aquí. El skill solo pasa a cada subagente **las rutas** de los ficheros de entrada y su rol.

**CRITICAL — `README.md` es el ÚNICO fichero de la plantilla que el motor conoce por nombre.** El skill **MUST NOT** nombrar, leer ni ejecutar ningún otro fichero de la plantilla. Esos los descubren y usan los subagentes leyendo el `README.md`. Único acoplamiento por nombre: `README.md` (contrato) y el contrato fijo de entrada (`test-e2e-desc.md`) / salida (`test-e2e/`).

**REQUIRED — el README es leído por los tres roles** (descomponedor, ejecutor, corrector). Cualquier `README.md` de plantilla (la `template-system/` actual o una futura apuntada con `--template-dir=`) **MUST** estar redactado para esos tres roles: debe delimitar, por rol, qué tarea hace y qué ficheros de la plantilla le aplican.

### 2.2 La app la gestiona el MOTOR (única excepción al agnosticismo)

La aplicación bajo prueba es un **recurso compartido** por todos los ejecutores secuenciales y **MUST** sobrevivir entre subagentes. Por eso **la gestiona el motor**, no los subagentes (un proceso en segundo plano lanzado por un subagente muere al cerrarse su contexto). Esta es la **única** parte específica que el motor ejecuta: los comandos concretos (cómo arrancarla, cómo pararla por puerto, cómo sondear que responde, dónde va el log) los define `README.md` en su sección **«Gestión de la app»**, que el motor lee en la Fase 1 y sigue al pie de la letra.

Reglas que el motor **MUST** cumplir (los comandos exactos están en el README):

- **Arrancar** la app como **tarea tracked en segundo plano** (`Bash` con `run_in_background: true`), redirigiendo el log al fichero que indique el README, y **esperar** sondeando hasta que responda `200` (**LIMIT** de sondeo amplio; el arranque hace un `clean build`). **MUST NOT** dejar que un subagente arranque la app.
- **CRITICAL — limpiar el puerto de verdad antes de arrancar**: una instancia previa colgada hace que el connector falle el bind en silencio y la app nunca dé `200`. Sigue el procedimiento de limpieza del README.
- **Parar** siempre **por puerto**, nunca por handle de proceso.
- El arranque es **idempotente**: comprueba el `200` y arranca solo si no responde. **MUST NOT** levantar una segunda instancia.
- Tras una corrección de código, el motor **para y rearranca** la app (el arranque recompila) antes de reejecutar el test.

### 2.3 No tocar el contrato — solo código Java

**MUST NOT** modificar `test-e2e-desc.md` ni los ficheros de `test-e2e/` para que un test pase (eso es trampa). **MUST NOT** editar XML de dominios/vistas materializados ni cambiar firmas declaradas por el diseño: si el fallo exige eso, el corrector devuelve `BLOQUEADO` y el motor **STOP** y pregunta al usuario — la corrección vuelve a `/sdd-designer`. Este skill corrige **lógica Java** (servicios, controladores, repositorios, validaciones, jobs, datos iniciales).

### 2.4 Orquestación de subagentes

- El **descomponedor** corre **una vez** (§7). Los **ejecutores** y **correctores** corren **de uno en uno y en secuencia** (§9): comparten el puerto 8080 y se pisarían en paralelo.
- **MUST NOT** lanzar subagentes en paralelo ni con `run_in_background` (salvo el arranque de la app, que sí es background tracked y lo hace el motor, no un subagente): el skill necesita el resultado de cada subagente para continuar.
- Cada rol responde con un **token literal** que el skill parsea (definidos en cada fase). El skill compara por literal exacto.
- Los subagentes **MUST NOT** usar `AskUserQuestion`: ante un bloqueo lo reportan con el token de su rol y el motor lleva la decisión al usuario.

### 2.5 Cada fichero de test es autocontenido

Cada `test-e2e-desc_NN.md` que escribe el descomponedor **MUST** incluir la cabecera común (`Estado inicial de la base de datos` + la tabla de credenciales de login) **además** del bloque del test, porque el ejecutor recibe **solo ese fichero** y necesita las credenciales y el estado previo para pilotar la app sin depender de ningún otro fichero. El contrato (`README.md`) define el formato exacto.

### 2.6 Progreso reanudable por el índice

El índice `tests-e2e-desc.md` es el checkpoint: un test pasa → el motor marca su línea `[x]`. Al (re)invocar el skill, los tests ya `[x]` se **descartan** y solo se procesan los pendientes (`[ ]`). El test es la unidad atómica: si el skill se interrumpe a mitad de un test, ese test queda `[ ]` y se reejecuta entero. `--fresh` reinicia todos a `[ ]` (Apéndice A).

---

## 3. Flujo general

```
┌──────────────────────────────────────────────────────────────────────┐
│  Fase 0  Localizar test-e2e-desc.md (ruta explícita | más nueva)     │
│  Fase 1  Cargar el contrato (README) y resolver rutas                │
│  Fase 2  descomponedor(test-e2e-desc.md) → test-e2e/ (índice + tests)│
│          + cargar el índice → descartar tests ya [x]                 │
│  Fase 3  Motor: arrancar la app (tracked bg) hasta 200               │
│  Fase 4  Por cada test PENDIENTE (en secuencia):                     │
│            ├─ ejecutor(test) ──► SUCCESS | FAIL + descripción        │
│            ├─ SUCCESS → marcar [x] en el índice → siguiente test     │
│            └─ FAIL → bucle (LIMIT 10):                               │
│                 corrector(problema + log app) → parar/rearrancar app │
│                 → reejecutar; al pasar marcar [x]; al agotar, FAIL   │
│  Fase 5  Reporte final SUCCESS/FAIL de todos los tests               │
└──────────────────────────────────────────────────────────────────────┘
```

Las fases se ejecutan **en orden**. Salvo la confirmación inicial (Fase 0) y los `STOP` ante una excepción real, el flujo **no pide aprobación**: tras descomponer, se ejecuta automáticamente.

---

## 4. Fase 0 — Localizar el `test-e2e-desc.md`

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca con una ruta a un `test-e2e-desc.md`: comprueba que existe. La **carpeta de la iniciativa** es la que contiene la subcarpeta `implementation/` donde vive ese fichero. Pasa a la Fase 1.

### 4.2 Caso 2 — Sin ruta (auto-detección)

1. Lista las carpetas con formato de iniciativa:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordena alfabéticamente (el prefijo timestamp = orden cronológico) y toma la **última** que contenga `implementation/test-e2e-desc.md`. **MUST** buscar el fichero **solo** en `implementation/` (nunca en `design/`).
3. Si ninguna lo contiene, indica que no hay tests disponibles (¿se ha ejecutado ya `/sdd-implementer`, que es quien propaga `test-e2e-desc.md` a `implementation/`?) y pide una ruta. Detente.
4. Muestra la ruta detectada y confirma con `AskUserQuestion` (usar / dar otra ruta).

**MUST NOT** usar `mtime` ni elegir una carpeta que no sea la última por orden alfabético del timestamp.

---

## 5. Fase 1 — Cargar el contrato y resolver rutas

1. **REQUIRED — lee con `Read` la guía `template-system/README.md`** (resuelta contra `--template-dir`): confirma que existe (si no → **ERROR**, STOP condition), entiende a alto nivel qué pide a cada rol, y **lee su sección «Gestión de la app»** (los comandos que el motor ejecutará en la Fase 3/4 — §2.2). El resto del contrato lo leen los subagentes de disco.
2. **Resuelve las rutas de entrada** que se pasarán a los subagentes (no su contenido):
   - la ruta de `template-system/README.md` (las reglas),
   - la ruta del `test-e2e-desc.md` (la descripción de tests) y la carpeta de la iniciativa,
   - la ruta de la carpeta de salida `{iniciativa}/test-e2e/`.
3. **Valida que el `test-e2e-desc.md` contiene al menos un bloque `## T-`**. Si no, **ERROR** y detente.

No hay más preparación: el skill no carga skills técnicos ni explora el código — eso lo hace cada subagente leyendo el README.

---

## 6. Fases con subagentes — patrón común del prompt

Cada prompt de subagente (§7, §9) **MUST** pasar, además de su tarea específica:

- **Reglas**: `lee {ruta de template-system/README.md} y todos los ficheros que referencie. Es el contrato: define qué hacer, cómo y con qué estructura. Síguelo al pie de la letra.`
- **MUST NOT** usar `AskUserQuestion`: ante una duda que no puedan resolver, la reportan con el token de bloqueo de su rol.

---

## 7. Fase 2 — Descomponer (subagente descomponedor)

**Lanza un subagente descomponedor** (uno solo, `subagent_type: claude`, `run_in_background: false`). Su tarea: leer el `test-e2e-desc.md` y **escribir la carpeta `test-e2e/`** siguiendo el contrato: un fichero `test-e2e-desc_NN.md` **autocontenido** por test (cabecera común + bloque del test) y el índice `tests-e2e-desc.md` con un **checkbox sin marcar** por test. **MUST NOT** ejecutar ningún test ni tocar código.

**Prompt del subagente descomponedor**:

> Eres un experto en testing E2E de la secretaría virtual (Axelor) que tienes que **descomponer una descripción de tests E2E en ficheros individuales** siguiendo unas reglas.
>
> - **Reglas para la descomposición**: lee `{ruta de template-system/README.md}` y **todos los ficheros que referencie** —en particular el contrato de **descomposición**—. Define cómo trocear `test-e2e-desc.md` en un fichero por test, qué cabecera común copiar en cada uno para que sea autocontenido, las plantillas exactas de `test-e2e-desc_NN.md` y del índice `tests-e2e-desc.md` (con checkbox por test) y el checklist. Síguelo al pie de la letra.
> - **Entrada**: lee `{ruta del test-e2e-desc.md}` íntegro.
> - **Salida**: escribe en `{iniciativa}/test-e2e/` un `test-e2e-desc_NN.md` por test y el índice `tests-e2e-desc.md`, con la estructura exacta que define el contrato. **MUST NOT** ejecutar tests ni materializar código.
> - **MUST NOT** usar `AskUserQuestion`. Aplica el **checklist** del contrato antes de terminar (**LIMIT**: 3 iteraciones de autocorrección).
> - Al terminar, responde con este formato **exacto**:
>   - Primera línea: **exactamente** `ESCRITO: test-e2e/`.
>   - Una línea **exactamente** `=== TESTS ===` y, debajo, **una línea por test** en orden, con el formato `{fichero} | {T-NNN} | {nombre}` (p.ej. `test-e2e-desc_01.md | T-001 | Crear un grupo con alumnos`).
>   - **MUST NOT** pegar el contenido de los ficheros en la respuesta (ya está en disco).

El skill parsea `ESCRITO: test-e2e/` y, tras `=== TESTS ===`, la lista ordenada de tests (fichero + id + nombre) para iterar en la Fase 4. Si el token no aparece o no hay ninguna línea de test, **reintenta 1 vez**; si vuelve a fallar → **STOP**.

**Cargar el progreso** (§2.6): tras la descomposición, lee el índice `tests-e2e-desc.md`. Los tests ya marcados `[x]` se **descartan** (salvo `--fresh`); la Fase 4 solo recorre los `[ ]`. Si el usuario pasó ids concretos, filtra a esos. Indica cuántos se saltan y cuántos quedan pendientes.

- ✅ CORRECTO (respuesta): `ESCRITO: test-e2e/` + `=== TESTS ===` + `test-e2e-desc_01.md | T-001 | Crear un grupo`
- ❌ INCORRECTO: `He generado 9 tests` (token no parseable), pegar el contenido de los ficheros, ejecutar un test (eso es de la Fase 4)

---

## 8. Fase 3 — Arrancar la app (la gestiona el motor)

Antes de ejecutar el primer test, el motor deja la app respondiendo `200` siguiendo la sección **«Gestión de la app»** del README (§2.2): limpia el puerto, arranca `./run.sh` como tarea tracked en segundo plano con el log al fichero indicado, y sondea hasta `200`. Si tras el sondeo no responde `200` → **STOP** y `AskUserQuestion` (reintentar / ver `test-e2e/app.log` / abortar). **MUST NOT** continuar a la Fase 4 sin la app en `200`.

---

## 9. Fase 4 — Ejecutar y corregir cada test (en secuencia)

Recorre la lista de **tests pendientes** (`[ ]` en el índice) **en orden**. Para cada test:

### 9.1 Lanzar el ejecutor

Lanza **un** subagente con `Agent` (`subagent_type: claude`, `run_in_background: false`). Recibe **solo** la ruta de **su** fichero de test (autocontenido) y el contrato.

**Prompt del subagente ejecutor**:

> Eres un **ejecutor de un único test E2E** de la secretaría virtual (Axelor). NO modifiques código fuente: solo ejecutas el test y reportas el resultado.
>
> - **Reglas para la ejecución**: lee `{ruta de template-system/README.md}` y los ficheros que referencie —en particular el contrato de **ejecución** (qué skill cargar para pilotar el navegador, la URL base, cómo interpretar Given/When/Then, los errores recurrentes a evitar, y el criterio de equivalencia semántica de los mensajes)—. Síguelo.
> - **Test a ejecutar**: lee `{ruta del test-e2e-desc_NN.md}` —es **autocontenido**: trae la cabecera común (estado inicial + credenciales de login) y el bloque del test (precondiciones, pasos, resultado esperado)—.
> - **Premisa**: la app YA está levantada en `http://localhost:8080` (la arrancó el orquestador). NO la arranques ni la pares. Si no responde, repórtalo como `FAIL` con motivo "app no disponible".
> - **MUST NOT** usar `AskUserQuestion`. **MUST NOT** modificar ficheros del proyecto.
> - **Formato de salida (REQUIRED)**: primera línea **exactamente** `SUCCESS {T-NNN}` o `FAIL {T-NNN}`. Si `FAIL`, debajo un bloque `=== FALLO ===` con la descripción concreta de qué falló (paso, valor esperado vs observado) y toda la información de la UI en el momento del fallo (snapshot/mensajes/toasts de error, valores de campos, URL, errores de consola y peticiones fallidas con su status) que el contrato pida recoger.

El skill parsea la primera línea:

- `SUCCESS {T-NNN}` → **marca `[x]`** en `tests-e2e-desc.md` (Edit sobre la línea de ese test) y pasa al siguiente test.
- `FAIL {T-NNN}` → entra en el **bucle de corrección** (§9.2) con el bloque `=== FALLO ===`.

### 9.2 Bucle de corrección (solo si FAIL)

**Variables**: `**LIMIT**: max_ciclos = 10`, `ciclo = 1`.

Cada **ciclo**:

1. **Reúne el contexto del fallo**: la descripción `=== FALLO ===` del ejecutor + el **extracto relevante del log de la app** (`Read` sobre `test-e2e/app.log`, normalmente el final; busca trazas/excepciones del momento del fallo). El motor las pasa al corrector.
2. **Lanza el subagente corrector** (`Agent`, `subagent_type: claude`, `run_in_background: false`). Su tarea: analizar la causa, **decidir qué skills necesita y cargarlos**, y corregir el código Java según el contrato (delegando en `code-implementer` si el contrato lo indica). **MUST NOT** tocar el contrato (§2.3).

   **Prompt del subagente corrector**:

   > Eres un experto arquitecto en Java y el framework Axelor que tiene que **corregir el código para que un test E2E pase**.
   >
   > - **Reglas para la corrección**: lee `{ruta de template-system/README.md}` y los ficheros que referencie —en particular el contrato de **corrección** (cómo localizar la causa, cómo decidir y cargar los skills de dominio necesarios, cómo delegar el código en `code-implementer`, y qué **MUST NOT** tocarse)—. Síguelo.
   > - **Test que falla**: lee `{ruta del test-e2e-desc_NN.md}` (autocontenido).
   > - **Problema observado** (lo reportó el ejecutor): `{bloque === FALLO ===}`.
   > - **Log de la app** (extracto relevante): `{extracto de test-e2e/app.log}`.
   > - **OBLIGATORIO**: analiza qué skills de dominio necesitas para el arreglo (p.ej. `k-secure-coding` + `k-code-quality` si tocas entidades/servicios/controladores; además `k-sistemas`, `k-scheduler`, `k-validaciones`, `k-i18n`… según el fallo), **cárgalos con la herramienta `Skill` antes de corregir** y, si el contrato lo indica, delega el código en `code-implementer`.
   > - **MUST NOT** usar `AskUserQuestion`. **MUST NOT** modificar `test-e2e-desc.md` ni los ficheros de `test-e2e/`, ni editar XML de dominios/vistas materializados o el contrato del diseño para cuadrar el test.
   > - Al terminar, responde con **exactamente uno** de estos tokens en la primera línea + 1-2 líneas de resumen:
   >   - `CORREGIDO: {T-NNN}` — aplicaste un cambio de código que debería hacer pasar el test.
   >   - `BLOQUEADO: {T-NNN} — {motivo}` — la corrección exigiría tocar el contrato/diseño/XML, o falta un recurso; no se puede arreglar solo con código Java.

3. **Interpreta el token del corrector**:
   - `BLOQUEADO: …` → **STOP** y `AskUserQuestion` mostrando el motivo (la corrección vuelve a `/sdd-designer` o requiere decisión del usuario). **MUST NOT** seguir con este test por tu cuenta.
   - `CORREGIDO: …` → continúa al paso 4.
4. **Para y rearranca la app** (la gestiona el motor, §2.2): párala por puerto y arráncala de nuevo (el arranque recompila el fix); sondea hasta `200`. Si no compila o no levanta (lee el final de `test-e2e/app.log`; si ves `BUILD FAILED` es error de compilación), trátalo como fallo de este ciclo.
5. **Reejecuta el test**: vuelve a lanzar el ejecutor (§9.1) para el mismo test.
   - `SUCCESS` → **marca `[x]`** en el índice y sal del bucle (pasa al siguiente test).
   - `FAIL` → **fin de ciclo**: incrementa `ciclo`. Si `ciclo <= 10`, repite desde el paso 1 con la nueva descripción. Si `ciclo > 10`, el test queda **FAIL** (no se marca `[x]`); pasa al siguiente test.

**MUST NOT** superar los **10** ciclos por test.

- ✅ CORRECTO (ejecutor): `SUCCESS T-001` / `FAIL T-001` + bloque `=== FALLO ===`
- ✅ CORRECTO (corrector): `CORREGIDO: T-001` + 1 línea de resumen
- ❌ INCORRECTO: `El test pasa ✅` (token no exacto), pegar el código en la respuesta, que el corrector edite `test-e2e-desc.md` o un XML del diseño

---

## 10. Fase 5 — Reporte final

Tras procesar todos los tests pendientes, **MUST** mostrar en la conversación el listado final, construido **desde el índice `tests-e2e-desc.md`** (la fuente acumulada: incluye los `[x]` de ejecuciones anteriores). Relee el índice y, por cada test, `[x]` = SUCCESS, `[ ]` = FAIL en esta pasada.

```
Resultado del testing E2E ({N} tests) — .sdd/drafts/{iniciativa}/test-e2e/

SUCCESS  T-001 — Crear un grupo con alumnos y notas iniciales
FAIL     T-009 — Envío automático con éxito
         ↳ {última descripción del fallo}
SUCCESS  T-011 — Reenvío de un grupo cerrado
...

Resumen: {P} SUCCESS / {F} FAIL.
```

Para cada `FAIL` incluye en una línea sangrada la última descripción del fallo. Si quedaron tests en `FAIL`, indícalo claramente. **MUST NOT** ocultar fallos ni declarar éxito si algún test quedó en `FAIL`.

---

## Quick Guidelines

- **CRITICAL — agnosticismo**: este SKILL es un **motor de flujo**; **no sabe qué es un test, cómo se descompone, cómo se ejecuta ni cómo se corrige el código**. Todo lo específico lo define `template-system/README.md` (configurable con `--template-dir`), que **leen los subagentes**. Único contrato fijo: entrada `test-e2e-desc.md`, salida en `test-e2e/` y en el árbol del proyecto. Única parte específica que ejecuta el motor: la **gestión de la app** (§2.2, comandos en el README).
- **Localizar** (§4): ruta explícita, o auto-detectar la **última** iniciativa de `.sdd/drafts/` con `implementation/test-e2e-desc.md` (el fichero se busca **siempre en `implementation/`**, nunca en `design/`) y **confirmar**. **MUST NOT** usar `mtime`.
- **Descomponer** (§7): **un** descomponedor escribe `test-e2e/` (un fichero **autocontenido** por test + índice con checkbox); responde `ESCRITO: test-e2e/` + bloque `=== TESTS ===`. Cada `test-e2e-desc_NN.md` incluye la cabecera común (estado inicial + credenciales) para que el ejecutor no dependa de otros ficheros (§2.5).
- **App por el motor** (§2.2, §8): arrancar como **tarea tracked en segundo plano**, limpiar el puerto de verdad antes, sondear hasta `200`, parar por puerto. **MUST NOT** dejar que un subagente arranque la app. Idempotente: no levantar dos instancias.
- **Ejecutar** (§9.1): **un ejecutor por test, en secuencia** (nunca en paralelo ni `run_in_background`). Responde `SUCCESS {id}` / `FAIL {id}` + `=== FALLO ===`. **MUST NOT** modificar código.
- **Corregir** (§9.2): ante `FAIL`, **un corrector** que **analiza qué skills necesita, los carga** y arregla el código Java (delegando en `code-implementer` si el contrato lo dice), con el problema + el log de la app. Tras `CORREGIDO`, el motor rearranca la app y reejecuta. **LIMIT** 10 ciclos por test; al agotar, `FAIL` y siguiente. `BLOQUEADO` → **STOP** y `AskUserQuestion`.
- **No tocar el contrato** (§2.3): **MUST NOT** modificar `test-e2e-desc.md`, los ficheros de `test-e2e/`, ni el XML/contrato del diseño para que un test pase; si hace falta, `BLOQUEADO` → vuelve a `/sdd-designer`.
- **Progreso reanudable** (§2.6): al pasar un test, el motor marca `[x]` en el índice; al (re)invocar, salta los `[x]`. `--fresh` reinicia todos a `[ ]`.
- **Contrato de tokens**: literal exacto — `ESCRITO: test-e2e/`, `SUCCESS`/`FAIL`, `CORREGIDO`/`BLOQUEADO`. Los subagentes **MUST NOT** pegar contenido que ya está en disco.

---

## Apéndice A — Override de rutas (para testing y versatilidad)

- `--template-dir=<ruta>` — **carpeta de plantillas** alternativa a `template-system/`. **MUST** contener un `README.md` redactado para los **tres roles** (descomponedor, ejecutor, corrector) y con una sección «Gestión de la app»; si falta → **ERROR**. Permite usar el mismo flujo con otro tipo de tests sin tocar el skill.
- `--in=<ruta>` — `test-e2e-desc.md` de entrada explícito (normalmente bajo `implementation/`). **Desactiva la auto-detección** de la Fase 0 caso 2.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`.
- `--fresh` — reinicia el progreso: pone todos los checkboxes del índice a `[ ]` antes de la Fase 4 (reejecuta todos los tests). En uso normal no se especifica.

En uso normal no se especifican: se usa la carpeta `template-system/`, la carpeta de la iniciativa y `.sdd/drafts/`.
