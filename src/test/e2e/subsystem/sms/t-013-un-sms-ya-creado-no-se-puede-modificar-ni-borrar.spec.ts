import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-013 — Un SMS ya creado no se puede modificar ni borrar
// origen: ESC-013  |  verifica: V-Sms-012, V-Sms-013, U-sms-todos-001, U-sms-todos-002, U-sms-todos-005
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-013-un-sms-ya-creado-no-se-puede-modificar-ni-borrar.desc.md
test.describe('SMS — Todos', () => {
  test('Un SMS ya creado no se puede modificar ni borrar', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por ejecución
    // para identificar SIEMPRE la fila que crea este run.
    // Teardown: precisamente lo que verifica este test es que un SMS creado no se puede
    // modificar ni borrar desde la UI (dato inmutable de auditoría), así que no hay borrado;
    // se confía en el mensaje único para no colisionar (excepción documentada §4.5).
    const mensaje = `Mañana no hay clase t013-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    // (hecho arriba con login)

    // Paso 2: Cuando abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata",
    // escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono
    // «600111222» y el mensaje «Mañana no hay clase», y pulsa "Guardar".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El widget de teléfono internacional arranca con el prefijo "+34": se deja solo el
    // prefijo y se teclea el número detrás (un fill() lo reinterpretaría como otro país).
    const telefono = page.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.press('ControlOrMeta+a');
    await telefono.press('Backspace');
    await telefono.pressSequentially('+34');
    await telefono.pressSequentially('600111222');
    await page.getByLabel('Mensaje').fill(mensaje);
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page).toHaveURL(/\/list\//);
    await expect(page.getByRole('row', { name: mensaje })).toBeVisible();

    // Paso 3: Y espera unos segundos y recarga el listado.
    // El envío es asíncrono: se recarga en bucle hasta que el SMS sale de "Pendiente".
    await expect(async () => {
      await page.reload();
      await expect(page.getByRole('row', { name: mensaje }).getByRole('gridcell', { name: /^(Enviado|Fallido)$/ }))
        .toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 60_000, intervals: [2_000] });

    // Paso 4: Y pulsa sobre el SMS «Mañana no hay clase».
    await page.getByRole('row', { name: mensaje }).click();
    await page.waitForURL(/\/edit\/\d+/);

    // Resultado esperado: el sistema muestra el SMS con todos sus datos en solo lectura
    // (no se puede escribir en ningún campo del panel "Datos del SMS").
    const datosSms = page.getByRole('region', { name: 'Datos del SMS' });
    await expect(datosSms).toBeVisible();
    await expect(datosSms.getByLabel('DNI del destinatario')).toHaveValue('86862719E');
    await expect(datosSms.getByLabel('DNI del destinatario')).toBeDisabled();
    await expect(datosSms.getByLabel('Nombre', { exact: true })).toHaveValue('Alumno1');
    await expect(datosSms.getByLabel('Nombre', { exact: true })).toBeDisabled();
    await expect(datosSms.getByLabel('Apellidos')).toHaveValue('CIPFP Mislata');
    await expect(datosSms.getByLabel('Apellidos')).toBeDisabled();
    // Centro, teléfono y mensaje se muestran como valor (enlace/texto), no como campo editable.
    await expect(datosSms.getByRole('button', { name: 'CIPFP Mislata' })).toBeVisible();
    await expect(datosSms.getByRole('link', { name: '+34 600 111 222' })).toBeVisible();
    await expect(datosSms.getByText(mensaje, { exact: true })).toBeVisible();
    // Ningún campo del panel admite escritura: no queda textbox/combobox/spinbutton habilitado.
    await expect(datosSms.getByRole('textbox', { disabled: false })).toHaveCount(0);
    await expect(datosSms.getByRole('combobox', { disabled: false })).toHaveCount(0);
    await expect(datosSms.getByRole('spinbutton', { disabled: false })).toHaveCount(0);
    await expect(datosSms.locator('input:not([disabled]):not([type="hidden"]), textarea:not([disabled])')).toHaveCount(0);

    // Resultado esperado: muestra el panel "Datos del envío" con el estado, el número de reintentos
    // y las fechas de creación, del primer intento y del último intento.
    const datosEnvio = page.getByRole('region', { name: 'Datos del envío' });
    await expect(datosEnvio).toBeVisible();
    // El campo "Estado" es un grupo de radios de solo lectura: la opción real es el único radio marcado
    // y, como el envío ya se intentó, tiene que ser "Enviado" o "Fallido" (nunca "Pendiente").
    await expect(datosEnvio.getByRole('radio', { checked: true })).toHaveCount(1);
    await expect(datosEnvio.getByRole('radio', { checked: true })).toHaveAccessibleName(/^(Enviado|Fallido)$/);
    await expect(datosEnvio.getByLabel('Número de reintentos')).toHaveValue(/^\d+$/);
    const fecha = /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/;
    await expect(datosEnvio.getByLabel('Fecha de creación')).toHaveValue(fecha);
    await expect(datosEnvio.getByLabel('Fecha del primer intento de envío')).toHaveValue(fecha);
    await expect(datosEnvio.getByLabel('Fecha del último intento de envío')).toHaveValue(fecha);

    // Resultado esperado: no muestra el botón "Guardar" ni el botón "Borrar".
    await expect(page.getByRole('button', { name: 'Reenviar' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Borrar' })).toHaveCount(0);

    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
