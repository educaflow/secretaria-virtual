---
name: sdd-create-tests-e2e
description: Paso del pipeline SDD posterior a `/sdd-debug-with-test-e2e-desc`. Dada una iniciativa cuya carpeta `test-e2e-desc/` ya está descompuesta y depurada (un índice `tests-e2e-desc.md` con checkbox por test + un `t-NNN-<slug>.desc.md` autocontenido por test), persiste como tests de regresión Playwright los tests que pasaron (`[x]`) y los marcados no automatizables (`[-]`), nunca los `[ ]`: copia cada `t-NNN-<slug>.desc.md` a la carpeta destino bajo `src/test/e2e/` como snapshot "as-tested" y genera su `t-NNN-<slug>.spec.ts` hermano, ejecutándolo contra la app real hasta que pasa (los `[-]` no se ejecutan: se persisten con la marca que el contrato declare, excluida por defecto de la suite, y se reportan como MANUAL). El skill es un MOTOR genérico y agnóstico al artefacto: aporta solo el flujo (localizar la iniciativa, cargar el contrato, seleccionar+copiar, arrancar la app, y por cada test generar→ejecutar→sanar) y delega TODO lo específico (cómo se genera el `.spec.ts`, cómo se resuelve la carpeta destino, el ciclo de login/logout, cómo se sana un test roto) en la guía `template-system/README.md` (configurable con `--template-dir`), que los subagentes leen como contrato. La salida son los pares `.desc.md` + `.spec.ts` bajo la carpeta destino y el helper `src/test/e2e/_support/auth.ts`.
handoffs:
  - label: Cerrar la iniciativa
    agent: sdd-close
    prompt: Cerrar la iniciativa en .sdd/drafts/{carpeta-iniciativa}/ tras crear los tests E2E de regresión — regenerar desde el código el CLAUDE.md + modelo.puml/png de cada sistema afectado y archivar el draft verbatim en .sdd/archive/.
allowed-tools: Bash, Read, Write, Edit, Skill, AskUserQuestion, Agent, Monitor
---

# sdd-create-tests-e2e

Eres un **motor de creación de tests E2E de regresión** del pipeline SDD: tomas la descomposición ya **depurada** de una iniciativa (`test-e2e-desc/`, producida por `/sdd-debug-with-test-e2e-desc`), seleccionas los tests que pasaron (`[x]`) y los marcados no automatizables (`[-]`, §2.6) —nunca los `[ ]`— y, por cada uno, **copias su descripción** a la carpeta destino bajo `src/test/e2e/` (`{destino}`, §1.4) y **generas su `.spec.ts`** pilotando la app real, dejándolo verde (los `[-]` no se ejecutan: §9.4). La salida son tests Playwright versionados que reproducen la cobertura ya verificada.

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
- **Identificador de test concreto** (`T-007`) o una lista (`T-001 T-007`) para materializar solo esos (si no están `[ ]`). Sin esto, se materializan **todos** los pendientes: los `[x]` y los `[-]` (§2.6).
- **Sin argumentos** → auto-detección de la **última** iniciativa de `.sdd/drafts/` que contenga `test-e2e-desc/tests-e2e-desc.md` (§4.2).
- Flags de override `--template-dir=`, `--in=`, `--out=`, `--root=`, `--fresh` (Apéndice A).

---

## Outline

1. **Fase 0 — Localizar** la iniciativa y su `test-e2e-desc/`, y **confirmar** la ruta detectada (§4).
2. **Fase 1 — Cargar** el contrato (`template-system/README.md`), leer sus tres secciones que ejecuta el motor —«Carpeta destino», «Gestión de la app» y «Puerta de regresión»—, resolver con la primera la **carpeta destino** `{destino}` (§1.4) y componer las demás rutas (§5).
3. **Fase 2 — Seleccionar y copiar** (§7): leer el índice, quedarse con los tests `[x]` (vía normal) y `[-]` (vía manual, §2.6) y descartar los `[ ]`, resolver por **identidad** (`id:` + `Origen ESC`) los destinos ya ocupados —saltar los ya materializados (salvo `--fresh`), registrar las colisiones con otra iniciativa—, copiar cada `.desc.md` a `{destino}/` con la cabecera-banner de snapshot, y **comprobar el helper `_support/auth.ts` (crearlo si no existe)**.
4. **Fase 3 — Arrancar la app** (la gestiona el motor, §2.2): dejarla respondiendo `200` antes de generar el primer test (§8).
5. **Fase 4 — Generar, verificar y sanar** (§9), tres subagentes aislados por test:
   - **§9.0 — validar el helper de auth** (login/logout) contra la app real **una vez**; si está roto, corregirlo antes de generar nada.
   - Por cada test, en orden: **generador** escribe `t-NNN-<slug>.spec.ts` (no juzga) → el motor lo **ejecuta** con el runner real → si **verde**, el **verificador** independiente audita que es fiel (`OK`/`INFIEL`).
   - si **RED** o **INFIEL** → bucle **sanador** → reejecutar y volver a verificar (**LIMIT** 8 ciclos). Como el test ya pasó al depurar, la causa por defecto es el `.spec.ts`, no el código (§2.3). Si no se logra, se trata como **fallo por test** (§2.7): registrar, borrar el `.spec.ts` y seguir.
   - los tests de la **vía manual** (`[-]`) van por §9.4: se generan y se **verifican**, pero **no se ejecutan** (nadie puede hacerlo sin una persona delante).
6. **Fase 5 — Puerta de regresión** sobre **toda** la suite `src/test/e2e` (§10.1, la define el README en su sección «Puerta de regresión»), **reportar** el listado final SUCCESS/FAIL y **parar la app** (§10). **CRITICAL**: la puerta corre **con la app aún viva**, antes de pararla.

**CRITICAL — el skill es AUTÓNOMO por defecto** (§2.7): la **única** pregunta al usuario es en la **Fase 0** (qué iniciativa trabajar, como todos los `/sdd-*`); a partir de ahí **MUST NOT** usar `AskUserQuestion`. Un test que no se puede crear se **registra** en `fail_create_tests.log`, se **borra** su `.spec.ts` a medias y se **salta al siguiente**.

**Condiciones de ABORTO global** (solo estas detienen toda la pasada; **ERROR** con mensaje, sin preguntar):

- `--template-dir=` apunta a una carpeta que **no contiene `README.md`** → **ERROR** y detente.
- No se encuentra ninguna carpeta `test-e2e-desc/` con `tests-e2e-desc.md` → **ERROR**: indica que hay que ejecutar antes `/sdd-debug-with-test-e2e-desc` y detente.
- El `README.md` del contrato **no tiene** la sección «Carpeta destino», o la carpeta destino no se puede resolver aplicándola (§1.4): falta el `design/design.md`, falta el dato que la sección pide, su valor no valida, la carpeta de código que declara no existe, o el destino resulta ambiguo → **ERROR** con el motivo y detente. **MUST NOT** caer al nombre del draft. **Excepción**: con `--out=` (Apéndice A) el destino viene dado y esta resolución no se ejecuta.
- El índice **no tiene ningún test `[x]` ni `[-]`** → **ERROR** e informa: no hay nada que materializar.
- La app no responde `200` en `http://localhost:8080` tras arrancarla → **ERROR**: indica revisar `src/test/e2e/.app.log` y detente (sin la app no se puede generar nada).
- El **validador de auth** (§9.0) devuelve `BLOQUEADO` → **ERROR**: el helper de auth afecta a **todos** los tests; detente e indica el motivo (sin auth válida, todo fallaría).

**Fallos por test** (autónomos, **NO** abortan; §2.7): el **generador** o el **sanador** devuelven `BLOQUEADO`, o se agotan los **8** ciclos de sanación → registrar en `fail_create_tests.log`, borrar el `.spec.ts` a medias, marcar FAIL y continuar con el resto. **MUST NOT** tocar código Java para forzar un test.

---

## 1. Entrada y salida

### 1.1 Entrada

La carpeta `test-e2e-desc/` de una iniciativa, **ya descompuesta y depurada** por `/sdd-debug-with-test-e2e-desc`:

- el índice `tests-e2e-desc.md` (`type: test-e2e-index`) con una línea `- [x]` / `- [ ]` / `- [-]` por test (§2.6), y
- un `t-NNN-<slug>.desc.md` (`type: test-e2e`, `id: T-NNN`) **autocontenido** por test (cabecera con `Estado inicial de la base de datos` + tabla de credenciales + bloque del test).

El skill **no asume su estructura interna**: la conoce el subagente leyendo el contrato. Solo necesita el índice para saber **en qué estado está cada test** (§2.6) y a qué fichero apunta.

**REQUIRED — segunda entrada obligatoria**: el `design/design.md` de la misma iniciativa. El motor **no** conoce su estructura: lee de él **solo** lo que le indiquen las secciones «Carpeta destino» (resuelve `{destino}`, §1.4) y «Puerta de regresión» (qué hacer con un test rojo de otra iniciativa, §10.1) del README del contrato. Su ausencia es **ERROR** (aborto global), salvo con `--out=` (Apéndice A): ese modo fija el destino a mano y, sin `design.md`, la puerta de regresión (§10.1) trata **cualquier** rojo ajeno como REGRESIÓN.

### 1.2 Salida

- En `{destino}/`: por cada test seleccionado, un par `t-NNN-<slug>.desc.md` (copia-snapshot con cabecera-banner) + `t-NNN-<slug>.spec.ts` — verde para los `[x]`; para los `[-]`, marcado como manual y **no ejecutado** (§9.4).
- En `src/test/e2e/_support/auth.ts`: el helper de login/logout compartido (creado si no existe; su contenido lo define el contrato).
- En `{destino}/fail_create_tests.log`: una entrada por cada test que **no** se pudo crear (autónomo, §2.7), separada por `--------------------`. Su `.spec.ts` a medias se borra.
- En la conversación: el listado final SUCCESS/FAIL por test (§10).

**MUST NOT** modificar `test-e2e-desc/` ni ningún artefacto de `.sdd/` (es la fuente; el destino en `src/test/e2e/` es una copia regenerable). **MUST NOT** modificar código Java: este skill solo crea tests; si un test no pasa por un fallo de la app, es una **regresión** que se reporta, no se oculta.

### 1.3 Estructura de carpetas

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
├── design/
│   └── design.md                        ← ENTRADA: de él salen la carpeta destino (§1.4) y el criterio de la puerta (§10.1)
└── test-e2e-desc/                       ← ENTRADA (la dejó /sdd-debug-with-test-e2e-desc)
    ├── tests-e2e-desc.md            ← índice con checkbox por test
    └── t-001-<slug>.desc.md … t-NNN-<slug>.desc.md

src/test/e2e/                            ← SALIDA
├── _support/
│   └── auth.ts                          ← helper login/logout (creado si falta)
└── {destino}/                          ← REPLICA la ruta del código que se prueba (§1.4)
    ├── t-001-<slug>.desc.md         ← snapshot "as-tested" (con cabecera-banner)
    └── t-001-<slug>.spec.ts         ← test Playwright generado
```

### 1.4 CRITICAL — la carpeta destino replica la del código, y la declara el README

En este skill `{destino}` es **siempre** la carpeta destino resuelta en la Fase 1 (§5). Vive **bajo `src/test/e2e/`** y es el **espejo de la carpeta del código que se prueba**. **MUST NOT** derivarla del nombre del draft: dos iniciativas sobre el mismo código comparten carpeta de tests, igual que comparten carpeta de código.

**CRITICAL — cómo se resuelve lo declara la plantilla, no este skill.** Qué se lee del `design/design.md`, cómo se valida y con qué se compone la ruta está en la sección **«Carpeta destino»** del `README.md` del contrato, que el motor lee en la Fase 1 (§5) y sigue al pie de la letra. Distintos artefactos la resuelven de forma distinta, y cambiar `--template-dir` **MUST** poder cambiarla sin tocar este skill.

Reglas que el motor **MUST** cumplir, sea cual sea la plantilla:

1. `{destino}` **MUST** empezar por `src/test/e2e/` y tener al menos un segmento más.
2. `{destino}` **MUST** ser **una sola** carpeta, no una lista: un destino ambiguo es **ERROR**.
3. Si la sección «Carpeta destino» del README no existe, o su procedimiento no se puede completar (falta el `design.md`, falta el dato que pide, su valor no valida, o la carpeta de código que declara no existe) → **ERROR** con el motivo y detente. **MUST NOT** inventar la carpeta ni caer al nombre del draft.
4. Con `--out=` (Apéndice A) el destino viene dado y esta resolución **no** se ejecuta.

- ❌ INCORRECTO: `src/test/e2e/deshabilitar-certificado-digital/` (nombre del draft, no replica el código)
- ❌ INCORRECTO: `src/test/e2e/` a pelo (sin segmento propio: mezclaría los tests de todo el proyecto)
- ❌ INCORRECTO: que el motor deduzca el destino de memoria en vez de aplicar la sección «Carpeta destino» del README

**Carpetas fuera del espejo**: en `src/test/e2e/` conviven tests que no pertenecen a ningún sistema (p.ej. `login/`, `seed.spec.ts`) o anteriores a esta convención. **MUST NOT** tocarlas ni migrarlas: este skill solo escribe en el destino que resuelve. La puerta de regresión (§10.1) sí las ejecuta, porque corre la suite entera.

**Colisión de nombres entre iniciativas**: como la numeración `t-NNN` es local a cada iniciativa y ahora varias comparten carpeta, un `t-NNN-<slug>` de esta iniciativa puede chocar con uno ya persistido por otra. La **identidad** de un test persistido es la pareja (`id:`, `Origen ESC`) de su `.desc.md`, no su nombre de fichero. Al copiar (§7), comparando el `.desc.md` de origen con el que ya haya en el destino:

- **Misma identidad** → es el mismo test ya materializado: aplica la idempotencia de §2.5 (saltar, salvo `--fresh`).
- **Distinta identidad** → es una colisión real entre iniciativas: **MUST NOT** sobrescribir ni renumerar (renumerar desincronizaría el nombre del fichero con el `id:` del frontmatter, que §7 paso 4 prohíbe tocar y del que dependen la trazabilidad de `generation.md`, el informe §10 y `fail_create_tests.log`). Trátalo como **fallo por test** (§2.7) con motivo `colisión de nombre con {ruta} (identidad distinta)` y sigue con el siguiente.

---

## 2. Principios (aplican a todas las fases)

### 2.1 El README es el contrato único

Todo lo específico (cómo generar el `.spec.ts`, el ciclo de autenticación, la plantilla del test y del helper, cómo sanar un test roto y qué **MUST NOT** tocarse) lo define `template-system/README.md` y los ficheros que él referencie. Los subagentes los **leen de disco**; el skill **MUST NOT** asumirlos ni hardcodearlos. El skill solo pasa a cada subagente **las rutas** de los ficheros de entrada y su rol.

**CRITICAL — `README.md` es el ÚNICO fichero de la plantilla que el motor conoce por nombre.** Los demás los descubren los subagentes leyéndolo. Único acoplamiento por nombre: `README.md` (contrato), la entrada (`test-e2e-desc/`) y la salida (`{destino}/`).

### 2.2 La app la gestiona el MOTOR (siguiendo el README, como el destino y la puerta de regresión)

El generador pilota la app real y el motor ejecuta los `.spec.ts` contra ella, así que la app es un **recurso compartido** que **MUST** sobrevivir entre subagentes. Por eso **la gestiona el motor**, no los subagentes (un proceso en segundo plano lanzado por un subagente muere al cerrarse su contexto). Los comandos concretos (arrancar, parar por puerto, sondear `200`, dónde va el log) los define `README.md` en su sección **«Gestión de la app»**, que el motor lee en la Fase 1 y sigue al pie de la letra.

Reglas que el motor **MUST** cumplir:

- **Arrancar** la app como **tarea tracked en segundo plano** (`Bash` con `run_in_background: true`), redirigiendo el log al fichero que indique el README, y **esperar** sondeando hasta `200` (**LIMIT** de sondeo amplio; el arranque hace un `clean build`). **MUST NOT** dejar que un subagente arranque la app.
- **CRITICAL — limpiar el puerto de verdad antes de arrancar**: una instancia previa colgada hace que el connector falle el bind en silencio. Sigue el procedimiento de limpieza del README.
- **Parar** siempre **por puerto**, nunca por handle de proceso. Parar la app al terminar (§10).
- El arranque es **idempotente**: comprueba el `200` y arranca solo si no responde. **MUST NOT** levantar una segunda instancia.
- **CRITICAL — higiene de sesiones de navegador entre subagentes**: una sesión MCP de Playwright o un Chromium headless **huérfano** (de un subagente cuyo contexto se cerró) bloquea al siguiente generador/sanador durante minutos. El motor **MUST** barrer esos procesos huérfanos **antes de lanzar cada generador**, siguiendo la sección «Gestión de la app» del README (**MUST NOT** matar el server MCP de Playwright). Los subagentes, por su parte, **MUST** cerrar su sesión de navegador al terminar (lo exige el contrato).

### 2.3 La causa por defecto de un fallo es el `.spec.ts`, no el código

- **CRITICAL** — el test **ya pasó** al depurar con `/sdd-debug-with-test-e2e-desc` (por eso está `[x]` y este skill se ejecuta justo después). Por tanto, cuando un `.spec.ts` recién generado falla, la causa **por defecto es el propio `.spec.ts`** (locator, timing, equivalencia de textos, selectores de auth): **se arregla el test, NUNCA el código**. Por eso existe el bucle generar→ejecutar→verificar→sanar (§9).
- **MUST NOT** modificar código Java (`src/main/...`) ni la carpeta `test-e2e-desc/` ni nada bajo `.sdd/` (es la fuente de verdad; el `.desc.md` de `src/test/e2e/` es una copia regenerable).
- **Excepción** — solo si el fallo demostrablemente **no** es del `.spec.ts` sino de que la app se comporta distinto a lo que la descripción ya depurada espera, es una **regresión de la app**: el sanador devuelve `BLOQUEADO` y el motor lo trata como **fallo por test** (§2.7: registrar en `fail_create_tests.log`, borrar el `.spec.ts` y seguir). **MUST NOT** ocultarla tocando código ni debilitando aserciones.

### 2.4 Orquestación de subagentes

- Los **generadores**, **verificadores** y **sanadores** corren **de uno en uno y en secuencia** (§9): comparten el puerto 8080 y se pisarían en paralelo. Para un mismo test van en contextos **distintos** (aislados), que es justo lo que evita las trampas.
- **MUST NOT** lanzar subagentes en paralelo ni con `run_in_background` (salvo el arranque de la app, que sí es background tracked y lo hace el motor, no un subagente).
- Cada rol responde con un **token literal** que el skill parsea (definidos en cada fase). El skill compara por literal exacto.
- Los subagentes **MUST NOT** usar `AskUserQuestion`: ante un bloqueo lo reportan con el token de su rol y el **motor lo gestiona de forma autónoma** (§2.7).

### 2.5 Idempotencia y progreso por la presencia del `.spec.ts`

El **checkpoint** es el propio `.spec.ts`: un test materializado y verde es un `.spec.ts` que existe y pasa. Al (re)invocar el skill, los tests cuyo `.spec.ts` ya existe **y corresponde al mismo test** se **descartan** (salvo `--fresh`, que regenera todos los seleccionados). Esto hace el skill reanudable sin un índice propio.

**CRITICAL — descartar exige identidad, no solo nombre de fichero**: como la carpeta destino la comparten varias iniciativas (§1.4), un `.spec.ts` con el mismo nombre puede ser **otro** test. **MUST** comparar la pareja (`id:`, `Origen ESC`) del `.desc.md` de origen con la del `.desc.md` hermano ya persistido: misma identidad → saltar; distinta → colisión, que se trata como fallo por test (§1.4, §2.7).

### 2.6 Solo se materializa lo verificado — y los tests manuales

El índice de entrada tiene **tres** estados por línea, y cada uno se trata distinto:

| Estado | Qué es | Qué hace este skill |
|---|---|---|
| `- [x]` | pasó contra la app real al depurar | **vía normal** (§9): generar → ejecutar → verificar → sanar hasta verde |
| `- [-]` | **no automatizable**: necesita una persona | **vía manual** (§9.4): generar y **verificar**, pero **NO** ejecutar |
| `- [ ]` | no pasó al depurar | **MUST NOT** materializarse: metería un test rojo en la suite |

**REQUIRED** — si se pide un id concreto que está `[ ]`, se ignora y se anota en `fail_create_tests.log` (§2.7).

**CRITICAL — un test de la vía manual NO pasa por la puerta del runner.** Nadie puede ejecutarlo de forma desatendida: por eso se persiste con la marca que declare el contrato (un tag que la suite **excluye por defecto**, también en CI/CD) y su snapshot **MUST** decir que no se verificó mecánicamente. Sigue pasando por el **verificador** (§9.2 paso 2), que es una auditoría estática y no necesita ejecutarlo. **MUST NOT** contarlo como test verde en el reporte final.

### 2.7 Autonomía por defecto y registro de fallos

**CRITICAL** — el skill funciona **sin intervención del usuario**, con **una única excepción**: en la **Fase 0** pregunta con `AskUserQuestion` sobre **qué iniciativa** trabajar (igual que el resto de `/sdd-*`). A partir de ahí el motor **MUST NOT** usar `AskUserQuestion` en ningún otro punto. Tras la Fase 0 hay tres desenlaces — los dos siguientes más la **REGRESIÓN** de la puerta final (§10.1: un test de otra iniciativa se pone rojo y la sección «Puerta de regresión» del README no autoriza a retirarlo → reportar y parar, **sin** retirar el test ni tocar código; la app se para igual, §10.2 paso 3):

1. **Aborto global** (§Outline): un fallo de setup que impide procesar **cualquier** test (sin input, app caída, auth no válida) → **ERROR** con mensaje y detente. No se pregunta.
2. **Fallo por test**: un test que **no se puede crear** (el generador o el sanador devuelven `BLOQUEADO`, o se agotan los **8** ciclos de sanación). En ese caso el motor **MUST**, en este orden:
   1. **Borrar** el `.spec.ts` a medias si existe: `rm -f {ruta del .spec.ts}` (no se deja un test roto en la suite). El `.desc.md` snapshot se **conserva** (es regenerable).
   2. **Anexar** (append, nunca sobrescribir) una entrada al fichero **`{destino}/fail_create_tests.log`** con **toda** la información del fallo, terminada por una línea separadora de 20 guiones.
   3. Marcar el test como **FAIL** y **continuar con el siguiente** (no abortar la pasada).

**Formato literal de cada entrada de `fail_create_tests.log`** (texto plano; el motor lo escribe, no un subagente):

```
T-NNN — <nombre del test>
Fase: <selección | generación | sanación (ciclo k/8)>
Motivo: <el texto tras "BLOQUEADO:" del subagente, o "8 ciclos de sanación agotados">
Detalle: <extracto de la salida del runner y/o del log de la app que explique qué pasó>
.spec.ts borrado: <ruta del .spec.ts eliminado>
--------------------
```

- ✅ CORRECTO: la entrada termina **siempre** con la línea `--------------------` (20 guiones), una entrada por test fallido, en **append**.
- ❌ INCORRECTO: sobrescribir el log, omitir el separador, dejar el `.spec.ts` a medias en disco, o lanzar `AskUserQuestion`.

---

## 3. Flujo general

```
┌──────────────────────────────────────────────────────────────────────┐
│  Fase 0  Localizar test-e2e-desc/ (ruta explícita | más nueva)        │
│            auto-detección → confirmar con AskUserQuestion (§4.2)    │
│  Fase 1  Cargar contrato (README: «Carpeta destino» + «Gestión de la │
│          app» + «Puerta de regresión») → {destino} = lo que resuelva │
│          «Carpeta destino» (§1.4)                                    │
│  Fase 2  Leer índice → [x] vía normal, [-] vía manual, [ ] fuera →   │
│          por identidad (id, Origen ESC):                             │
│          saltar / colisión → copiar .desc.md (banner) → _support/auth.ts │
│  Fase 3  Motor: arrancar la app (tracked bg) hasta 200               │
│  Fase 4  §9.0 validar _support/auth.ts (login/logout) una vez        │
│          Por cada test seleccionado (en secuencia, 3 contextos):     │
│            ├─ generador(test) ──► escribe t-NNN-<slug>.spec.ts       │
│            ├─ motor ejecuta el .spec.ts (runner real) ─► RED | GREEN │
│            ├─ GREEN → verificador(indep.) ─► OK | INFIEL             │
│            │           OK → siguiente test                           │
│            └─ RED | INFIEL → bucle (LIMIT 8): sanador → reejecutar    │
│                 → volver a verificar; no se logra → FAIL autónomo:   │
│                 log fail_create_tests.log + borrar .spec.ts + seguir │
│          (vía manual [-] §9.4: generar → verificar, SIN ejecutar,    │
│           bucle sanador↔verificador; se reporta MANUAL, no SUCCESS)  │
│  Fase 5  Puerta de regresión: suite COMPLETA con la app viva (§10.1) │
│            rojo ajeno → lo que diga «Puerta de regresión»; si no      │
│            autoriza retirarlo → REGRESIÓN: reportar y parar          │
│          → reporte final SUCCESS/FAIL → parar la app                 │
└──────────────────────────────────────────────────────────────────────┘
```

Las fases se ejecutan **en orden**; salvo la confirmación de iniciativa de la Fase 0, no se pide aprobación (skill autónomo, §2.7): un test que no se puede crear se registra y se salta; solo un fallo de setup global aborta la pasada con **ERROR**, y una **REGRESIÓN** en la puerta final se reporta y para (§10.1).

---

## 4. Fase 0 — Localizar el `test-e2e-desc/`

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca con una ruta a `test-e2e-desc/` (o a la carpeta de la iniciativa): comprueba que existe `test-e2e-desc/tests-e2e-desc.md`. La **carpeta de la iniciativa** es la que contiene `test-e2e-desc/`. Pasa a la Fase 1, que es quien resuelve la carpeta destino a partir del `design.md` (§1.4).

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

1. **REQUIRED — lee con `Read` la guía `template-system/README.md`** (resuelta contra `--template-dir`): confirma que existe (si no → **ERROR**), entiende a alto nivel qué pide a cada rol, y **lee las tres secciones que ejecuta el propio motor**: «Carpeta destino» (paso 2), «Gestión de la app» (comandos de las Fases 3/4) y «Puerta de regresión» (comando y criterio de la Fase 5, §10.1). Si falta alguna de las tres → **ERROR**. El resto del contrato lo leen los subagentes de disco.
2. **REQUIRED — resuelve `{destino}`** ejecutando **el procedimiento que declara la sección «Carpeta destino»** del README, sin añadir ni suponer nada (§1.4): qué leer del `design/design.md`, cómo validarlo y cómo componer la ruta lo dice esa sección. Comprueba después las dos invariantes del motor: que empieza por `src/test/e2e/` con al menos un segmento más, y que es **una sola** carpeta. Ante cualquier fallo del procedimiento o de las invariantes → **ERROR** y detente. Con `--out=` (Apéndice A) sáltate este paso entero: el destino ya viene dado.
3. **Resuelve** las demás rutas que se pasarán a los subagentes (no su contenido):
   - la ruta de `template-system/README.md` (las reglas),
   - la carpeta de entrada `{carpeta-iniciativa}/test-e2e-desc/` y su índice,
   - la carpeta de salida del paso 2 y el helper `src/test/e2e/_support/auth.ts`.
4. **Valida** que el índice `tests-e2e-desc.md` existe y tiene al menos una línea de test. Si no, **ERROR** y detente.

---

## 6. Fases con subagentes — patrón común del prompt

Cada prompt de subagente (§9) **MUST** pasar, además de su tarea específica:

- **Reglas**: `lee {ruta de template-system/README.md} y todos los ficheros que referencie. Es el contrato: define qué hacer, cómo y con qué estructura. Síguelo al pie de la letra.`
- **MUST NOT** usar `AskUserQuestion`: ante una duda que no puedan resolver, la reportan con el token de bloqueo de su rol.

---

## 7. Fase 2 — Seleccionar y copiar

1. **Lee el índice** `tests-e2e-desc.md`. Por cada línea `- [x] [T-NNN — <nombre>](t-NNN-<slug>.desc.md)`, registra `(T-NNN, fichero, vía normal)`; por cada `- [-]`, registra `(T-NNN, fichero, vía manual)` **y su motivo** (§2.6). **Descarta** las `- [ ]`. Si se pasaron ids concretos, filtra a esos (si alguno está `[ ]` se ignora y se anota en `fail_create_tests.log` con motivo "no materializable: `[ ]` en el índice", §2.7).
2. Si tras filtrar **no queda ninguno** → **ERROR** (aborto global: no hay tests verificados que materializar).
3. **Resuelve cada destino ya ocupado** en `{destino}/`. **CRITICAL — el disparador es el `.desc.md` destino, NO el `.spec.ts`**: un test fallido deja su `.desc.md` y borra su `.spec.ts` (§2.7), así que mirar solo el `.spec.ts` dejaría pasar una copia que pisaría el snapshot de otra iniciativa. Si existe el `.desc.md` destino, lee su (`id:`, `Origen ESC`) y compáralos con los del origen (§2.5):
   - **Distinta identidad** → **colisión** (§1.4): regístrala como fallo por test (§2.7) con motivo `colisión de nombre con {ruta} (identidad distinta)`. **MUST NOT** copiar ni sobrescribir un `.desc.md` ajeno.
   - **Misma identidad y su `.spec.ts` existe** → **descarta** el test (salvo `--fresh`).
   - **Misma identidad y su `.spec.ts` NO existe** (pasada anterior fallida) → **reprocesa**: sobrescribir su propio `.desc.md` es inocuo porque es el mismo test.

   Indica cuántos se saltan, cuántos colisionan y cuántos quedan.
4. Por cada test seleccionado, **copia** su `t-NNN-<slug>.desc.md` desde `test-e2e-desc/` a `{destino}/`, **anteponiendo la cabecera-banner de snapshot** (formato y contenido los define el contrato en el README; lleva el origen, `T-NNN`, `Origen ESC` y el aviso de "NO editar a mano"). **MUST NOT** alterar el resto del contenido del `.desc.md`.
5. **Comprueba el helper de auth `src/test/e2e/_support/auth.ts`** (la parte de login/logout): si **no existe**, créalo con la plantilla literal que define el contrato (guía de generación); si **ya existe**, **MUST NOT** sobrescribirlo. Su login/logout se **valida** contra la app real en la Fase 4 (§9.0) antes de generar ningún test, porque sus selectores son best-effort.

---

## 8. Fase 3 — Arrancar la app (la gestiona el motor)

Antes de generar el primer test, el motor deja la app respondiendo `200` siguiendo la sección **«Gestión de la app»** del README (§2.2): limpia el puerto, arranca como tarea tracked en segundo plano con el log al fichero indicado, y sondea hasta `200`. Si tras el sondeo no responde `200` → **ERROR** (aborto global): indica revisar `src/test/e2e/.app.log` y detente. **MUST NOT** continuar a la Fase 4 sin la app en `200`.

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

El motor parsea: `AUTH-OK` → continúa al bucle por test; `BLOQUEADO: …` → **ERROR** (aborto global): el helper de auth afecta a **todos** los tests, así que sin él validado no se genera nada; detente indicando el motivo. **MUST NOT** generar tests con un helper de auth no validado.

Recorre luego los tests seleccionados **en orden**. Los de **vía normal** (`[x]`) siguen §9.1 → §9.2 → §9.3; los de **vía manual** (`[-]`, §2.6) siguen §9.4. Para cada test:

### 9.1 Lanzar el generador

**Antes** de lanzarlo, el motor **MUST** barrer las sesiones de navegador huérfanas (README, sección «Gestión de la app»): un Chromium/worker colgado del test anterior bloquearía a este generador. **MUST NOT** matar el server MCP.

Lanza **un** subagente con `Agent` (`subagent_type: claude`, `run_in_background: false`). Recibe la ruta de **su** `.desc.md` (la copia en `{destino}/`), la ruta destino del `.spec.ts` hermano, la ruta del helper `_support/auth.ts` y el contrato.

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

El skill parsea la primera línea: `ESCRITO:` → continúa al paso 9.2; `BLOQUEADO:` → **fallo por test** (§2.7): registra la entrada en `fail_create_tests.log` (Fase: generación; Motivo: el texto del `BLOQUEADO`), borra el `.spec.ts` si quedó a medias, marca FAIL y **pasa al siguiente test**. **CRITICAL** — el generador **MUST NOT** declarar si el test pasa: solo lo escribe. Quién decide si pasa son el runner mecánico y el **verificador independiente** (§9.2), nunca el que lo escribió.

### 9.2 Ejecutar (runner mecánico) y verificar (subagente independiente)

**CRITICAL — separación de poderes anti-trampa**: el que **crea** el test no es el que **decide si vale**. El veredicto sale de (a) el runner real, imposible de falsear, y (b) un verificador en **contexto aislado** que no escribió el test.

1. **El motor ejecuta el `.spec.ts`** con `Bash` (comando del README; típicamente `npx playwright test {ruta} --project=chromium --reporter=line` — `--reporter=line` evita que el reporter `html` cuelgue el comando abriendo el informe al fallar). El exit code es el **veredicto objetivo** rojo/verde (el motor no lo delega: un subagente podría mentir sobre el resultado).
   - **RED** (exit ≠ 0) → al **bucle de sanación** (§9.3) con la salida del runner.
   - **GREEN** (exit 0) → al **verificador** (paso 2): pasar verde no basta, hay que comprobar que el test **es fiel** a la descripción (un test verde pero con aserciones débiles o saltadas sería una trampa).
2. **Lanza el subagente verificador** (`Agent`, `subagent_type: claude`, `run_in_background: false`) — **independiente del generador**. Audita la fidelidad del `.spec.ts` (verde) contra su `.desc.md`. **MUST NOT** escribir ni "arreglar" el test (lo escribió otro): solo dictamina.

   **Prompt del subagente verificador**:

   > Eres un revisor **adversarial** de tests E2E de la secretaría virtual (Axelor). Tu trabajo es **detectar si un test, aunque pase en verde, es infiel o tramposo** respecto a su descripción. NO escribas ni modifiques el test.
   >
   > - **Reglas para la verificación**: lee `{ruta de template-system/README.md}` y los ficheros que referencie —en particular el contrato de **verificación** (qué hace fiel a un test: cubrir con aserciones reales todo lo que la descripción declara como resultado esperado, login/logout del usuario correcto, sin aserciones debilitadas/triviales ni pasos saltados)—. **Carga `/k-playwright`** si lo necesitas.
   > - **Test a auditar** (ya pasó en verde): lee `{ruta del .spec.ts}` y su descripción `{ruta del .desc.md}` (autocontenida).
   > - Comprueba punto por punto que el `.spec.ts` materializa fielmente **todo** lo que declara la descripción, según el contrato de verificación. Sospecha de: aserciones ausentes para un punto del resultado esperado, `expect(true)`/aserciones triviales, pasos comentados o saltados, login con otro usuario, `toBeVisible` sobre algo que siempre está.
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
   >   - `BLOQUEADO: {T-NNN} — {motivo}` — el fallo no es del `.spec.ts` (posible regresión de la app o recurso del entorno).

3. Interpreta el token:
   - `BLOQUEADO: …` → **fallo por test** (§2.7): registra en `fail_create_tests.log` (Fase: sanación; Motivo: el texto del `BLOQUEADO`; Detalle: el fallo del runner), **borra** el `.spec.ts` (`rm -f`), marca FAIL y **pasa al siguiente test**. **MUST NOT** preguntar al usuario ni tocar código Java.
   - `CORREGIDO: …` → **reejecuta** el `.spec.ts` (§9.2 paso 1); si vuelve a estar verde, **vuelve a verificar** (§9.2 paso 2). El ciclo solo termina con `OK` del verificador.
     - `OK` → sal del bucle (siguiente test).
     - sigue `RED`/`INFIEL` → incrementa `ciclo`. Si `ciclo <= 8`, repite. Si `ciclo > 8` → **fallo por test** (§2.7): registra en `fail_create_tests.log` (Fase: sanación (ciclo 8/8); Motivo: "8 ciclos de sanación agotados"; Detalle: último fallo del runner), **borra** el `.spec.ts` (`rm -f`), marca FAIL y pasa al siguiente test.

**MUST NOT** superar los **8** ciclos por test.

- ✅ CORRECTO (generador): `ESCRITO: src/test/e2e/subsystem/sistemaeducativo/t-001-crear-un-grupo-con-sus-alumnos.spec.ts`
- ✅ CORRECTO (verificador): `OK: T-001` / `INFIEL: T-007 — no asierta que la matrícula de honor cuenta como 10 en la media`
- ✅ CORRECTO (sanador): `CORREGIDO: T-001` / `BLOQUEADO: T-014 — el botón "Reabrir" no aparece para el alumno; posible regresión`
- ❌ INCORRECTO: que el **generador** diga si pasa, que el **verificador** edite el test, que el **sanador** borre aserciones para que pase, o pegar el contenido del `.spec.ts` en la respuesta.

### 9.4 Vía manual — tests que ninguna automatización puede ejecutar

Para los tests seleccionados como **vía manual** (`[-]` en el índice, §2.6). Se diferencian de la vía normal en **una sola cosa**: **el motor NO ejecuta el `.spec.ts`**, porque nadie puede ejecutarlo sin una persona delante. Todo lo demás se mantiene.

1. **Lanza el generador** (§9.1) con el mismo prompt, **añadiéndole** estas dos líneas:
   > - **Este test es MANUAL**: el índice lo marcó no automatizable por este motivo: `{motivo de la línea [-]}`. Genera el `.spec.ts` **completo** —todo el camino automatizable y **todas** las aserciones—, marcado como manual **según el contrato** (la guía de generación dice qué tag lleva, cómo se escribe la espera del paso humano y qué timeout fija). **MUST NOT** omitir aserciones ni dejar el test vacío.
   > - **MUST NOT** ejecutarlo tú ni declararlo verde: no se puede ejecutar de forma desatendida.
2. **NO ejecutes el runner** sobre este `.spec.ts`. **MUST NOT** lanzarlo "a ver qué pasa": se quedaría colgado esperando a una persona y consumiría el timeout entero.
3. **Lanza el verificador** (§9.2 paso 2) igual que siempre, **añadiéndole**:
   > - **Este test es MANUAL y NO se ha ejecutado**: audita solo su **fidelidad** a la descripción; **MUST NOT** exigir que esté verde ni penalizar la espera del paso humano que el contrato prescribe.
   - `OK:` → el test queda materializado como **MANUAL**; pasa al siguiente.
   - `INFIEL:` → **bucle de sanación** (§9.3) con el motivo, **sin** reejecución: el ciclo es sanador → verificador, y termina con el `OK` del verificador. Mismo **LIMIT** de 8 ciclos y mismo tratamiento al agotarse (fallo por test, §2.7).
4. **Registra** el test como `MANUAL` para el reporte final (§10.3), **nunca** como SUCCESS.

**CRITICAL — la vía manual no es un atajo.** Solo entran los tests que el índice marcó `[-]`. **MUST NOT** desviar por aquí un test `[x]` porque su `.spec.ts` no consiga ponerse verde: eso es un fallo por test (§2.7), y colarlo por la vía manual metería en la suite un test roto disfrazado de no automatizable.

---

## 10. Fase 5 — Puerta de regresión, reporte final y parar la app

### 10.1 Puerta de regresión (con la app aún viva)

**CRITICAL** — con todos los tests de la iniciativa verdes y verificados, y **antes** de parar la app, el motor **MUST** ejecutar la **suite completa** siguiendo la sección **«Puerta de regresión»** del `README.md` del contrato (que el motor **MUST** haber leído en la Fase 1, §5 paso 1, junto con «Gestión de la app»). Esa sección define el comando y **qué hacer con cada test rojo de una iniciativa anterior**; el motor la aplica tal cual y **MUST NOT** completarla de memoria. Lo único que el motor fija, sea cual sea la plantilla: un rojo ajeno que la sección **no** autoriza a retirar es una **REGRESIÓN** → reportar y **parar**, sin retirar el test y **sin tocar código**. Hay plantillas cuya sección **no autoriza retirar nada**: entonces **todo** rojo ajeno es REGRESIÓN.

Es la salvaguarda que hace viable compartir carpeta entre iniciativas (§1.4): sin ella, una iniciativa puede romper los tests de otra sin que nadie se entere.

### 10.2 Reporte final y parada

1. **MUST** mostrar en la conversación el listado final de los tests procesados en esta pasada:

```
Tests E2E de regresión creados — {destino}/

SUCCESS  T-001 — Crear un grupo con sus alumnos        → t-001-crear-un-grupo-con-sus-alumnos.spec.ts
FAIL     T-014 — El alumno consulta sus notas          → ver fail_create_tests.log (8 ciclos agotados)
MANUAL   T-017 — El interesado firma la solicitud      → t-017-el-interesado-firma-la-solicitud.spec.ts (no ejecutado)
SUCCESS  T-016 — Crear un grupo sin curso              → t-016-crear-un-grupo-sin-curso.spec.ts
...

Resumen: {P} SUCCESS / {F} FAIL / {M} MANUAL  ({S} saltados por ya existir, {C} colisiones).
Puerta de regresión (suite completa): {R} passed / {X} failed.
  REGRESIÓN    t-0NN-<slug>.spec.ts (iniciativa {otra}) → NO retirado, requiere revisión
  {si la sección «Puerta de regresión» del README contempla retirar tests, una línea por cada
   uno retirado, con el motivo que esa sección exija}
```

2. Para cada `FAIL` indica el motivo (ciclos agotados / bloqueo / colisión) y que el detalle está en `{destino}/fail_create_tests.log` (§2.7). **MUST NOT** ocultar fallos ni declarar éxito si algún test quedó en FAIL. Los `.spec.ts` de los FAIL **ya se borraron** (§2.7), así que los tests **de esta iniciativa** quedan todos verdes; el estado de la suite completa lo dice la puerta de regresión (§10.1), que **MUST** reportarse aunque sea roja. Indica los comandos: `npx playwright test {destino}` (esta iniciativa) y `npx playwright test src/test/e2e` (suite completa).
   Para cada `MANUAL` (§9.4) indica el motivo por el que no es automatizable y **avisa de que no se ha ejecutado**: existe en la suite pero está **excluido por defecto**, y se lanza con el comando que declare la sección correspondiente del README del contrato. **MUST NOT** sumarlos a los `SUCCESS` ni presentar la pasada como completamente verificada si hay alguno.
3. **Parar la app** por puerto (§2.2).

---

## Quick Guidelines

- **CRITICAL — agnosticismo**: este SKILL es un **motor de flujo**; **no sabe** cómo se genera ni se sana un `.spec.ts`. Todo lo específico lo define `template-system/README.md` (configurable con `--template-dir`), que **leen los subagentes**. Único contrato fijo: entrada `test-e2e-desc/`, salida bajo `src/test/e2e/`. Lo único que el motor ejecuta por su cuenta son las **tres secciones del README** que le van dirigidas: «Carpeta destino» (§1.4), «Gestión de la app» (§2.2) y «Puerta de regresión» (§10.1) — y las sigue al pie de la letra, sin completarlas de memoria.
- **Requiere `/sdd-debug-with-test-e2e-desc`** ejecutado: la entrada es su carpeta `test-e2e-desc/` ya depurada. Si no existe → **ERROR**.
- **Localizar** (§4): ruta explícita, o auto-detectar la **última** iniciativa con `test-e2e-desc/tests-e2e-desc.md` y **confirmar**. **MUST NOT** usar `mtime`.
- **Destino = espejo del código, y lo declara el README** (§1.4): `{destino}` lo resuelve el motor aplicando la sección «Carpeta destino» del contrato; **MUST** empezar por `src/test/e2e/` y ser una sola carpeta. Varias iniciativas sobre el mismo código comparten carpeta. **MUST NOT** derivarla del nombre del draft ni deducirla de memoria; si no se puede resolver → **ERROR**.
- **Nunca los `[ ]`** (§2.6): los `[x]` van por la vía normal (hasta verde); los `[-]` (no automatizables) por la **vía manual** (§9.4): se generan y se **verifican**, pero **no se ejecutan**, se persisten con la marca que declare el contrato —la suite los excluye por defecto, también en CI/CD— y se reportan como `MANUAL`, jamás como SUCCESS. Los `[ ]` no se materializan.
- **Snapshot** (§7): el `.desc.md` copiado lleva cabecera-banner ("NO editar a mano"); es regenerable desde `.sdd/`. Idempotente: salta los que ya tienen `.spec.ts` (salvo `--fresh`).
- **App por el motor** (§2.2, §8): arrancar tracked bg, limpiar puerto, sondear `200`, parar por puerto al final. **MUST NOT** dejar que un subagente arranque la app. Barrer **sesiones de navegador huérfanas antes de cada generador** (sin matar el server MCP) y ejecutar el runner con `--reporter=line` para que no se cuelgue al fallar.
- **Autónomo por defecto** (§2.7): la **única** pregunta al usuario es en Fase 0 (qué iniciativa, como todos los `/sdd-*`). Después, **MUST NOT** `AskUserQuestion`: un test que no se puede crear (generador/sanador `BLOQUEADO` o 8 ciclos agotados) → registrar en `{destino}/fail_create_tests.log` (entrada + línea `--------------------`), **borrar su `.spec.ts`** y seguir. Solo un fallo de setup global (sin input, app caída, auth no válida) aborta con **ERROR**.
- **Helper de auth** (§7, §9.0): comprobar `_support/auth.ts` al lanzar (crearlo si no existe) y **validar login/logout contra la app real una vez** antes de generar nada (selectores best-effort); si está roto, corregir el helper. `BLOQUEADO` → **ERROR** (aborto global: afecta a todos los tests).
- **Separación de poderes anti-trampa** (§9): **tres subagentes en contextos aislados** por test — **generador** (crea, no juzga), **verificador** (independiente, audita fidelidad), **sanador** (independiente, arregla). El veredicto rojo/verde lo da el **runner mecánico** (`npx playwright test`), no un agente. El generador **MUST NOT** declarar si pasa; el verificador **MUST NOT** tocar el test; el sanador **MUST NOT** debilitar aserciones.
- **Bucle por test** (§9): generador → runner; si RED → sanador; si GREEN → verificador; si `INFIEL` → sanador. Tras `CORREGIDO`, reejecutar **y** volver a verificar; solo cierra con `OK` del verificador (**LIMIT** 8). En secuencia, nunca en paralelo ni `run_in_background`.
- **La causa por defecto del fallo es el test, no el código** (§2.3): como el test **ya pasó** en `/sdd-debug-with-test-e2e-desc`, un `.spec.ts` que falla se arregla **en el `.spec.ts`**. **MUST NOT** editar `.sdd/` ni `src/main/...`. Solo si demostrablemente es la app la que cambió, es una **regresión** → `BLOQUEADO`, reportar, no ocultar.
- **Contrato de tokens**: literal exacto — `ESCRITO:` (generador), `OK:`/`INFIEL:` (verificador), `CORREGIDO:`/`BLOQUEADO:` (sanador), `AUTH-OK` (validador). Los subagentes **MUST NOT** pegar contenido que ya está en disco.

---

## Apéndice A — Override de rutas (para testing y versatilidad)

- `--template-dir=<ruta>` — **carpeta de plantillas** alternativa a `template-system/`. **MUST** contener un `README.md` redactado para los **tres roles** (generador, verificador, sanador) y con las tres secciones que ejecuta el motor: «Carpeta destino», «Gestión de la app» y «Puerta de regresión»; si falta el `README.md` o alguna de las tres → **ERROR**.
- `--in=<ruta>` — carpeta `test-e2e-desc/` de entrada explícita. **Desactiva la auto-detección** de la Fase 0 caso 2. Si su iniciativa no tiene `design/design.md`, **MUST** acompañarse de `--out=` (sin uno de los dos no hay carpeta destino y es **ERROR**, §1.4).
- `--out=<ruta>` — carpeta destino explícita, **salta la resolución de §1.4** (no aplica la sección «Carpeta destino» ni valida nada del `design.md`). Es el único modo de ejercitar el skill en un sandbox sin un draft completo.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`.
- `--fresh` — regenera **todos** los tests seleccionados (`[x]` y `[-]`) aunque su `.spec.ts` ya exista (en uso normal no se especifica: se saltan los existentes).

En uso normal no se especifican: se usa la carpeta `template-system/`, la última iniciativa, `.sdd/drafts/` y el destino resuelto por §1.4.
