// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts
// target: .sdd/drafts/2026-05-21_20-14_correos/implementation/test_e2e/T-005_alta-manual-sin-asunto-rechazada.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Alta manual sin asunto rechazada', async ({ page }) => {
    // 1. Login admin/admin
    await page.goto('/#/login');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();
    await expect(page).toHaveURL(/#\//);

    // 2. Navegar a Todos los correos
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');

    // 3. Click "Nuevo correo" → el hash pasa a .../edit
    await page.getByTestId('item:new').getByTestId('button').click();
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/edit/);

    // 4. Rellenar DNI destinatario y pulsar Tab para autocompletar el email
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('24362574P');
    await page.keyboard.press('Tab');
    // Verificar que el email se autocompleta
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('lorenzo.profesor@gmail.com');

    // 5. Asunto: DEJAR VACÍO (no tocar)

    // 6. Cuerpo (contenteditable): click + fill
    await page.locator('.custom-html-editor-content').click();
    await page.locator('.custom-html-editor-content').fill('Cuerpo de prueba T-005 sin asunto.');

    // 7. Guardar: el sistema debe rechazar el alta por asunto vacío
    await page.getByTestId('widget:btnSave').getByTestId('button').click();

    // 8a. VERIFICACIÓN: alert visible que menciona el asunto obligatorio (alert transitorio — verificar inmediatamente)
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.getByRole('alert')).toContainText('sunto');

    // 8b. El formulario sigue en modo alta sin persistir (sin número de registro en la URL)
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/edit$/);

    // 9. Logout
    await page.goto('/logout');
  });
});
