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
