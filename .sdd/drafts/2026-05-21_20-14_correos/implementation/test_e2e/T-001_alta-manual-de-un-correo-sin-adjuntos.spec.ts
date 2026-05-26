// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';

test.describe('correos — debug E2E', () => {
  test('Alta manual de un Correo sin adjuntos', async ({ page }) => {
    // Asunto único para evitar colisiones entre ejecuciones
    const asunto = `Asunto T-001 ${Date.now()}`;

    // Precondición: ir a la página de login
    await page.goto('/#/login');

    // 1. Login como admin
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.locator('[data-testid="btn-login"]').click();

    // 2. Navegar a "Todos los correos" por el menú
    await page.locator('[data-testid="item:correos-menuitem"] [data-testid="title"]').click();
    await page.locator('text=Todos los correos').click();

    // 3. Pulsar "Nuevo correo" del toolbar
    await page.locator('[data-testid="item:new"] [data-testid="button"]').click();

    // 4. En el panel "Destinatario": DNI y Email
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('12345678Z');
    await page.getByRole('textbox', { name: 'Email destinatario' }).fill('destinatario@example.com');

    // 5. En el panel "Mensaje": Asunto único
    await page.getByRole('textbox', { name: 'Asunto' }).fill(asunto);

    // 6. Cuerpo del mensaje en el editor HTML enriquecido (no es textbox accesible)
    await page.locator('.custom-html-editor-content').click();
    await page.keyboard.press('Control+a');
    await page.locator('.custom-html-editor-content').fill('Cuerpo del correo de prueba T-001');

    // El campo email puede perder su valor tras el primer render; re-rellenar y verificar justo antes de Guardar
    await page.getByRole('textbox', { name: 'Email destinatario' }).fill('destinatario@example.com');
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('destinatario@example.com');

    // 7. Pulsar "Guardar"
    await page.locator('[data-testid="widget:btnSave"] [data-testid="button"]').click();

    // 8. Verificar en el grid "Todos los correos" que el correo aparece (JUSTO tras guardar, antes de que el scheduler lo envíe)
    //    Localizar la fila por el asunto único y verificar cada columna por su data-testid estable
    const nuevaFila = page.getByRole('row', { name: new RegExp(asunto) }).first();
    await expect(nuevaFila).toBeVisible();
    await expect(nuevaFila.getByTestId('column:dniDestinatario')).toHaveText('12345678Z');
    await expect(nuevaFila.getByTestId('column:emailDestinatario')).toHaveText('destinatario@example.com');
    await expect(nuevaFila.getByTestId('column:estado')).toHaveText('Pendiente');
    // numeroIntentos=0: usar data-testid de columna para evitar substring-match con otros spans de la fila
    await expect(nuevaFila.getByTestId('column:numeroIntentos')).toHaveText('0');

    // 9. Cerrar sesión
    await page.locator('[data-testid="item:user"] [data-testid="button"]').click();
    await page.locator('[data-testid="item:logout"]').click();
    await expect(page).toHaveURL(/#\/login/);
  });
});
