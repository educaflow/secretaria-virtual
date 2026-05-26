// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Alta manual sin DNI rechazada', async ({ page }) => {
    // 1. Login admin/admin
    await page.goto('/#/login');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // 2. Navigate to list: Todos los correos
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');
    await page.getByText('Nuevo correo').first().waitFor({ state: 'visible' });

    // 3. Click "Nuevo correo" button → modo alta
    await page.getByRole('button', { name: 'Nuevo correo' }).click();
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/edit/);

    // 4. DNI destinatario: DEJAR VACÍO (no se rellena)

    // 5. Fill Email destinatario (no se autocompleta sin DNI)
    await page.getByRole('textbox', { name: 'Email destinatario' }).fill('test@ejemplo.com');

    // 6. Fill Asunto
    await page.getByRole('textbox', { name: 'Asunto' }).fill('Asunto de prueba T-003');

    // 7. Fill Cuerpo (contenteditable)
    await page.locator('.custom-html-editor-content').click();
    await page.locator('.custom-html-editor-content').fill('Cuerpo de prueba del correo T-003');

    // 8. Click Guardar
    await page.getByTestId('tab-panel:subsysCorreos.Correo@Todos-action').getByRole('button', { name: 'Guardar' }).click();

    // 9a. VERIFICACIÓN: aparece alerta de error visible con texto que menciona "DNI"
    const alert = page.getByRole('alert');
    await expect(alert).toBeVisible();
    await expect(alert).toContainText('DNI');

    // 9b. VERIFICACIÓN: el formulario sigue en modo alta (URL termina en /edit)
    await expect(page).toHaveURL(/#\/ds\/subsysCorreos\.Correo%40Todos-action\/edit$/);

    // 9c. VERIFICACIÓN: el correo NO se creó — navegar a lista y comprobar que el asunto no aparece
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');
    await page.getByText('Nuevo correo').first().waitFor({ state: 'visible' });
    await expect(page.getByText('Asunto de prueba T-003')).not.toBeVisible();

    // 10. Logout
    await page.goto('/logout');
  });
});
