// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('Login', () => {
  test('successful login with admin/admin', async ({ page }) => {
    // 1. Abrir la pantalla de login
    await page.goto('/#/login');

    // 2. Introducir "admin" en el campo de usuario
    await page.getByTestId('field-username').getByTestId('input').fill('admin');

    // 3. Introducir "admin" en el campo de contraseña
    await page.getByTestId('field-password').getByTestId('input').fill('admin');

    // 4. Pulsar el botón de iniciar sesión
    await page.getByTestId('btn-login').click();

    // 5. Verificar que la URL ya no es la de login (login correcto)
    await expect(page).not.toHaveURL(/#\/login/);
  });
});
