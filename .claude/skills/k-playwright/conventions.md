# Convenciones de Playwright en este proyecto

Estructura, nombres y patrones para todos los tests E2E de la secretaría virtual.

## URLs

- **Base local:** `http://localhost:8080/`
- **Login:** `http://localhost:8080/#/login`
- La aplicación se compila y arranca con `./run.sh` (ver `CLAUDE.md`).

`playwright.config.ts` tiene `baseURL` = `http://localhost:8080`. **MUST** preferir rutas relativas en los tests (`page.goto('/#/login')`); usa la URL absoluta solo si necesitas otro host.

## Estructura de carpetas

```
secretaria-virtual/
├── playwright.config.ts          # config global del runner
├── package.json                  # @playwright/test, @types/node
├── specs/                        # PLANES de test (markdown)
│   ├── README.md
│   ├── login.plan.md
│   └── <area>.plan.md
├── tests/
│   ├── seed.spec.ts              # punto de entrada común (goto /login)
│   ├── example.spec.ts           # plantilla por defecto — borrar
│   └── <area>/                   # un directorio por pantalla/flujo
│       ├── <escenario>.spec.ts
│       └── ...
└── .playwright-mcp/              # trazas temporales del MCP — NO commitear
```

## Reglas de nombres

### Planes (`specs/*.plan.md`)

- Un fichero por área funcional: `login.plan.md`, `expedientes.plan.md`, `registro-entrada-salida.plan.md`.
- Nombre en kebab-case sin sufijos.

### Tests (`tests/<area>/<escenario>.spec.ts`)

- **Un test por fichero.** Esta es la convención del `generator` — respétala.
- Carpeta = área del plan (`tests/login/`, `tests/expedientes/`).
- Nombre del fichero = nombre del escenario en kebab-case: `successful-login.spec.ts`, `wrong-password.spec.ts`, `forgot-password-back-to-login.spec.ts`.
- Dentro del fichero:
  - Un `test.describe(...)` cuyo nombre coincide con el grupo del plan.
  - Un único `test(...)` cuyo nombre coincide con el escenario.

### Seed (`tests/seed.spec.ts`)

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
npx playwright test tests/login

# Un fichero
npx playwright test tests/login/successful-login.spec.ts

# Modo debug interactivo
npx playwright test --debug tests/login/successful-login.spec.ts

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

Los tests E2E **necesitan la aplicación corriendo** en `localhost:8080`. Hoy hay que arrancarla manualmente con Gradle antes de lanzar los tests.

Si se configura `webServer` en `playwright.config.ts` en el futuro, Playwright arrancará la app automáticamente.
