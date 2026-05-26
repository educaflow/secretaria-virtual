---
name: sdd-debug-app
description: Dado el `implementation/tests.md` de una iniciativa SDD (por defecto el de la carpeta `.sdd/drafts/` más nueva), ejecuta cada test E2E Given/When/Then contra la aplicación real lanzando un subagente por test; cuando un test falla, corrige el código Java, reinicia la app y reintenta (LIMIT 3 por test). La primera vez un test se ejecuta con `playwright-cli` y, al pasar, se **cachea como un `.spec.ts`** (lo autora el subagente `playwright-test-generator` y, si la validación con `npx` falla, lo sana el subagente `playwright-test-healer` en un bucle mixto de hasta 5 intentos) en `implementation/test_e2e/`; las siguientes veces ese test se ejecuta directamente con `npx playwright test` (mucho más rápido, sin bucle LLM↔navegador). Va anotando el progreso test a test en un fichero JSON Lines reanudable (`implementation/progress.jsonl`): si se interrumpe, al relanzarlo salta los tests ya resueltos y sigue por el primero pendiente. Termina con un listado PASS/FAIL de todos los tests. Es una herramienta de depuración E2E independiente del pipeline; modifica código en `src/main/...` y mantiene los artefactos de progreso/reporte/spec en `implementation/`.
allowed-tools: Bash(playwright-cli:*) Bash(npx:*) Bash(curl:*) Bash(./run.sh:*) Bash(./gradlew:*) Bash(fuser:*) Bash(lsof:*) Bash(kill:*) Bash(xargs:*) Bash(ls:*) Bash(find:*) Bash(grep:*) Bash(printf:*) Bash(mkdir:*) Bash(mv:*) Bash(cp:*) Bash(rm:*) Read Write(.sdd/**) Skill AskUserQuestion Agent Monitor mcp__intellij-index__ide_search_text mcp__intellij-index__ide_find_class mcp__intellij-index__ide_find_file mcp__intellij-index__ide_find_definition mcp__intellij-index__ide_find_references mcp__intellij-index__ide_find_implementations mcp__intellij-index__ide_find_super_methods mcp__intellij-index__ide_call_hierarchy mcp__intellij-index__ide_type_hierarchy mcp__intellij-index__ide_diagnostics mcp__intellij-index__ide_index_status mcp__intellij-index__ide_sync_files
---

# sdd-debug-app

Eres un orquestador de depuración E2E. Lees un `tests.md`, ejecutas cada test contra la aplicación real **delegando la ejecución en un subagente** (uno por test), y cuando un test falla **corriges el código** del proyecto, reinicias la app y reintentas hasta que pase o se agote el `**LIMIT**`. Al final muestras un listado con `PASS`/`FAIL` por cada test.

Cada test se ejecuta de una de dos formas, según exista o no su `.spec.ts` cacheado:

- **Sin spec cacheado** (primera vez, o tras invalidarlo): el subagente lo ejecuta con `playwright-cli` interpretando el Given/When/Then. Si pasa, **se genera y cachea un `.spec.ts`** (paso 6.4) para futuras ejecuciones.
- **Con spec cacheado**: el subagente lo ejecuta directamente con `npx playwright test` contra ese fichero — sin bucle LLM↔navegador, mucho más rápido. Este es el motivo de existir del caché: pasar de nuevo un test ya resuelto no debe repetir el coste de pilotar el navegador paso a paso.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- Una **ruta explícita** a un `tests.md` (p.ej. `.sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md`). Se valida que existe y se entra directamente en la Fase 1.
- Un **identificador de test concreto** (`T-007`) o una lista (`T-001 T-007`) para depurar solo esos. Sin esto, se ejecutan **todos**.
- **Sin argumentos** → auto-detección de la Fase 0 (carpeta `.sdd/drafts/` más nueva que contenga `implementation/tests.md`).
- Overrides de testing del Apéndice A (`--in=`, `--root=`).

---

## Outline

1. **Localizar** el `tests.md` (Fase 0 — ruta explícita o auto-detección de la carpeta más nueva).
2. **Parsear** el `tests.md` y **cargar el progreso** (`progress.jsonl`): extraer la **cabecera común** y la lista de tests, y descartar los tests ya resueltos en una ejecución anterior (Fase 1).
3. **Ejecutar cada test pendiente** en bucle secuencial, un subagente por test (Fase 2):
   - Subagente arranca la app si está parada y ejecuta el test: con `npx playwright test` si existe su `.spec.ts` cacheado, o con `playwright-cli` interpretando el Given/When/Then si no. Devuelve `PASS`/`FAIL`.
   - Si `FAIL`: diagnosticar → delegar la corrección en `code-implementer` → parar app → compilar → reintentar (`**LIMIT**`: 3 intentos por test).
   - Si `PASS` ejecutado con `playwright-cli` y aún **no** hay `.spec.ts`: autorarlo con `playwright-test-generator`, **validarlo con `npx`** y, si falla, **sanarlo con `playwright-test-healer`** (es problema del spec, no del código); `**LIMIT**` 5 intentos de sanado (Fase 2.4).
   - Al alcanzar un resultado terminal (`PASS` o `FAIL` definitivo), **añadir una línea** a `progress.jsonl` antes de pasar al siguiente test.
4. **Reportar** el listado final `PASS`/`FAIL` de todos los tests, generado desde `progress.jsonl` (Fase 3).

**STOP conditions**:

- El `tests.md` no existe o no contiene ningún bloque `## T-NNN` → **ERROR** y detente sin ejecutar nada.
- La app no levanta a `200` en `http://localhost:8080` tras arrancarla (compila pero no responde) → **STOP** y `AskUserQuestion` (reintentar / ver log / abortar).
- El proyecto no compila tras `**LIMIT**: 3` correcciones de compilación dentro de un intento → ese intento cuenta como fallido; al agotar los 3 intentos del test se marca `FAIL` y se pasa al siguiente.
- Una corrección requeriría tocar XML de dominios/vistas materializados o cambiar el contrato del diseño → **STOP** y pregunta al usuario (no es trabajo de este skill; eso vuelve a `/sdd-designer-system`).
- No se logra generar un `.spec.ts` que pase tras `**LIMIT**: 5` intentos aunque el test pasa con `playwright-cli` → **STOP** y pregunta al usuario (algo anómalo; ver Fase 2.4).

---

## 1. Entrada y salida

### 1.1 Entrada

Un único `tests.md` con la estructura del contrato SDD: una **cabecera común** (todo lo anterior al primer `## T-`) seguida de bloques `## T-NNN — <nombre>` con `Precondiciones`, `Pasos` y `Resultado esperado`.

### 1.2 Salida

- En el árbol del proyecto (`src/main/java/com/educaflow/...`): las correcciones de código Java necesarias para que los tests pasen.
- Un fichero de log de la app en `.sdd/drafts/{iniciativa}/implementation/app-debug.log` (lo lees al diagnosticar fallos).
- El **checkpoint de progreso** en `.sdd/drafts/{iniciativa}/implementation/progress.jsonl` (JSON Lines, una línea por test resuelto; se va escribiendo durante la Fase 2 y permite reanudar — ver principio 2.7).
- Los **`.spec.ts` cacheados** en `.sdd/drafts/{iniciativa}/implementation/test_e2e/` (uno por test que pasó vía `playwright-cli`; permiten reejecutar ese test con `npx playwright test` — ver principio 2.8).
- El reporte final en `.sdd/drafts/{iniciativa}/implementation/debug-report.md` (Fase 3, generado desde `progress.jsonl`).
- En la conversación: el mismo listado final `PASS`/`FAIL` por test (Fase 3).

**MUST NOT** escribir ni modificar `tests.md`, ni ningún artefacto de `analysis/` o `design/`. Este skill solo lee `tests.md` y corrige código; `progress.jsonl`, `debug-report.md` y `test_e2e/*.spec.ts` son artefactos propios de depuración.

### 1.3 Estructura de carpetas

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── implementation/
    ├── tests.md            ← entrada (contrato fijo, NO se modifica)
    ├── app-debug.log       ← log de la app, escrito al arrancarla
    ├── progress.jsonl      ← checkpoint reanudable (una línea JSON por test resuelto)
    ├── test_e2e/           ← `.spec.ts` cacheados (un fichero por test que pasó vía playwright-cli)
    │   ├── T-001_alta-manual-de-un-correo-sin-adjuntos.spec.ts
    │   └── ...
    └── debug-report.md     ← salida de la Fase 3 (listado PASS/FAIL)

playwright.sdd.config.ts          ← config dedicada (testDir → .sdd) para ejecutar los specs de arriba
src/main/java/com/educaflow/...   ← código Java que se corrige
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 Gestión de la app idempotente y por puerto

La app se considera **levantada** si `http://localhost:8080` responde `200`. Comprobación:

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
```

- **Arrancar** (cuando no responde `200`): usa **siempre** `./run.sh` (compila con `./gradlew clean build` y arranca la app en el 8080 con la configuración correcta; ver `CLAUDE.md`). **MUST NOT** invocar `gradlew run` a mano ni añadir `--debug-jvm` (ese flag suspende la JVM esperando a que se conecte un depurador y la app nunca llega a `200`). Redirige el log a fichero (dentro del proyecto, escribible) y lanza en segundo plano:
  ```bash
  ./run.sh > .sdd/drafts/{iniciativa}/implementation/app-debug.log 2>&1
  ```
  Lánzalo con `run_in_background: true` y **espera** sondeando `curl` hasta que devuelva `200` (sondeo con un bucle `Monitor`/reintentos; **LIMIT**: ~300 s, porque `run.sh` hace `clean build` antes de arrancar). Si no llega a `200`, **STOP** (ver STOP conditions).
- **CRITICAL**: `./run.sh` **MUST** poder ejecutarse **fuera del sandbox** (entrada en `sandbox.excludedCommands` de `.claude/settings.local.json`), porque al compilar escribe en `~/.gradle`, fuera del directorio de trabajo; sandboxeado falla con "sistema de archivos de solo lectura".
- **Parar**: siempre por puerto, **nunca** por handle de proceso (el subagente y el orquestador son contextos distintos; matar por puerto funciona en ambos):
  ```bash
  fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
  ```

**CRITICAL**: el arranque es **idempotente** — quien necesite la app comprueba el `200` y solo arranca si está parada. **MUST NOT** arrancar una segunda instancia si ya responde `200`.

### 2.2 El runner ejecuta, el orquestador diagnostica, `code-implementer` corrige

El subagente runner **solo ejecuta el test** y reporta `PASS`/`FAIL` con una descripción del fallo. **MUST NOT** modificar código. La corrección de un fallo se reparte en dos responsabilidades:

- **Diagnóstico** (lo hace el orquestador, este skill): a partir de la **descripción del fallo** que devuelve el runner y del **log de la app** (`implementation/app-debug.log`), localiza la causa en el código con el MCP de IntelliJ.
- **Escritura del fix** (se delega en `code-implementer`): el orquestador construye un **plan de corrección pequeño** y lo pasa a `code-implementer` con la herramienta `Skill`, junto con los skills de dominio aplicables. **MUST NOT** escribir el fix tú mismo con `Edit`: el código del proyecto se escribe a través de `code-implementer` —que implementa, verifica y revisa cada paso— igual que en `/sdd-implementer-system`.

**CRITICAL**: el plan de corrección **MUST** incluir `k-secure-coding` y `k-code-quality` entre los skills cuando toque entidades, servicios o controladores. Las correcciones **MUST NOT** introducir mass-assignment, saltarse `AllowProperties` ni la asignación incondicional de campos `servidor`.

### 2.3 No tocar el contrato — solo código Java

**MUST NOT** modificar `tests.md` para que un test pase (eso es trampa). **MUST NOT** editar XML de dominios o vistas materializados ni cambiar firmas declaradas por el diseño para cuadrar un test: si el fallo exige eso, **STOP** y pregunta al usuario — la corrección vuelve a `/sdd-designer-system`. Este skill corrige **lógica Java** (servicios, controladores, repositorios, validaciones, jobs, datos iniciales).

### 2.4 Un subagente por test, en secuencia

Cada test lo ejecuta **un subagente** lanzado con `Agent` (`subagent_type: claude`). **CRITICAL**: los subagentes se lanzan **estrictamente en secuencia** (uno, esperas su resultado, luego el siguiente) — **MUST NOT** lanzar varios en paralelo (comparten el puerto 8080 y se pisarían). **MUST NOT** usar `run_in_background` en el `Agent`: necesitas el `PASS`/`FAIL` antes de continuar. El subagente **MUST NOT** usar `AskUserQuestion`.

### 2.5 Detenerse y preguntar ante un bloqueo

`AskUserQuestion` solo para lo imprescindible: confirmación de la ruta auto-detectada (Fase 0), app que no levanta a `200`, y bloqueos del principio 2.3. **MUST NOT** adivinar soluciones ni pedir aprobaciones cosméticas.

### 2.6 Mensajes de validación: equivalencia semántica, no literal

Cuando el "Resultado esperado" de un test cita el **texto de un mensaje de validación, error o aviso**, el criterio de éxito es la **equivalencia semántica**, no la coincidencia literal carácter a carácter. Si el sistema rechaza la operación en el momento esperado y muestra un mensaje **que comunica la misma causa** (p. ej. que un campo obligatorio falta), el test es **PASS** aunque la redacción difiera.

- ✅ PASS: esperado "El DNI del destinatario es obligatorio." / observado "DNI destinatario es requerido" (misma causa: el DNI es obligatorio; lo emite la validación `required` de Axelor en el cliente).
- ✅ PASS: esperado "La fecha final no puede ser anterior a la fecha inicial." / observado "La fecha de fin debe ser posterior a la de inicio" (misma causa).
- ❌ FAIL: esperado un mensaje de DNI obligatorio / observado un mensaje sobre el email, o ningún mensaje, o la operación **no** se rechaza (causa distinta o comportamiento incorrecto).

**CRITICAL**: esto aplica **solo al texto del mensaje**. El resto del "Resultado esperado" (que la operación se rechace o complete, el estado en que queda la entidad, los campos afectados) **MUST** cumplirse exactamente. **MUST NOT** usar la equivalencia semántica para dar por bueno un comportamiento funcional distinto del esperado.

### 2.7 Checkpoint de progreso reanudable (JSON Lines)

El progreso se persiste en `.sdd/drafts/{iniciativa}/implementation/progress.jsonl` en formato **JSON Lines** (un objeto JSON por línea, sin coma final ni envoltorio de array). El formato es **append-only**: cada test que alcanza un resultado **terminal** (`PASS` o `FAIL` definitivo tras agotar intentos) añade **exactamente una** línea. Así el fichero se puede escribir incrementalmente y releer para reanudar sin reescribirlo entero.

**El test es la unidad atómica de checkpoint.** Solo se escribe la línea cuando el test queda resuelto. Si el skill se interrumpe **a mitad** de un test (p. ej. durante el bucle de corrección de un `FAIL`), ese test **no** tiene línea → al reanudar se **reejecuta entero desde el intento 1**. **MUST NOT** persistir estados intermedios (`EN_PROGRESO`, nº de intento parcial): el código ya corregido permanece en disco, pero el test se vuelve a verificar de cero.

Esquema de cada línea (campos en este orden):

```jsonl
{"id":"T-001","nombre":"Alta manual de un Correo sin adjuntos","resultado":"PASS","intentos":0,"detalle":null,"ts":"2026-05-25T00:45:00"}
{"id":"T-009","nombre":"Envío automático con éxito","resultado":"FAIL","intentos":3,"detalle":"El estado quedó en PENDIENTE tras 70 s; la tarea periódica no procesó el correo.","ts":"2026-05-25T00:52:00"}
```

- `id` — identificador `T-NNN` del test.
- `nombre` — nombre del test (el del encabezado `## T-NNN — <nombre>`).
- `resultado` — `"PASS"` o `"FAIL"`. **MUST NOT** usar otros valores.
- `intentos` — nº de correcciones aplicadas antes de resolver (`0` si pasó a la primera; hasta `3`).
- `detalle` — `null` si `PASS`; string con la última descripción del fallo si `FAIL` (los saltos de línea van escapados como `\n` dentro del string JSON).
- `ts` — instante de resolución en ISO 8601.

Reglas de escritura:

- **REQUIRED**: una línea por test, **añadida** (no reescrita) al resolverse el test, antes de pasar al siguiente.
- **MUST NOT** escribir un array JSON ni JSON multilínea: cada test es una línea independiente y autocontenida.
- **MUST NOT** volver a escribir un test que ya tiene línea (no hay duplicados: un test resuelto se salta en la reanudación).

- ✅ CORRECTO: `{"id":"T-001","nombre":"...","resultado":"PASS","intentos":0,"detalle":null,"ts":"2026-05-25T00:45:00"}` (objeto en una sola línea).
- ❌ INCORRECTO: `[{"id":"T-001",...}, {"id":"T-002",...}]` (es un array; no es JSON Lines y obliga a reescribir el fichero).
- ❌ INCORRECTO: objeto repartido en varias líneas con saltos reales dentro de `detalle` (rompe el parseo línea a línea; los saltos van como `\n`).
- ❌ INCORRECTO: `{"id":"T-009","resultado":"EN_PROGRESO","intentos":2,...}` (estado intermedio; viola la atomicidad del checkpoint).

### 2.8 Caché de tests como `.spec.ts` (acelera reejecuciones)

El bucle LLM↔navegador de `playwright-cli` es lento porque cada paso (snapshot → razonar → actuar) es una ronda de inferencia. Para no pagar ese coste cada vez que se reejecuta un test ya resuelto, **cuando un test pasa por primera vez con `playwright-cli` se materializa como un `.spec.ts`** ejecutable. Las siguientes veces ese test se ejecuta con `npx playwright test`, que no necesita modelo: corre el navegador a velocidad de máquina.

- **Ubicación**: `.sdd/drafts/{iniciativa}/implementation/test_e2e/`.
- **Nombre de fichero**: `{id}_{slug}.spec.ts`, donde `{id}` es el `T-NNN` y `{slug}` es el nombre del test en kebab-case (minúsculas, sin acentos, espacios y signos → `-`). Ejemplo: el test `## T-001 — Alta manual de un Correo sin adjuntos` → `T-001_alta-manual-de-un-correo-sin-adjuntos.spec.ts`. El prefijo `T-NNN` es el ancla estable: el orquestador comprueba existencia y construye el nombre a partir del `id`, que no cambia aunque se reescriba el nombre.
- **Ejecución**: con la config dedicada `playwright.sdd.config.ts` (su `testDir` es `.sdd`, porque la config base solo descubre `tests/`) y reporter no bloqueante. **MUST NOT** usar la config base ni el reporter `html`: al acabar levanta un servidor y **bloquea el comando indefinidamente**.
  ```bash
  PLAYWRIGHT_HTML_OPEN=never npx playwright test --config=playwright.sdd.config.ts "<ruta-relativa-del-spec>" --project=chromium --reporter=line
  ```
- **El generator escribe bajo `tests/`** (CRITICAL): `generator_write_test` ignora la ruta `<test-file>` y escribe en el `testDir` del MCP (`tests/...`); el generator no tiene `Write`/`Edit`. El orquestador **MUST** localizar el fichero bajo `tests/` y dejarlo ahí mientras se sana (el MCP del healer solo ve `tests/`); al pasar la validación autoritativa lo **promueve** a `test_e2e/` (ver Fase 2.4), dejando `tests/` limpio.
- **Quién autora y quién sana el spec**: el subagente `playwright-test-generator` **autora** el spec una vez (Fase 2.4); si la validación falla, el subagente `playwright-test-healer` lo **sana** con ediciones quirúrgicas (locators/asserts/esperas) sobre el `.spec.ts`, sin tocar la app. **MUST NOT** escribir ni arreglar el spec tú mismo con `Write`/`Edit`: el generator pilota el navegador real y el healer depura el test fallido con sus herramientas MCP.
- **El runner instruye al generator** (CRITICAL): el generator solo conoce el Given/When/Then de negocio, así que **adivina** cómo navegar y se equivoca (la app no tiene URLs "amigables"). Para evitarlo, el `playwright-cli` runner devuelve al pasar una `=== RECETA DE EJECUCIÓN ===` (navegación real paso a paso, URLs técnicas reales, locators que funcionaron) y el orquestador **MUST** pasársela al generator como fuente autoritativa a reproducir. Sin la receta, el spec no navega.
- **Cuándo se materializa**: **solo** si el test alcanzó `PASS` ejecutado con `playwright-cli` **y** aún no existe su `.spec.ts`. Si pasó ejecutado ya desde el spec (vía `npx`), **MUST NOT** rehacerlo.
- **Validación autoritativa + sanado (LIMIT 5)**: autorar el fichero no basta. Tras autorarlo se **valida** con `npx playwright test` (config dedicada). Como el código ya está probado por `playwright-cli`, un fallo es **del spec**, no del código → se **sana con `playwright-test-healer`** (no se toca el código), hasta `**LIMIT**: 5` intentos de sanado (sistema mixto: el healer itera dentro de cada intento; el orquestador reintenta si la validación autoritativa sigue fallando). Si tras 5 sigue sin pasar → **STOP** (Fase 2.4): algo anómalo ocurre.

**Invalidación (spec obsoleto)**: si un test que se ejecutó **desde su spec cacheado** falla, entra en el bucle de corrección normal. Si al diagnosticar se concluye que la causa **no es un defecto del código** sino que el spec quedó desfasado (la pantalla/flujo cambió legítimamente respecto a cuando se generó), **borra el spec** y deja que el test se reejecute con `playwright-cli` (se regenerará al volver a pasar):
```bash
find .sdd/drafts/{iniciativa}/implementation/test_e2e -name '{id}_*.spec.ts' -delete
```
**MUST NOT** "arreglar a mano" un spec para ocultar un cambio de la app ni borrarlo para esquivar un fallo real del código.

**`--no-cache`** (Apéndice A) desactiva por completo este mecanismo: ni genera ni usa specs, todo va por `playwright-cli` como antes.

### 2.9 Fallos recurrentes al autorar/validar specs (esperas, caché de la SPA, editores)

Estos fallos NO son del código de la app —el `playwright-cli` runner ya lo probó—, sino del **mecanismo del spec**. El orquestador **MUST** prevenirlos en el prompt del generator/healer y **MUST** reconocerlos al diagnosticar una validación que falla o que se queda colgada.

1. **Esperar el VALUE de un input cuelga el pilotaje.** Los campos que rellena el servidor (autocompletados; p. ej. el email que se rellena al teclear el DNI) son el `value` de un `<input>`, **no** texto visible del DOM. `browser_wait_for(text: ...)` / `verify_text` sobre ese valor **no resuelve nunca** y cuelga al generator/healer.
   - **MUST** instruir: para comprobar el valor de un input usar `browser_verify_value` (genera `toHaveValue`), **NUNCA** `browser_wait_for`/`verify_text` sobre el valor.
   - Si ese valor es **incidental** (no es lo que el test verifica): **MUST** instruir explícitamente que **NO** lo espere ni lo compruebe — teclear y seguir.
   - ✅ CORRECTO: `await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('lorenzo.profesor@gmail.com')`.
   - ❌ INCORRECTO: `browser_wait_for(text: "lorenzo.profesor@gmail.com")` (es un value de input, no texto visible → cuelga indefinidamente).

2. **La SPA de Axelor cachea el formulario abierto y el grid.** Con routing por hash, `page.goto` a la **misma** URL solo cambia el hash y **NO** recarga: re-navegar al mismo detalle o a la misma lista sirve datos cacheados y nunca refleja un cambio hecho en el servidor. Para sondear un cambio **asíncrono** (p. ej. que la tarea periódica pase un registro de PENDIENTE a ENVIADO/FALLIDO) el polling **MUST** usar una fuente que re-consulte de verdad:
   - **REST autenticado** con `page.request` (comparte las cookies de la sesión del navegador) contra `/ws/rest/<FQN>/search` filtrando por un campo único — fuente determinista de la BD; o
   - `page.reload()` (recarga dura que reinicia la SPA y vuelve a fetchear).
   - **MUST NOT** sondear con `page.goto(mismaUrlDetalle)` ni `page.goto(mismaUrlLista)`: ambos quedan cacheados (el grid además puede paginar y no mostrar el registro nuevo).

3. **Toda espera MUST llevar timeout acotado.** **MUST NOT** introducir esperas indefinidas.
   - En el spec: usar `await expect.poll(fn, { timeout, intervals })`; y `test.setTimeout(300_000)` cuando la espera supere el default de 30 s de Playwright (típico en tests que dependen del cron).
   - En el `playwright-cli` runner: cualquier `wait_for` lleva timeout explícito, y los comandos `npx`/sondeos por shell se lanzan con `timeout` acotado.
   - ✅ CORRECTO: `await expect.poll(async () => (await rest()).estado, { timeout: 240_000, intervals: [5000, 10000, 15000] }).toBe('ENVIADO')`.
   - ❌ INCORRECTO: un bucle de espera sin `timeout` o un `wait_for` sin límite (si el cambio no llega, el test cuelga hasta el timeout global y desperdicia minutos).

4. **El editor de cuerpo es un `contenteditable`: `.fill()` no sincroniza el modelo.** Rellenar `.custom-html-editor-content` con `.fill()` deja el campo vacío de cara al servidor → el alta se rechaza por validación y el registro no se crea. **MUST** instruir: `await locator.click()` + `page.keyboard.type('…')`.

### 2.10 Las correcciones del spec van por el healer; las del código por code-implementer

Al diagnosticar una validación fallida (Fase 2.4), clasifica la causa **antes** de actuar:

- Causa en el **spec** (locator frágil/ambiguo, assert mal traducido, espera mal hecha, navegación adivinada, los fallos de §2.9) → **MUST** sanarla con `playwright-test-healer`. **MUST NOT** tocar el código de la app.
- Causa en el **código** (un test que se ejecutaba y el comportamiento es incorrecto) → bucle de corrección de la Fase 2.3 vía `code-implementer`.

**MUST NOT** editar el `.spec.ts` tú mismo con `Edit`/`Write` salvo un arreglo **mecánico e inequívoco** de una sola línea (p. ej. añadir un `await page.goto('/')` inicial que el generator omitió) cuando una ronda completa del generator/healer sería desproporcionada; en ese caso, deja constancia del arreglo manual. Cualquier cambio no trivial **MUST** ir por el healer.

---

## 3. Flujo general

```
┌────────────────────────────────────────────────────────────────────┐
│  Fase 0   Localizar tests.md (ruta explícita o carpeta más nueva)   │
│  Fase 1   Parsear: cabecera común + lista de tests                  │
│           + cargar progress.jsonl → descartar tests ya resueltos    │
│  Fase 2   Por cada test PENDIENTE (en secuencia):                   │
│             ├─ Runner: ¿existe .spec.ts? → npx | playwright-cli     │
│             │            ──► PASS | FAIL+descripción                │
│             ├─ PASS vía playwright-cli y sin spec → autorar spec    │
│             │   (generator, bajo tests/) → validar (npx --config sdd)│
│             │   → si falla, SANAR con healer (LIMIT 5; es el spec,  │
│             │   no el código) → al pasar, promover a test_e2e/      │
│             ├─ PASS → append a progress.jsonl y siguiente test      │
│             └─ FAIL → bucle de corrección (LIMIT 3 intentos):       │
│                  diagnosticar → code-implementer → parar app →      │
│                  compilar → reintentar; al resolver, append         │
│  Fase 3   Reporte final PASS/FAIL desde progress.jsonl              │
└────────────────────────────────────────────────────────────────────┘
```

Las fases se ejecutan **en orden**. Dentro de la Fase 2 los tests se procesan en el orden en que aparecen en `tests.md`.

---

## 4. Fase 0 — Localizar el `tests.md`

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca con una ruta a un `tests.md`: comprueba que existe; la **carpeta de la iniciativa** es la que contiene su `implementation/`. Pasa a la Fase 1.

### 4.2 Caso 2 — Sin ruta (auto-detección)

1. Lista las carpetas con formato de iniciativa:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordena alfabéticamente (el prefijo timestamp hace que coincida con el orden cronológico) y toma la **última**.
3. Comprueba que contiene `implementation/tests.md`. Si no, indica al usuario que no hay tests disponibles y pide una ruta. Detente.
4. Muestra la ruta detectada y confirma con `AskUserQuestion` (usar / dar otra ruta).

**MUST NOT** usar `mtime` ni elegir una carpeta que no sea la última por orden alfabético del timestamp.

---

## 5. Fase 1 — Parsear el `tests.md`

1. Lee **todo** el `tests.md` con la herramienta `Read`.
2. **Cabecera común**: todo el texto desde el inicio hasta justo antes del primer `## T-`. Guárdalo **verbatim**: explica la convención de independencia, el fixture de identidad y la tarea periódica, y es contexto imprescindible para el subagente.
3. **Lista de tests**: cada bloque que empieza por `## T-NNN — <nombre>` hasta el siguiente `## T-` (o el final). Para cada uno guarda: el `id` (`T-NNN`), el `nombre`, y el **bloque verbatim** completo (incluyendo `Precondiciones`, `Pasos`, `Resultado esperado`).
4. Si el usuario pasó ids concretos (`T-007`), filtra la lista a esos. Si no, usa todos.
5. Si no hay ningún bloque `## T-` → **ERROR** y detente.
6. **Cargar el progreso** (principio 2.7): si existe `.sdd/drafts/{iniciativa}/implementation/progress.jsonl`, léelo con `Read`, parsea cada línea como un objeto JSON y construye el conjunto de `id` ya resueltos. **Descarta** de la lista de tests esos `id`: la Fase 2 solo recorre los **pendientes**. Si el fichero no existe o está vacío, todos los tests están pendientes. Si se pasó `--fresh`, **ignora** el fichero existente (ver Apéndice A). Indica al usuario cuántos tests se saltan por estar ya resueltos y cuántos quedan pendientes.

---

## 6. Fase 2 — Ejecutar cada test

Recorre la lista de **tests pendientes** (los que no están ya resueltos en `progress.jsonl`, ver Fase 1) **en orden**. Para cada test:

### 6.1 Lanzar el subagente runner

Antes de lanzar el runner, el orquestador **MUST** calcular el `{spec_relpath}` del test (`.sdd/drafts/{iniciativa}/implementation/test_e2e/{id}_{slug}.spec.ts`, ver principio 2.8) y **comprobar si ya existe** (`ls`/`find`). Registra ese dato (`spec_existía = sí|no`): lo necesitarás en la Fase 2.4 para decidir si generar el spec. Pasa al runner `{usar_cache}` = `sí` (o `no` si se invocó con `--no-cache`).

Lanza **un** subagente con `Agent` (`subagent_type: claude`, `run_in_background: false`). El prompt **MUST** construirse con **exactamente** esta plantilla, sustituyendo los `{...}`:

```
Eres un runner de un único test E2E de la secretaría virtual (Axelor). NO modifiques código fuente: solo ejecutas el test y reportas el resultado. CRITICAL: ejecuta TODOS los comandos desde la raíz del proyecto ({raíz del proyecto}), NO desde un worktree: el `.spec.ts` cacheado, `playwright.sdd.config.ts` y `node_modules` están ahí y pueden estar sin commitear (en un worktree no existirían y el MODO spec fallaría).

PASOS OBLIGATORIOS, en orden:
1. Carga el skill `playwright-cli` con la herramienta Skill.
2. Muestra un mensaje indicando qué test estás probando: "Probando {id} — {nombre}".
3. Comprueba si la app responde 200:
     curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
   Si NO responde 200, arráncala con `./run.sh` (compila y arranca; NO uses `gradlew run` a mano ni `--debug-jvm`) en segundo plano (run_in_background: true) y espera (sondeando curl, hasta ~300 s, porque `run.sh` hace `clean build` primero) a que responda 200:
     ./run.sh > {ruta-app-debug.log} 2>&1
   Si tras ~300 s no responde 200, devuelve FAIL con motivo "la aplicación no levanta".
4. DECIDE EL MODO DE EJECUCIÓN:
   - Si {usar_cache} es "sí" Y existe el fichero {spec_relpath} (compruébalo con: test -f {spec_relpath} && echo EXISTE), ejecuta el test en MODO spec (paso 5a).
   - En cualquier otro caso, ejecuta el test en MODO cli (paso 5b).
5a. MODO spec — ejecuta el `.spec.ts` cacheado, sin pilotar el navegador a mano. CRITICAL: usa SIEMPRE la config dedicada y reporter no bloqueante; NO uses la config base ni el reporter `html` (levanta un servidor y bloquea el comando para siempre):
      PLAYWRIGHT_HTML_OPEN=never npx playwright test --config=playwright.sdd.config.ts "{spec_relpath}" --project=chromium --reporter=line
    El test PASA si el comando termina con código de salida 0; FALLA en otro caso. Para el detalle del fallo usa la salida del reporter `line` (test/línea que falló, assert, error) y menciona la traza retenida en `test-results/` si la hay. En MODO spec NO necesitas `playwright-cli`.
5b. MODO cli — ejecuta el test con `playwright-cli`: interpreta el Given/When/Then del bloque del test (precondiciones, pasos y resultado esperado) y condúcelo en el navegador (login, navegación, alta de datos, asserts). Usa snapshots para localizar refs. La URL base es http://localhost:8080. CRITERIO PARA TEXTOS DE MENSAJES: cuando el resultado esperado cita el texto de un mensaje de validación/error/aviso, basta con la EQUIVALENCIA SEMÁNTICA, no la coincidencia literal. Si el sistema rechaza la operación en el momento esperado y el mensaje comunica la misma causa, es PASS aunque la redacción difiera (p. ej. esperado "El DNI del destinatario es obligatorio." y observado "DNI destinatario es requerido" → PASS). Esto aplica SOLO al texto del mensaje: el resto del resultado esperado (que la operación se rechace/complete, el estado de la entidad, los campos afectados) debe cumplirse exactamente. Cierra el navegador al terminar (playwright-cli close).

FORMATO DE SALIDA OBLIGATORIO (y nada más):
- Primera línea exactamente `PASS {id}` o `FAIL {id}`.
- Segunda línea exactamente `MODO spec` o `MODO cli` (el modo en que ejecutaste).
- Si PASA en MODO cli: tras las dos líneas anteriores, añade un bloque `=== RECETA DE EJECUCIÓN ===` con el detalle CONCRETO y reproducible de lo que hiciste y FUNCIONÓ, para que otro lo encodee en un `.spec.ts` SIN tener que redescubrir nada. CRITICAL: la app no tiene URLs "amigables"; las rutas son técnicas (p. ej. `#/ds/subsysCorreos.Correo%40Todos-action/list/1`). NO inventes URLs. Incluye:
    * NAVEGACIÓN: cómo llegaste a cada pantalla, paso a paso (qué entradas de menú/pestañas/botones de toolbar pulsaste, EN ORDEN) y, tras cada navegación, la URL REAL que quedó en la barra (cópiala literal con `playwright-cli eval "location.hash"` o equivalente). Si navegaste pulsando menú, dilo explícitamente (es lo robusto; el `.spec.ts` debe pulsar el menú, no teclear una URL adivinada).
    * LOCATORS: para cada campo/botón con el que interactuaste, el locator robusto que funcionó (rol+nombre, label, placeholder o `data-testid`) y el valor que introdujiste.
    * VERIFICACIÓN: cómo comprobaste cada punto del "Resultado esperado" (qué elemento leíste y qué valor mostraba).
- Si FALLA en MODO cli: ANTES de reportar, recoge con `playwright-cli` toda la información posible que muestre la interfaz en el momento del fallo: `snapshot` de la pantalla (o del panel relevante), texto de mensajes/alertas/toasts de error visibles, valores de los campos implicados, URL y título de la página, y `console` y `requests` (errores de consola y peticiones fallidas con su status/cuerpo). A continuación, una descripción concreta de qué falló (paso, valor esperado vs observado) con esa información de la UI.
- Si FALLA en MODO spec: incluye la salida relevante de `npx playwright test` (test que falló, assert/error, y ruta de la traza si existe).

NO uses AskUserQuestion. NO modifiques ficheros del proyecto.

=== CABECERA COMÚN DE LOS TESTS (contexto) ===
{cabecera común verbatim}

=== TEST A EJECUTAR ===
{bloque verbatim del test}
```

Donde `{ruta-app-debug.log}` es `.sdd/drafts/{iniciativa}/implementation/app-debug.log` y `{spec_relpath}` la ruta calculada arriba.

### 6.2 Interpretar el resultado

- Si la primera línea es `PASS {id}` → registra el test como **PASS**. Si el modo fue `MODO cli`, **retén el bloque `=== RECETA DE EJECUCIÓN ===`** que devolvió el runner: es la entrada autoritativa de la Fase 2.4. Luego **genera el `.spec.ts` si procede** (paso 6.4), **escribe su línea en `progress.jsonl`** (paso 6.5) y pasa al siguiente test (vuelve a 6.1 con el siguiente).
- Si la primera línea es `FAIL {id}` → entra en el **bucle de corrección** (6.3) con la descripción devuelta. Si el runner reportó `MODO spec`, ten en cuenta la **invalidación de spec obsoleto** del principio 2.8 al diagnosticar.

### 6.3 Bucle de corrección (solo si FAIL)

**Variables**: `**LIMIT**: max_intentos = 3`, `intento = 1`.

Cada **intento**:

1. **Diagnostica**: lee la descripción del fallo del subagente y el log de la app (`Read` sobre `implementation/app-debug.log`, normalmente el final). Localiza la causa en el código con los tools del **MCP de IntelliJ** (`ide_search_text`, `ide_find_class`, `ide_find_references`, …), NO con grep/find.
2. **Delega la corrección en `code-implementer`** (principio 2.2). Construye un **plan de corrección pequeño** en markdown: un paso por causa localizada (fichero/clase, qué cambiar y por qué), con la descripción del fallo y el extracto relevante del log; **MUST NOT** volver a pasar todos los tests, solo lo que hay que arreglar. Invoca `code-implementer` con la herramienta `Skill` pasándole ese plan y los skills de dominio aplicables (`k-secure-coding` + `k-code-quality` si toca entidades/servicios/controladores; además `k-sistemas`, `k-scheduler`, `k-i18n`, etc. según el fallo). **MUST NOT** escribir el fix tú mismo con `Edit`. Si la corrección exigiera tocar XML materializado o el contrato del diseño → **STOP** y pregunta al usuario (principio 2.3).
3. **Para la app** por puerto (principio 2.1):
   ```bash
   fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
   ```
4. **Recompila y reinicia**: relanza el subagente runner (6.1) para ese mismo test. El runner arranca la app con `./run.sh`, que **recompila el fix** (`./gradlew clean build`) y la levanta en un solo paso. **MUST NOT** invocar `gradlew` a mano ni `--debug-jvm`. Como `run.sh` lleva `set -e`, si la compilación falla el script aborta y la app nunca responde `200`: el runner devuelve `FAIL` con motivo "la aplicación no levanta"; lee entonces el final de `implementation/app-debug.log` y, si ves `BUILD FAILED`, trata el intento como error de compilación y vuelve al paso 2 a corregirlo. **LIMIT**: máximo 3 ciclos de compilación dentro de un intento; si tras el 3º sigue sin compilar, este intento se da por fallido — ve al control de fin de intento.
5. **Resultado del relanzamiento**:
   - `PASS` → registra **PASS** con `intentos = intento` (correcciones aplicadas), **genera el `.spec.ts` si procede** (paso 6.4), **escribe su línea en `progress.jsonl`** (paso 6.5) y sal del bucle de corrección.
   - `FAIL` → **fin de intento**: incrementa `intento`. Si `intento <= max_intentos`, repite el intento desde el paso 1 con la nueva descripción. Si `intento > max_intentos`, registra el test como **FAIL** (con la última descripción), **escribe su línea en `progress.jsonl`** (paso 6.5) y pasa al siguiente test.

**MUST NOT** superar los 3 intentos por test. Tras agotarlos, el test queda **FAIL** y el bucle continúa con el resto.

### 6.4 Materializar el `.spec.ts`: autoría (generator) + sanado (healer), LIMIT 5

Tras un **PASS**, antes de escribir el checkpoint, el orquestador decide si materializa el test como `.spec.ts` (principio 2.8). **Genera el spec si y solo si** se cumplen las dos condiciones:

- `spec_existía` era `no` (no había `.spec.ts` para este `id` cuando se lanzó el runner), **y**
- el runner reportó `MODO cli` (el PASS se obtuvo pilotando el navegador, no desde un spec ya existente).

Si se invocó con `--no-cache`, **MUST NOT** generar nada (salta a 6.5). Si `spec_existía` era `sí` o el modo fue `spec`, tampoco hay que generar: el caché ya está.

**CRITICAL — premisa de este paso**: el código **ya es correcto** (`playwright-cli` acaba de probarlo). Por tanto, si el `.spec.ts` **no pasa**, el problema **NO es del código** sino del **propio spec** (locator frágil, assert mal traducido, paso mal grabado). La respuesta es **arreglar el spec** (sanándolo con el healer), NO tocar el código. **MUST NOT** invocar `code-implementer` ni abrir el bucle de corrección de código (6.3) desde aquí.

**División de responsabilidades** dentro de este paso:

- **`playwright-test-generator` AUTORA el spec una sola vez** a partir de la `RECETA DE EJECUCIÓN` (necesita la navegación real; ver 6.4.1).
- **`playwright-test-healer` SANA el spec** si la validación falla: hace ediciones quirúrgicas sobre el `.spec.ts` (locators, asserts, esperas) y re-ejecuta, sin tocar la app. **MUST NOT** regenerar el spec desde cero con el generator en los reintentos: una vez autorado, todos los arreglos los hace el healer.

La app **MUST** seguir respondiendo `200` durante todo este paso (lo está: el test acaba de pasar). Ni el generator ni el healer arrancan o paran la app.

**Dónde vive el spec mientras se sana (CRITICAL)**: las herramientas MCP del healer (`test_run`/`test_debug`) descubren los tests por el `testDir` del MCP, que es `tests/` (la config base), **NO** `.sdd/`; y **no** se les puede pasar `--config=playwright.sdd.config.ts`. Por eso la **copia de trabajo** del spec vive bajo `tests/` (justo donde `generator_write_test` lo escribe) durante todo el sanado, y solo se **promueve** a `implementation/test_e2e/` cuando pasa la validación autoritativa. La **validación autoritativa** —la que decide si el spec es bueno— **MUST** hacerse con `npx --config=playwright.sdd.config.ts`, porque es exactamente como se ejecutará el test en MODO spec (6.1, paso 5a).

Asegura la carpeta destino: `mkdir -p .sdd/drafts/{iniciativa}/implementation/test_e2e`.

#### 6.4.1 Autoría inicial (una vez)

**CRITICAL — el runner instruye al generator**: el `playwright-cli` runner **ya navegó con éxito** y devolvió en su `=== RECETA DE EJECUCIÓN ===` (paso 6.1) la navegación real, las URLs técnicas reales y los locators que funcionaron. El generator **MUST** reproducir **esa** receta, **NO** inventar su propia ruta. Sin la receta, el generator adivina URLs "amigables" inexistentes (p. ej. `#/ds/todos-correos/list` en vez de la real `#/ds/subsysCorreos.Correo%40Todos-action/list/1`) y el spec no navega. Por tanto, el orquestador **MUST** pasar la receta verbatim en el prompt del generator.

Lanza **un** subagente con `Agent` (`subagent_type: playwright-test-generator`, `run_in_background: false`) con esta plantilla. **MUST NOT** generar el spec tú mismo con `Write`/`Edit`.

```
Genera un test de Playwright a partir de este escenario YA VALIDADO a mano contra la app (http://localhost:8080). Otro agente lo ejecutó con éxito con playwright-cli y te pasa su RECETA DE EJECUCIÓN con la navegación, URLs y locators REALES que funcionaron.

REGLAS CRÍTICAS:
- Reproduce EXACTAMENTE la navegación de la RECETA. La app NO tiene URLs "amigables": NO inventes rutas. Si la receta llega a una pantalla pulsando entradas de menú/toolbar, haz lo mismo en el test (es lo robusto); si navega por URL, usa la URL técnica REAL de la receta (p. ej. `#/ds/subsysCorreos.Correo%40Todos-action/list/1`), no una inventada.
- Usa los locators de la RECETA (rol+nombre, label, placeholder o data-testid). NO uses ids autogenerados de Axelor (#x-123).
- El fichero debe ser autónomo y ejecutable: `import { test, expect } from '@playwright/test';`, un único `test()` dentro de un `describe`, y un comentario con el texto del paso antes de cada acción.
- El login es admin/admin; baseURL ya configurada, usa rutas relativas.

<test-suite>{iniciativa} — debug E2E</test-suite>
<test-name>{nombre}</test-name>
<test-file>{spec_relpath}</test-file>
<seed-file>tests/seed.spec.ts</seed-file>
<body>
ESCENARIO (lenguaje de negocio):
{bloque verbatim del test}

RECETA DE EJECUCIÓN (navegación, URLs y locators REALES que funcionaron — REPRODÚCELA):
{receta de ejecución devuelta por el runner}

Contexto común (convención de independencia, fixture de identidad/login, tarea periódica):
{cabecera común verbatim}
</body>
```

Tras invocarlo, **localiza** el fichero recién escrito bajo `tests/` por el `{slug}` (`find tests -name '*{slug}*.spec.ts'`); esa es la **copia de trabajo** `{spec_work}`. El `generator_write_test` **siempre escribe bajo `tests/`** (su `testDir`), normalmente en `tests/{slug}.spec.ts` o `tests/{describe-en-kebab}/{slug}.spec.ts`, **ignorando** la ruta `<test-file>`, y el generator **no tiene `Write`/`Edit`**. **NO lo muevas todavía**: el healer lo necesita bajo `tests/` para poder ejecutarlo (6.4.2). Si **no aparece** ningún fichero nuevo bajo `tests/` (el generator falló al escribir), no hay spec que sanar → **STOP** y pregunta al usuario.

#### 6.4.2 Bucle de sanado + validación (LIMIT 5)

Sistema **mixto**: el healer itera internamente en cada invocación; el orquestador envuelve esa invocación en un `**LIMIT**: max_heal = 5`. `intento_heal = 1`. Cada **intento**:

1. **Sana el spec con el healer**: lanza **un** subagente (`Agent`, `subagent_type: playwright-test-healer`, `run_in_background: false`) acotado a la copia de trabajo `{spec_work}`. **MUST NOT** sanarlo tú mismo con `Edit`. En `intento_heal > 1`, incluye al final la salida del fallo de `npx --config=playwright.sdd.config.ts` del intento anterior. El prompt **MUST** construirse con esta plantilla:

   ```
   Eres el Playwright Test Healer. Arregla UN único `.spec.ts` que falla, editando SOLO el código del test. La aplicación (http://localhost:8080) es CORRECTA y ya está levantada: NUNCA modifiques código de la aplicación, ni arranques/pares la app.

   FICHERO A SANAR (trabaja SOLO sobre este, no sobre otros tests):
   {spec_work}

   REGLAS CRÍTICAS:
   - Ejecuta y depura SOLO este fichero: `test_run` con locations ["{spec_work}"] y `test_debug` del test de este fichero. NO ejecutes la suite entera.
   - El comportamiento esperado es FIJO (el del ESCENARIO de abajo): ajusta locators, asserts, esperas y pasos del test para que reflejen la app real; NO cambies lo que el test verifica para "esquivar" el fallo.
   - NO marques `test.fixme()`, `test.skip()` ni `test.only()`: si no consigues que pase, dilo explícitamente; NO lo ocultes saltándolo.
   - Usa locators robustos (rol+nombre, label, placeholder, data-testid); para datos dinámicos usa regex. NO uses ids autogenerados de Axelor (#x-123). NO esperes `networkidle`.
   - Itera hasta que `test_run` de este fichero pase limpio, o hasta que tengas alta confianza de que no es arreglable (entonces repórtalo, sin saltarlo).

   ESCENARIO (lenguaje de negocio — comportamiento esperado, NO lo cambies):
   {bloque verbatim del test}

   RECETA DE EJECUCIÓN (navegación, URLs y locators REALES que funcionaron a mano):
   {receta de ejecución devuelta por el runner}

   Contexto común (convención de independencia, fixture de identidad/login, tarea periódica):
   {cabecera común verbatim}
   ```

2. **Validación autoritativa**: **copia** `{spec_work}` a `{spec_relpath}` (`cp` — conserva la copia de trabajo bajo `tests/`) y ejecútalo con la config dedicada y reporter no bloqueante; `timeout` acotado (~120 s). **MUST NOT** usar la config base ni el reporter `html` (levanta un servidor y bloquea el comando indefinidamente):
   ```bash
   PLAYWRIGHT_HTML_OPEN=never npx playwright test --config=playwright.sdd.config.ts "{spec_relpath}" --project=chromium --reporter=line
   ```
   - **Código de salida 0** (PASS) → el spec es **válido**: **promuévelo** — borra la copia de trabajo de `tests/` y cualquier subcarpeta sobrante que dejara el generator (p. ej. `tests/{describe-en-kebab}/`), para no contaminar la suite real. El caché queda listo en `test_e2e/`. Sal del bucle y continúa a 6.5.
   - **Distinto de 0** (FAIL) → borra la copia de `test_e2e/` (`{spec_relpath}`) y **conserva** la copia de trabajo bajo `tests/` para el siguiente intento. Ve al control de fin de intento (paso 3) con la salida del reporter como contexto para el healer.

3. **Fin de intento**: incrementa `intento_heal`. Si `intento_heal <= 5`, vuelve al paso 1 reinvocando al healer con la salida del fallo de npx. Si `intento_heal > 5` → **STOP** (control de agotamiento, abajo).

**STOP — agotamiento del bucle** (`intento_heal > 5`): si tras **5** intentos el healer no logra un `.spec.ts` que pase la validación autoritativa aunque el código funciona, **algo anómalo ocurre** (la pantalla no es automatizable de forma estable, el spec no es estabilizable, etc.) y **MUST NOT** seguir como si nada. Antes de parar, **borra los specs sobrantes** para no dejar caché roto (una reanudación reejecutará el test por `playwright-cli`):
```bash
find .sdd/drafts/{iniciativa}/implementation/test_e2e -name '{id}_*.spec.ts' -delete
find tests -name '*{slug}*.spec.ts' -delete
```
Luego **STOP**: detén la Fase 2 (no proceses más tests), informa al usuario de que el test `{id}` pasa con `playwright-cli` pero no se ha podido sanar un spec fiable en 5 intentos (incluye el último error de `npx playwright test`) y pregunta con `AskUserQuestion` cómo proceder. **MUST NOT** convertir el PASS en FAIL ni escribir el checkpoint de este test: al no checkpointarlo, una reanudación lo reejecuta por `playwright-cli`.

### 6.5 Escribir el checkpoint del test (append a `progress.jsonl`)

En cuanto un test alcanza un resultado **terminal** (`PASS` o `FAIL` definitivo) y ya se ha intentado cachear el spec si procedía (6.4), **MUST** añadir **una** línea JSON a `.sdd/drafts/{iniciativa}/implementation/progress.jsonl` **antes** de pasar al siguiente test (principio 2.7). Esto es lo que hace la ejecución reanudable: si el skill se interrumpe después de esta escritura, ese test ya no se reejecuta.

Como `Write` reescribe el fichero entero, para **añadir** sin perder lo anterior usa `Bash` con redirección `>>` sobre una sola línea ya serializada (el `{...}` es el objeto JSON del test, en una línea):

```bash
printf '%s\n' '{"id":"T-001","nombre":"Alta manual de un Correo sin adjuntos","resultado":"PASS","intentos":0,"detalle":null,"ts":"2026-05-25T00:45:00"}' >> .sdd/drafts/{iniciativa}/implementation/progress.jsonl
```

- **MUST** escapar comillas y saltos de línea dentro de `detalle` como JSON válido (`\"`, `\n`) para que la línea sea parseable.
- **MUST NOT** usar `Write` para esto (reescribiría el fichero y perdería el historial de tests previos).
- **MUST NOT** escribir más de una línea por test.

---

## 7. Fase 3 — Reporte final

Tras procesar todos los tests pendientes, **MUST** hacer las dos cosas: escribir el reporte a fichero y mostrarlo en la conversación. El reporte se construye **desde `progress.jsonl`** (la fuente de verdad acumulada, que incluye los tests resueltos en ejecuciones anteriores), no solo desde los tests procesados en esta ejecución.

0. **Relee** `progress.jsonl` con `Read` y parsea todas las líneas. Ordena los tests por el orden en que aparecen en `tests.md` (no por orden de resolución). Cada línea aporta `id`, `nombre`, `resultado`, `intentos`, `detalle`.
1. **Escribe** `.sdd/drafts/{iniciativa}/implementation/debug-report.md` con `Write`, usando **exactamente** esta plantilla (una fila por test, en el orden de `tests.md`):

   ```
   ---
   type: debug-report
   ---

   # Reporte de depuración E2E — {iniciativa}

   {N} tests · {P} PASS · {F} FAIL · {fecha-hora}

   | Test | Resultado | Intentos | Detalle del fallo |
   |------|-----------|----------|-------------------|
   | T-001 — Alta manual de un Correo sin adjuntos | PASS | 0 | — |
   | T-009 — Envío automático con éxito | FAIL | 3 | El estado quedó en PENDIENTE tras 70 s; la tarea periódica no procesó el correo. |
   | T-011 — Reenvío de un Correo FALLIDO | PASS | 1 | — |
   ```

   Reglas de relleno:
   - Una fila por test ejecutado, en el orden de `tests.md`.
   - `Intentos` = nº de correcciones aplicadas antes de pasar (`0` si pasó a la primera; hasta `3`).
   - `Detalle del fallo` = `—` si `PASS`; la última descripción del subagente si `FAIL`.

2. **Muestra** en la conversación el mismo resultado en formato escaneable:

   ```
   Resultado de la depuración E2E ({N} tests) — ver .sdd/drafts/{iniciativa}/implementation/debug-report.md

   PASS  T-001 — Alta manual de un Correo sin adjuntos
   FAIL  T-009 — Envío automático con éxito
         ↳ El estado quedó en PENDIENTE tras 70 s; la tarea periódica no procesó el correo.
   PASS  T-011 — Reenvío de un Correo FALLIDO
   ...

   Resumen: {P} PASS / {F} FAIL.
   ```

Para cada `FAIL` incluye en una línea sangrada la última descripción del fallo. Si quedaron tests en `FAIL`, indícalo claramente al usuario para que decida los siguientes pasos.

**MUST NOT** ocultar fallos ni declarar éxito si algún test quedó en `FAIL`. **MUST NOT** escribir el reporte sin mostrarlo también en la conversación, ni al revés.

---

## Quick Guidelines

- Eres un **orquestador**: parseas `tests.md`, lanzas **un subagente por test en secuencia** y, ante `FAIL`, **diagnosticas** y **delegas la corrección en `code-implementer`**, luego reintentas.
- El subagente runner **solo ejecuta** el test (con `npx playwright test` si su `.spec.ts` ya existe, o con `playwright-cli` si no) y devuelve `PASS {id}`/`FAIL {id}` + `MODO spec`/`MODO cli` + descripción. **MUST NOT** modificar código ni usar `AskUserQuestion`.
- **Caché de specs** (principio 2.8): al pasar un test por `playwright-cli` sin `.spec.ts` previo, genera uno con `playwright-test-generator` en `implementation/test_e2e/{id}_{slug}.spec.ts`; las reejecuciones usan ese spec vía `npx` (sin bucle LLM↔navegador). `--no-cache` desactiva todo el mecanismo.
- **El runner instruye al generator**: al pasar en `MODO cli`, el runner devuelve una `=== RECETA DE EJECUCIÓN ===` con la navegación real, URLs técnicas reales y locators que funcionaron. El orquestador la pasa al generator (y al healer) para que **reproduzca** esa ruta y no invente URLs "amigables" inexistentes. **MUST** pasar la receta.
- **El generator escribe bajo `tests/`**: `generator_write_test` ignora la ruta `<test-file>` y no tiene `Write`/`Edit`. La copia de trabajo se queda bajo `tests/` mientras se sana (el MCP del healer solo ve ese `testDir`) y solo se **promueve** a `implementation/test_e2e/` al pasar la validación autoritativa, dejando `tests/` limpio.
- **Sanado del spec, sistema mixto** (Fase 2.4): `playwright-test-generator` **autora** el spec una vez; la **validación autoritativa** es `npx --config=playwright.sdd.config.ts --reporter=line` con `PLAYWRIGHT_HTML_OPEN=never` (el reporter `html` de la config base **cuelga** sirviendo el report) y `timeout` acotado. Como el código ya está probado por `playwright-cli`, un fallo es **del spec, no del código**: **sánalo con `playwright-test-healer`** (edita el `.spec.ts`, NO el código, NO abras el bucle 6.3). El healer itera internamente; el orquestador lo reinvoca si la validación sigue fallando, `**LIMIT**` 5 intentos. A las 5 sin pasar → borra los specs rotos y **STOP** (pregunta al usuario; algo anómalo). Un fallo de spec **nunca** convierte el PASS en FAIL.
- **Fallos recurrentes de specs** (principio 2.9): **toda espera lleva `timeout` acotado** (`expect.poll({timeout, intervals})` + `test.setTimeout` si supera 30 s). **NUNCA** `browser_wait_for`/`verify_text` sobre el value de un input (cuelga; usar `verify_value`/`toHaveValue`). Para sondear cambios async (cron) usar REST autenticado (`page.request`) o `page.reload()`, **NUNCA** `page.goto` a la misma url (la SPA cachea form y grid). El cuerpo (`contenteditable`) se rellena con `keyboard.type`, no `.fill()`.
- App **idempotente y por puerto**: comprobar `200` con `curl`, arrancar en segundo plano con log a fichero, parar con kill por puerto 8080. **MUST NOT** levantar dos instancias.
- Diagnostica con el **MCP de IntelliJ** y el log `implementation/app-debug.log`; el fix lo escribe **`code-implementer`** con un plan pequeño y los skills de dominio (`k-secure-coding` + `k-code-quality` si toca entidades/servicios/controladores). **MUST NOT** escribir el fix con `Edit`.
- **MUST NOT** modificar `tests.md` ni XML/contrato del diseño para que un test pase; si hace falta, **STOP** y vuelve a `/sdd-designer-system`.
- **LIMIT**: 3 intentos de corrección por test; tras agotarlos, `FAIL` y siguiente test.
- **Checkpoint reanudable**: al resolver cada test (PASS o FAIL definitivo) **añade** una línea JSON a `implementation/progress.jsonl` (con `>>`, nunca `Write`). Al arrancar, lee ese fichero y **salta** los tests ya resueltos. El test es atómico: si se interrumpe a mitad, no tiene línea y se reejecuta entero. `--fresh` ignora el progreso previo.
- Termina **siempre** escribiendo el listado `PASS`/`FAIL` en `implementation/debug-report.md` (generado desde `progress.jsonl`) **y** mostrándolo en la conversación, sin ocultar fallos.

---

## Apéndice A — Override de rutas (para testing)

- `--in=<ruta>` — `tests.md` de entrada explícito. **Desactiva la auto-detección** de la Fase 0 caso 2. La carpeta de la iniciativa es la que contiene su `implementation/`.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`.
- `--fresh` — reejecuta todos los tests desde cero (la Fase 1 no descarta ninguno). Si existe `progress.jsonl`, **trunca** el fichero al inicio de la Fase 2 (`printf '' > .sdd/drafts/{iniciativa}/implementation/progress.jsonl`) para que la pasada empiece limpia. Útil tras cambiar `tests.md` o para una pasada completa. **Nota**: `--fresh` no borra los `.spec.ts` cacheados; si quieres regenerarlos, combínalo con `--no-cache` o borra `implementation/test_e2e/` a mano.
- `--no-cache` — desactiva la caché de specs (principio 2.8): no genera ni usa ningún `.spec.ts`, todo se ejecuta con `playwright-cli`. Útil para depurar el propio skill o forzar una pasada "desde la UI".

En uso normal no se especifican.
