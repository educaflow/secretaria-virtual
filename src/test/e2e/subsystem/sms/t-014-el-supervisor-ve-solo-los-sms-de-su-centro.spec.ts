import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-014 — El supervisor ve solo los SMS de su centro
// origen: ESC-014  |  verifica: U-sms-centro-001, U-sms-centro-002
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-014-el-supervisor-ve-solo-los-sms-de-su-centro.desc.md

// Da de alta un SMS desde el formulario "Nuevo SMS" del listado "Todos" y vuelve al listado.
async function altaSms(
  page: Page,
  datos: { centro: string; dni: string; nombre: string; apellidos: string; telefono: string; mensaje: string },
): Promise<void> {
  await page.getByRole('button', { name: 'Nuevo SMS' }).click();
  // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
  await page.getByRole('combobox', { name: /^Centro\b/ }).click();
  await page.getByRole('option', { name: datos.centro }).click();
  await page.getByLabel('DNI del destinatario').fill(datos.dni);
  await page.getByLabel('Nombre', { exact: true }).fill(datos.nombre);
  await page.getByLabel('Apellidos').fill(datos.apellidos);
  // El widget de teléfono internacional arranca con el prefijo "+34": se deja solo el
  // prefijo y se teclea el número detrás (un fill() lo reinterpretaría como otro país).
  const telefono = page.getByPlaceholder('+34 123 456 789');
  await telefono.click();
  await telefono.press('ControlOrMeta+a');
  await telefono.press('Backspace');
  await telefono.pressSequentially('+34');
  await telefono.pressSequentially(datos.telefono);
  await page.getByLabel('Mensaje').fill(datos.mensaje);
  await page.getByRole('button', { name: 'Guardar' }).click();
  await expect(page).toHaveURL(/\/list\//);
  await expect(page.getByRole('row', { name: datos.mensaje })).toBeVisible();
}

test.describe('SMS — Del centro', () => {
  test('El supervisor ve solo los SMS de su centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): los mensajes llevan un sufijo único por
    // ejecución para identificar SIEMPRE las filas que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const run = Date.now();
    const avisoMislata = `Aviso Mislata t014-${run}`;
    const avisoBatoi = `Aviso Batoi t014-${run}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI
    // «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el
    // mensaje «Aviso Mislata».
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await altaSms(page, {
      centro: 'CIPFP Mislata', dni: '86862719E', nombre: 'Alumno1', apellidos: 'CIPFP Mislata',
      telefono: '600111222', mensaje: avisoMislata,
    });

    // Paso 3: Y da de alta otro SMS del centro "CIPFP Batoi" con el DNI «65399546N», el nombre «Alumno1»,
    // los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
    await altaSms(page, {
      centro: 'CIPFP Batoi', dni: '65399546N', nombre: 'Alumno1', apellidos: 'CIPFP Batoi',
      telefono: '600333444', mensaje: avisoBatoi,
    });

    // Paso 4: Y cierra sesión.
    await logout(page);

    // Paso 5: Y el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre
    // el menú "SMS" → "Del centro".
    await login(page, 'supervisor1@mislata.es', 'demo1234');
    await page.getByText('SMS', { exact: true }).click();
    await page.getByText('Del centro', { exact: true }).click();
    await expect(page).toHaveURL(/\/list\//);

    // Resultado esperado: el listado muestra el SMS «Aviso Mislata» y no muestra el SMS «Aviso Batoi».
    const filaMislata = page.getByRole('row', { name: avisoMislata });
    await expect(filaMislata).toBeVisible();
    await expect(filaMislata.getByTestId('column:centro')).toHaveText('CIPFP Mislata');
    await expect(page.getByRole('row', { name: avisoBatoi })).toHaveCount(0);
    await expect(page.getByText(avisoBatoi)).toHaveCount(0);

    // Paso 6: Y pulsa sobre el SMS «Aviso Mislata».
    await filaMislata.click();
    await page.waitForURL(/\/edit\/\d+/);

    // Resultado esperado: el detalle se muestra en solo lectura, con el panel "Datos del SMS"
    // (centro "CIPFP Mislata", DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata»,
    // teléfono «+34600111222» y mensaje «Aviso Mislata»).
    const datosSms = page.getByRole('region', { name: 'Datos del SMS' });
    await expect(datosSms).toBeVisible();
    await expect(datosSms.getByRole('button', { name: 'CIPFP Mislata' })).toBeVisible();
    await expect(datosSms.getByLabel('DNI del destinatario')).toHaveValue('86862719E');
    await expect(datosSms.getByLabel('DNI del destinatario')).toBeDisabled();
    await expect(datosSms.getByLabel('Nombre', { exact: true })).toHaveValue('Alumno1');
    await expect(datosSms.getByLabel('Nombre', { exact: true })).toBeDisabled();
    await expect(datosSms.getByLabel('Apellidos')).toHaveValue('CIPFP Mislata');
    await expect(datosSms.getByLabel('Apellidos')).toBeDisabled();
    // El teléfono en solo lectura se pinta como enlace tel: con el valor E.164 guardado.
    const enlaceTelefono = datosSms.getByRole('link', { name: '+34 600 111 222' });
    await expect(enlaceTelefono).toBeVisible();
    await expect(enlaceTelefono).toHaveAttribute('href', 'tel:+34600111222');
    await expect(datosSms.getByText(avisoMislata, { exact: true })).toBeVisible();
    // Solo lectura: no queda ningún campo editable en el panel.
    await expect(datosSms.getByRole('textbox', { disabled: false })).toHaveCount(0);
    await expect(datosSms.getByRole('combobox', { disabled: false })).toHaveCount(0);
    await expect(datosSms.locator('input:not([disabled]):not([type="hidden"]), textarea:not([disabled])')).toHaveCount(0);

    // Resultado esperado: … y el panel "Datos del envío" con el estado y el número de reintentos.
    const datosEnvio = page.getByRole('region', { name: 'Datos del envío' });
    await expect(datosEnvio).toBeVisible();
    // El envío es asíncrono: el estado puede ser cualquiera de los tres según el momento.
    // "Estado" es un grupo de radios (RadioSelect): hay exactamente un radio marcado.
    await expect(datosEnvio.getByRole('radio', { checked: true })).toHaveCount(1);
    await expect(datosEnvio.getByRole('radio', { checked: true })).toHaveAccessibleName(/^(Pendiente|Enviado|Fallido)$/);
    await expect(datosEnvio.getByLabel('Número de reintentos')).toHaveValue(/^\d+$/);

    // Resultado esperado: muestra el botón "Salir" y no muestra los botones "Guardar" ni "Borrar".
    await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Borrar' })).toHaveCount(0);

    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
