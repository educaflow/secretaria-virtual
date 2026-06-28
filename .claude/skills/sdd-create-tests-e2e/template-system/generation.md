# Generación: de `t-NNN-<slug>.desc.md` a su `.spec.ts`

Lo lee el **generador** (§2.1 del `README.md`). Tarea: convertir **una** descripción autocontenida en su test Playwright hermano, pilotando la app real.

**MUST** cargar `/k-playwright` antes de empezar (convenciones de locators, baseURL, estructura). **MUST** usar las tools MCP `generator_setup_page` / `browser_*` / `generator_write_test` para grabar el test ejecutando los pasos contra la app levantada.

---

## 1. Qué leer de la descripción

El `.desc.md` es **autocontenido**. Extrae:

1. **Frontmatter** `id: T-NNN` y la línea `**Origen ESC:**` → para los comentarios de trazabilidad.
2. **Usuario que inicia sesión**: lo dice `## Precondiciones` (p.ej. "El usuario `supervisor1@mislata.es` ha iniciado sesión"). Busca ese login en la tabla **Usuarios de acceso** de `## Estado inicial de la base de datos` para obtener su **contraseña**. Si la precondición no nombra usuario, usa el más razonable según el bloque (normalmente el actor del escenario).
3. **Pasos** (`## Pasos`, Given/When/Then) → las acciones del test.
4. **Resultado esperado** (`## Resultado esperado`) → las **aserciones** (`expect`). **MUST** materializar **todas**; no omitir ninguna.

---

## 2. Nombre y ubicación del fichero

- **MUST** escribir el `.spec.ts` con **el mismo nombre base** que el `.desc.md` y en **la misma carpeta**: `t-001-crear-un-grupo-con-sus-alumnos.desc.md` → `t-001-crear-un-grupo-con-sus-alumnos.spec.ts`.
- ❌ INCORRECTO: nombre distinto, otra carpeta, repartir en varios ficheros.

---

## 3. Ciclo de autenticación (autocontenido)

Cada test **MUST** gestionar su propio ciclo de sesión con el helper `src/test/e2e/_support/auth.ts`:

1. `await ensureLoggedOut(page)` — logout defensivo si quedó sesión de un test anterior.
2. `await login(page, '<login>', '<contraseña>')` — el usuario de la precondición.
3. Los pasos del escenario + las aserciones del resultado esperado.
4. `await logout(page)` — cerrar sesión al final.

**El helper `_support/auth.ts` es test code, no código de la app**: si los selectores de login/logout no casan con la UI real, **MUST** ajustarlo (lo verificas pilotando la app). **MUST NOT** tocar `src/main/...`.

---

## 4. Plantilla literal del `.spec.ts`

```ts
import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-NNN — <nombre del test>
// origen: ESC-NNN  |  verifica: <contenido de la línea "Verifica:" del .desc.md>
// fuente: .sdd/drafts/<iniciativa>/test-e2e-desc/t-NNN-<slug>.desc.md
test.describe('<pantalla o grupo del escenario>', () => {
  test('<nombre del test, tal cual el título del .desc.md>', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, '<login de la precondición>', '<contraseña de la tabla>');

    // Paso 1: <texto del paso 1 del .desc.md>
    await page.goto('/#/...');            // rutas relativas — baseURL ya configurada
    // ...acciones con locators por rol/label (ver /k-playwright)...

    // Resultado esperado: <texto del resultado esperado>
    await expect(page.getByText('...')).toBeVisible();
    // ...una aserción por cada punto del Resultado esperado...

    await logout(page);
  });
});
```

- Comentario con el texto del paso **antes** de cada acción.
- Locators por rol/label (`getByRole`, `getByLabel`), **nunca** IDs autogenerados de Axelor ni `waitForTimeout` (usa `expect(...).toBeVisible()`). Lo detalla `/k-playwright`.

---

## 5. Plantilla literal de `_support/auth.ts`

El motor **comprueba y crea** este helper al lanzar (Fase 2) y **valida su login/logout contra la app real una vez** antes de generar ningún test (Fase 4 §9.0): ahí se ajustan los selectores best-effort a la UI real. Por eso, al generar cada test, **asume que `_support/auth.ts` ya funciona** y solo corrígelo si aún ves un selector roto.

Si el helper no existe, créalo con esta plantilla (best-effort; **ajusta los selectores a la UI real** pilotando la app). Si ya existe, reúsalo (y corrígelo solo si un selector falla).

```ts
import { Page, expect } from '@playwright/test';

const LOGIN_PATH = '/#/login';
const LOGIN_BTN = /Iniciar sesión|Iniciar sessió|Login/;
const USER_FIELD = /Usuario|Usuari/;
const PASS_FIELD = /Contraseña|Contrasenya|Password/;

async function enLogin(page: Page): Promise<boolean> {
  return await page.getByRole('button', { name: LOGIN_BTN }).isVisible().catch(() => false);
}

// Logout defensivo: si quedó sesión abierta, ciérrala. Deja la app en el login.
export async function ensureLoggedOut(page: Page): Promise<void> {
  await page.goto(LOGIN_PATH);
  if (!(await enLogin(page))) {
    await logout(page);
  }
  await expect(page.getByLabel(USER_FIELD)).toBeVisible();
}

export async function login(page: Page, usuario: string, contrasena: string): Promise<void> {
  await page.goto(LOGIN_PATH);
  await page.getByLabel(USER_FIELD).fill(usuario);
  await page.getByLabel(PASS_FIELD).fill(contrasena);
  await page.getByRole('button', { name: LOGIN_BTN }).click();
  await expect(page).toHaveURL(/#\/(?!login)/);
}

export async function logout(page: Page): Promise<void> {
  // Menú de usuario (arriba a la derecha) → Cerrar sesión.
  // AJUSTAR a la UI real si los nombres difieren (es test code, no app code).
  await page.getByRole('button', { name: /cuenta|account|usuario|perfil/i }).first().click().catch(() => {});
  await page.getByRole('menuitem', { name: /Cerrar sesión|Tancar sessió|Logout|Log out/i }).click().catch(() => {});
  await expect(page.getByRole('button', { name: LOGIN_BTN })).toBeVisible();
}
```

---

## 6. Checklist del generador

**MUST NOT** terminar si queda algún punto sin cumplir. **LIMIT**: 3 iteraciones de autocorrección.

- [ ] ¿El `.spec.ts` tiene **el mismo nombre base** que el `.desc.md` y está en **la misma carpeta**?
- [ ] ¿Importa y usa `ensureLoggedOut` → `login(usuario, contraseña)` → pasos → `logout`?
- [ ] ¿El usuario y la contraseña salen de la precondición + la tabla de credenciales del `.desc.md`?
- [ ] ¿Hay una **aserción por cada punto** del `## Resultado esperado` (ninguna omitida ni debilitada)?
- [ ] ¿Los locators son por rol/label, sin IDs autogenerados ni `waitForTimeout`?
- [ ] ¿Lleva los comentarios de trazabilidad (`// T-NNN`, `// origen: ESC-NNN`, `// fuente: …`)?
- [ ] ¿`_support/auth.ts` existe y sus selectores funcionan contra la app real (si no, ajustado)?
- [ ] ¿La respuesta es **exactamente** `ESCRITO: {ruta del .spec.ts}` (o `BLOQUEADO: {T-NNN} — {motivo}`)?
