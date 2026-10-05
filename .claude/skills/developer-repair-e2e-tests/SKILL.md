---
name: developer-repair-e2e-tests
description: Deja en verde la suite E2E de Playwright (`src/test/e2e/`) arrancando la app, ejecutando los tests y, por cada uno que falla, diagnosticando con un subagente si lo que está mal es el test (desactualizado respecto a un cambio intencionado de la app) o el código de la app (regresión) y reparando lo que corresponda (un test desactualizado se actualiza, se renombra o, si lo que comprobaba ya no existe, se borra; nunca se crean tests nuevos); cada reparación la verifica sobre su diff alguien distinto de quien la hizo y la que falla se deshace; lo que no se puede decidir no se toca. No recibe entrada obligatoria; la salida es el fichero de estado `repair-e2e-test.md` de la raíz del proyecto (una línea `- [x] <test>` / `- [ ] <test>` por test), los `.spec.ts`/`.desc.md` o el código reparados, sin ningún commit, y un informe final en tabla. Es reanudable — si `repair-e2e-test.md` ya existe solo prueba los tests que no están en `[x]`.
---

# developer-repair-e2e-tests

Asumes el rol de **reparador de la suite E2E**: transformas una suite con tests en rojo en una suite en verde, arreglando en cada fallo lo que de verdad está mal (el test o la app) y dejando constancia test a test en `repair-e2e-test.md`.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).
Argumentos esperables:

- Vacío → pregunta el modo ante dudas (Fase 0).
- `--preguntar` / `--no-preguntar` → fija el modo ante dudas (§2.3) sin preguntarlo.

---

## Outline

1. **Preguntar** el modo ante dudas (Fase 0).
2. **Seleccionar** los tests a probar a partir de `repair-e2e-test.md` (Fase 1).
3. **Arrancar** la app (Fase 2).
4. **Ejecutar** los tests seleccionados y volcar el resultado (Fase 3).
5. **Reparar** cada test fallido, uno a uno: diagnosticar → reparar → verificar → comprobar, y deshacer la reparación que falla (Fase 4).
6. **Informar** (Fase 5).

**STOP conditions**:

- `npx playwright test --list` falla → **ERROR**: muestra el error y detente.
- No queda ningún test sin `[x]` → informa de que no hay nada que probar y termina sin arrancar la app.
- El puerto 8080 lo ocupa la instancia de `/build` (§4.2) → **ERROR**: no la toques y detente.
- La app no responde `200` en el arranque inicial (§4.2) → **ERROR**: indica revisar `src/test/e2e/.app.log` y detente.
- Tras deshacer una reparación (§6.8) la app no vuelve a arrancar → **STOP** y avisa al usuario: sin app no se puede seguir.
- Modo `preguntar` y un diagnóstico devuelve `DUDA` → **STOP** en ese test y pregunta al usuario (§6.3).
- Una reparación fallida no queda deshecha (**LIMIT**: 2 peticiones, §6.8) → **STOP** y lista al usuario los ficheros que siguen cambiados.

---

## 1. Entrada y salida

### 1.1 Entrada

- La suite `src/test/e2e/**/*.spec.ts` y sus descripciones hermanas `*.desc.md`.
- `repair-e2e-test.md` de la raíz del proyecto, si existe, de una ejecución anterior.

### 1.2 Salida

- `repair-e2e-test.md` (§2.1).
- Los `.spec.ts`/`.desc.md` sanados (tests actualizados, renombrados o borrados; ninguno nuevo) y el código de `src/main` arreglado, **sin commit**.
- El informe final en la conversación (§7).

---

## 2. Principios

### 2.1 Fichero de estado `repair-e2e-test.md`

Una línea por test, con esta forma exacta:

```markdown
- [x] <test>
- [x] <test> ⇒ <causa>: <qué se hizo>
- [ ] <test> ⇒ <causa>: <motivo>
```

- `<test>` es `<fichero .spec.ts relativo a src/test/e2e> › <describe> › <título>`, sin número de línea (cambia al sanar).
- Un test renombrado pasa a identificarse por su nombre nuevo y uno borrado se queda sin línea: lo resincroniza §6.6 (paso 5).
- `[x]` → el test pasa (de entrada o tras repararlo) o se da por terminado por omitido (§4.3). `[ ]` → no pasa o está sin procesar.
- `<causa>` es una de: `test desactualizado`, `bug de app`, `pasa en solitario`, `sin determinar`, `no reparado`, `omitido`.
- ✅ CORRECTO: `- [x] system/gestion/t-003-alta.spec.ts › Gestión › Alta de un cargo ⇒ test desactualizado: el campo «tipo» pasó a RadioSelect`
- ✅ CORRECTO: `- [ ] system/gestion/t-004-baja.spec.ts › Gestión › Baja de un cargo ⇒ sin determinar: el botón falta pero ningún cambio reciente lo explica`
- ❌ INCORRECTO: `- [x] t-003-alta.spec.ts:138 › Alta de un cargo` (ruta incompleta y con número de línea: no casa con el runner)
- ❌ INCORRECTO: `- [ ] system/gestion/t-004-baja.spec.ts › Gestión › Baja de un cargo — sin determinar` (el separador de la nota es ` ⇒ `; los títulos ya usan ` — `)

**MUST** leer y escribir el fichero solo con `estado.py` (en la carpeta de este skill), nunca a mano:

```bash
E="python3 .claude/skills/developer-repair-e2e-tests/estado.py"
$E sync <list.json>               # crea el fichero o añade como `[ ]` los tests que falten y elimina los que ya no existen
$E pendientes                     # ficheros .spec.ts con algún test sin `[x]`, uno por línea
$E aplicar <report.json>          # vuelca una ejecución: imprime `PASA|FALLA|OMITIDO <test>` y el resumen
$E marcar <ok|ko> "<test>" "<causa>: <texto>"
$E resumen                        # TOTAL · PASAN · PENDIENTES
```

- `sync` elimina la línea (esté en `[x]` o en `[ ]`) de un test borrado o con el título o un `describe` cambiado, e imprime `AÑADIDO <test>` y `ELIMINADO <test>` por cada línea que añade o elimina y los recuentos `AÑADIDOS <n>` y `ELIMINADOS <n>`; con un listado parcial solo elimina las de los ficheros listados o que ya no existen.
- `aplicar` nunca toca una línea que ya está en `[x]`; si ese test ahora falla imprime `REGRESION <test>`, que solo usa §6.7 (paso 5).
- `aplicar` escribe por su cuenta dos notas sin `<causa>`: `pasa al reintentar` (en `[x]`) y `omitido por el propio test (skip)` (en `[ ]`).
- Un test `OMITIDO` se queda en `[ ]` y su fichero se vuelve a ejecutar en cada reanudación, hasta que §4.3 lo da por terminado.
- **MUST** actualizar la línea de un test en cuanto se conoce su resultado, no al final: es lo que permite reanudar una ejecución interrumpida.

### 2.2 El test o la app

- **CRITICAL**: **MUST NOT** cambiar un test para que pase si lo que está mal es la app, ni cambiar la app para que pase un test que comprueba un comportamiento que ya no es el querido.
- El criterio es la **intención**: un test está desactualizado solo si un cambio **intencionado** de la app modificó lo que comprueba (el formato de un campo, un widget, el flujo de una fase…) o lo eliminó.
  El `git log` (y el `git diff` de lo aún sin commitear) de los ficheros implicados sirve para ver qué ha ocurrido últimamente y con qué intención.
- Sin un cambio intencionado que lo justifique, el fallo es una regresión: se arregla el código.
- Si no se puede decidir con confianza, **MUST NOT** tocarse nada (§2.3).
- `.sdd/` **MUST NOT** leerse ni usarse como evidencia (ver `CLAUDE.md`).

### 2.3 Modo ante dudas

- `preguntar` → ante un `DUDA` se para en ese test y decide el usuario.
- `no preguntar` → ante un `DUDA` no se toca nada, se anota `sin determinar` y se pasa al siguiente.
- Modo subagente (el skill corre dentro de un `Agent`, sin usuario): el modo es siempre `no preguntar` y las demás **STOP conditions** se devuelven como resultado final.

### 2.4 Recursos compartidos

- **CRITICAL**: todo es **secuencial**. La app, la BD y el puerto 8080 son únicos: **MUST NOT** lanzar dos subagentes ni dos ejecuciones de Playwright a la vez, ni usar `run_in_background` con los subagentes.
- La app la gestiona **solo el orquestador**: un proceso lanzado por un subagente muere al cerrarse su contexto. Los subagentes **MUST NOT** arrancarla ni pararla, ni lanzar Gradle salvo la compilación que §6.4 pide al implementador.
- Los subagentes **MUST NOT** usar `AskUserQuestion` ni hacer commits.
- «Reanudar» un subagente es enviarle un mensaje con `SendMessage` al mismo subagente, no lanzar otro: quien editó es quien corrige y quien deshace.
- **MUST NOT** hacer commit, `git stash`, `git checkout` ni `git restore` de nada.
- La instancia Docker de `/build` **MUST NOT** tocarse (`agent_docs/deploy.md`).

### 2.5 Ejecutar tests

Siempre por CLI, con el reporter JSON a un fichero del scratchpad y la salida a un log (nunca al contexto):

```bash
PLAYWRIGHT_JSON_OUTPUT_NAME="<scratchpad>/report.json" npx playwright test <ficheros…> --reporter=json > "<scratchpad>/playwright.log" 2>&1
```

- `<ficheros…>` son rutas relativas a la raíz (`src/test/e2e/<fichero>`).
- El código de salida distinto de 0 solo indica que algún test falló: el resultado se lee con `$E aplicar`.

### 2.6 Foto del árbol

Sin commits, el árbol acumula cambios del usuario y de tests anteriores: lo que cambia una reparación se aísla comparando dos fotos (un `tree` de git sacado con un índice temporal; no toca el índice real ni crea commits):

```bash
I="<scratchpad>/idx"; cp "$(git rev-parse --git-path index)" "$I" && GIT_INDEX_FILE="$I" git add -A && GIT_INDEX_FILE="$I" git write-tree   # imprime el hash de la foto
git diff --name-only <foto A> <foto B> -- . ':!repair-e2e-test.md'   # ficheros que cambian de A a B (el fichero de estado no cuenta)
```

- `ANTES` es la foto que se toma **una vez por test**, justo antes de lanzar su subagente de reparación (§6.4, §6.5): contra ella se verifica (§6.6) y a ella se vuelve al deshacer (§6.8), también en el 2.º intento.

---

## 3. Fase 0 — Modo ante dudas

1. Si los argumentos traen `--preguntar` o `--no-preguntar`, o estás en modo subagente (§2.3), usa ese modo y pasa a la Fase 1.
2. Si no, pregunta **una única vez** con `AskUserQuestion`:
   - Pregunta: `Cuando no se pueda determinar si un fallo es del test o de la app, ¿qué hago?`
   - Opción `Preguntarme` → modo `preguntar`.
   - Opción `No preguntar` → modo `no preguntar`: no tocar nada, anotarlo como «sin determinar» y seguir.

---

## 4. Fases 1 a 3 — Seleccionar, arrancar y ejecutar

### 4.1 Fase 1 — Seleccionar

1. Lista la suite: `npx playwright test --list --reporter=json > "<scratchpad>/list.json"`.
2. `$E sync "<scratchpad>/list.json" | tail -n 3` (solo los recuentos `AÑADIDOS <n>` y `ELIMINADOS <n>` y el resumen `TOTAL · PASAN · PENDIENTES`): crea `repair-e2e-test.md` con todos los tests en `[ ]` o, si ya existía, añade solo los que falten y elimina los que ya no están en la suite.
3. `$E pendientes` da los ficheros a ejecutar.
   - Si el fichero no existía, son todos: se prueba la suite entera.
   - Si ya existía, son solo los de tests sin `[x]`. **MUST NOT** ejecutar los ficheros cuyos tests están todos en `[x]`.
   - Si no devuelve ninguno → termina (STOP conditions).

### 4.2 Fase 2 — Arrancar la app

1. Comprueba si ya responde: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080`.
   - `200` → **MUST NOT** arrancar otra instancia; pasa a la Fase 3.
2. Libera el puerto 8080 **del host** (antes confirma que el proceso dueño es de la ruta host, no de `/build`): `fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill`.
   - El dueño es la instancia de `/build` → **ERROR** (STOP conditions).
3. Arranca con `Bash`, `run_in_background: true` y `dangerouslyDisableSandbox: true`, sin `&` ni `nohup`:
   ```bash
   exec ./run.sh > src/test/e2e/.app.log 2>&1
   ```
4. Sondea el `curl` del paso 1 hasta `200`. **LIMIT**: 3 ventanas de 420 s.
   - `BUILD FAILED` en el log → la app no va a arrancar; no sigas sondeando.
   - Sin `200` → **ERROR** (STOP conditions), salvo en un reinicio de la Fase 4, que lo tratan §6.4 y §6.8.

### 4.3 Fase 3 — Ejecutar

1. Ejecuta los ficheros de §4.1 según §2.5, con `run_in_background: true` (la suite completa tarda), y espera a que termine.
2. `$E aplicar "<scratchpad>/report.json"` marca `[x]` los que pasan.
3. La lista de trabajo de la Fase 4 son las líneas `FALLA <test>` de su salida, en ese orden. Las `OMITIDO` no se reparan:
   - Modo subagente (§2.3) → dalas por terminadas: `$E marcar ok "<test>" "omitido: skip del propio test"` por cada una.
   - Si no → pregunta **una única vez** con `AskUserQuestion`, listándolas: `Estos tests se omiten a sí mismos (skip). ¿Qué hago con ellos?`
     - Opción `Darlos por terminados` → márcalos como en modo subagente.
     - Opción `Dejarlos pendientes` → se quedan en `[ ]`.
4. Si no hay ninguna `FALLA` → pasa a la Fase 5.

---

## 5. Flujo de la Fase 4

```
por cada test FALLA, en secuencia (salvo el que la reparación de otro ya dejó en [x]):
  diagnóstico (subagente claude)
    ├─ PASA  → [x] «pasa en solitario»
    ├─ TEST  → sanador (playwright-test-healer), o borrador (claude) si el diagnóstico trae BORRAR ──┐
    ├─ APP   → implementador (claude) ──────────────────────────────────────────────────────────────┤
    │                                              ▼
    │                         verificar el diff ANTES→DESPUES (orquestador; en TEST, + subagente verificador y resincronizar el estado)
    │                           ├─ rechazada → deshacer → [ ] «no reparado»
    │                           └─ conforme → (APP: reiniciar app) → reejecutar el fichero del test (orquestador)
    │                                 ├─ pasa → [x] con la causa
    │                                 ├─ falla → 2.º y último intento (mismo subagente) → deshacer → [ ] «no reparado»
    │                                 └─ BORRADO → falla un test que pasaba → deshacer → [ ] «no reparado»
    │                                              si no (o ya no hay fichero) → sin línea; va al informe
    │   (BLOQUEADO en cualquier intento, o APP cuyo arreglo no compila o no arranca → deshacer → [ ] «no reparado»)
    └─ DUDA  → modo preguntar: AskUserQuestion → TEST | APP | dejarlo
               modo no preguntar: [ ] «sin determinar»
```

---

## 6. Fase 4 — Reparar cada test fallido

**CRITICAL**: cada test se procesa en **sus propios subagentes**, uno detrás de otro, para que la salida de Playwright no llene tu contexto. **MUST NOT** lanzar un único subagente para varios fallos ni diagnosticar tú mismo leyendo trazas.

Un test de la lista que §6.7 (pasos 3 y 5) ya marcó `[x]` al reparar otro se salta: **MUST NOT** diagnosticarlo ni cambiar su nota.

### 6.1 Diagnosticar

Lanza un subagente (`subagent_type: claude`). Rellena solo los placeholders; el resto pásalo tal cual:

```text
Un test E2E de Playwright de la secretaría virtual falla. Tienes que DIAGNOSTICAR si lo que está mal es el test o el código de la app. MUST NOT modificar ningún fichero, ni arrancar o parar la app (ya está en http://localhost:8080), ni usar AskUserQuestion.

Test: <test>
Fichero: src/test/e2e/<fichero .spec.ts> (y su descripción hermana .desc.md, si existe)

Lee §2.2 de .claude/skills/developer-repair-e2e-tests/SKILL.md: es el criterio de decisión.

Pasos:
1. Ejecuta su fichero (`npx playwright test src/test/e2e/<fichero> --reporter=list`) y mira solo el resultado de ESTE test. Si pasa, responde PASA.
2. Si falla, localiza el paso que falla y qué comprueba (lee el .spec.ts y el .desc.md; usa las tools de Playwright para ver la pantalla real si hace falta).
3. Localiza el código de la app implicado (vista XML, controlador, servicio, modelo).
4. Mira en el `git log` (y en el `git diff` sin commitear) de esos ficheros y del propio test qué ha ocurrido últimamente y con qué intención.
5. Decide con §2.2. Ante la duda razonable, responde DUDA: es preferible a acertar por casualidad.

Responde SOLO con uno de estos formatos (la primera línea es literal):

PASA

TEST
Cambio intencionado: <commit o cambio sin commitear, y qué comportamiento cambió>
Qué hay que actualizar en el test: <paso/aserción y nuevo comportamiento esperado>
(si ese cambio eliminó la funcionalidad que el test comprueba, esa línea es en su lugar, literal: `BORRAR: <funcionalidad que ya no existe>`; MUST empezar por `BORRAR:`, no `Qué hay que actualizar en el test: BORRAR: …`)

APP
Regresión: <qué hace la app y qué debería hacer según el test>
Dónde: <fichero:línea del código sospechoso>
Evidencia: <por qué no es un cambio intencionado>

DUDA
Síntoma: <qué falla>
A favor de test desactualizado: <evidencia>
A favor de bug de app: <evidencia>
```

- ✅ CORRECTO: primera línea `TEST`
- ❌ INCORRECTO: `Creo que probablemente es el test` (sin token literal: trátalo como `DUDA`)
- ✅ CORRECTO: `BORRAR: la exportación a CSV, que el commit a1b2c3d retiró` (al principio de su propia línea, en lugar de la línea `Qué hay que actualizar en el test: …`)
- ❌ INCORRECTO: `Qué hay que actualizar en el test: BORRAR: la exportación a CSV` (la línea no empieza por `BORRAR:`: §6.5 lanzaría el sanador en vez del borrador)

### 6.2 Actuar según el diagnóstico

1. `PASA` → `$E marcar ok "<test>" "pasa en solitario: falló en la ejecución conjunta"` y siguiente test.
2. `TEST` → sanar o borrar (§6.5), verificar (§6.6) y comprobar (§6.7).
3. `APP` → arreglar el código (§6.4), verificar (§6.6) y comprobar (§6.7).
4. `DUDA` → §6.3.

### 6.3 Dudas

- Modo `no preguntar` → `$E marcar ko "<test>" "sin determinar: <síntoma en una frase>"` y siguiente test.
- Modo `preguntar` → muestra al usuario el síntoma y las dos evidencias y pregunta con `AskUserQuestion`:
  - `Es el test` → sigue como `TEST`.
  - `Es la app` → sigue como `APP`.
  - En ambos casos, donde un prompt pide el `<bloque TEST|APP del diagnóstico>` pasa el bloque `DUDA` entero precedido de la línea `Decidido por el usuario: es el test; el cambio intencionado es el de «A favor de test desactualizado».` o `Decidido por el usuario: es la app; la regresión es la de «A favor de bug de app».`
  - `Dejarlo` → márcalo `sin determinar` y siguiente test.

### 6.4 Arreglar el código (APP)

1. Toma la foto `ANTES` (§2.6) y lanza un subagente (`subagent_type: claude`) con el bloque `APP` del diagnóstico y estas instrucciones:
   ```text
   Un test E2E ha destapado una regresión en la app. Arregla el CÓDIGO de la app; MUST NOT modificar nada bajo src/test/e2e.

   Test: <test>
   <bloque APP del diagnóstico>

   - Carga y aplica /k-secure-coding y /k-code-quality, más el skill de dominio del fichero que toques (/k-vistas, /k-sistemas, /k-validaciones, /k-tipo-expediente…).
   - Si colocas una pieza nueva, respeta agent_docs/architecture.md.
   - Haz el cambio mínimo que restaura el comportamiento. Comprueba que compila con `./gradlew compileJava compileTestJava`.
   - MUST NOT arrancar ni parar la app, lanzar ninguna otra tarea de Gradle ni `./run.sh`, hacer commits ni usar AskUserQuestion.

   Responde SOLO con uno de estos formatos (la primera línea es literal):

   CORREGIDO
   Cambios: <fichero: qué se cambió>

   BLOQUEADO
   Motivo: <por qué no se puede arreglar o qué decisión hace falta>
   ```
   - ✅ CORRECTO: primera línea `CORREGIDO`
   - ❌ INCORRECTO: `He revisado el servicio y creo que ya funciona` (sin token literal: trátalo como `BLOQUEADO`)
2. `BLOQUEADO` → deshacer (§6.8), `$E marcar ko "<test>" "no reparado: <motivo>"` y siguiente test.
3. `CORREGIDO` → verifica (§6.6) y, si es conforme, reinicia la app: para por puerto y arranca de nuevo (§4.2, pasos 2 a 4).
   - **LIMIT**: 3 builds por test en total (cada reinicio es un build), sin reiniciar la cuenta en el 2.º intento de reparación (§6.7). Única excepción: el 2.º intento dispone de los builds que queden de los 3 y, si no queda ninguno, de 1 garantizado (el 4.º del test).
   - El reinicio falla si el log trae `BUILD FAILED` o la app no llega a `200`; aquí no es el **ERROR** de §4.2. Si falla y al test le quedan builds, reanuda al implementador (§2.4) con el extracto del log; esa reanudación no cuenta como intento de reparación (§6.7).
     - `CORREGIDO` → repite este paso (verificar y reiniciar).
     - `BLOQUEADO`, respuesta sin token o falla el último build disponible → deshacer (§6.8), `$E marcar ko "<test>" "no reparado: el arreglo no compilaba — <error del build>"` (o `la app no arrancaba — <error del log>` si compiló sin llegar a `200`) y siguiente test. El build **MUST NOT** quedar roto.

### 6.5 Sanar o borrar el test (TEST)

Toma la foto `ANTES` (§2.6) y lanza **un** subagente de reparación con el bloque `TEST` del diagnóstico: el borrador si el bloque trae una línea que empieza por `BORRAR:`, el sanador si no.

**Sanador** (`playwright-test-healer`):

```text
Sana este test de Playwright, que quedó desactualizado por un cambio INTENCIONADO de la app (la app ya está en http://localhost:8080; no la arranques ni la pares).

Test: <test>
Fichero: src/test/e2e/<fichero .spec.ts>
<bloque TEST del diagnóstico>

- Actualiza solo lo que ese cambio dejó obsoleto. MUST NOT quitar ni debilitar aserciones, añadir `test.skip`/`test.fixme`, tocar otros tests ni crear tests o ficheros nuevos (tampoco partiendo o duplicando este).
- Puedes cambiar el título del test si ese cambio deja sin sentido el actual; el de un `describe`, solo si además no contiene ningún otro test.
- MUST NOT borrar el test ni ningún fichero: si ese cambio eliminó la funcionalidad que el test comprueba, no toques nada y responde BLOQUEADO.
- Si existe la descripción hermana (mismo nombre base, .desc.md), actualízala para que siga describiendo lo que hace el .spec.ts, también el título nuevo.
- MUST NOT modificar nada fuera de src/test/e2e.
- Si para que pase tendrías que aceptar un comportamiento que parece un error de la app, no lo sanes.

Empieza tu respuesta con una de estas líneas literales y no añadas nada más:
SANADO: <qué se actualizó>
BLOQUEADO: <motivo>
```

**Borrador** (`subagent_type: claude`; necesita Bash para borrar ficheros y para recrearlos si hay que deshacer):

```text
Borra este test E2E de Playwright: un cambio INTENCIONADO de la app eliminó la funcionalidad que comprueba. MUST NOT arrancar ni parar la app, ejecutar tests, hacer commits, usar AskUserQuestion ni comandos git que modifiquen el índice o el árbol (`git rm`, `git restore`, `git checkout`, `git stash`): borra con `rm`.

Test: <test>
Fichero: src/test/e2e/<fichero .spec.ts>
<bloque TEST del diagnóstico>

1. Confirma en el código de la app que esa funcionalidad ya no existe. Si sigue existiendo (lo que falla es una aserción sobre algo que sigue ahí), no toques nada y responde BLOQUEADO: borrar MUST NOT ser la forma de librarse de una aserción que falla.
2. Si es el único test de su fichero, borra el .spec.ts y su descripción hermana (mismo nombre base, .desc.md). Si hay más tests, quita solo este (entero, sin vaciarlo ni comentarlo) y su parte de la descripción hermana.
3. MUST NOT tocar ningún otro test ni fichero, ni crear ninguno.

Empieza tu respuesta con una de estas líneas literales y no añadas nada más:
BORRADO: <qué funcionalidad comprobaba y por qué ya no existe>
BLOQUEADO: <motivo>
```

- ✅ CORRECTO (sanador): `SANADO: el campo «tipo» se elige ahora como RadioSelect`
- ✅ CORRECTO (borrador): `BORRADO: comprobaba la exportación a CSV, que el commit a1b2c3d retiró`
- ❌ INCORRECTO: `El test ya pasa tras ajustar el selector` (sin token literal: trátalo como `BLOQUEADO`)
- ❌ INCORRECTO: `BORRADO: …` como respuesta del sanador (ese token es solo del borrador: trátalo como `BLOQUEADO`)
- `SANADO` o `BORRADO` → verificar (§6.6).
- `BLOQUEADO` → deshacer (§6.8), `$E marcar ko "<test>" "no reparado: <motivo>"` y siguiente test.

### 6.6 Verificar la reparación

Lo que el runner no ve lo comprueba alguien distinto de quien reparó, sobre el diff de esta reparación:

1. Toma la foto `DESPUES` y lista los ficheros que cambian de `ANTES` a `DESPUES` (§2.6).
2. `APP`: alguno bajo `src/test/e2e` → rechazada; si no → conforme.
3. `TEST`: alguno fuera de `src/test/e2e`, o alguno nuevo (`git diff --name-only --diff-filter=A <ANTES> <DESPUES>` no sale vacío) → rechazada; si no, lanza un subagente verificador (`subagent_type: claude`) y toma su respuesta como veredicto:
   ```text
   Revisa el diff de la reparación de un test E2E de Playwright que ha hecho OTRO agente. MUST NOT modificar ningún fichero, ejecutar tests ni usar AskUserQuestion.

   Test: <test>
   Diff: `git diff <ANTES> <DESPUES> -- src/test/e2e`
   <bloque TEST del diagnóstico>
   Respuesta de quien reparó: <su línea `SANADO`/`BORRADO`>

   Checklist (se cumple solo si la respuesta a todo es sí; un ítem «Si…» cuyo caso no se da cuenta como sí):
   - [ ] ¿Ninguna aserción se ha quitado, comentado, hecho menos estricta o vuelto condicional sin que el cambio intencionado del diagnóstico lo obligue? (Las de un test borrado entero las juzga el último ítem.)
   - [ ] ¿No se ha añadido ningún `test.skip`, `test.fixme` ni `test.fail`?
   - [ ] ¿Solo cambian el test indicado y su .desc.md hermano, y ningún otro test (del mismo fichero o de otro)?
   - [ ] ¿El diff no añade ningún test, tampoco partiendo o duplicando el indicado?
   - [ ] Si cambia el título del test o el de un `describe`: ¿el cambio intencionado deja sin sentido el anterior y el `describe` renombrado no contiene otros tests?
   - [ ] ¿Cada cambio se explica por el cambio intencionado del diagnóstico?
   - [ ] Si el test se ha borrado: ¿el cambio intencionado del diagnóstico eliminó la funcionalidad que comprobaba (confírmalo en el código de la app), se ha borrado entero y no vaciado ni comentado, y el .desc.md hermano ya no lo describe (o se ha borrado con el .spec.ts, si era su único test)? Un borrado que solo esquiva una aserción que falla sobre algo que sigue existiendo incumple este ítem.

   Responde SOLO con una de estas líneas literales:
   CONFORME
   RECHAZADO: <ítem incumplido — fichero:línea>
   ```
   - ✅ CORRECTO: `RECHAZADO: aserción quitada — system/gestion/t-003-alta.spec.ts:52`
   - ❌ INCORRECTO: `El diff parece razonable` (sin token literal: trátalo como `RECHAZADO`)
4. Rechazada → deshacer (§6.8), `$E marcar ko "<test>" "no reparado: reparación rechazada — <incumplimiento>"` y siguiente test. **MUST NOT** pedir otro intento.
5. Conforme en `TEST` → resincroniza siempre el fichero de estado con la suite entera: `npx playwright test --list --reporter=json > "<scratchpad>/list.json"` y `$E sync "<scratchpad>/list.json"`.
   - El `--list` falla → rechazada (paso 4), sin lanzar `$E sync`.
   - Las únicas salidas válidas de `sync` son estas; cualquier otra línea `AÑADIDO` o `ELIMINADO` (un test nuevo, o un cambio en otro test) → rechazada (paso 4):
     - `SANADO`: ninguna línea `AÑADIDO` ni `ELIMINADO`, o bien `ELIMINADO <test>` con exactamente un `AÑADIDO` (el test se renombró).
     - `BORRADO`: solo `ELIMINADO <test>`.
   - Si se renombró, desde aquí `<test>` es el de esa línea `AÑADIDO` (para `$E aplicar`, `$E marcar`, §6.7 y el informe); conserva el original: lo necesitan §6.8 y el informe.
6. Conforme → comprobar (§6.7); en `APP`, tras reiniciar la app (§6.4, paso 3).

### 6.7 Comprobar

El veredicto lo da el runner, no el subagente:

1. `BORRADO` y `src/test/e2e/<fichero>` ya no existe → no hay nada que reejecutar (el `--list` de §6.6 ya comprobó la suite): sigue en el paso 5 como si no hubiera salido ninguna línea.
2. Solo antes de la 1.ª reejecución de este test, anota los tests de su fichero que están en `[ ]`, con su nota (única lectura directa del fichero de estado, §2.1): `grep -F -- "- [ ] <fichero .spec.ts relativo a src/test/e2e> ›" repair-e2e-test.md > "<scratchpad>/pendientes-antes.txt"`.
3. Reejecuta el fichero del test (§2.5) y lee el resultado con `$E aplicar`. `BORRADO` → paso 5. Pasa → `$E marcar ok "<test>" "<test desactualizado|bug de app>: <qué se cambió>"` y, por cada otra línea `PASA <test>` de los `$E aplicar` de este test, `$E marcar ok "<test>" "<la misma causa>: lo arregló la reparación de «<título del test en curso>»"`.
4. Falla u `OMITIDO` → reanuda al subagente de reparación (§2.4) con el error nuevo (si es `OMITIDO`: que el test ahora se omite y **MUST** ejecutarse) y trata su respuesta como la primera (§6.4, pasos 2 y 3, o §6.5). **LIMIT**: 2 intentos de reparación por test, contando el primero; después deshacer (§6.8), `$E marcar ko "<test>" "no reparado: <causa diagnosticada> — <error que persiste>"` y siguiente test.
5. `BORRADO` (el test ya no tiene línea; el veredicto son los tests que quedan en su fichero):
   - Alguna línea `REGRESION <otro test>` → deshacer (§6.8), `$E marcar ko "<test>" "no reparado: test desactualizado — el borrado hace fallar «<título del otro test>»"` y siguiente test. **MUST NOT** pedir otro intento.
   - Si no → por cada línea `PASA <otro test>`, `$E marcar ok "<otro test>" "test desactualizado: lo arregló el borrado de «<título del test en curso>»"` (las `FALLA` siguen en `[ ]` hasta su turno); conserva `<test>` y el motivo del borrador para el informe (§7), que el fichero de estado ya no los tiene, y siguiente test.

### 6.8 Deshacer una reparación fallida

**CRITICAL**: un test que acaba en `[ ] no reparado` **MUST** quedar sobre el árbol de `ANTES`; si no, una reejecución posterior podría marcar `[x]` un test con la aserción debilitada. Lo deshace el subagente que editó: tú **MUST NOT** editar los ficheros.

1. Toma una foto `AHORA` y lista los ficheros que cambian de `ANTES` a `AHORA` (§2.6). Ninguno → pasa al paso 4.
2. Guarda el diff (`git diff <ANTES> <AHORA> -- . ':!repair-e2e-test.md' > "<scratchpad>/deshacer.diff"`) y reanuda al subagente de reparación (§2.4):
   ```text
   La reparación de este test se descarta. Deshaz TODAS tus ediciones, que son exactamente las de <scratchpad>/deshacer.diff: deja cada fichero como estaba antes, borra los que creaste y recrea los que borraste (el diff trae su contenido). MUST NOT usar git restore, git checkout ni git stash.

   Responde SOLO con la línea literal: DESHECHO
   ```
3. Repite el paso 1, responda lo que responda. **LIMIT**: 2 peticiones; si siguen cambiando ficheros → **STOP** (STOP conditions).
4. Si en algún intento de este test §6.6 (paso 5) llegó a lanzar `$E sync`, repite su `--list` y su `$E sync`: la línea de un test renombrado o borrado vuelve en `[ ]` con su nombre original (y la del nombre nuevo desaparece). Desde aquí `<test>` es otra vez el nombre original: con él se anota el `no reparado`.
5. Si §6.7 llegó a reejecutar, devuelve a `[ ]` los otros tests que pasaron a `[x]` gracias a esas ediciones: por cada línea `PASA <test>` de sus `$E aplicar` que no sea el test en curso (con ninguno de sus nombres), `$E marcar ko "<test>" "<nota que tenía en pendientes-antes.txt>"` (`""` si no tenía nota; **MUST** pasar siempre el argumento).
6. Si la app se reinició, o se intentó reiniciar, con esas ediciones (`APP`), reiníciala (§4.2, pasos 2 a 4). Si el build falla o la app no llega a `200` → **STOP** (STOP conditions).

---

## 7. Fase 5 — Informe

1. Deja la app arrancada.
2. Devuelve exactamente esta plantilla, con una fila por test de la lista de trabajo de la Fase 4:

```markdown
## Reparación de la suite E2E

Estado: `repair-e2e-test.md` — <resumen de `$E resumen`>

| Test | Causa | Acción |
|---|---|---|
| `<fichero>` › <título; si se renombró, el nuevo y `(antes «<título antiguo>»)`> | test desactualizado / bug de app / pasa en solitario / sin determinar / no reparado | <qué se cambió, `borrado`, o por qué no se tocó> |

**Tests borrados porque lo que comprobaban ya no existe**:
- `<fichero>` › <título>: <motivo del borrador> | ninguno

**Sin tocar porque no se pudo decidir**:
- `<fichero>` › <título>: <síntoma> | ninguno

**Omitidos por el propio test (skip)**:
- `<fichero>` › <título>: dado por terminado / pendiente | ninguno

**Ficheros modificados** (sin commit):
- `<ruta>`
```

---

## Quick Guidelines

- El estado vive en `repair-e2e-test.md` y se maneja solo con `estado.py`; cada resultado se anota al momento.
- Si el fichero ya existe, solo se prueban los tests sin `[x]`; para probarlo todo se borra el fichero.
- Un test se cambia solo si un cambio intencionado de la app lo dejó obsoleto; si no, se arregla la app; si no se sabe, no se toca nada.
- Un subagente de diagnóstico por test y otro de reparación, siempre en secuencia; el diff de la reparación lo verifica alguien distinto de quien reparó y el veredicto final es del runner.
- Una reparación bloqueada, rechazada, que no compila, que no deja arrancar la app o que agota sus 2 intentos la deshace el subagente que la hizo: un `[ ]` nunca queda sobre código a medio reparar, el build nunca queda roto y los otros tests que su reejecución puso en `[x]` vuelven a `[ ]`.
- Un test omitido (`skip`) se da por terminado en modo subagente; con usuario, se le pregunta.
- La app la arranca y reinicia el orquestador con `./run.sh`; tras cada arreglo de código hay que reiniciarla.
- Los tests se sanan con `playwright-test-healer` (`.spec.ts` y su `.desc.md`), sin debilitar aserciones, borrar ni crear tests o ficheros; el test se puede renombrar (su nombre nuevo lo da `sync`, no el sanador).
- Un test cuya funcionalidad ya no existe lo borra un subagente `claude` (con su `.spec.ts` y su `.desc.md` si era el único del fichero): lo confirma el verificador, no puede romper los tests que quedan en su fichero y va al informe.

---

## Apéndice A — Override de rutas (para testing)

- `--out=<ruta>` — fichero de estado alternativo a `repair-e2e-test.md`; pásalo también a `estado.py` (`$E --out=<ruta> …`) y sustituye por esa ruta el `repair-e2e-test.md` del `grep` de §6.7 y de la exclusión `':!…'` de §2.6 y §6.8.

En uso normal no se especifica.
