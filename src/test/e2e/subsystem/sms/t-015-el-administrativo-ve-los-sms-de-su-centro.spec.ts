import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-015 — El administrativo ve los SMS de su centro
// origen: ESC-015  |  verifica: —
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-015-el-administrativo-ve-los-sms-de-su-centro.desc.md

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
  test('El administrativo ve los SMS de su centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): los mensajes llevan un sufijo único por
    // ejecución para identificar SIEMPRE las filas que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const run = Date.now();
    const avisoMislata = `Aviso Mislata t015-${run}`;
    const avisoBatoi = `Aviso Batoi t015-${run}`;

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

    // Paso 5: Y el administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234»
    // y abre el menú "SMS" → "Del centro".
    await login(page, 'administrativo1@mislata.es', 'demo1234');
    await page.getByText('SMS', { exact: true }).click();
    await page.getByText('Del centro', { exact: true }).click();
    await expect(page).toHaveURL(/\/list\//);

    // Resultado esperado: el listado muestra el SMS «Aviso Mislata».
    const filaMislata = page.getByRole('row', { name: avisoMislata });
    await expect(filaMislata).toBeVisible();
    await expect(filaMislata.getByTestId('column:centro')).toHaveText('CIPFP Mislata');

    // Resultado esperado: el listado no muestra el SMS «Aviso Batoi».
    await expect(page.getByRole('row', { name: avisoBatoi })).toHaveCount(0);
    await expect(page.getByText(avisoBatoi)).toHaveCount(0);

    await logout(page);
  });
});
