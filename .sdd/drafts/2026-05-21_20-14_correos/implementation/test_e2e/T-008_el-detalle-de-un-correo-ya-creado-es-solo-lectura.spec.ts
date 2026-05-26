// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('El detalle de un correo ya creado es solo lectura', async ({ page }) => {
    const asunto = 'Asunto T-008 ' + Date.now();

    // 1. Login admin/admin
    await page.goto('/#/login');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // 2. Navegar a "Todos los correos"
    await page.goto('/#/ds/subsysCorreos.Correo%40Todos-action/list/1');

    // 3. Crear nuevo correo
    await page.getByTestId('item:new').getByTestId('button').click();

    // 4. DNI destinatario + Tab → email autocompletado
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('24362574P');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('lorenzo.profesor@gmail.com');

    // 5. Asunto único
    await page.getByRole('textbox', { name: 'Asunto' }).fill(asunto);

    // 6. Cuerpo del mensaje en el editor HTML
    await page.locator('.custom-html-editor-content').click();
    await page.locator('.custom-html-editor-content').fill('Cuerpo de prueba para T-008');

    // 7. Guardar → vuelve automáticamente a la lista
    await page.getByTestId('widget:btnSave').getByTestId('button').click();

    // 8. Abrir detalle: doble click en la fila del correo recién creado
    await page.getByRole('row', { name: new RegExp(asunto) }).dblclick();

    // 9a. Verificar campos deshabilitados en el detalle
    await expect(page.getByRole('textbox', { name: 'DNI destinatario' })).toBeDisabled();
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toBeDisabled();
    await expect(page.getByRole('textbox', { name: 'Asunto' })).toBeDisabled();

    // 9b. Sin botones Guardar ni Cancelar
    await expect(page.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);

    // 9c. Botón Cerrar presente
    await expect(page.getByTestId('widget:btnCerrar').getByTestId('button')).toBeVisible();

    // 9d. Sin botón "Añadir adjunto"
    await expect(page.getByRole('button', { name: 'Añadir adjunto' })).toHaveCount(0);

    // 10. Cerrar y logout
    await page.getByTestId('widget:btnCerrar').getByTestId('button').click();
    await page.goto('/logout');
  });
});
