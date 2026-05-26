// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('No se puede reenviar un Correo que no está en FALLIDO', async ({ page }) => {
    test.setTimeout(120_000);

    const asunto = 'T-012-' + Date.now();

    // 1. Navigate to the app root
    await page.goto('/');

    // 2. Login with admin/admin credentials
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // 3. Navigate to "Todos los correos" list view
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');

    // 4. Click "Nuevo correo" button
    await page.getByTestId('item:new').getByTestId('button').click();

    // 5. Fill DNI field with '24362574P' and press Tab (email se autocompleta solo; no esperar ni verificar)
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('24362574P');
    await page.keyboard.press('Tab');

    // 6. Fill Asunto field with unique subject value
    await page.getByRole('textbox', { name: 'Asunto' }).fill(asunto);

    // 7. Click on the body editor and type 'Cuerpo de prueba T-012'
    //    (el editor es un contenteditable: .fill() NO sincroniza el modelo; hay que teclear)
    await page.locator('.custom-html-editor-content').click();
    await page.keyboard.type('Cuerpo de prueba T-012');

    // 8. Click Guardar button to save the correo
    await page.getByTestId('widget:btnSave').getByTestId('button').click();

    // 9. Fetch the correo ID via REST API search (page.request comparte las cookies de sesión).
    //    Se envuelve en expect.poll porque el guardado puede tardar un instante en persistir.
    let rec: any;
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
          rec = (await resp.json())?.data?.[0];
          return rec?.id ?? null;
        },
        { timeout: 20_000, intervals: [1000, 2000, 3000] },
      )
      .not.toBeNull();

    // 10. Navigate to the correo detail page and reload
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/edit/' + rec.id);
    await page.reload();

    // Wait for the detail form to load
    await page.getByText('Estado').first().waitFor({ state: 'visible' });

    // 11. Verify the Estado combobox does NOT have value 'Fallido'
    await expect(page.getByRole('combobox', { name: 'Estado' })).not.toHaveValue('Fallido');

    // 12. Verify the "Reenviar" button is NOT present (only appears for estado FALLIDO)
    await expect(page.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);

    // 13. Logout
    await page.goto('/logout');
  });
});
