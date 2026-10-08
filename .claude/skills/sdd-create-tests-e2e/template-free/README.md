# Contrato de plantilla — creación de tests E2E de regresión (feature libre)

Esta carpeta es el **contrato** que leen los subagentes de `/sdd-create-tests-e2e` para una **feature libre**. El `SKILL.md` es un motor agnóstico: **todo** lo específico de cómo se genera, verifica y sana un `.spec.ts` está aquí.

Lo que cambia respecto a las plantillas de sistema y de expediente: **la carpeta destino la declara el `design.md`** en su campo `**Carpeta de tests E2E:**` (§3.1), el **import** de `_support/auth.ts` es fijo (§3.2), y **sí hay tests manuales** (`Manual: sí`), que se persisten con el tag `@manual` sin ejecutarse (§3.3).

---

## 1. Ficheros de la plantilla

| Fichero | Lo lee | Para qué |
|---|---|---|
| `README.md` (este) | los tres roles + el motor | índice, contexto, y las tres secciones que ejecuta el motor: **«Carpeta destino»** (§3.1), **«Gestión de la app»** (§4) y **«Puerta de regresión»** (§6) |
| `generation.md` | **generador** | de `t-NNN-<slug>.desc.md` a `.spec.ts`: login/logout, idempotencia, plantilla del test (normal y manual), plantilla de `_support/auth.ts`, checklist |
| `verification.md` | **verificador** | qué hace **fiel** a un test verde (o a un manual no ejecutado) y cómo auditarlo sin tocarlo |
| `healing.md` | **sanador** | cómo arreglar un `.spec.ts` rojo o `INFIEL` sin tocar el código de la app |

---

## 2. Roles

**CRITICAL — separación de poderes anti-trampa**: los tres roles corren en **contextos aislados**. Quien **crea** el test no decide si vale; lo decide el **runner mecánico** (el motor) más un **verificador** independiente.

| Rol | Qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **generador** (§2.1) | **Genera** un `.spec.ts` desde su descripción pilotando la app real; no declara si pasa | la ruta de **un** `t-NNN-<slug>.desc.md` (ya copiado en `{destino}`) + la ruta destino del `.spec.ts` | `generation.md` (+ §3) | el `.spec.ts` hermano + `ESCRITO:` |
| **verificador** (§2.2) | **Audita** que un `.spec.ts` verde (o manual) es **fiel** a su descripción. NO lo modifica | el `.spec.ts` y su `.desc.md` | `verification.md` (+ §3) | `OK:` o `INFIEL: — {motivo}` |
| **sanador** (§2.3) | **Arregla** un `.spec.ts` rojo o `INFIEL` | el `.spec.ts`, su `.desc.md` y el fallo | `healing.md` (+ §3) | el `.spec.ts` corregido en sitio + `CORREGIDO:`/`BLOQUEADO:` |

Los tres roles:

- El skill `/k-playwright`: el **generador MUST** cargarlo siempre; el verificador y el sanador, si lo necesitan.
- **MUST NOT** modificar código de la app (`src/main/...`) ni `.sdd/`.
- **MUST NOT** usar `AskUserQuestion`.
- **MUST NOT** pegar el contenido de los ficheros en la respuesta.
- El **generador MUST NOT** declarar si el test pasa; el **verificador MUST NOT** editar el test; el **sanador MUST NOT** debilitar ni borrar aserciones.
- **CRITICAL** — al terminar, **MUST** cerrar la sesión de navegador con `browser_close`.

---

## 3. Contexto del proyecto y carpeta destino

- Secretaría virtual sobre **Axelor**, en `http://localhost:<APP_PORT>/` (`APP_PORT` de `ports.env` del worktree, 8080 si no existe; ver `agent_docs/deploy.md`); login en `/#/login`. El `baseURL` ya está configurado: **MUST** rutas relativas (`page.goto('/#/login')`).
- Convenciones de tests, locators y estructura: `/k-playwright`. Pares `t-NNN-<slug>.desc.md` ↔ `t-NNN-<slug>.spec.ts`, mismo nombre base y carpeta; helper `src/test/e2e/_support/auth.ts`.
- Multicentro y bilingüe (es/ca); locators por texto en español salvo que el test diga otra cosa.
- **CRITICAL — la BD es compartida y NO se resetea**: cada `.spec.ts` **MUST** ser idempotente (`generation.md` §4). Una feature libre a menudo cambia **estado global** (una configuración, un aviso para todos): el test **MUST** dejarlo como estaba en `finally`, o contamina toda la suite.
- Varias iniciativas pueden compartir carpeta: reutiliza los helpers hermanos, **MUST NOT** modificarlos.

### 3.1 Carpeta destino (la resuelve el MOTOR en la Fase 1)

1. Lee el campo **`**Carpeta de tests E2E:**`** del `design/design.md` de la iniciativa (lo obliga `sdd-designer/template-free/design-contract.md` §2):
   ```bash
   grep -m1 '^\*\*Carpeta de tests E2E:\*\*' {carpeta-iniciativa}/design/design.md
   ```
2. **MUST** validar que su valor casa con `^src/test/e2e/[a-z][a-z0-9_-]*(/[a-z][a-z0-9_.-]*)+/?$` y que es **una sola** carpeta.
3. **MUST** validar que la carpeta de familia (`system/<x>`, `subsystem/<x>`, `base/<slug>` o `transversal/<slug>`) es coherente con `**Ubicación del código:**` del mismo `design.md`: si el código está en un único `<capa>/<sistema>`, el destino debe ser `src/test/e2e/<capa>/<sistema>/`; si todo está en `base/`, `src/test/e2e/base/<slug>/`; si toca varias partes, `src/test/e2e/transversal/<slug>/`. Una incoherencia es **ERROR** (el diseño eligió mal el destino: se corrige en `/sdd-designer`).
4. `{destino}` = ese valor con barra final.
5. Si no hay `design.md`, falta el campo, no valida o es incoherente → **ERROR**. **MUST NOT** inventar la carpeta ni caer al nombre del draft.

- ✅ CORRECTO: `**Carpeta de tests E2E:** src/test/e2e/transversal/aviso-global-mantenimiento/`
- ✅ CORRECTO: `**Carpeta de tests E2E:** src/test/e2e/system/gestioncentro/` (todo el código en `system/gestioncentro`)
- ❌ INCORRECTO: `src/test/e2e/aviso-global/` (sin carpeta de familia), `src/test/e2e/2026-06-12_18-30_aviso-global/` (timestamp del draft), dos carpetas.

### 3.2 Profundidad del import de `_support/auth.ts`

Para los tres destinos válidos de §3.1 (`<capa>/<sistema>/`, `base/<slug>/`, `transversal/<slug>/`) `_support/` está dos niveles arriba: el import es siempre `'../../_support/auth'`.

- ✅ CORRECTO: `import { login, logout } from '../../_support/auth';`
- ❌ INCORRECTO: `'../_support/auth'` («Cannot find module»)

### 3.3 Tests manuales (`Manual: sí`)

El `.desc.md` trae el campo `**Manual:**`. Si es `sí — <motivo>`, su línea en el índice de entrada es `- [-]` y el motor lo lleva por la **vía manual**: se genera y se verifica pero **no se ejecuta**.

| Qué | Cómo |
|---|---|
| **Marca en el `.spec.ts`** | el tag `@manual` en las opciones del `test(...)`, `test.setTimeout(...)` amplio, comentario `MANUAL:` con el motivo y una **puerta manual** en el paso humano (`generation.md` §6) |
| **Cómo se ejecuta** | `E2E_MANUAL=1 npx playwright test --grep @manual --headed`, con una persona delante |
| **Qué pasa en CI/CD** | nada: `playwright.config.ts` lleva `grepInvert: /@manual/` salvo `E2E_MANUAL=1`, así que la suite los excluye por defecto |
| **Snapshot `.desc.md`** | el banner (§5) lleva la línea `Manual: sí — <motivo> — NO verificado mecánicamente` |
| **Informe** | `MANUAL`, nunca verde |

Las prohibiciones sobre `@manual` y `test.skip`/`test.fixme` las fija `generation.md` §6.

---

## 4. Gestión de la app (la ejecuta el MOTOR, no los subagentes)

### 4.1 Comprobar si está levantada

```bash
[ -f ports.env ] && . ./ports.env   # APP_PORT del worktree; 8080 si no hay ports.env
curl -s -o /dev/null -w "%{http_code}" "http://localhost:${APP_PORT:-8080}"
```

### 4.2 Limpiar el puerto `<APP_PORT>` (antes de arrancar)

```bash
[ -f ports.env ] && . ./ports.env   # APP_PORT del worktree; 8080 si no hay ports.env
fuser -k "${APP_PORT:-8080}/tcp" 2>/dev/null || lsof -ti "tcp:${APP_PORT:-8080}" | xargs -r kill
pkill -f "$PWD/.*(TomcatRunner|GradleWrapperMain.*run)" 2>/dev/null   # solo los de ESTE worktree
ss -ltn | grep ":${APP_PORT:-8080} " || echo "puerto libre"
```

**MUST NOT** tocar el contenedor Docker `secretaria-virtual-dev`.

### 4.3 Arrancar (tarea tracked en segundo plano)

Siempre `./run.sh` (ver `CLAUDE.md`), con `Bash`, `run_in_background: true` y `dangerouslyDisableSandbox: true`:

```bash
exec ./run.sh > src/test/e2e/.app.log 2>&1
```

Sondea hasta `200`. **LIMIT**: 420 s por ventana, máximo 3 ventanas; si agotadas no hay `200` → **ERROR** (aborto global, lo fija el skill).

### 4.4 Ejecutar un `.spec.ts`

```bash
npx playwright test {ruta del .spec.ts} --project=chromium --reporter=line
```

Exit `0` = PASS. **CRITICAL — `--reporter=line` es obligatorio** (el reporter html cuelga el comando al fallar). Un test `Manual: sí` **no se ejecuta** (el `grepInvert` lo excluiría de todos modos).

### 4.5 Parar

```bash
[ -f ports.env ] && . ./ports.env   # APP_PORT del worktree; 8080 si no hay ports.env
fuser -k "${APP_PORT:-8080}/tcp" 2>/dev/null || lsof -ti "tcp:${APP_PORT:-8080}" | xargs -r kill
```

> `src/test/e2e/.app.log` es del motor; no se commitea.

### 4.6 Limpiar sesiones de navegador huérfanas (entre subagentes)

```bash
pkill -9 -f 'workerProcessEntry|chrome-headless-shell' 2>/dev/null; true
```

**MUST NOT** matar el server MCP de Playwright (`run-test-mcp-server`).

---

## 5. Cabecera-banner del snapshot (la escribe el MOTOR en la Fase 2)

Al copiar el `.desc.md` a `{destino}`, el motor antepone este bloque **justo después del frontmatter**, dejando el resto verbatim:

```markdown
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/{carpeta-iniciativa}/test-e2e-desc/{fichero}.desc.md
     Iniciativa: {carpeta-iniciativa}
     Test: {T-NNN}  |  Origen ESC: {ESC-NNN, leído de la línea "Origen ESC:" del propio fichero}
     Manual: {no | sí — <motivo> — NO verificado mecánicamente}
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->
```

**CRITICAL — `Iniciativa:` es parte de la identidad del test**: la carpeta puede ser compartida y `T-NNN`/`ESC-NNN` son locales a cada iniciativa. **MUST NOT** omitirse.

---

## 6. Puerta de regresión (la ejecuta el MOTOR, tras persistir los tests nuevos)

Antes de parar la app, **toda** la suite:

```bash
npx playwright test src/test/e2e --project=chromium --reporter=line
```

(los `@manual` quedan fuera por el `grepInvert`). Para cada test **de una iniciativa anterior** en rojo:

- Si su ruta figura en `## 9. Tests E2E supersedidos` del `design.md` de **esta** iniciativa → se retira el par (`git rm`) y se reporta «supersedido por {REQ-/ESC-}».
- Si **NO** figura → **REGRESIÓN**: reportar y **parar**, sin retirar nada ni tocar código.

Un rojo **de esta misma iniciativa** aquí se trata como FAIL del bucle (§4.4). Sin `design.md` (modo `--out=`), todo rojo ajeno es REGRESIÓN.
