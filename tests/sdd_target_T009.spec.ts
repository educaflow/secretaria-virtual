// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts
// TARGET: .sdd/drafts/2026-05-21_20-14_correos/implementation/test_e2e/T-009_envio-automatico-con-exito.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Envío automático con éxito', async ({ page }) => {
    test.setTimeout(300_000);

    const asunto = 'T-009-' + Date.now();

    // 1. Login admin/admin
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // 2. Navigate to "Todos los correos"
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');

    // 3. Click "Nuevo correo" button to open creation form
    await page.getByTestId('item:new').getByTestId('button').click();

    // 4. Fill DNI, press Tab to trigger autocomplete, verify email
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('24362574P');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('lorenzo.profesor@gmail.com');

    // 5. Fill asunto with unique value and cuerpo
    await page.getByRole('textbox', { name: 'Asunto' }).fill(asunto);
    await page.locator('.custom-html-editor-content').click();
    await page.keyboard.type('Cuerpo de prueba T-009');

    // 6. Guardar — saves and returns to the list
    await page.getByTestId('widget:btnSave').getByTestId('button').click();

    // 7. Poll the LIST (reloading it on each attempt) until the row for this asunto contains "Enviado".
    //    The list always re-queries the server on load, so it reflects the current DB state.
    //    The cron job runs every minute; allow up to 240 s (approx 2-3 cron cycles).
    //    IMPORTANT: do NOT poll the detail view — Axelor's SPA caches the form and re-navigating
    //    to the same hash does NOT refetch from server, so Estado would always read "Pendiente".
    await expect.poll(
      async () => {
        await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');
        const row = page.getByRole('row', { name: new RegExp(asunto) });
        return (await row.count()) ? (await row.first().textContent() ?? '') : '';
      },
      { timeout: 240_000, intervals: [5000, 10000, 15000] }
    ).toContain('Enviado');

    // 8. Open fresh detail by double-clicking the row (triggers a new fetch, shows updated state)
    await page.getByRole('row', { name: new RegExp(asunto) }).first().dblclick();

    // 9. Verify Seguimiento panel fields in the freshly-fetched detail view

    // Estado = Enviado
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Enviado');

    // fechaEnvio NOT empty
    await expect(page.getByRole('textbox', { name: 'Fecha de envío' })).not.toHaveValue('');

    // numeroIntentos > 0 (not '0' and not '')
    await expect(page.getByRole('textbox', { name: 'Número de intentos' })).not.toHaveValue('0');
    await expect(page.getByRole('textbox', { name: 'Número de intentos' })).not.toHaveValue('');

    // fechaUltimoIntento NOT empty
    await expect(page.getByRole('textbox', { name: 'Fecha del último intento' })).not.toHaveValue('');

    // 10. Logout
    await page.goto('/logout');
  });
});
