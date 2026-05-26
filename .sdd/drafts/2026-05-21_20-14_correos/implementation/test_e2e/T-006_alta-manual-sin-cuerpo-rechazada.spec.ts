// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Alta manual sin cuerpo rechazada', async ({ page }) => {
    // 1. Login admin/admin
    await page.goto('/#/login');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();
    await expect(page).toHaveURL(/#\//);

    // 2. Navegar a "Todos los correos"
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');

    // 3. Hacer clic en "Nuevo correo" → hash /edit
    await page.getByTestId('item:new').getByTestId('button').click();
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/edit$/);

    // 4. DNI: teclear y presionar Tab para autocompletar el email
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('24362574P');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('lorenzo.profesor@gmail.com');

    // 5. Rellenar asunto
    await page.getByRole('textbox', { name: 'Asunto' }).fill('Asunto de prueba T-006');

    // 6. Cuerpo: NO tocar (queda vacío)

    // 7. Guardar
    await page.getByTestId('widget:btnSave').getByTestId('button').click();

    // 8. Verificar: alert visible con "Cuerpo es requerido" y hash sigue en /edit sin número
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.getByRole('alert')).toContainText('uerpo');
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/edit$/);

    // 9. Logout
    await page.goto('/logout');
  });
});
