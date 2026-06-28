# Playwright Test Agents (MCP)

Tres subagentes especializados que cooperan para construir y mantener una suite de tests E2E. Documentación oficial: https://playwright.dev/docs/test-agents.

## Infraestructura instalada

| Pieza | Ubicación | Para qué |
|-------|-----------|----------|
| Servidor MCP | `.mcp.json` (`playwright run-test-mcp-server`) | Expone tools `mcp__playwright-test__*` a Claude |
| Subagente planner | `.claude/agents/playwright-test-planner.md` | Explora la app y genera planes de test |
| Subagente generator | `.claude/agents/playwright-test-generator.md` | Convierte cada escenario del plan en un `.spec.ts` |
| Subagente healer | `.claude/agents/playwright-test-healer.md` | Depura y arregla tests rotos |
| Runner | `@playwright/test` en `package.json` | Ejecutar la suite (también vía CLI tradicional) |
| Config | `playwright.config.ts` | Navegadores, paralelismo, reporter, baseURL |

## Flujo de trabajo

```
planner  →  generator  →  healer
   ↓            ↓            ↓
src/test/e2e/X/X.desc.md  src/test/e2e/X/X.spec.ts  src/test/e2e/X/X.spec.ts (arreglado)
```

### 1. Planner — diseñar el plan de tests

**Cuándo:** al empezar a testear una pantalla o flujo nuevo (login, expedientes, registro entrada/salida, etc.).

**Cómo invocar:**

> "Usa el subagente `playwright-test-planner` para explorar la pantalla de login en http://localhost:8080/#/login y guardar el plan en `src/test/e2e/login/login.desc.md`."

**Qué hace internamente:**
1. Llama a `planner_setup_page` (una sola vez).
2. Navega y observa con `browser_snapshot`, `browser_click`, `browser_navigate`, etc.
3. Identifica elementos interactivos, formularios, navegación.
4. Diseña escenarios (happy path, errores, edge cases, accesibilidad).
5. Llama a `planner_save_plan` para escribir el `.desc.md`.

**Output:** un fichero `src/test/e2e/<iniciativa>/<nombre>.desc.md` (nombre base libre y descriptivo del flujo; en el pipeline SDD es `t-NNN-<slug>`) con escenarios agrupados, cada uno con pasos en lenguaje natural.

### 2. Generator — escribir el test

**Cuándo:** ya tienes el plan y quieres convertir sus escenarios en código.

**Cómo invocar:**

> "Usa el subagente `playwright-test-generator` para generar el test del plan `src/test/e2e/login/login.desc.md`. Guárdalo en `src/test/e2e/login/login.spec.ts` (mismo nombre base que el plan, misma carpeta) usando `src/test/e2e/seed.spec.ts` como semilla."

**Qué hace internamente:**
1. Llama a `generator_setup_page` con la semilla.
2. Ejecuta cada paso del escenario en el navegador real con `browser_click`, `browser_type`, etc.
3. Cada paso lleva como `intent` la descripción del plan (queda como comentario en el test).
4. Llama a `generator_read_log` para obtener la traza limpia.
5. Llama a `generator_write_test` con el código `.spec.ts` final.

**Convenciones que respeta el generator:**
- **Un único `.spec.ts` por descripción, con el mismo nombre base** (`login.desc.md` → `login.spec.ts`) y en la **misma carpeta**.
- Todos los escenarios de la descripción dentro de ese fichero: un `describe` por suite/grupo y un `test()` por escenario.
- Comentario con el texto del paso antes de cada acción.
- Locators robustos basados en roles (`getByRole`, `getByLabel`).

### 3. Healer — arreglar tests rotos

**Cuándo:** después de cambiar la UI, antes de un release, o cuando `npx playwright test` falla.

**Cómo invocar:**

> "Usa el subagente `playwright-test-healer` para arreglar todos los tests que fallan en `src/test/e2e/login/`."

**Qué hace internamente:**
1. `test_run` para identificar fallos.
2. `test_debug` sobre cada test roto.
3. Inspecciona con `browser_snapshot`, `browser_console_messages`, `browser_network_requests`.
4. Diagnostica: selector que cambió, timing, assertion obsoleta, datos.
5. Edita el `.spec.ts` con `Edit` / `MultiEdit`.
6. Re-ejecuta para validar.

**Estrategias preferidas por el healer:**
- Para datos dinámicos → regex en los locators (`getByText(/Expediente \d+/)`).
- Para timing → `await expect(...).toBeVisible()` en vez de `waitForTimeout`.
- Para selectores rotos → `browser_generate_locator` sobre el elemento actual.

## Tools MCP relevantes

Las que pueden invocarse directamente sin pasar por los subagentes (útil si quieres una sola acción):

| Tool | Para qué |
|------|----------|
| `mcp__playwright-test__test_list` | Listar todos los tests |
| `mcp__playwright-test__test_run` | Ejecutar la suite o una selección |
| `mcp__playwright-test__test_debug` | Lanzar un test paso a paso |
| `mcp__playwright-test__browser_navigate` | `page.goto(url)` |
| `mcp__playwright-test__browser_snapshot` | Árbol de accesibilidad |
| `mcp__playwright-test__browser_click` / `_type` / `_press_key` | Interacción |
| `mcp__playwright-test__browser_console_messages` | Logs de la consola |
| `mcp__playwright-test__browser_network_requests` | Tráfico HTTP |
| `mcp__playwright-test__browser_generate_locator` | Generar un locator robusto |

## Ejecución manual (sin Claude)

Los tests generados son `@playwright/test` estándar. Se ejecutan con la CLI clásica:

```bash
npx playwright test                          # toda la suite
npx playwright test src/test/e2e/login       # solo un grupo
npx playwright test --debug                  # modo debug
npx playwright show-report                   # informe HTML
```

## Reglas para Claude al usar Test Agents

1. **No mezcles roles.** Si la tarea es planificar, lanza solo al planner. No le pidas que también genere.
2. **El plan se revisa con el usuario antes de generar.** El plan es el artefacto humano; los tests son su consecuencia.
3. **No edites a mano un `.spec.ts` recién generado** salvo para ajustes menores. Si está mal, vuelve a invocar al generator con un escenario corregido.
4. **El healer edita tests, no código de producción.** Si el test falla porque la app está rota, dilo y para. No "arregles" el test ocultando un bug real (el porqué, en `when-to-use.md` § *Modelo mental — dónde actúa la IA*).
5. **Los snapshots en `.playwright-mcp/` son trazas temporales.** No se commitean. Ya están en `.gitignore`.