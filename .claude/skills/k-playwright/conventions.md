# Convenciones de Playwright en este proyecto

Estructura, nombres y patrones para todos los tests E2E de la secretaría virtual.

## URLs

- **Base local:** `http://localhost:8080/`
- **Login:** `http://localhost:8080/#/login`
- La aplicación se compila y arranca con `./run.sh` (ver `CLAUDE.md`).

`playwright.config.ts` tiene `baseURL` = `http://localhost:8080`. **MUST** preferir rutas relativas en los tests (`page.goto('/#/login')`); usa la URL absoluta solo si necesitas otro host.

## Estructura de carpetas

**CRITICAL — los tests replican la ruta del código que prueban.** La carpeta de un test es **`src/test/e2e/<capa>/<sistema>/`**, espejo de `src/main/java/com/educaflow/<capa>/<sistema>/`, con `<capa>` = `system` o `subsystem`. La unidad de agrupación es el **sistema**, no la iniciativa: varias iniciativas sobre el mismo sistema **comparten carpeta**, igual que comparten carpeta de código.

- ✅ CORRECTO: `src/test/e2e/subsystem/criptografia/`, `src/test/e2e/subsystem/correos/`, `src/test/e2e/system/gestioncentro/`
- ❌ INCORRECTO: `src/test/e2e/deshabilitar-certificado-digital/` (nombre de la iniciativa, no del sistema)
- ❌ INCORRECTO: `src/test/e2e/criptografia/` (falta la capa; no distingue `system` de `subsystem`)

Dentro de esa carpeta puede haber **varios pares descripción/test**. Cada par **comparte el mismo nombre base** y vive en la **misma carpeta**: `AAAA.desc.md` ↔ `AAAA.spec.ts`, `BBBB.desc.md` ↔ `BBBB.spec.ts`, etc. `src/test/e2e/` es el `testDir` configurado en `playwright.config.ts` y es **recursivo**: el runner descubre cualquier `.spec.ts` en cualquier subcarpeta por debajo, a cualquier profundidad.

Quedan **fuera del espejo** los tests que no pertenecen a ningún sistema (`login/`, `seed.spec.ts`) y las carpetas anteriores a esta convención. Conviven sin problema —el `testDir` es recursivo— y **MUST NOT** migrarse sin que el usuario lo pida.

La descripción (`*.desc.md`) **convive en la misma carpeta** que su `.spec.ts`. El runner **ignora los `.md`** (su `testMatch` por defecto solo recoge `*.spec.ts` / `*.test.ts`), así que tener las descripciones dentro de `testDir` no rompe nada.

```
secretaria-virtual/
├── playwright.config.ts          # config global del runner (testDir: ./src/test/e2e)
├── package.json                  # @playwright/test, @types/node
└── src/test/e2e/                 # raíz de los tests E2E (testDir, recursivo)
    ├── _support/                 # helpers compartidos (auth.ts…) — el runner los ignora (no son *.spec.ts)
    │   └── auth.ts               # ensureLoggedOut / login / logout
    ├── seed.spec.ts              # punto de entrada común (goto /login)
    ├── login/                    # fuera del espejo: no pertenece a ningún sistema
    │   └── successful-login.spec.ts
    ├── smoke-test-desarrollador/ # legacy: anterior a esta convención, se deja estar
    ├── system/                   # espejo de .../com/educaflow/system/ (aún sin tests)
    └── subsystem/                # espejo de .../com/educaflow/subsystem/
        └── criptografia/         # = subsystem/criptografia del código
            ├── t-001-<slug>.desc.md  # la DESCRIPCIÓN del test (markdown) — el runner la ignora
            ├── t-001-<slug>.spec.ts  # su TEST — MISMO nombre base, misma carpeta
            ├── t-002-<slug>.desc.md
            └── t-002-<slug>.spec.ts
```

**Ojo con la profundidad del import**: desde `<capa>/<sistema>/` el helper compartido está **dos** niveles arriba — `import { login } from '../../_support/auth'`, no `'../_support/auth'`.

(`.playwright-mcp/` en la raíz son trazas temporales del MCP — NO commitear.)

## Reglas de nombres

### Descripción y test: mismo nombre base, misma carpeta

- El markdown acompañante de un test es su **descripción** `<base>.desc.md`. **CRITICAL:** cada `<capa>/<sistema>/<base>.desc.md` se empareja con **un único** `<capa>/<sistema>/<base>.spec.ts`, **mismo nombre base** y en la **misma carpeta**.
- Como varias iniciativas comparten carpeta y la numeración `t-NNN` es **local a cada iniciativa**, dos tests distintos pueden querer el mismo nombre. La **identidad** de un test es la pareja (`id:`, `Origen ESC`) de su `.desc.md`, no su nombre de fichero: ante un choque con distinta identidad, `/sdd-create-tests-e2e` lo reporta como colisión y **MUST NOT** sobrescribir.
- **`.desc.md` es la convención del proyecto.** El antiguo `.plan.md` (flujo basado en el planner) queda **deprecado**; los tests nuevos usan `.desc.md`.
- En el pipeline SDD el nombre base es `t-NNN-<slug>` (lo fija `/sdd-debug-with-test-e2e-desc` al descomponer y `/sdd-create-tests-e2e` lo copia tal cual): `t-001-crear-un-grupo-con-sus-alumnos.desc.md` ↔ `t-001-crear-un-grupo-con-sus-alumnos.spec.ts`.
- **Todos los escenarios de una descripción van dentro de su único `.spec.ts`** (no se reparte en un fichero por escenario): un `test.describe(...)` por suite y un `test(...)` por escenario. En el flujo SDD cada `.desc.md` es **un solo test** → un solo `test(...)`.

### Auth compartida y tests autocontenidos (`src/test/e2e/_support/auth.ts`)

- Helpers reutilizables: `ensureLoggedOut(page)`, `login(page, usuario, contraseña)`, `logout(page)`.
- Cada `.spec.ts` es **autocontenido**: empieza con `ensureLoggedOut` (logout defensivo si quedó sesión), hace `login` con el usuario del test, ejecuta los pasos y termina con `logout`.
- `_support/` no contiene `*.spec.ts`, así que el runner lo **ignora** (es código de apoyo, no tests).

### Seed (`src/test/e2e/seed.spec.ts`)

Punto de entrada común — abre `/#/login` y verifica que la URL es la esperada. Sirve como semilla del generator: cualquier escenario que necesite empezar desde el login se basa en esta semilla.

**No tocar a mano** salvo para cambiar la URL base si cambia el puerto/host.

## Patrones de código

### Locators

**Preferir, en este orden:**

1. `page.getByRole('button', { name: 'Iniciar sesión' })` — accesible, resiliente.
2. `page.getByLabel('Usuario')` — para inputs con label.
3. `page.getByPlaceholder('Contraseña')` — fallback razonable.
4. `page.getByText(/Expediente \d+/)` — para datos dinámicos, con regex.

**Evitar:**

- `page.locator('css=.btn-primary')` — frágil.
- `page.locator('xpath=//div[3]/button')` — muy frágil.
- IDs autogenerados de Axelor (`#x-123`).

### Asserciones

```ts
await expect(page).toHaveURL(/#\/login/);
await expect(page.getByRole('alert')).toContainText('Credenciales incorrectas');
await expect(page.getByRole('button', { name: 'Login' })).toBeDisabled();
```

**Nunca** `page.waitForTimeout(1000)` — usa `expect(...).toBeVisible()` que espera por defecto.

### Estructura mínima de un test

```ts
import { test, expect } from '@playwright/test';

test.describe('Login', () => {
  test('successful login', async ({ page }) => {
    // Paso 1: abrir login (ruta relativa — baseURL ya configurada)
    await page.goto('/#/login');

    // Paso 2: introducir credenciales
    await page.getByLabel('Usuario').fill('admin');
    await page.getByLabel('Contraseña').fill('admin');

    // Paso 3: enviar
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();

    // Verificación
    await expect(page).toHaveURL(/#\/(?!login)/);
  });
});
```

### i18n en los tests

La aplicación es bilingüe (es/ca). Los locators por texto **deben asumir el idioma por defecto del usuario de test**, normalmente español. Si un test debe ejecutarse en ambos idiomas:

- Configura el `language` del usuario en la BBDD de test, o
- Usa regex para aceptar ambas variantes: `getByRole('button', { name: /Iniciar sesión|Iniciar sessió/ })`.

## Ejecución

```bash
# Todos los tests en chromium
npx playwright test --project=chromium

# Una carpeta
npx playwright test src/test/e2e/login

# Un fichero
npx playwright test src/test/e2e/login/successful-login.spec.ts

# Modo debug interactivo
npx playwright test --debug src/test/e2e/login/successful-login.spec.ts

# Informe HTML del último run
npx playwright show-report
```

## Lo que NO se commitea

Ya está en `.gitignore`:

- `node_modules/`
- `test-results/`
- `playwright-report/`
- `blob-report/`
- `playwright/.cache/`
- `playwright/.auth/`
- `.playwright-mcp/` — trazas del servidor MCP (Test Agents).
- `.playwright-cli/` y `playwright/.sessions/` — sesiones y caché del Agent CLI.

## Pre-requisito: arrancar la app

Los tests E2E **necesitan la aplicación corriendo** en `localhost:8080`. Hoy hay que arrancarla antes de lanzar los tests, **siempre con `./run.sh`** (compila, pasa los tests y arranca en el 8080 con la config correcta). **MUST NOT** usar `gradlew run` a mano — ver `CLAUDE.md`.

Si se configura `webServer` en `playwright.config.ts` en el futuro, Playwright arrancará la app automáticamente.
