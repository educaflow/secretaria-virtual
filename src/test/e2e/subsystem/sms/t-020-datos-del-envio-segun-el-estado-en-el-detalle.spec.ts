import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-020 — Datos del envío según el estado, en el detalle
// origen: ESC-020  |  verifica: R-Sms-004, R-Sms-005, R-Sms-006, U-sms-todos-003, U-sms-todos-004
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-020-datos-del-envio-segun-el-estado-en-el-detalle.desc.md
test.describe('SMS — Todos', () => {
  test('Datos del envío según el estado, en el detalle', async ({ page }) => {
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por
    // ejecución para identificar SIEMPRE la fila que crea este run, aunque haya SMS
    // «Aviso detalle» de ejecuciones anteriores.
    const mensaje = `Aviso detalle t020-${Date.now()}`;
    const fecha = /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata",
    // escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono
    // «600111222» y el mensaje «Aviso detalle», y pulsa "Guardar".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata', exact: true }).click();
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El widget de teléfono internacional arranca con "+34": se vacía, se teclea el prefijo y
    // después el número local, tal cual lo escribe el usuario.
    const telefono = page.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.press('ControlOrMeta+a');
    await telefono.press('Backspace');
    await telefono.pressSequentially('+34');
    await telefono.pressSequentially('600111222');
    await expect(telefono).toHaveValue('+34 600 111 222');
    await page.getByLabel('Mensaje').fill(mensaje);
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page).toHaveURL(/\/list\//);
    await expect(page.getByRole('row', { name: mensaje })).toBeVisible();

    // Paso 3: Y espera unos segundos, recarga el listado y pulsa sobre el SMS «Aviso detalle».
    // El envío es asíncrono: se recarga en bucle hasta que el SMS sale de "Pendiente".
    await expect(async () => {
      await page.reload();
      await expect(page.getByRole('row', { name: mensaje }).getByRole('gridcell', { name: /^(Enviado|Fallido)$/ }))
        .toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 60_000, intervals: [2_000] });
    await page.getByRole('row', { name: mensaje }).click();
    await page.waitForURL(/\/edit\/\d+/);

    // Resultado esperado: el panel "Datos del envío" muestra 1 reintento y las fechas de creación,
    // del primer intento de envío y del último intento de envío.
    const datosEnvio = page.getByRole('region', { name: 'Datos del envío' });
    await expect(datosEnvio).toBeVisible();
    await expect(datosEnvio.getByLabel('Número de reintentos')).toHaveValue('1');
    await expect(datosEnvio.getByLabel('Fecha de creación', { exact: true })).toHaveValue(fecha);
    await expect(datosEnvio.getByLabel('Fecha del primer intento de envío', { exact: true })).toHaveValue(fecha);
    await expect(datosEnvio.getByLabel('Fecha del último intento de envío', { exact: true })).toHaveValue(fecha);

    // El campo "Estado" es un grupo de radios (Pendiente / Enviado / Fallido): debe estar marcado
    // exactamente uno, y ha de ser «Enviado» o «Fallido».
    await expect(datosEnvio.getByRole('radio', { name: /^(Enviado|Fallido)$/, checked: true })).toHaveCount(1);
    if (await datosEnvio.getByRole('radio', { name: 'Enviado' }).isChecked()) {
      // Resultado esperado: si está "Enviado", muestra la fecha de envío y no la descripción del último fallo.
      await expect(datosEnvio.getByLabel('Fecha de envío', { exact: true })).toHaveValue(fecha);
      await expect(datosEnvio.getByText(/Descripción del último fallo/)).toHaveCount(0);
    } else {
      // Resultado esperado: si está "Fallido", muestra la descripción del último fallo y no la fecha de envío.
      // La descripción es un texto (no un input): label seguido de contenido no vacío.
      await expect(datosEnvio).toContainText(/Descripción del último fallo\??\s*\S+/);
      await expect(datosEnvio.getByText('Fecha de envío', { exact: true })).toHaveCount(0);
    }

    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (es un dato
    // inmutable de auditoría, ver T-013). Se confía en el mensaje único para no colisionar
    // entre ejecuciones (excepción documentada §4.5 de generation.md).
    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
