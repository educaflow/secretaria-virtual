// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts
// NOTE: Este test debe ejecutarse con playwright.sdd.config.ts ya que el fichero
//       canónico vive en .sdd/drafts/.../test_e2e/T-010_envio-automatico-con-fallo.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Envío automático con fallo', async ({ page }) => {
    test.setTimeout(300_000);

    const asunto = 'T010-FALLO-' + Date.now();

    // 1. Navegar a la raíz de la aplicación como primer paso
    await page.goto('/');

    // 2. Login con admin/admin
    await page.goto('/#/login');
    await page.locator('[data-testid="field-username"] [data-testid="input"]').fill('admin');
    await page.locator('[data-testid="field-password"] [data-testid="input"]').fill('admin');
    await page.locator('[data-testid="btn-login"]').click();
    await expect(page).toHaveURL(/#\//);

    // 3. Navegar a la lista de todos los correos
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');
    await expect(page.getByRole('button', { name: 'Nuevo correo' })).toBeVisible();

    // 4. Hacer clic en "Nuevo correo"
    await page.getByRole('button', { name: 'Nuevo correo' }).click();
    await expect(page.getByRole('textbox', { name: 'DNI destinatario' })).toBeVisible();

    // 5. Rellenar DNI destinatario con 99999999R (DNI inexistente, no autocompleta email) y presionar Tab
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('99999999R');
    await page.getByRole('textbox', { name: 'DNI destinatario' }).press('Tab');

    // 6. Rellenar Email destinatario con email malformado a@@a.com y verificar que quedó fijado
    await page.getByRole('textbox', { name: 'Email destinatario' }).click();
    await page.getByRole('textbox', { name: 'Email destinatario' }).fill('a@@a.com');
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('a@@a.com');

    // 7. Rellenar Asunto con valor único
    await page.getByRole('textbox', { name: 'Asunto' }).fill(asunto);

    // 8. Rellenar Cuerpo con texto de prueba
    await page.locator('.custom-html-editor-content').click();
    await page.keyboard.type('Cuerpo de prueba T-010 con email malformado.');

    // 9. Guardar el correo (estado inicial: PENDIENTE)
    await page.locator('[data-testid="tab-panel:subsysCorreos.Correo@Todos-action"]').getByRole('button', { name: 'Guardar' }).click();
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/list/);

    // 10. Sondear REST hasta que el estado sea FALLIDO (la tarea periódica procesa el correo cada minuto)
    let rec: any;
    await expect.poll(async () => {
      // Obtener el CSRF token de las cookies del contexto del navegador
      const cookies = await page.context().cookies();
      const csrfCookie = cookies.find(c => c.name === 'CSRF-TOKEN');
      const csrfToken = csrfCookie?.value ?? '';

      const resp = await page.request.post('/ws/rest/com.educaflow.subsystem.correos.db.Correo/search', {
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-Token': csrfToken,
        },
        data: {
          fields: ['estado', 'numeroIntentos', 'motivoUltimoFallo', 'fechaEnvio', 'fechaUltimoIntento'],
          data: {
            criteria: [{ fieldName: 'asunto', operator: '=', value: asunto }],
            operator: 'and',
          },
        },
      });
      rec = (await resp.json())?.data?.[0];
      return rec?.estado ?? '';
    }, { timeout: 180_000, intervals: [5000, 10000, 15000] }).toBe('FALLIDO');

    // 11. Verificar los campos del correo FALLIDO: numeroIntentos>0, motivoUltimoFallo, fechaUltimoIntento, fechaEnvio vacío
    expect(rec.numeroIntentos).toBeGreaterThan(0);
    expect(rec.motivoUltimoFallo, 'motivoUltimoFallo no vacío').toBeTruthy();
    expect(rec.fechaUltimoIntento).toBeTruthy();
    expect(rec.fechaEnvio == null || rec.fechaEnvio === '').toBeTruthy();

    // 12. Navegar al detalle del correo por ID y verificar Estado = Fallido en el combobox
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/edit/' + rec.id);
    await page.reload();
    await expect(page.getByRole('combobox', { name: 'Estado' })).toBeVisible();
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Fallido');

    // 13. Guardar intentos actuales y esperar otro ciclo del cron (75 s) para verificar NO-REINTENTO
    const intentosTras1 = rec.numeroIntentos;
    await new Promise(resolve => setTimeout(resolve, 75_000));

    // 14. Volver a consultar REST: estado sigue FALLIDO y numeroIntentos no cambió
    const cookies2 = await page.context().cookies();
    const csrfCookie2 = cookies2.find(c => c.name === 'CSRF-TOKEN');
    const csrfToken2 = csrfCookie2?.value ?? '';

    const resp2 = await page.request.post('/ws/rest/com.educaflow.subsystem.correos.db.Correo/search', {
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-Token': csrfToken2,
      },
      data: {
        fields: ['estado', 'numeroIntentos'],
        data: {
          criteria: [{ fieldName: 'asunto', operator: '=', value: asunto }],
          operator: 'and',
        },
      },
    });
    const rec2 = (await resp2.json())?.data?.[0];
    expect(rec2.estado).toBe('FALLIDO');
    expect(rec2.numeroIntentos).toBe(intentosTras1);

    // 15. Cerrar sesión
    await page.goto('/logout');
    await expect(page).toHaveURL(/#\/login/);
  });
});
