import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-023 — Reenvío por el administrativo
// origen: ESC-023  |  verifica: V-Sms-014, V-Sms-015, R-Sms-003, R-Sms-004, U-sms-centro-003
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-023-reenvio-por-el-administrativo.desc.md

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
  test('Reenvío por el administrativo', async ({ page }) => {
    test.setTimeout(180_000);
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por ejecución
    // para identificar SIEMPRE la fila que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const avisoMislata = `Aviso Mislata t023-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI
    // «95591733F», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el
    // mensaje «Aviso Mislata», y comprueba que en el listado aparece con el teléfono «+34600111222».
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await altaSms(page, {
      centro: 'CIPFP Mislata', dni: '95591733F', nombre: 'Alumno1', apellidos: 'CIPFP Mislata',
      telefono: '600111222', mensaje: avisoMislata,
    });
    await expect(
      page.getByRole('row', { name: avisoMislata }).getByRole('gridcell', { name: '+34600111222', exact: true }),
    ).toBeVisible();

    // Paso 3: Y cierra sesión.
    await logout(page);

    // Paso 4: Y el administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234»,
    // espera unos segundos y abre el menú "SMS" → "Del centro".
    await login(page, 'administrativo1@mislata.es', 'demo1234');
    await page.getByText('SMS', { exact: true }).click();
    await page.getByText('Del centro', { exact: true }).click();
    await expect(page).toHaveURL(/\/list\//);
    // "Espera unos segundos": el envío es asíncrono; se recarga hasta que el SMS sale de "Pendiente".
    await expect(async () => {
      await page.reload();
      await expect(
        page.getByRole('row', { name: avisoMislata }).getByRole('gridcell', { name: /^(Enviado|Fallido)$/ }),
      ).toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 60_000, intervals: [2_000] });

    // Paso 5: Y pulsa sobre el SMS «Aviso Mislata».
    await page.getByRole('row', { name: avisoMislata }).click();
    await page.waitForURL(/\/edit\/\d+/);

    const datosEnvio = page.getByRole('region', { name: 'Datos del envío' });
    // "Estado" es un grupo de radios (Pendiente / Enviado / Fallido): se espera a que el marcado
    // sea "Enviado" o "Fallido" y se elige la rama según el radio "Fallido".
    await expect(
      datosEnvio.getByRole('radio', { name: /^(Enviado|Fallido)$/, checked: true }),
    ).toHaveCount(1);
    const reenviar = page.getByRole('button', { name: 'Reenviar' });

    if (await datosEnvio.getByRole('radio', { name: 'Fallido' }).isChecked()) {
      // Resultado esperado (Fallido): el sistema muestra el botón "Reenviar"; al pulsarlo muestra
      // «El reenvío del SMS se ha puesto en marcha.».
      await expect(reenviar).toBeVisible();
      await reenviar.click();
      await expect(
        page.getByRole('status').filter({ hasText: 'El reenvío del SMS se ha puesto en marcha.' }),
      ).toBeVisible();

      // Resultado esperado (Fallido): pasados unos segundos, al recargar, el SMS muestra 2 reintentos.
      await expect(async () => {
        await page.reload();
        await expect(
          page.getByRole('region', { name: 'Datos del envío' }).getByLabel('Número de reintentos'),
        ).toHaveValue('2', { timeout: 3_000 });
      }).toPass({ timeout: 60_000, intervals: [2_000] });
    } else {
      // Resultado esperado (Enviado): el sistema no muestra el botón "Reenviar".
      await expect(datosEnvio.getByRole('radio', { name: 'Enviado' })).toBeChecked();
      await expect(reenviar).toHaveCount(0);
    }

    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
