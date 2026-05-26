// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts
// TARGET: .sdd/drafts/2026-05-21_20-14_correos/implementation/test_e2e/T-011_reenvio-de-un-correo-fallido.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Reenvío de un Correo FALLIDO', async ({ page }) => {
    test.setTimeout(300_000);

    const asunto = 'T011-Reenvio-' + Date.now();

    // 1. Navegar a la raíz de la aplicación como primer paso
    await page.goto('/');

    // 2. Login con admin/admin
    await page.goto('/#/login');
    await page.locator('[data-testid="field-username"] [data-testid="input"]').fill('admin');
    await page.locator('[data-testid="field-password"] [data-testid="input"]').fill('admin');
    await page.locator('[data-testid="btn-login"]').click();
    await expect(page).toHaveURL(/#\//);

    // 3. Navegar a la lista "Todos los correos"
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');
    await expect(page.getByRole('button', { name: 'Nuevo correo' })).toBeVisible();

    // 4. Hacer clic en "Nuevo correo"
    await page.getByRole('button', { name: 'Nuevo correo' }).click();
    await expect(page.getByRole('textbox', { name: 'DNI destinatario' })).toBeVisible();

    // 5. Rellenar DNI destinatario con 99999999R (DNI inexistente, no autocompleta email) y presionar Tab
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('99999999R');
    await page.getByRole('textbox', { name: 'DNI destinatario' }).press('Tab');

    // 6. Rellenar Email destinatario con email malformado a@@a.com DESPUÉS del Tab (el Tab repinta el panel y borra el email si se pone antes)
    await page.getByRole('textbox', { name: 'Email destinatario' }).click();
    await page.getByRole('textbox', { name: 'Email destinatario' }).fill('a@@a.com');
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('a@@a.com');

    // 7. Rellenar Asunto con valor único basado en timestamp
    await page.getByRole('textbox', { name: 'Asunto' }).fill(asunto);

    // 8. Rellenar Cuerpo con texto de prueba en el editor rich-text
    await page.locator('.custom-html-editor-content').click();
    await page.keyboard.type('Cuerpo de prueba T-011');

    // 9. Guardar el correo (estado inicial: PENDIENTE)
    await page.locator('[data-testid="tab-panel:subsysCorreos.Correo@Todos-action"]').getByRole('button', { name: 'Guardar' }).click();
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/list/);

    // 10. Sondear REST hasta que el estado sea FALLIDO (la tarea periódica cron procesa el correo cada minuto; email malformado → FALLIDO)
    let rec: any;
    await expect.poll(async () => {
      const cookies = await page.context().cookies();
      const csrfCookie = cookies.find(c => c.name === 'CSRF-TOKEN');
      const csrfToken = csrfCookie?.value ?? '';

      const resp = await page.request.post('/ws/rest/com.educaflow.subsystem.correos.db.Correo/search', {
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-Token': csrfToken,
        },
        data: {
          fields: ['estado', 'numeroIntentos', 'motivoUltimoFallo'],
          data: {
            criteria: [{ fieldName: 'asunto', operator: '=', value: asunto }],
            operator: 'and',
          },
        },
      });
      rec = (await resp.json())?.data?.[0];
      return rec?.estado ?? '';
    }, { timeout: 180_000, intervals: [5000, 10000, 15000] }).toBe('FALLIDO');

    // 11. Verificar que motivoUltimoFallo no está vacío
    expect(rec.motivoUltimoFallo, 'motivoUltimoFallo no vacío').toBeTruthy();

    // 12. Abrir el correo FALLIDO en la UI por ID y recargar para obtener estado fresco
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/edit/' + rec.id);
    await page.reload();

    // 13. Verificar que el combobox Estado muestra "Fallido"
    await expect(page.getByRole('combobox', { name: 'Estado' })).toBeVisible();
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Fallido');

    // 14. Pulsar "Reenviar" (visible solo para Administrador con estado FALLIDO; sin diálogo de confirmación)
    await page.getByTestId('widget:btnReenviar').getByTestId('button').click();

    // 15. Verificar que el estado cambia a 'Pendiente' inmediatamente (sin esperar al cron)
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Pendiente');

    // 16. Cerrar sesión
    await page.goto('/logout');
    await expect(page).toHaveURL(/#\/login/);
  });
});
