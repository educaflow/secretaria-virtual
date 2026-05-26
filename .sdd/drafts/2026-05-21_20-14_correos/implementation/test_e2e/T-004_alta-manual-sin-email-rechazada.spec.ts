// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Alta manual sin email rechazada', async ({ page }) => {
    // 1. Navigate to http://localhost:8080 and login with admin/admin
    await page.goto('/#/login');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();
    await expect(page).toHaveURL(/#\//);

    // 2. Navigate to Todos los correos list view
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');

    // 3. Click "Nuevo correo" button
    await page.getByTestId('item:new').getByTestId('button').click();
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/edit/);

    // 4. Fill DNI destinatario with 99999999R (inexistent DNI — email will NOT be autocompleted, stays empty)
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('99999999R');

    // 5. Leave Email destinatario empty (do NOT touch it)

    // 6. Fill Asunto with "Asunto de prueba T-004"
    await page.getByRole('textbox', { name: 'Asunto' }).fill('Asunto de prueba T-004');

    // 7. Fill body (contenteditable .custom-html-editor-content)
    await page.locator('.custom-html-editor-content').click();
    await page.locator('.custom-html-editor-content').fill('Cuerpo de prueba para T-004');

    // 8. Click Guardar button — the save should be rejected
    await page.getByTestId('widget:btnSave').getByTestId('button').click();

    // 9a. Verify alert is visible and contains the email required message (alert is transient — check immediately)
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.getByRole('alert')).toContainText('mail');

    // 9b. Verify the URL hash is still on the edit (new record) form
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/edit$/);

    // 9c. Navigate to list and confirm "Asunto de prueba T-004" was NOT created
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');
    await expect(page.getByText('Asunto de prueba T-004')).toHaveCount(0);

    // 10. Logout
    await page.goto('/logout');
  });
});
