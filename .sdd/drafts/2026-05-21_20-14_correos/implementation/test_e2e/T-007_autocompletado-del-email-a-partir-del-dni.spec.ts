// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Autocompletado del email a partir del DNI', async ({ page }) => {
    // 1. Navigate to app and login with admin/admin
    await page.goto('/#/login');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // 2. Navigate to Todos los correos list view
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');

    // 3. Click "Nuevo correo" button
    await page.getByTestId('item:new').getByTestId('button').click();

    // 4. DNI: fill with '24362574P' and press Tab to trigger onChange/autocomplete
    const dniField = page.getByRole('textbox', { name: 'DNI destinatario' });
    const emailField = page.getByRole('textbox', { name: 'Email destinatario' });

    await dniField.fill('24362574P');
    await dniField.press('Tab');

    // 5. Verify email autocompleted to 'lorenzo.profesor@gmail.com'
    await expect(emailField).toHaveValue('lorenzo.profesor@gmail.com');

    // 6. Clear DNI field, fill with non-existing DNI '99999999R' and press Tab
    await dniField.fill('');
    await dniField.fill('99999999R');
    await dniField.press('Tab');

    // 7. Verify email field is empty for non-existing DNI
    await expect(emailField).toHaveValue('');

    // 8. Logout
    await page.goto('/logout');
  });
});
