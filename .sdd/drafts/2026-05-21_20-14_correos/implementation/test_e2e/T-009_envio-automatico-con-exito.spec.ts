// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Envío automático con éxito', async ({ page }) => {
    test.setTimeout(300_000);

    const asunto = 'T-009-' + Date.now();

    // 1. Login admin/admin
    await page.goto('/');
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

    // 7. Esperar a que la tarea periódica (cron, cada minuto) procese el correo.
    //    La espera se hace contra la API REST con la sesión ya autenticada del navegador
    //    (page.request comparte las cookies del contexto): es determinista y refleja la BD real.
    //    NO se sondea por la UI: la SPA de Axelor cachea el form abierto y el grid (page.goto al
    //    mismo hash no recarga), y el correo nuevo no siempre cae en la primera página del grid.
    let correoId: number | undefined;
    await expect
      .poll(
        async () => {
          const resp = await page.request.post(
            '/ws/rest/com.educaflow.subsystem.correos.db.Correo/search',
            {
              headers: { 'Content-Type': 'application/json' },
              data: {
                fields: ['estado'],
                data: {
                  criteria: [{ fieldName: 'asunto', operator: '=', value: asunto }],
                  operator: 'and',
                },
              },
            },
          );
          const json = await resp.json();
          const rec = json?.data?.[0];
          correoId = rec?.id;
          return rec?.estado ?? '';
        },
        { timeout: 240_000, intervals: [5000, 10000, 15000] },
      )
      .toBe('ENVIADO');

    // 8. Abrir el detalle fresco por id (no se abrió antes → la SPA lo fetchea limpio) y verificar
    //    en la UI los campos del panel "Seguimiento".
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/edit/' + correoId);
    await page.reload();

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
