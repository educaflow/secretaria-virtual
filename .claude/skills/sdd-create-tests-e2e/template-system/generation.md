# Generación: de `t-NNN-<slug>.desc.md` a su `.spec.ts`

Lo lee el **generador** (§2.1 del `README.md`). Tarea: convertir **una** descripción autocontenida en su test Playwright hermano, pilotando la app real.

**MUST** cargar `/k-playwright` antes de empezar (convenciones de locators, baseURL, estructura). **MUST** usar las tools MCP `generator_setup_page` / `browser_*` / `generator_write_test` para grabar el test ejecutando los pasos contra la app levantada.

**CRITICAL — disciplina de tiempo** (redescubrir toda la UI en cada test es el mayor coste de tiempo del flujo):

1. **Reutiliza los `.spec.ts` hermanos ya verdes** de la misma carpeta `src/test/e2e/<capa>/<sistema>/` **antes** de pilotar nada: ya tienen resueltos el login, la navegación a las pantallas, los locators de Axelor y el patrón de idempotencia (§4). Cópialos como base y usa el navegador **solo** para lo específico de **este** test.
2. **MUST NOT** explorar la UI de forma exhaustiva si un spec hermano ya muestra el camino.
3. **MUST** cerrar tu sesión de navegador con `browser_close` al terminar: una sesión MCP huérfana bloquea al siguiente subagente durante minutos.

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

## 4. Idempotencia OBLIGATORIA (BD compartida, sin reset)

**CRITICAL** — la BD es **compartida entre todos los tests y NO se resetea** entre ejecuciones (lo recuerda el §3 del `README.md`). Un `.spec.ts` que pasa una vez pero falla al reejecutarse es un test **ROTO**. Cada test **MUST** poder ejecutarse en verde **repetidamente** sin limpiar la BD a mano:

1. **Nombres únicos por ejecución**: a cualquier dato que el test cree (grupos, etc.) **MUST** añadirle un sufijo único con `Date.now()`. Un nombre fijo choca con datos de runs previos y con las restricciones de unicidad.
   - ✅ CORRECTO: ``const grupo = `1º DAM A t001-${Date.now()}`;`` y usar `grupo` en el `fill` y en **todas** las aserciones que comparan el nombre.
   - ❌ INCORRECTO: `await nombre.fill('1º DAM A');` (nombre fijo → al reejecutar la app no vuelve a la lista y el test cuelga/falla).
2. **Teardown en `try/finally`**: lo que el test crea **MUST** borrarlo al final, **aunque una aserción falle** (el cuerpo del test va en `try`, el borrado en `finally`). Borrar el registro padre libera sus hijos (composición/orphanRemoval).
3. **Pre-limpieza defensiva (self-healing)** para recursos que una **regla de negocio** deja "pegados": si un run anterior abortó y dejó basura (p.ej. un alumno que no puede estar en dos grupos del mismo curso académico queda atado a un grupo residual), el test **MUST** liberar ese recurso **al arrancar**, no solo al final. Así un run roto no envenena los siguientes.
4. **Datos poco contendidos**: elige del seed datos que otros tests no fijen; documenta en comentario por qué usas ese alumno/curso concreto.
5. **Excepción documentada**: si una regla de negocio impide borrar lo creado (p.ej. un grupo cerrado no se puede borrar), confía en el nombre único para no colisionar; documenta en comentario por qué el teardown no lo borra (y que el `.catch(() => {})` es intencional, no un error tapado).

**MUST** comprobar la idempotencia ejecutando el test **2 veces seguidas** en verde **sin** limpiar la BD entre medias antes de devolver `ESCRITO`.

---

## 5. Plantilla literal del `.spec.ts`

**CRITICAL — profundidad del import**: el test vive en `src/test/e2e/<capa>/<sistema>/`, así que `_support/` está **dos** niveles arriba. **MUST** usar `'../../_support/auth'`; `'../_support/auth'` no resuelve y deja el test rojo.

```ts
import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

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

## 6. Plantilla literal de `_support/auth.ts`

El motor **comprueba y crea** este helper al lanzar (Fase 2) y **valida su login/logout contra la app real una vez** antes de generar ningún test (Fase 4 §9.0): ahí se ajustan los selectores best-effort a la UI real. Por eso, al generar cada test, **asume que `_support/auth.ts` ya funciona** y solo corrígelo si aún ves un selector roto.

Si el helper no existe, créalo con esta plantilla (best-effort; **ajusta los selectores a la UI real** pilotando la app). Si ya existe, reúsalo (y corrígelo solo si un selector falla).

```ts
import { Page, expect } from '@playwright/test';

const LOGIN_PATH = '/#/login';
const LOGIN_BTN = /Entrar|Sign in|Iniciar sesión|Iniciar sessió|Login/;
const USER_FIELD = /Usuario|Usuari/;
const PASS_FIELD = /Contraseña|Contrasenya|Password/;

async function enLogin(page: Page): Promise<boolean> {
  return await page.getByRole('button', { name: LOGIN_BTN }).isVisible().catch(() => false);
}

// Logout defensivo: si quedó sesión abierta, ciérrala. Deja la app en el login.
export async function ensureLoggedOut(page: Page): Promise<void> {
  await page.goto(LOGIN_PATH);
  // Esperar a que la SPA renderice antes de decidir: o aparece el login
  // (sesión cerrada) o la barra de usuario de Axelor (sesión abierta).
  // `isVisible()` es instantáneo y daría falsos negativos justo tras el goto.
  const loginBtn = page.getByRole('button', { name: LOGIN_BTN });
  const userMenu = page.getByRole('toolbar', { name: /User Menu/i });
  await loginBtn.or(userMenu).first().waitFor();
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
  // El botón muestra las iniciales del usuario (dinámicas), así que se localiza
  // por la toolbar "User Menu" de Axelor, no por el nombre del botón.
  await page.getByRole('toolbar', { name: /User Menu/i }).getByRole('button').first().click().catch(() => {});
  await page.getByRole('menuitem', { name: /Cerrar sesión|Tancar sessió|Logout|Log out/i }).click().catch(() => {});
  await expect(page.getByRole('button', { name: LOGIN_BTN })).toBeVisible();
}
```

---

## 7. Checklist del generador

**MUST NOT** terminar si queda algún punto sin cumplir. **LIMIT**: 3 iteraciones de autocorrección.

- [ ] ¿El `.spec.ts` tiene **el mismo nombre base** que el `.desc.md` y está en **la misma carpeta**?
- [ ] ¿Importa y usa `ensureLoggedOut` → `login(usuario, contraseña)` → pasos → `logout`?
- [ ] ¿El usuario y la contraseña salen de la precondición + la tabla de credenciales del `.desc.md`?
- [ ] ¿Hay una **aserción por cada punto** del `## Resultado esperado` (ninguna omitida ni debilitada)?
- [ ] ¿Los locators son por rol/label, sin IDs autogenerados ni `waitForTimeout`?
- [ ] ¿El test es **idempotente** (§4): nombres únicos `Date.now()`, teardown `try/finally`, y pre-limpieza defensiva si una regla de negocio puede dejar recursos pegados?
- [ ] ¿Has comprobado que pasa **2 veces seguidas** en verde sin limpiar la BD?
- [ ] ¿Lleva los comentarios de trazabilidad (`// T-NNN`, `// origen: ESC-NNN`, `// fuente: …`)?
- [ ] ¿`_support/auth.ts` existe y sus selectores funcionan contra la app real (si no, ajustado)?
- [ ] ¿Has **cerrado la sesión de navegador** (`browser_close`) al terminar?
- [ ] ¿La respuesta es **exactamente** `ESCRITO: {ruta del .spec.ts}` (o `BLOQUEADO: {T-NNN} — {motivo}`)?
