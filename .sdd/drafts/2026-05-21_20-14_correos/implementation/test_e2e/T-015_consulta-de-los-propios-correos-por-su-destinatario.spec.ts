// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts
// NOTE: This file is a proxy. The actual test lives at:
//   .sdd/drafts/2026-05-21_20-14_correos/implementation/test_e2e/T-015_consulta-de-los-propios-correos-por-su-destinatario.spec.ts
// It must be run with: npx playwright test --config=playwright.sdd.config.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Consulta de los propios correos por su destinatario', async ({ page }) => {
    test.setTimeout(120_000);

    const prefijo = 'T015-' + Date.now();
    const asuntoA = prefijo + '-A';
    const asuntoB = prefijo + '-B';

    // 1. Login as admin/admin
    await page.goto('/');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // 2. Navigate to "Todos los correos"
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');

    // 3. Create email A: DNI 24362574P, asuntoA
    await page.getByTestId('item:new').getByTestId('button').click();
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('24362574P');
    // Press Tab to autocomplete email; do not wait for or check the email value
    await page.keyboard.press('Tab');
    await page.getByRole('textbox', { name: 'Asunto' }).fill(asuntoA);
    await page.locator('.custom-html-editor-content').click();
    await page.locator('.custom-html-editor-content').fill('Cuerpo de prueba T-015');
    await page.locator('[data-testid="tab-panel:subsysCorreos.Correo@Todos-action"] [data-testid="button"]:has-text("Guardar")').click();
    await page.waitForURL(/Correo%40Todos-action\/list/);

    // 4. Create email B: DNI 20000001T, asuntoB
    await page.locator('[data-testid="item:new"] [data-testid="button"]').click();
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('20000001T');
    // Press Tab to autocomplete email; do not wait for or check the email value
    await page.keyboard.press('Tab');
    await page.getByRole('textbox', { name: 'Asunto' }).fill(asuntoB);
    await page.locator('.custom-html-editor-content').click();
    await page.locator('.custom-html-editor-content').fill('Cuerpo de prueba T-015');
    await page.locator('[data-testid="tab-panel:subsysCorreos.Correo@Todos-action"] [data-testid="button"]:has-text("Guardar")').click();
    await page.waitForURL(/Correo%40Todos-action\/list/);

    // 5. Logout and re-login as admin/admin
    await page.goto('/logout');
    await page.goto('/');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // 6. Navigate to "Mis correos"
    await page.goto('/#/ds/subsysCorreos.Correo%40Mios-action/list/1');

    // 7. Filter by prefix in the Asunto search box
    const buscar = page.getByRole('textbox', { name: 'Buscar...' }).first();
    await buscar.fill(prefijo);
    await buscar.press('Enter');

    // 8. Verify only correo A appears (exactly 1 matching row)
    await expect(page.getByRole('row', { name: new RegExp(asuntoA) })).toHaveCount(1);

    // 9. Verify correo B does NOT appear (belongs to a different DNI)
    await expect(page.getByRole('row', { name: new RegExp(asuntoB) })).toHaveCount(0);

    // 10. Verify column headers: Asunto, Estado, Fecha de creación, Fecha de envío
    await expect(page.getByTestId('header-row').getByTestId('column:asunto')).toBeVisible();
    await expect(page.getByTestId('header-row').getByTestId('column:estado')).toBeVisible();
    await expect(page.getByTestId('header-row').getByTestId('column:fechaCreacion')).toBeVisible();
    await expect(page.getByTestId('header-row').getByTestId('column:fechaEnvio')).toBeVisible();

    // 11. Verify there is NO "Nuevo correo" button
    await expect(page.getByRole('button', { name: 'Nuevo correo' })).toHaveCount(0);

    // 12. Verify there is NO "Reenviar" button
    await expect(page.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);

    // 13. Logout
    await page.goto('/logout');
  });
});
