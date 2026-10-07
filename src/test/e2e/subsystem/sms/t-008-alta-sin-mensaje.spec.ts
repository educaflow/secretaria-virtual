import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-008 — Alta sin mensaje
// origen: ESC-008  |  verifica: V-Sms-007
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-008-alta-sin-mensaje.desc.md

// Cuenta los SMS guardados con el mensaje vacío (null o cadena vacía) usando la API REST
// de Axelor con la sesión del navegador. Como el mensaje va vacío, no se le puede poner un
// sufijo único por ejecución: la comprobación "no se guarda" compara este recuento antes y
// después del intento de alta, así no depende de datos de otros tests o runs anteriores.
async function contarSmsSinMensaje(page: Page): Promise<number> {
  const csrf = (await page.context().cookies()).find((c) => c.name === 'CSRF-TOKEN')?.value ?? '';
  const respuesta = await page.request.post('/ws/rest/com.educaflow.subsystem.sms.db.Sms/search', {
    headers: { 'X-CSRF-Token': csrf },
    data: {
      fields: ['id'],
      limit: 1,
      data: {
        operator: 'or',
        criteria: [
          { fieldName: 'mensaje', operator: 'isNull' },
          { fieldName: 'mensaje', operator: '=', value: '' },
        ],
      },
    },
  });
  expect(respuesta.ok()).toBeTruthy();
  const cuerpo = await respuesta.json();
  expect(cuerpo.status).toBe(0);
  return cuerpo.total ?? 0;
}

test.describe('SMS — Todos', () => {
  test('Alta sin mensaje', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): no hay teardown porque el test no debe crear
    // nada (si lo crease, la aserción del recuento falla). Se toma la línea base antes del alta.
    const sinMensajeAntes = await contarSmsSinMensaje(page);

    // Paso 2: Cuando abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();

    // Paso 3: Y elige el centro "CIPFP Mislata", escribe el DNI del destinatario «95591733F»,
    // el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y deja vacío
    // el mensaje.
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata', exact: true }).click();
    await page.getByLabel('DNI del destinatario').fill('95591733F');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    await page.getByPlaceholder('+34 123 456 789').fill('600111222');
    // El mensaje se deja vacío: no se toca.
    await expect(page.getByLabel('Mensaje')).toHaveValue('');

    // Paso 4: Y pulsa "Guardar".
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra «El mensaje es obligatorio».
    await expect(page.getByText('El mensaje es obligatorio')).toBeVisible();
    // Sigue en el formulario de alta: no ha vuelto al listado.
    await expect(page).toHaveURL(/\/edit/);

    // Resultado esperado: no se guarda el SMS.
    // Se vuelve al listado con el botón "Cancelar" del propio formulario de alta.
    await page.getByRole('button', { name: 'Cancelar' }).click();
    // Axelor pregunta si se descartan los cambios sin guardar: se acepta.
    const dialogo = page.getByRole('dialog').filter({ hasText: 'Los cambios actuales se perderán' });
    await dialogo.getByRole('button', { name: 'Aceptar' }).click();
    await expect(page).toHaveURL(/\/list\//);
    await expect(page.getByRole('button', { name: 'Nuevo SMS' })).toBeVisible();
    // Ningún SMS nuevo sin mensaje en la BD.
    expect(await contarSmsSinMensaje(page)).toBe(sinMensajeAntes);

    await logout(page);
  });
});
