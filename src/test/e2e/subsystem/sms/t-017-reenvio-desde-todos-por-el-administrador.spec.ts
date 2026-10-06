import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-017 — Reenvío desde «Todos» por el administrador
// origen: ESC-017  |  verifica: V-Sms-014, R-Sms-003, U-sms-todos-006
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-017-reenvio-desde-todos-por-el-administrador.desc.md

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

test.describe('SMS — Todos', () => {
  test('Reenvío desde «Todos» por el administrador', async ({ page }) => {
    test.setTimeout(120_000);
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por ejecución
    // para identificar SIEMPRE la fila que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const avisoBatoi = `Aviso Batoi t017-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Batoi",
    // escribe el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono
    // «600333444» y el mensaje «Aviso Batoi», y pulsa "Guardar".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await altaSms(page, {
      centro: 'CIPFP Batoi', dni: '65399546N', nombre: 'Alumno1', apellidos: 'CIPFP Batoi',
      telefono: '600333444', mensaje: avisoBatoi,
    });

    // Paso 3: Y comprueba que en el listado aparece con el teléfono «+34600333444».
    await expect(
      page.getByRole('row', { name: avisoBatoi }).getByRole('gridcell', { name: '+34600333444', exact: true }),
    ).toBeVisible();

    // Paso 4: Y espera unos segundos, recarga el listado y pulsa sobre el SMS «Aviso Batoi».
    // "Espera unos segundos": el envío es asíncrono; se recarga hasta que el SMS sale de "Pendiente".
    await expect(async () => {
      await page.reload();
      await expect(
        page.getByRole('row', { name: avisoBatoi }).getByRole('gridcell', { name: /^(Enviado|Fallido)$/ }),
      ).toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 60_000, intervals: [2_000] });
    await page.getByRole('row', { name: avisoBatoi }).click();
    await page.waitForURL(/\/edit\/\d+/);

    const datosEnvio = page.getByRole('region', { name: 'Datos del envío' });
    const estadoEnviado = datosEnvio.getByRole('radio', { name: 'Enviado' });
    const estadoFallido = datosEnvio.getByRole('radio', { name: 'Fallido' });
    // El estado es un grupo de radios: se espera a que el marcado sea «Enviado» o «Fallido».
    await expect(datosEnvio.getByRole('radio', { name: /^(Enviado|Fallido)$/, checked: true })).toHaveCount(1);
    const reenviar = page.getByRole('button', { name: 'Reenviar' });

    if (await estadoFallido.isChecked()) {
      await expect(estadoFallido).toBeChecked();
      // Resultado esperado (Fallido): el sistema muestra el botón "Reenviar" y, al pulsarlo, muestra
      // «El reenvío del SMS se ha puesto en marcha.».
      await expect(reenviar).toBeVisible();
      await reenviar.click();
      await expect(
        page.getByRole('status').filter({ hasText: 'El reenvío del SMS se ha puesto en marcha.' }),
      ).toBeVisible();
    } else {
      // Resultado esperado (Enviado): el sistema no muestra el botón "Reenviar".
      await expect(estadoEnviado).toBeChecked();
      await expect(reenviar).toHaveCount(0);
    }

    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
