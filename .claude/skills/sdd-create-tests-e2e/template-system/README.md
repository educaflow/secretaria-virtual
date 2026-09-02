# Contrato de plantilla — creación de tests E2E de regresión

Esta carpeta es el **contrato** que leen los subagentes de `/sdd-create-tests-e2e`. El `SKILL.md` es un motor agnóstico: **todo** lo específico de cómo se genera y se sana un `.spec.ts` está aquí. Si cambias esta carpeta (o apuntas `--template-dir` a otra), cambias qué y cómo se genera **sin tocar el skill**.

---

## 1. Ficheros de la plantilla

| Fichero | Lo lee | Para qué |
|---------|--------|----------|
| `README.md` (este) | los tres roles + el motor | índice del contrato, contexto del proyecto y las tres secciones que ejecuta el motor: **«Carpeta destino»** (§3.1), **«Gestión de la app»** (§4) y **«Puerta de regresión»** (§6) |
| `generation.md` | **generador** | cómo convertir un `t-NNN-<slug>.desc.md` en su `.spec.ts`: ciclo de login/logout, plantilla del test, plantilla de `_support/auth.ts`, trazabilidad, checklist |
| `verification.md` | **verificador** | qué hace **fiel** a un test ya verde: cubrir todo el `Resultado esperado` con aserciones reales, auth correcta, sin debilitar ni saltar; cómo auditarlo sin tocarlo |
| `healing.md` | **sanador** | cómo diagnosticar y arreglar un `.spec.ts` rojo o declarado `INFIEL`, sin tocar el código Java |

---

## 2. Roles

**CRITICAL — separación de poderes anti-trampa**: los tres roles corren en **contextos aislados**. El que **crea** el test (generador) **no** decide si vale; quien lo decide es el **runner mecánico** (lo ejecuta el motor) más un **verificador** que no escribió el test. Un test verde pero infiel (aserciones que faltan o debilitadas) lo caza el verificador.

| Rol | Qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **generador** (§2.1) | **Genera** un `.spec.ts` desde su descripción, pilotando la app real. **No** declara si pasa | la ruta de **un** `t-NNN-<slug>.desc.md` (la copia en `src/test/e2e/<capa>/<sistema>/`) + la ruta destino del `.spec.ts` | `generation.md` (+ contexto §3) | el fichero `t-NNN-<slug>.spec.ts` hermano + token `ESCRITO:` |
| **verificador** (§2.2) | **Audita** de forma adversarial que un `.spec.ts` ya verde es **fiel** a su descripción. NO modifica el test | la ruta del `.spec.ts` (verde) y su `.desc.md` | `verification.md` (+ contexto §3) | token `OK:` (fiel) o `INFIEL: — {motivo}` |
| **sanador** (§2.3) | **Arregla** un `.spec.ts` rojo o declarado `INFIEL` | la ruta del `.spec.ts`, su `.desc.md` y el fallo (salida del runner o motivo `INFIEL`) | `healing.md` (+ contexto §3) | el `.spec.ts` corregido **en sitio** + token `CORREGIDO:`/`BLOQUEADO:` |

Los tres roles:

- **MUST** cargar el skill `/k-playwright` (convenciones del proyecto: estructura, locators, baseURL, `_support/auth.ts`, login común).
- **MUST NOT** modificar código Java (`src/main/...`) ni la fuente en `.sdd/`.
- **MUST NOT** usar `AskUserQuestion`: ante un bloqueo, devuelven su token.
- **MUST NOT** pegar el contenido de los ficheros en la respuesta (ya está en disco): solo el token de estado.
- El **generador MUST NOT** declarar si el test pasa; el **verificador MUST NOT** editar el test; el **sanador MUST NOT** debilitar ni borrar aserciones.
- **CRITICAL** — al terminar, **MUST** cerrar su sesión de navegador con `browser_close`. Una sesión MCP de Playwright (o su Chromium headless) que queda viva al cerrarse el contexto del subagente **bloquea al siguiente subagente** durante minutos.

---

## 3. Contexto del proyecto y carpeta destino

- La app es una secretaría virtual sobre **Axelor 8.1**, servida en `http://localhost:8080/`; login en `http://localhost:8080/#/login`. El `baseURL` de `playwright.config.ts` ya es `http://localhost:8080`: **MUST** usar rutas relativas (`page.goto('/#/login')`).
- Convenciones de tests, locators y estructura de carpetas: las define `/k-playwright` (cárgalo). En particular: pares `t-NNN-<slug>.desc.md` ↔ `t-NNN-<slug>.spec.ts`, **mismo nombre base y misma carpeta**; helper compartido `src/test/e2e/_support/auth.ts`.
- **Los tests replican la ruta del código que prueban**: `src/test/e2e/<capa>/<sistema>/` es espejo de `src/main/java/com/educaflow/<capa>/<sistema>/`, con `<capa>` = `system` o `subsystem` (p.ej. `src/test/e2e/subsystem/criptografia/`). La carpeta la resuelve el motor; el generador la recibe ya resuelta y **MUST NOT** crear otra. Como varias iniciativas comparten carpeta, los hermanos que veas ahí pueden ser de otra iniciativa: reutiliza sus helpers, pero **MUST NOT** modificarlos.
- La app es **multicentro y bilingüe (es/ca)**: los locators por texto asumen español salvo que el test diga lo contrario.
- **CRITICAL — la BD es compartida y NO se resetea entre ejecuciones**: los tests acumulan datos de runs anteriores. Por eso cada `.spec.ts` **MUST** ser **idempotente** (nombres únicos por run + teardown + pre-limpieza defensiva); lo detalla `generation.md`. Un test que pasa una vez pero falla al reejecutarse está **roto**.
- **En esta familia NO hay tests manuales**: la plantilla `system` de `/sdd-debug-with-test-e2e-desc` no declara ningún test como no automatizable (su `correction.md` prohíbe incluso devolver el token `MANUAL`), así que ningún test llega marcado `- [-]` en el índice de entrada.
  Por eso este contrato **no** define vía manual: no hay marca ni tag de exclusión que poner en un `.spec.ts`, ni forma de anotar el snapshot como no verificado.
  Si aun así apareciera una línea `- [-]`, es una incoherencia del artefacto de entrada: **MUST NOT** inventarse un tag, un `test.skip`/`test.fixme` ni un camino manual — el rol devuelve `BLOQUEADO: {T-NNN} — la plantilla system no contempla tests manuales` y el motor lo trata como fallo por test.

### 3.1 Carpeta destino (la resuelve el MOTOR en la Fase 1)

El destino de los tests de un **sistema o subsistema** es `src/test/e2e/<capa>/<sistema>/`, espejo de `src/main/java/com/educaflow/<capa>/<sistema>/`, donde `<capa>` es `system` o `subsystem`. Procedimiento:

1. Lee el campo **`**Capa:**`** del `design/design.md` de la iniciativa (el campo que `/sdd-designer` obliga a poner en el diseño de un sistema: `sdd-designer/template-system/design-contract.md`). Su valor es literalmente `<capa>/<sistema>`.
   ```bash
   grep -m1 '^\*\*Capa:\*\*' {carpeta-iniciativa}/design/design.md
   ```
2. **MUST** validar que declara **un solo** sistema y casa con `^(system|subsystem)/[a-z][a-z0-9_-]*$`. Un valor con varios sistemas (`subsystem/a, subsystem/b`) falla aquí: el destino sería ambiguo.
3. **MUST** validar que existe la carpeta `src/main/java/com/educaflow/<capa>/<sistema>/`. Si no existe, el diseño declara un sistema que no está en el código.
4. Compón `src/test/e2e/<capa>/<sistema>/`.
5. Si el `design/design.md` no existe, no tiene `Capa:`, su valor no valida o el sistema no existe → **ERROR** y detente. **MUST NOT** inventar la carpeta ni caer al nombre del draft.

- ✅ CORRECTO: `**Capa:** subsystem/criptografia` → `src/test/e2e/subsystem/criptografia/`
- ✅ CORRECTO: `**Capa:** system/gestioncentro` → `src/test/e2e/system/gestioncentro/`
- ❌ INCORRECTO: `src/test/e2e/deshabilitar-certificado-digital/` (nombre del draft, no replica el código)
- ❌ INCORRECTO: `src/test/e2e/criptografia/` (falta la capa; no distingue `system` de `subsystem`)

---

## 4. Gestión de la app (la ejecuta el MOTOR, no los subagentes)

El motor deja la app respondiendo `200` antes de generar el primer test y la para al terminar. **MUST** seguir estos comandos al pie de la letra.

### 4.1 Comprobar si está levantada

La app se considera levantada si responde `200`:

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
```

### 4.2 Limpiar el puerto 8080 (antes de arrancar)

**CRITICAL**: una instancia previa colgada hace que el connector falle el bind en silencio. Limpia de verdad, **excluyendo** IntelliJ y similares:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
pkill -f 'TomcatRunner|GradleWrapperMain.*run' 2>/dev/null
ss -ltn | grep ':8080' || echo "8080 libre"
```

### 4.3 Arrancar (tarea tracked en segundo plano)

Usa **siempre** `./run.sh` (hace `./gradlew clean build` y arranca en el 8080 con la config correcta). Lánzalo con `Bash`, `run_in_background: true` y `dangerouslyDisableSandbox: true` (`run.sh` escribe en `~/.gradle`, fuera del sandbox), redirigiendo el log:

```bash
exec ./run.sh > src/test/e2e/.app.log 2>&1
```

**Sondea** hasta `200` con margen amplio (el `clean build` + el bind pueden tardar varios minutos): repite el `curl` de §4.1 con `Monitor`/reintentos, **LIMIT** de sondeo ~420 s por ventana, varias ventanas si hace falta.

### 4.4 Ejecutar un `.spec.ts` (lo hace el motor tras el generador / tras cada `CORREGIDO`)

```bash
npx playwright test {ruta del .spec.ts} --project=chromium --reporter=line
```

Exit code `0` = **PASS**; distinto de `0` = **FAIL** (pasa la salida al sanador). La app **MUST** estar en `200` antes de ejecutar.

**CRITICAL — `--reporter=line` es obligatorio aquí**: el reporter `html` del `playwright.config.ts` tiene `open: 'on-failure'` por defecto, así que al fallar un test **arranca el servidor del informe y deja el comando colgado** (el motor esperaría indefinidamente). `--reporter=line` lo evita y nunca abre nada.

### 4.5 Parar (al terminar, §10 del skill)

Siempre **por puerto**, nunca por handle de proceso:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
```

> El log `src/test/e2e/.app.log` es del motor; no se commitea (añadir a `.gitignore` si hiciera falta) y los subagentes lo ignoran.

### 4.6 Limpiar sesiones de navegador huérfanas (entre subagentes)

**CRITICAL**: un Chromium headless o un worker de Playwright **huérfano** (de un subagente cuyo contexto ya se cerró) bloquea la sesión MCP del siguiente generador/sanador durante minutos — fue la causa de esperas de decenas de minutos. El motor **MUST** barrerlos **antes de lanzar cada generador** (los subagentes además cierran su sesión con `browser_close`, §2). **MUST NOT** matar el **server MCP** de Playwright (`run-test-mcp-server`), solo los procesos de navegador/worker de test:

```bash
pkill -9 -f 'workerProcessEntry|chrome-headless-shell' 2>/dev/null; true
```

> Los `<defunct>` (zombies) que queden no consumen recursos y los reapropia el server MCP; solo importan los procesos **vivos**.

---

## 5. Cabecera-banner del snapshot (la escribe el MOTOR en la Fase 2)

Al copiar un `t-NNN-<slug>.desc.md` de `test-e2e-desc/` a `src/test/e2e/<capa>/<sistema>/`, el motor **antepone** este bloque **justo después del frontmatter** (para no romper `type:`/`id:`), dejando el resto del contenido **verbatim**:

```markdown
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/{carpeta-iniciativa}/test-e2e-desc/{fichero}.desc.md
     Iniciativa: {carpeta-iniciativa}
     Test: {T-NNN}  |  Origen ESC: {ESC-NNN, leído de la línea "Origen ESC:" del propio fichero}
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->
```

**CRITICAL — `Iniciativa:` es parte de la identidad del test, no decoración.** Varias iniciativas comparten esta carpeta (una que modifica el sistema escribe donde ya hay tests de la que lo creó), y `T-NNN` y `ESC-NNN` son **locales a cada iniciativa**: sin este campo, dos tests distintos con el mismo `T-001`/`ESC-001` se confundirían y el nuevo se descartaría como "ya materializado". Su valor es el **nombre de la carpeta** de la iniciativa, sin `.sdd/drafts/` ni barra final. **MUST NOT** omitirse ni abreviarse.

- ✅ CORRECTO: el banner va entre el `---` de cierre del frontmatter y el `# T-NNN — …`.
- ❌ INCORRECTO: ponerlo **antes** del frontmatter (rompería el parseo de `type:`/`id:`), o reescribir el cuerpo del test.

---

## 6. Puerta de regresión (la ejecuta el MOTOR, tras persistir los tests nuevos)

Tras dejar verdes y verificados todos los tests de la iniciativa (y **antes** de parar la app, §4.5), el motor **MUST** ejecutar **toda** la suite E2E persistida — la de esta iniciativa y las de las anteriores:

```bash
npx playwright test src/test/e2e --project=chromium --reporter=line
```

Con el resultado, para cada test **de una iniciativa anterior** que salga rojo:

- Si su ruta figura en la sección `## Tests E2E supersedidos` del `design.md` de **esta** iniciativa → el delta lo invalidó **a propósito**: el motor **retira** el par (`git rm` del `.desc.md` y del `.spec.ts`) y lo lista en el informe final como "supersedido por {ID de spec}".
- Si **NO** figura → es una **REGRESIÓN**: la iniciativa rompió comportamiento que ya funcionaba. El motor **MUST** reportarlo al usuario y **parar** (sin retirar el test ni tocar código). Las salidas son `/sdd-debug-with-test-e2e-desc` (si el arreglo es de código Java) o declarar el superseding en el diseño (si el cambio de comportamiento era intencionado y se olvidó declarar).

Un test rojo **de esta misma iniciativa** en este punto no debería existir (todos pasaron ya el bucle generar→verificar→sanar); si ocurre, trátalo como FAIL normal del bucle (§4.4).

Si **no hay `design.md`** (modo `--out=`, sin draft completo), no existe la sección de supersedidos: **MUST** tratar **cualquier** rojo ajeno como REGRESIÓN y **MUST NOT** retirar ningún par.

- ✅ CORRECTO: `t-002-crear-correo.spec.ts` (iniciativa anterior) rojo y listado en `## Tests E2E supersedidos` → retirar el par y reportar "supersedido por VAL-Correo-003".
- ❌ INCORRECTO: retirar un test rojo de una iniciativa anterior que NO está declarado como supersedido (eso oculta una regresión), o dar el skill por terminado sin ejecutar la suite completa.
