import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-019 — Cancelar el alta de un SMS
// origen: ESC-019  |  verifica: U-sms-todos-002, U-sms-todos-005
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-019-cancelar-el-alta-de-un-sms.desc.md

// Cuenta los SMS guardados con un mensaje exacto usando la API REST de Axelor con la sesión
// del navegador. Complementa la comprobación del listado: el grid está paginado y una fila
// guardada por error podría quedar fuera de la primera página.
async function contarSmsConMensaje(page: Page, mensaje: string): Promise<number> {
  const csrf = (await page.context().cookies()).find((c) => c.name === 'CSRF-TOKEN')?.value ?? '';
  const respuesta = await page.request.post('/ws/rest/com.educaflow.subsystem.sms.db.Sms/search', {
    headers: { 'X-CSRF-Token': csrf },
    data: {
      fields: ['id'],
      limit: 1,
      data: { criteria: [{ fieldName: 'mensaje', operator: '=', value: mensaje }] },
    },
  });
  expect(respuesta.ok()).toBeTruthy();
  const cuerpo = await respuesta.json();
  expect(cuerpo.status).toBe(0);
  return cuerpo.total ?? 0;
}

test.describe('SMS — Todos', () => {
  test('Cancelar el alta de un SMS', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por ejecución,
    // así un SMS «Alta cancelada» guardado por error en un run anterior no hace fallar este.
    // No hay teardown: el test no debe crear nada (si lo crease, las aserciones fallan).
    const mensaje = `Alta cancelada t019-${Date.now()}`;

    // Paso 2: Cuando abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();

    // Paso 3: Entonces el sistema muestra el formulario con los botones "Guardar" y "Cancelar",
    // sin el botón "Salir" y sin el panel "Datos del envío".
    await expect(page).toHaveURL(/\/edit/);
    await expect(page.getByRole('button', { name: 'Guardar' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cancelar' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Salir' })).toHaveCount(0);
    await expect(page.getByText('Datos del envío')).toHaveCount(0);

    // Paso 4: Cuando elige el centro "CIPFP Mislata", escribe el DNI «95591733F», el nombre
    // «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Alta cancelada».
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata', exact: true }).click();
    await page.getByLabel('DNI del destinatario').fill('95591733F');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    await page.getByPlaceholder('+34 123 456 789').fill('600111222');
    await page.getByLabel('Mensaje').fill(mensaje);

    // Paso 5: Y pulsa "Cancelar".
    await page.getByRole('button', { name: 'Cancelar' }).click();
    // Axelor pregunta si se descartan los cambios sin guardar: se acepta.
    const dialogo = page.getByRole('dialog').filter({ hasText: 'Los cambios actuales se perderán' });
    await dialogo.getByRole('button', { name: 'Aceptar' }).click();

    // Resultado esperado: el sistema vuelve al listado.
    await expect(page).toHaveURL(/\/list\//);
    await expect(page.getByRole('button', { name: 'Nuevo SMS' })).toBeVisible();

    // Resultado esperado: en el listado no aparece ningún SMS con el mensaje «Alta cancelada».
    await expect(page.getByRole('row', { name: mensaje })).toHaveCount(0);
    expect(await contarSmsConMensaje(page, mensaje)).toBe(0);

    await logout(page);
  });
});
