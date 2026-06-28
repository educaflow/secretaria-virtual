---
name: sdd-create-tests-e2e
description: Paso del pipeline SDD posterior a `/sdd-debug-with-test-e2e-desc`. Dada una iniciativa cuya carpeta `test-e2e-desc/` ya está descompuesta y depurada (un índice `tests-e2e-desc.md` con checkbox por test + un `t-NNN-<slug>.desc.md` autocontenido por test), persiste como tests de regresión Playwright **solo los tests que pasaron** (`[x]`): copia cada `t-NNN-<slug>.desc.md` a `src/test/e2e/<iniciativa>/` como snapshot "as-tested" y genera su `t-NNN-<slug>.spec.ts` hermano, ejecutándolo contra la app real hasta que pasa. El skill es un MOTOR genérico y agnóstico al artefacto: aporta solo el flujo (localizar la iniciativa, cargar el contrato, seleccionar+copiar, arrancar la app, y por cada test generar→ejecutar→sanar) y delega TODO lo específico (cómo se genera el `.spec.ts`, el ciclo de login/logout, cómo se sana un test roto) en la guía `template-system/README.md` (configurable con `--template-dir`), que los subagentes leen como contrato. La salida son los pares `.desc.md` + `.spec.ts` bajo `src/test/e2e/<iniciativa>/` y el helper `src/test/e2e/_support/auth.ts`.
handoffs:
  - label: Cerrar la iniciativa
    agent: sdd-close-spec
    prompt: Cerrar la iniciativa en .sdd/drafts/{carpeta-iniciativa}/ tras crear los tests E2E de regresión — archivar en .sdd/specs/ y actualizar los CLAUDE.md afectados.
allowed-tools: Bash, Read, Write, Edit, Skill, AskUserQuestion, Agent, Monitor
---

# sdd-create-tests-e2e

Eres un **motor de creación de tests E2E de regresión** del pipeline SDD: tomas la descomposición ya **depurada** de una iniciativa (`test-e2e-desc/`, producida por `/sdd-debug-with-test-e2e-desc`), seleccionas **solo los tests que pasaron** (`[x]` en el índice) y, por cada uno, **copias su descripción** a `src/test/e2e/<iniciativa>/` y **generas su `.spec.ts`** pilotando la app real, dejándolo verde. La salida son tests Playwright versionados que reproducen la cobertura ya verificada.

**CRITICAL — eres agnóstico al artefacto.** Este `SKILL.md` define **solo el flujo y la orquestación de agentes**. **No sabe** cómo se genera un `.spec.ts`, cuál es el ciclo de autenticación, qué hace fiel a un test, ni cómo se sana uno roto: todo eso lo declara `template-system/README.md`, que los subagentes leen como contrato. **MUST NOT** asumir de memoria ningún detalle específico; **MUST NOT** nombrar plantillas ni comandos concretos de generación/verificación/sanación en este skill, salvo el contrato fijo de entrada/salida y la gestión de la app (§2.2). Apuntar `--template-dir` a otra carpeta con un README distinto cambia por completo cómo se generan los tests, **sin tocar este skill**.

El skill lanza **tres roles** de subagente en **contextos aislados** (todos leen el mismo `README.md`). **CRITICAL — separación de poderes anti-trampa**: el que crea un test **no** es el que decide si vale; el veredicto rojo/verde lo da el runner real (mecánico, no un agente).

- **generador** — coge **un** `.desc.md`, pilota la app real y escribe su `.spec.ts` hermano. **No** declara si pasa (§9.1).
- **verificador** — **independiente**; audita de forma adversarial que un `.spec.ts` ya verde es **fiel** a su descripción (no débil ni tramposo). Solo dictamina `OK`/`INFIEL`, no toca el test (§9.2).
- **sanador** — **independiente**; ante un `.spec.ts` rojo o declarado `INFIEL`, **arregla el `.spec.ts`** (nunca el código Java) (§9.3).

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- **Ruta explícita** a la carpeta `test-e2e-desc/` de una iniciativa (o a la carpeta de la iniciativa). El skill valida que contiene `tests-e2e-desc.md` y procede.
- **Identificador de test concreto** (`T-007`) o una lista (`T-001 T-007`) para materializar solo esos (si están `[x]`). Sin esto, se materializan **todos** los `[x]` pendientes.
- **Sin argumentos** → auto-detección de la **última** iniciativa de `.sdd/drafts/` que contenga `test-e2e-desc/tests-e2e-desc.md` (§4.2).
- Flags de override `--template-dir=`, `--in=`, `--root=`, `--fresh` (Apéndice A).

---

## Outline

1. **Fase 0 — Localizar** la iniciativa y su `test-e2e-desc/`, y **confirmar** la ruta detectada (§4).
2. **Fase 1 — Cargar** el contrato (`template-system/README.md`), resolver `<iniciativa>` y las rutas, y leer la sección «Gestión de la app» (§5).
3. **Fase 2 — Seleccionar y copiar** (§7): leer el índice, quedarse con los tests `[x]`, descartar los que ya tienen `.spec.ts` (salvo `--fresh`), copiar cada `.desc.md` a `src/test/e2e/<iniciativa>/` con la cabecera-banner de snapshot, y **comprobar el helper `_support/auth.ts` (crearlo si no existe)**.
4. **Fase 3 — Arrancar la app** (la gestiona el motor, §2.2): dejarla respondiendo `200` antes de generar el primer test (§8).
5. **Fase 4 — Generar, verificar y sanar** (§9), tres subagentes aislados por test:
   - **§9.0 — validar el helper de auth** (login/logout) contra la app real **una vez**; si está roto, corregirlo antes de generar nada.
   - Por cada test, en orden: **generador** escribe `t-NNN-<slug>.spec.ts` (no juzga) → el motor lo **ejecuta** con el runner real → si **verde**, el **verificador** independiente audita que es fiel (`OK`/`INFIEL`).
   - si **RED** o **INFIEL** → bucle **sanador** → reejecutar y volver a verificar (**LIMIT** 8 ciclos). Como el test ya pasó al depurar, la causa por defecto es el `.spec.ts`, no el código (§2.3). Al agotar, dejar el `.spec.ts` y reportar FAIL.
6. **Fase 5 — Reportar** el listado final SUCCESS/FAIL y **parar la app** (§10).

**STOP conditions**:

- `--template-dir=` apunta a una carpeta que **no contiene `README.md`** → **ERROR** y detente.
- No se encuentra ninguna carpeta `test-e2e-desc/` con `tests-e2e-desc.md` → **ERROR**: indica que hay que ejecutar antes `/sdd-debug-with-test-e2e-desc` y detente.
- El índice **no tiene ningún test `[x]`** → **STOP** e informa: no hay nada verificado que materializar (deja pasar antes los tests con `/sdd-debug-with-test-e2e-desc`).
- El usuario no confirma la ruta auto-detectada (Fase 0 caso 2) → **STOP** y pide la ruta.
- La app no responde `200` en `http://localhost:8080` tras arrancarla → **STOP** y `AskUserQuestion` (reintentar / ver log / abortar).
- El **validador de auth** (§9.0), el **generador** o el **sanador** devuelven `BLOQUEADO` (falta un recurso del entorno, el helper de auth no se puede cuadrar, o el fallo no es del `.spec.ts` sino de la app: posible regresión) → **STOP** y pregunta al usuario. **MUST NOT** tocar código Java para forzar el test.
- Tras **8** ciclos de sanación un test sigue en FAIL → ese test queda FAIL (su `.spec.ts` se conserva) y el bucle continúa con el resto.

---

## 1. Entrada y salida

### 1.1 Entrada

La carpeta `test-e2e-desc/` de una iniciativa, **ya descompuesta y depurada** por `/sdd-debug-with-test-e2e-desc`:

- el índice `tests-e2e-desc.md` (`type: test-e2e-index`) con una línea `- [x]`/`- [ ]` por test, y
- un `t-NNN-<slug>.desc.md` (`type: test-e2e`, `id: T-NNN`) **autocontenido** por test (cabecera con `Estado inicial de la base de datos` + tabla de credenciales + bloque del test).

El skill **no asume su estructura interna**: la conoce el subagente leyendo el contrato. Solo necesita el índice para saber **qué tests están `[x]`** y a qué fichero apunta cada uno.

### 1.2 Salida

- En `src/test/e2e/<iniciativa>/`: por cada test `[x]`, un par `t-NNN-<slug>.desc.md` (copia-snapshot con cabecera-banner) + `t-NNN-<slug>.spec.ts` (test Playwright verde).
- En `src/test/e2e/_support/auth.ts`: el helper de login/logout compartido (creado si no existe; su contenido lo define el contrato).
- En la conversación: el listado final SUCCESS/FAIL por test (§10).

**MUST NOT** modificar `test-e2e-desc/` ni ningún artefacto de `.sdd/` (es la fuente; el destino en `src/test/e2e/` es una copia regenerable). **MUST NOT** modificar código Java: este skill solo crea tests; si un test no pasa por un fallo de la app, es una **regresión** que se reporta, no se oculta.

### 1.3 Estructura de carpetas

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── test-e2e-desc/                       ← ENTRADA (la dejó /sdd-debug-with-test-e2e-desc)
    ├── tests-e2e-desc.md            ← índice con checkbox por test
    └── t-001-<slug>.desc.md … t-NNN-<slug>.desc.md

src/test/e2e/                            ← SALIDA
├── _support/
│   └── auth.ts                          ← helper login/logout (creado si falta)
└── {resumen}/                           ← <iniciativa> = el {resumen} del nombre del draft
    ├── t-001-<slug>.desc.md         ← snapshot "as-tested" (con cabecera-banner)
    └── t-001-<slug>.spec.ts         ← test Playwright generado
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 El README es el contrato único

Todo lo específico (cómo generar el `.spec.ts`, el ciclo de autenticación, la plantilla del test y del helper, cómo sanar un test roto y qué **MUST NOT** tocarse) lo define `template-system/README.md` y los ficheros que él referencie. Los subagentes los **leen de disco**; el skill **MUST NOT** asumirlos ni hardcodearlos. El skill solo pasa a cada subagente **las rutas** de los ficheros de entrada y su rol.

**CRITICAL — `README.md` es el ÚNICO fichero de la plantilla que el motor conoce por nombre.** Los demás los descubren los subagentes leyéndolo. Único acoplamiento por nombre: `README.md` (contrato), la entrada (`test-e2e-desc/`) y la salida (`src/test/e2e/<iniciativa>/`).

### 2.2 La app la gestiona el MOTOR (única excepción al agnosticismo)

El generador pilota la app real y el motor ejecuta los `.spec.ts` contra ella, así que la app es un **recurso compartido** que **MUST** sobrevivir entre subagentes. Por eso **la gestiona el motor**, no los subagentes (un proceso en segundo plano lanzado por un subagente muere al cerrarse su contexto). Los comandos concretos (arrancar, parar por puerto, sondear `200`, dónde va el log) los define `README.md` en su sección **«Gestión de la app»**, que el motor lee en la Fase 1 y sigue al pie de la letra.

Reglas que el motor **MUST** cumplir:

- **Arrancar** la app como **tarea tracked en segundo plano** (`Bash` con `run_in_background: true`), redirigiendo el log al fichero que indique el README, y **esperar** sondeando hasta `200` (**LIMIT** de sondeo amplio; el arranque hace un `clean build`). **MUST NOT** dejar que un subagente arranque la app.
- **CRITICAL — limpiar el puerto de verdad antes de arrancar**: una instancia previa colgada hace que el connector falle el bind en silencio. Sigue el procedimiento de limpieza del README.
- **Parar** siempre **por puerto**, nunca por handle de proceso. Parar la app al terminar (§10).
- El arranque es **idempotente**: comprueba el `200` y arranca solo si no responde. **MUST NOT** levantar una segunda instancia.

### 2.3 La causa por defecto de un fallo es el `.spec.ts`, no el código

- **CRITICAL** — el test **ya pasó** al depurar con `/sdd-debug-with-test-e2e-desc` (por eso está `[x]` y este skill se ejecuta justo después). Por tanto, cuando un `.spec.ts` recién generado falla, la causa **por defecto es el propio `.spec.ts`** (locator, timing, equivalencia de textos, selectores de auth): **se arregla el test, NUNCA el código**. Por eso existe el bucle generar→ejecutar→verificar→sanar (§9).
- **MUST NOT** modificar código Java (`src/main/...`) ni la carpeta `test-e2e-desc/` ni nada bajo `.sdd/` (es la fuente de verdad; el `.desc.md` de `src/test/e2e/` es una copia regenerable).
- **Excepción** — solo si el fallo demostrablemente **no** es del `.spec.ts` sino de que la app se comporta distinto a lo que la descripción ya depurada espera, es una **regresión de la app**: el sanador devuelve `BLOQUEADO`, el motor **STOP** y avisa. **MUST NOT** ocultarla tocando código ni debilitando aserciones.

### 2.4 Orquestación de subagentes

- Los **generadores**, **verificadores** y **sanadores** corren **de uno en uno y en secuencia** (§9): comparten el puerto 8080 y se pisarían en paralelo. Para un mismo test van en contextos **distintos** (aislados), que es justo lo que evita las trampas.
- **MUST NOT** lanzar subagentes en paralelo ni con `run_in_background` (salvo el arranque de la app, que sí es background tracked y lo hace el motor, no un subagente).
- Cada rol responde con un **token literal** que el skill parsea (definidos en cada fase). El skill compara por literal exacto.
- Los subagentes **MUST NOT** usar `AskUserQuestion`: ante un bloqueo lo reportan con el token de su rol y el motor lleva la decisión al usuario.

### 2.5 Idempotencia y progreso por la presencia del `.spec.ts`

El **checkpoint** es el propio `.spec.ts`: un test materializado y verde es un `.spec.ts` que existe y pasa. Al (re)invocar el skill, los tests cuyo `.spec.ts` ya existe se **descartan** (salvo `--fresh`, que regenera todos los seleccionados). Esto hace el skill reanudable sin un índice propio.

### 2.6 Solo se materializa lo verificado

**REQUIRED** — solo se procesan los tests marcados `[x]` en el índice de entrada. Los `[ ]` (no pasaron al depurar) **MUST NOT** materializarse: meterían tests rojos en la suite. Si el usuario pide un id concreto que está `[ ]`, **STOP** e indícalo.

---

## 3. Flujo general

```
┌──────────────────────────────────────────────────────────────────────┐
│  Fase 0  Localizar test-e2e-desc/ (ruta explícita | más nueva) + confirmar │
│  Fase 1  Cargar el contrato (README) + sección «Gestión de la app»   │
│  Fase 2  Leer índice → filtrar [x] → descartar los que ya tienen     │
│          .spec.ts → copiar .desc.md (banner) → asegurar _support/auth.ts │
│  Fase 3  Motor: arrancar la app (tracked bg) hasta 200               │
│  Fase 4  §9.0 validar _support/auth.ts (login/logout) una vez        │
│          Por cada test seleccionado (en secuencia, 3 contextos):     │
│            ├─ generador(test) ──► escribe t-NNN-<slug>.spec.ts       │
│            ├─ motor ejecuta el .spec.ts (runner real) ─► RED | GREEN │
│            ├─ GREEN → verificador(indep.) ─► OK | INFIEL             │
│            │           OK → siguiente test                           │
│            └─ RED | INFIEL → bucle (LIMIT 8): sanador → reejecutar    │
│                 → volver a verificar; BLOQUEADO → STOP + pregunta   │
│  Fase 5  Parar la app + reporte final SUCCESS/FAIL                   │
└──────────────────────────────────────────────────────────────────────┘
```

Las fases se ejecutan **en orden**. Salvo la confirmación inicial (Fase 0) y los `STOP` ante una excepción real, el flujo **no pide aprobación**: tras seleccionar, se genera automáticamente.

---

## 4. Fase 0 — Localizar el `test-e2e-desc/`

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca con una ruta a `test-e2e-desc/` (o a la carpeta de la iniciativa): comprueba que existe `test-e2e-desc/tests-e2e-desc.md`. La **carpeta de la iniciativa** es la que contiene `test-e2e-desc/`; `<iniciativa>` es el `{resumen}` de su nombre (`YYYY-MM-DD_HH-MM_{resumen}` → `{resumen}`). Pasa a la Fase 1.

### 4.2 Caso 2 — Sin ruta (auto-detección)

1. Lista las carpetas con formato de iniciativa:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordena alfabéticamente (el prefijo timestamp = orden cronológico) y toma la **última** que contenga `test-e2e-desc/tests-e2e-desc.md`.
3. Si ninguna lo contiene → **ERROR**: no hay descomposición depurada; ejecuta antes `/sdd-debug-with-test-e2e-desc`. Detente.
4. Muestra la ruta detectada y confirma con `AskUserQuestion` (usar / dar otra ruta).

**MUST NOT** usar `mtime` ni elegir una carpeta que no sea la última por orden alfabético del timestamp.

---

## 5. Fase 1 — Cargar el contrato y resolver rutas

1. **REQUIRED — lee con `Read` la guía `template-system/README.md`** (resuelta contra `--template-dir`): confirma que existe (si no → **ERROR**), entiende a alto nivel qué pide a cada rol, y **lee su sección «Gestión de la app»** (los comandos que el motor ejecutará en la Fase 3/4). El resto del contrato lo leen los subagentes de disco.
2. **Resuelve** `<iniciativa>` (el `{resumen}` del nombre del draft) y las rutas que se pasarán a los subagentes (no su contenido):
   - la ruta de `template-system/README.md` (las reglas),
   - la carpeta de entrada `{iniciativa}/test-e2e-desc/` y su índice,
   - la carpeta de salida `src/test/e2e/<iniciativa>/` y el helper `src/test/e2e/_support/auth.ts`.
3. **Valida** que el índice `tests-e2e-desc.md` existe y tiene al menos una línea de test. Si no, **ERROR** y detente.

---

## 6. Fases con subagentes — patrón común del prompt

Cada prompt de subagente (§9) **MUST** pasar, además de su tarea específica:

- **Reglas**: `lee {ruta de template-system/README.md} y todos los ficheros que referencie. Es el contrato: define qué hacer, cómo y con qué estructura. Síguelo al pie de la letra.`
- **MUST NOT** usar `AskUserQuestion`: ante una duda que no puedan resolver, la reportan con el token de bloqueo de su rol.

---

## 7. Fase 2 — Seleccionar y copiar

1. **Lee el índice** `tests-e2e-desc.md`. Por cada línea `- [x] [T-NNN — <nombre>](t-NNN-<slug>.desc.md)`, registra `(T-NNN, fichero)`. **Descarta** las `- [ ]` (§2.6). Si el usuario pasó ids concretos, filtra a esos (si alguno está `[ ]` → **STOP** e indícalo).
2. Si tras filtrar **no queda ninguno** → **STOP** (no hay tests verificados que materializar).
3. **Descarta** (salvo `--fresh`) los tests cuyo `.spec.ts` destino ya exista en `src/test/e2e/<iniciativa>/` (§2.5). Indica cuántos se saltan y cuántos quedan.
4. Por cada test seleccionado, **copia** su `t-NNN-<slug>.desc.md` desde `test-e2e-desc/` a `src/test/e2e/<iniciativa>/`, **anteponiendo la cabecera-banner de snapshot** (formato y contenido los define el contrato en el README; lleva el origen, `T-NNN`, `Origen ESC` y el aviso de "NO editar a mano"). **MUST NOT** alterar el resto del contenido del `.desc.md`.
5. **Comprueba el helper de auth `src/test/e2e/_support/auth.ts`** (la parte de login/logout): si **no existe**, créalo con la plantilla literal que define el contrato (guía de generación); si **ya existe**, **MUST NOT** sobrescribirlo. Su login/logout se **valida** contra la app real en la Fase 4 (§9.0) antes de generar ningún test, porque sus selectores son best-effort.

---

## 8. Fase 3 — Arrancar la app (la gestiona el motor)

Antes de generar el primer test, el motor deja la app respondiendo `200` siguiendo la sección **«Gestión de la app»** del README (§2.2): limpia el puerto, arranca como tarea tracked en segundo plano con el log al fichero indicado, y sondea hasta `200`. Si tras el sondeo no responde `200` → **STOP** y `AskUserQuestion` (reintentar / ver el log / abortar). **MUST NOT** continuar a la Fase 4 sin la app en `200`.

---

## 9. Fase 4 — Generar, verificar y sanar cada test (en secuencia)

### 9.0 Validar el helper de auth (una vez, antes del primer test)

Con la app ya en `200`, **valida que `_support/auth.ts` funciona contra la UI real** antes de generar ningún test (sus selectores son best-effort; si están mal, fallarían **todos** los tests). Lanza **un** subagente (`subagent_type: claude`, `run_in_background: false`):

**Prompt del subagente validador de auth**:

> Eres experto en testing E2E con Playwright de la secretaría virtual (Axelor). Tu tarea es **validar y, si hace falta, corregir el helper de autenticación** `src/test/e2e/_support/auth.ts` contra la app real.
>
> - **Reglas**: lee `{ruta de template-system/README.md}` y los ficheros que referencie (en particular la sección del **helper de auth** de la guía de generación). **Carga `/k-playwright`**. Usa las tools MCP de Playwright (`browser_*`) contra la app.
> - **Premisa**: la app YA está en `http://localhost:8080` (la arrancó el orquestador). NO la arranques ni la pares.
> - **Credenciales**: usa un usuario válido de la tabla de credenciales de `{ruta de un .desc.md cualquiera de la selección}`.
> - **Comprueba** que `ensureLoggedOut` → `login(usuario, contraseña)` entra de verdad y que `logout` cierra sesión y deja el login. Si algún selector no casa con la UI real, **corrige `src/test/e2e/_support/auth.ts`** (es **test code**; **MUST NOT** tocar código Java ni la fuente en `.sdd/`).
> - **MUST NOT** usar `AskUserQuestion`. Responde **exactamente** una línea: `AUTH-OK` o `BLOQUEADO: {motivo}`.

El motor parsea: `AUTH-OK` → continúa al bucle por test; `BLOQUEADO: …` → **STOP** y `AskUserQuestion`. **MUST NOT** generar tests con un helper de auth no validado.

Recorre luego los tests seleccionados **en orden**. Para cada test:

### 9.1 Lanzar el generador

Lanza **un** subagente con `Agent` (`subagent_type: claude`, `run_in_background: false`). Recibe la ruta de **su** `.desc.md` (la copia en `src/test/e2e/<iniciativa>/`), la ruta destino del `.spec.ts` hermano, la ruta del helper `_support/auth.ts` y el contrato.

**Prompt del subagente generador**:

> Eres un experto en testing E2E con Playwright de la secretaría virtual (Axelor). Tu tarea es **generar UN test `.spec.ts`** a partir de su descripción, pilotando la app real.
>
> - **Reglas para la generación**: lee `{ruta de template-system/README.md}` y los ficheros que referencie —en particular el contrato de **generación** (cómo convertir la descripción en `.spec.ts`, el ciclo de login/logout con `_support/auth.ts`, la plantilla del test, los locators a preferir)—. **Carga el skill `/k-playwright`** y usa las tools MCP de Playwright (`generator_*`/`browser_*`) para grabar el test contra la app. Síguelo al pie de la letra.
> - **Descripción a materializar**: lee `{ruta del t-NNN-<slug>.desc.md}` —es **autocontenida**: trae el estado inicial, la tabla de credenciales y el bloque del test—.
> - **Premisa**: la app YA está levantada en `http://localhost:8080` (la arrancó el orquestador). NO la arranques ni la pares.
> - **Salida**: escribe **exactamente** el fichero `{ruta destino del .spec.ts}` (mismo nombre base que el `.desc.md`, misma carpeta), con el ciclo de auth y la trazabilidad que pide el contrato. **MUST NOT** modificar código Java ni la fuente en `.sdd/`.
> - **MUST NOT** usar `AskUserQuestion`. Aplica el **checklist** del contrato antes de terminar (**LIMIT**: 3 iteraciones de autocorrección).
> - Al terminar, responde **exactamente** una de estas líneas:
>   - `ESCRITO: {ruta del .spec.ts}` — generado.
>   - `BLOQUEADO: {T-NNN} — {motivo}` — no se puede generar (falta un recurso del entorno o la app no responde como la descripción).

El skill parsea la primera línea: `ESCRITO:` → continúa al paso 9.2; `BLOQUEADO:` → **STOP** y `AskUserQuestion`. **CRITICAL** — el generador **MUST NOT** declarar si el test pasa: solo lo escribe. Quién decide si pasa son el runner mecánico y el **verificador independiente** (§9.2), nunca el que lo escribió.

### 9.2 Ejecutar (runner mecánico) y verificar (subagente independiente)

**CRITICAL — separación de poderes anti-trampa**: el que **crea** el test no es el que **decide si vale**. El veredicto sale de (a) el runner real, imposible de falsear, y (b) un verificador en **contexto aislado** que no escribió el test.

1. **El motor ejecuta el `.spec.ts`** con `Bash` (comando del README; típicamente `npx playwright test {ruta} --project=chromium`). El exit code es el **veredicto objetivo** rojo/verde (el motor no lo delega: un subagente podría mentir sobre el resultado).
   - **RED** (exit ≠ 0) → al **bucle de sanación** (§9.3) con la salida del runner.
   - **GREEN** (exit 0) → al **verificador** (paso 2): pasar verde no basta, hay que comprobar que el test **es fiel** a la descripción (un test verde pero con aserciones débiles o saltadas sería una trampa).
2. **Lanza el subagente verificador** (`Agent`, `subagent_type: claude`, `run_in_background: false`) — **independiente del generador**. Audita la fidelidad del `.spec.ts` (verde) contra su `.desc.md`. **MUST NOT** escribir ni "arreglar" el test (lo escribió otro): solo dictamina.

   **Prompt del subagente verificador**:

   > Eres un revisor **adversarial** de tests E2E de la secretaría virtual (Axelor). Tu trabajo es **detectar si un test, aunque pase en verde, es infiel o tramposo** respecto a su descripción. NO escribas ni modifiques el test.
   >
   > - **Reglas para la verificación**: lee `{ruta de template-system/README.md}` y los ficheros que referencie —en particular el contrato de **verificación** (qué hace fiel a un test: cubrir TODOS los puntos del `Resultado esperado` con aserciones reales, login/logout del usuario correcto, sin aserciones debilitadas/triviales ni pasos saltados)—. **Carga `/k-playwright`** si lo necesitas.
   > - **Test a auditar** (ya pasó en verde): lee `{ruta del .spec.ts}` y su descripción `{ruta del .desc.md}` (autocontenida).
   > - Comprueba punto por punto que el `.spec.ts` materializa fielmente `Precondiciones`/`Pasos`/`Resultado esperado`. Sospecha de: aserciones ausentes para un punto del resultado esperado, `expect(true)`/aserciones triviales, pasos comentados o saltados, login con otro usuario, `toBeVisible` sobre algo que siempre está.
   > - **MUST NOT** usar `AskUserQuestion`. **MUST NOT** modificar ningún fichero.
   > - Al terminar, responde **exactamente** una de estas líneas:
   >   - `OK: {T-NNN}` — verde **y** fiel a la descripción.
   >   - `INFIEL: {T-NNN} — {qué falta o se debilitó}` — pasa pero no verifica de verdad la descripción.

   3. Interpreta el token:
      - `OK:` → el test queda materializado; pasa al siguiente test.
      - `INFIEL:` → al **bucle de sanación** (§9.3), pasando el motivo del verificador como "fallo a corregir".

### 9.3 Bucle de sanación (subagente independiente)

Se entra desde un **RED** del runner o un **INFIEL** del verificador. **LIMIT**: `max_ciclos = 8`, `ciclo = 1`. Cada ciclo:

1. Reúne el contexto del fallo: la salida del runner (`npx playwright test`) **o** el motivo `INFIEL` del verificador, y si aplica el extracto del log de la app.
2. **Lanza el subagente sanador** (`Agent`, `subagent_type: claude`, `run_in_background: false`) — **independiente** del generador y del verificador. Su tarea: corregir el `.spec.ts` (nunca el código Java) según el contrato.

   **Prompt del subagente sanador**:

   > Eres un experto en testing E2E con Playwright de la secretaría virtual (Axelor). Tu tarea es **arreglar un `.spec.ts`** sin tocar el código de la app.
   >
   > - **Reglas para la sanación**: lee `{ruta de template-system/README.md}` y los ficheros que referencie —en particular el contrato de **sanación** (causas típicas: locator desactualizado, timing, equivalencia semántica de mensajes, **o aserción que falta** señalada por el verificador; qué **MUST NOT** tocarse)—. **Carga `/k-playwright`** si lo necesitas.
   > - **Test a arreglar**: lee `{ruta del .spec.ts}` y su descripción `{ruta del .desc.md}` (autocontenida).
   > - **Problema a resolver**: `{salida de npx playwright test + extracto del log}` **o** `{motivo INFIEL del verificador}`.
   > - **MUST NOT** modificar código Java (`src/main/...`) ni la fuente en `.sdd/`. **MUST NOT** debilitar ni borrar aserciones para que pase falsamente: si el problema es `INFIEL`, **añade** la aserción que falta.
   > - Distingue el **origen**: si el `.spec.ts` está mal escrito (locator/timing/texto/aserción que falta) es **sanable** → arréglalo; si la app se comporta distinto a lo que la descripción ya depurada espera, es una **regresión de la app** → NO la ocultes.
   > - Al terminar, responde **exactamente** una de estas líneas:
   >   - `CORREGIDO: {T-NNN}` — ajustaste el `.spec.ts`.
   >   - `BLOQUEADO: {T-NNN} — {motivo}` — el fallo no es del `.spec.ts` (posible regresión de la app o recurso del entorno); requiere decisión del usuario.

3. Interpreta el token:
   - `BLOQUEADO: …` → **STOP** y `AskUserQuestion` mostrando el motivo. **MUST NOT** seguir con este test por tu cuenta.
   - `CORREGIDO: …` → **reejecuta** el `.spec.ts` (§9.2 paso 1); si vuelve a estar verde, **vuelve a verificar** (§9.2 paso 2). El ciclo solo termina con `OK` del verificador.
     - `OK` → sal del bucle (siguiente test).
     - sigue `RED`/`INFIEL` → incrementa `ciclo`. Si `ciclo <= 8`, repite. Si `ciclo > 8`, el test queda **FAIL** (su `.spec.ts` se conserva); pasa al siguiente test.

**MUST NOT** superar los **8** ciclos por test.

- ✅ CORRECTO (generador): `ESCRITO: src/test/e2e/grupos-y-notas/t-001-crear-un-grupo-con-sus-alumnos.spec.ts`
- ✅ CORRECTO (verificador): `OK: T-001` / `INFIEL: T-007 — no asierta que la matrícula de honor cuenta como 10 en la media`
- ✅ CORRECTO (sanador): `CORREGIDO: T-001` / `BLOQUEADO: T-014 — el botón "Reabrir" no aparece para el alumno; posible regresión`
- ❌ INCORRECTO: que el **generador** diga si pasa, que el **verificador** edite el test, que el **sanador** borre aserciones para que pase, o pegar el contenido del `.spec.ts` en la respuesta.

---

## 10. Fase 5 — Parar la app y reporte final

1. **Parar la app** por puerto (§2.2).
2. **MUST** mostrar en la conversación el listado final de los tests procesados en esta pasada:

```
Tests E2E de regresión creados — src/test/e2e/{iniciativa}/

SUCCESS  T-001 — Crear un grupo con sus alumnos        → t-001-crear-un-grupo-con-sus-alumnos.spec.ts
FAIL     T-014 — El alumno consulta sus notas          → (8 ciclos agotados)
SUCCESS  T-016 — Crear un grupo sin curso              → t-016-crear-un-grupo-sin-curso.spec.ts
...

Resumen: {P} SUCCESS / {F} FAIL  ({S} saltados por ya existir).
```

Para cada `FAIL` indica el motivo (ciclos agotados / bloqueo). **MUST NOT** ocultar fallos ni declarar éxito si algún test quedó en FAIL. Indica el comando para ejecutar la suite: `npx playwright test src/test/e2e/{iniciativa}`.

---

## Quick Guidelines

- **CRITICAL — agnosticismo**: este SKILL es un **motor de flujo**; **no sabe** cómo se genera ni se sana un `.spec.ts`. Todo lo específico lo define `template-system/README.md` (configurable con `--template-dir`), que **leen los subagentes**. Único contrato fijo: entrada `test-e2e-desc/`, salida en `src/test/e2e/<iniciativa>/`. Única parte específica del motor: la **gestión de la app** (§2.2).
- **Requiere `/sdd-debug-with-test-e2e-desc`** ejecutado: la entrada es su carpeta `test-e2e-desc/` ya depurada. Si no existe → **ERROR**.
- **Localizar** (§4): ruta explícita, o auto-detectar la **última** iniciativa con `test-e2e-desc/tests-e2e-desc.md` y **confirmar**. **MUST NOT** usar `mtime`.
- **Solo `[x]`** (§2.6): se materializan únicamente los tests que pasaron al depurar; los `[ ]` no.
- **Snapshot** (§7): el `.desc.md` copiado lleva cabecera-banner ("NO editar a mano"); es regenerable desde `.sdd/`. Idempotente: salta los que ya tienen `.spec.ts` (salvo `--fresh`).
- **App por el motor** (§2.2, §8): arrancar tracked bg, limpiar puerto, sondear `200`, parar por puerto al final. **MUST NOT** dejar que un subagente arranque la app.
- **Helper de auth** (§7, §9.0): comprobar `_support/auth.ts` al lanzar (crearlo si no existe) y **validar login/logout contra la app real una vez** antes de generar nada (selectores best-effort); si está roto, corregir el helper. `BLOQUEADO` → **STOP**.
- **Separación de poderes anti-trampa** (§9): **tres subagentes en contextos aislados** por test — **generador** (crea, no juzga), **verificador** (independiente, audita fidelidad), **sanador** (independiente, arregla). El veredicto rojo/verde lo da el **runner mecánico** (`npx playwright test`), no un agente. El generador **MUST NOT** declarar si pasa; el verificador **MUST NOT** tocar el test; el sanador **MUST NOT** debilitar aserciones.
- **Bucle por test** (§9): generador → runner; si RED → sanador; si GREEN → verificador; si `INFIEL` → sanador. Tras `CORREGIDO`, reejecutar **y** volver a verificar; solo cierra con `OK` del verificador (**LIMIT** 8). En secuencia, nunca en paralelo ni `run_in_background`.
- **La causa por defecto del fallo es el test, no el código** (§2.3): como el test **ya pasó** en `/sdd-debug-with-test-e2e-desc`, un `.spec.ts` que falla se arregla **en el `.spec.ts`**. **MUST NOT** editar `.sdd/` ni `src/main/...`. Solo si demostrablemente es la app la que cambió, es una **regresión** → `BLOQUEADO`, reportar, no ocultar.
- **Contrato de tokens**: literal exacto — `ESCRITO:` (generador), `OK:`/`INFIEL:` (verificador), `CORREGIDO:`/`BLOQUEADO:` (sanador), `AUTH-OK` (validador). Los subagentes **MUST NOT** pegar contenido que ya está en disco.

---

## Apéndice A — Override de rutas (para testing y versatilidad)

- `--template-dir=<ruta>` — **carpeta de plantillas** alternativa a `template-system/`. **MUST** contener un `README.md` redactado para los **tres roles** (generador, verificador, sanador) y con una sección «Gestión de la app»; si falta → **ERROR**.
- `--in=<ruta>` — carpeta `test-e2e-desc/` de entrada explícita. **Desactiva la auto-detección** de la Fase 0 caso 2.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`.
- `--fresh` — regenera **todos** los tests `[x]` seleccionados aunque su `.spec.ts` ya exista (en uso normal no se especifica: se saltan los existentes).

En uso normal no se especifican: se usa la carpeta `template-system/`, la última iniciativa y `.sdd/drafts/`.
