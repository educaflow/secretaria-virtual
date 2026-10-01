import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-016 — Reenvío desde «Del centro» por el supervisor
// origen: ESC-016  |  verifica: V-Sms-014, V-Sms-015, R-Sms-003, R-Sms-004, U-sms-centro-003
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-016-reenvio-desde-del-centro-por-el-supervisor.desc.md

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
  test('Reenvío desde «Del centro» por el supervisor', async ({ page }) => {
    test.setTimeout(180_000);
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por ejecución
    // para identificar SIEMPRE la fila que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const avisoMislata = `Aviso Mislata t016-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI
    // «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el
    // mensaje «Aviso Mislata», y comprueba en el listado que aparece con el teléfono «+34600111222».
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await altaSms(page, {
      centro: 'CIPFP Mislata', dni: '86862719E', nombre: 'Alumno1', apellidos: 'CIPFP Mislata',
      telefono: '600111222', mensaje: avisoMislata,
    });
    await expect(
      page.getByRole('row', { name: avisoMislata }).getByRole('gridcell', { name: '+34600111222', exact: true }),
    ).toBeVisible();

    // Paso 3: Y cierra sesión.
    await logout(page);

    // Paso 4: Y el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234», espera
    // unos segundos y abre el menú "SMS" → "Del centro".
    await login(page, 'supervisor1@mislata.es', 'demo1234');
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
    const estado = datosEnvio.getByRole('combobox', { name: 'Estado' });
    await expect(estado).toHaveValue(/^(Enviado|Fallido)$/);
    const reenviar = page.getByRole('button', { name: 'Reenviar' });

    if ((await estado.inputValue()) === 'Fallido') {
      // Resultado esperado (Fallido): el sistema muestra el botón "Reenviar"; al pulsarlo muestra
      // «El reenvío del SMS se ha puesto en marcha.».
      await expect(reenviar).toBeVisible();
      await reenviar.click();
      await expect(
        page.getByRole('status').filter({ hasText: 'El reenvío del SMS se ha puesto en marcha.' }),
      ).toBeVisible();

      // Resultado esperado (Fallido): pasados unos segundos, al recargar, el SMS muestra 2 reintentos
      // y está en "Enviado" (con fecha de envío) o de nuevo en "Fallido" (con la descripción del nuevo fallo).
      await expect(async () => {
        await page.reload();
        const env = page.getByRole('region', { name: 'Datos del envío' });
        await expect(env.getByLabel('Número de reintentos')).toHaveValue('2', { timeout: 3_000 });
        await expect(env.getByRole('combobox', { name: 'Estado' })).toHaveValue(/^(Enviado|Fallido)$/, { timeout: 3_000 });
      }).toPass({ timeout: 60_000, intervals: [2_000] });

      const envFinal = page.getByRole('region', { name: 'Datos del envío' });
      if ((await envFinal.getByRole('combobox', { name: 'Estado' }).inputValue()) === 'Enviado') {
        await expect(envFinal.getByLabel('Fecha de envío', { exact: true })).not.toHaveValue('');
      } else {
        await expect(envFinal.getByRole('combobox', { name: 'Estado' })).toHaveValue('Fallido');
        // La descripción del fallo es un texto (no un input): label seguido de contenido no vacío.
        await expect(envFinal).toContainText(/Descripción del último fallo\??\s*\S+/);
      }
    } else {
      // Resultado esperado (Enviado): el sistema no muestra el botón "Reenviar".
      await expect(estado).toHaveValue('Enviado');
      await expect(reenviar).toHaveCount(0);
    }

    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
