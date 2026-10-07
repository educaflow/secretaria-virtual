# Generación: de `t-NNN-<slug>.desc.md` a su `.spec.ts` (feature libre)

Lo lee el **generador** (README §2.1). Tarea: convertir **una** descripción autocontenida en su test Playwright hermano, pilotando la app real (salvo que sea manual, §6).

**MUST** cargar `/k-playwright`. **MUST** usar las tools MCP `generator_setup_page` / `browser_*` / `generator_write_test` para grabar el test contra la app levantada.

**CRITICAL — disciplina de tiempo:**

1. **Antes** de pilotar, reutiliza los `.spec.ts` hermanos ya verdes de `{destino}` (y, si la carpeta es nueva, los de la parte de la aplicación más parecida bajo `src/test/e2e/`): ya resuelven login, navegación y locators de Axelor.
2. Usa el navegador solo para lo específico de **este** test.
3. **MUST** cerrar la sesión con `browser_close` al terminar.

---

## 1. Qué leer de la descripción

1. Frontmatter `id: T-NNN`, la línea `**Origen ESC:**` y **`**Manual:**`** → trazabilidad y vía (normal o manual, §6).
2. **Usuario que inicia sesión**: `## Precondiciones` nombra el login; su contraseña está en la tabla **Usuarios de acceso**. Si el test cambia de usuario a mitad, cada login sale de esa tabla.
3. **Pasos** → acciones. **Resultado esperado** → aserciones, **todas**, incluidas las de **no-regresión** y la de **deshacer lo global**.

---

## 2. Nombre y ubicación

**MUST** mismo nombre base que el `.desc.md`, misma carpeta `{destino}`: `t-001-publicar-un-aviso.desc.md` → `t-001-publicar-un-aviso.spec.ts`.

---

## 3. Ciclo de autenticación

Con el helper `src/test/e2e/_support/auth.ts`: `await ensureLoggedOut(page)` → `await login(page, '<login>', '<contraseña>')` → pasos y aserciones → `await logout(page)`. Si el escenario cambia de actor, repite `logout` → `login` en ese punto. El helper es test code: ajústalo si un selector no casa; **MUST NOT** tocar `src/main/...`.

---

## 4. Idempotencia OBLIGATORIA (BD compartida, sin reset)

**CRITICAL.** Cada test **MUST** pasar en verde **repetidamente**:

1. **Nombres únicos por ejecución** (`Date.now()`) en todo dato que el test cree, usados en el `fill` y en **todas** las aserciones.
2. **Teardown en `try/finally`**: borrar o revertir lo creado, aunque una aserción falle.
3. **Estado global: restaurar siempre.** Si el test activa una configuración, publica un aviso, cambia un parámetro para todos… el `finally` **MUST** dejarlo exactamente como estaba (desactivar, borrar, restaurar el valor anterior leído al principio). Un test que deja estado global cambiado rompe tests de **otras** iniciativas en la puerta de regresión.
4. **Pre-limpieza defensiva**: si un run anterior abortó y dejó basura que afecta al test (p. ej. un aviso activo con el prefijo del test), límpiala al arrancar.
5. **Datos poco contendidos** del seed, con comentario de por qué.
6. **Excepción documentada**: si una regla impide borrar lo creado, confía en el nombre único y documenta el `.catch(() => {})`.

**MUST** comprobarlo ejecutando el test **2 veces seguidas** en verde sin limpiar la BD antes de devolver `ESCRITO` (no aplica a los manuales, §6).

---

## 5. Plantilla literal del `.spec.ts` (test normal)

El import de `_support/auth` es siempre `'../../_support/auth'` (README §3.2).

```ts
import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-NNN — <nombre del test>
// origen: ESC-NNN  |  verifica: <contenido de la línea "Verifica:">  |  tipo: <happy|error|UI|no-regresion>
// fuente: .sdd/drafts/<iniciativa>/test-e2e-desc/t-NNN-<slug>.desc.md
test.describe('<parte de la aplicación o pantalla>', () => {
  test('<nombre del test, tal cual el título del .desc.md>', async ({ page }) => {
    const sufijo = `tNNN-${Date.now()}`;      // nombres únicos por ejecución (NNN = número del test)
    await ensureLoggedOut(page);
    await login(page, '<login de la precondición>', '<contraseña de la tabla>');
    try {
      // Paso 1: <texto del paso>
      await page.goto('/#/...');
      // ...acciones con locators por rol/label (ver /k-playwright)...

      // (si el escenario cambia de actor)
      await logout(page);
      await login(page, '<otro login>', '<su contraseña>');

      // Resultado esperado: <texto>
      await expect(page.getByText(/.../)).toBeVisible();
      // ...una aserción por cada punto del Resultado esperado...
    } finally {
      // Restaurar el estado global y borrar lo creado, aunque una aserción haya fallado.
      // (login del actor que puede deshacerlo si hace falta)
      // ...
      await logout(page).catch(() => {});
    }
  });
});
```

- Comentario con el texto del paso antes de cada acción. Locators por rol/label; **nunca** IDs autogenerados ni `waitForTimeout`.
- Elementos del **marco común** (banners, cabecera) existen en todas las pantallas: acota el locator al contenedor para no tener varios matches.

---

## 6. `Manual: sí` — tag `@manual` y puerta manual

Solo si el campo `Manual` es `sí`. El test se escribe **entero** (todos los pasos y aserciones) y **no se ejecuta** (ni 2 veces ni 1: el motor tampoco lo ejecuta). Cambian tres cosas:

1. **Tag `@manual`** en las opciones del `test(...)`.
2. **`test.setTimeout(...)`** amplio en la primera línea del test.
3. **Puerta manual** donde está el paso humano: comentario `// === PASO MANUAL ===` con **qué hace la persona** y una aserción con timeout largo que **espera al efecto observable** de ese paso. **MUST NOT** `page.pause()` a secas ni `waitForTimeout`.

```ts
// T-NNN — <nombre del test>
// origen: ESC-NNN  |  verifica: <…>  |  tipo: <…>
// MANUAL: <motivo verbatim del campo Manual>
// Ejecutar con:  E2E_MANUAL=1 npx playwright test --grep @manual --headed
// fuente: .sdd/drafts/<iniciativa>/test-e2e-desc/t-NNN-<slug>.desc.md
test('<nombre del test>', { tag: '@manual' }, async ({ page }) => {
  test.setTimeout(600_000);   // la persona necesita su tiempo
  // … todo el camino automatizable, igual que un test normal, con try/finally …

  // === PASO MANUAL ===
  // <qué debe hacer la persona, en una frase>
  // El test continúa solo cuando el efecto es visible en la UI:
  await expect(page.getByText(/<efecto observable>/)).toBeVisible({ timeout: 600_000 });

  // … el resto de aserciones, sin recortar ninguna …
});
```

- **MUST NOT** acortar el test «porque no se ejecuta en CI». **MUST NOT** `test.skip`/`test.fixme`. **MUST NOT** poner el tag a un test `Manual: no`.
- Devuelve `ESCRITO:` igual que siempre.

---

## 7. Plantilla literal de `_support/auth.ts`

El motor crea y valida este helper una vez (Fase 2 y §9.0 del skill). Si no existe, créalo con esta plantilla y ajusta los selectores a la UI real; si existe, reúsalo.

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
  await page.getByRole('toolbar', { name: /User Menu/i }).getByRole('button').first().click().catch(() => {});
  await page.getByRole('menuitem', { name: /Cerrar sesión|Tancar sessió|Logout|Log out/i }).click().catch(() => {});
  await expect(page.getByRole('button', { name: LOGIN_BTN })).toBeVisible();
}
```

---

## 8. Checklist del generador

**LIMIT**: 3 iteraciones de autocorrección.

- [ ] ¿Mismo nombre base y misma carpeta `{destino}` que el `.desc.md`?
- [ ] ¿Import de `_support/auth` igual a `'../../_support/auth'` (README §3.2)?
- [ ] ¿`ensureLoggedOut` → `login(usuario, contraseña)` → pasos → `logout`, con los cambios de actor que marque el escenario?
- [ ] ¿Una aserción real por **cada** punto del `Resultado esperado`, incluidas las de no-regresión?
- [ ] ¿Locators por rol/label acotados al contenedor; sin IDs autogenerados ni `waitForTimeout`?
- [ ] ¿Idempotente (§4): nombres únicos, `try/finally`, **estado global restaurado**, pre-limpieza defensiva?
- [ ] Si `Manual: no`: ¿pasa **2 veces seguidas** en verde sin limpiar la BD, y **no** lleva `@manual`?
- [ ] Si `Manual: sí`: ¿lleva `@manual`, `test.setTimeout`, el comentario `MANUAL:` y la puerta manual esperando a un efecto observable, sin `skip`/`fixme`, y **no** lo has ejecutado?
- [ ] ¿Comentarios de trazabilidad (`// T-NNN`, `// origen:`, `// fuente:`)?
- [ ] ¿`_support/auth.ts` existe y funciona?
- [ ] ¿`browser_close` al terminar?
- [ ] ¿La respuesta es **exactamente** `ESCRITO: {ruta del .spec.ts}` (o `BLOQUEADO: {T-NNN} — {motivo}`)?
