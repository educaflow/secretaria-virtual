// spec: Sistema educativo
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('Sistema educativo', () => {
  test('crear una nueva ley educativa', async ({ page }) => {
    // 1. Hacer login con admin/admin
    await page.goto('/#/login');
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // 2. Abrir el menú "Sistema educativo" → "Leyes Educativas"
    await page.getByTestId('item:sistemaEducativo-menuitem').getByTestId('title').click();
    await page.getByText('Leyes Educativas').click();

    // 3. Pulsar el botón de crear nuevo
    await page.getByTestId('item:new').getByTestId('button').click();

    // 4. Rellenar los campos obligatorios del formulario
    const timestamp = Date.now();
    const codigo = `LOMLOE-${timestamp}`;
    const nombre = `LOMLOE Test ${timestamp}`;
    await page.getByRole('textbox', { name: 'Código' }).fill(codigo);
    await page.getByRole('textbox', { name: 'Nombre' }).fill(nombre);

    // 5. Guardar
    await page.getByTestId('widget:btnSave').getByTestId('button').click();

    // 6. Verificar que la ley aparece en el listado
    await expect(page.getByText(nombre)).toBeVisible();
  });
});
