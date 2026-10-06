import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-018 — El destinatario ve sus SMS enviados y no los de otro
// origen: ESC-018  |  verifica: —
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-018-el-destinatario-ve-sus-sms-enviados-y-no-los-de-otro.desc.md

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

test.describe('SMS — Recibidos', () => {
  test('El destinatario ve sus SMS enviados y no los de otro', async ({ page }) => {
    test.setTimeout(150_000);
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): cada mensaje lleva un sufijo único por ejecución
    // para identificar SIEMPRE las filas que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const run = Date.now();
    const avisoAlumno1 = `Aviso para Alumno1 t018-${run}`;
    const avisoAlumno2 = `Aviso para Alumno2 t018-${run}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI
    // «95591733F», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el
    // mensaje «Aviso para Alumno1».
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await altaSms(page, {
      centro: 'CIPFP Mislata', dni: '95591733F', nombre: 'Alumno1', apellidos: 'CIPFP Mislata',
      telefono: '600111222', mensaje: avisoAlumno1,
    });

    // Paso 3: Y da de alta otro SMS del centro "CIPFP Mislata" con el DNI «99024353S», el nombre
    // «Alumno2», los apellidos «CIPFP Mislata», el teléfono «600555666» y el mensaje «Aviso para Alumno2».
    await altaSms(page, {
      centro: 'CIPFP Mislata', dni: '99024353S', nombre: 'Alumno2', apellidos: 'CIPFP Mislata',
      telefono: '600555666', mensaje: avisoAlumno2,
    });

    // Paso 4: Y espera unos segundos, recarga el listado y anota el estado del SMS «Aviso para Alumno1».
    // El envío es asíncrono: se recarga en bucle hasta que el SMS sale de "Pendiente".
    const estadoCelda = page.getByRole('row', { name: avisoAlumno1 }).getByRole('gridcell', { name: /^(Enviado|Fallido)$/ });
    await expect(async () => {
      await page.reload();
      await expect(estadoCelda).toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 60_000, intervals: [2_000] });
    const enviado = ((await estadoCelda.textContent()) ?? '').trim() === 'Enviado';

    // Paso 5: Y cierra sesión.
    await logout(page);

    // Paso 6: Y el alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234» y abre el
    // menú "SMS" → "Recibidos".
    await login(page, 'alumno1@mislata.es', 'demo1234');
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-recibidos-menuitem').click();
    await expect(page.getByRole('tab', { name: 'Mis SMS' })).toBeVisible();
    const grid = page.getByRole('tabpanel', { name: 'Mis SMS' }).getByRole('grid');
    await expect(grid.getByRole('columnheader', { name: 'Mensaje' })).toBeVisible();
    // Esperar a que el grid termine de cargar (filas de datos o el aviso de vacío).
    await expect(
      grid.getByRole('row', { name: 'No se encontraron registros.' }).or(grid.getByRole('row').filter({ hasText: 'Aviso' })).first(),
    ).toBeVisible();

    // Resultado esperado: en ningún caso el listado muestra el SMS «Aviso para Alumno2»
    // (ni el de este run ni ninguno de runs anteriores: no son del DNI de alumno1).
    await expect(grid.getByRole('row', { name: /Aviso para Alumno2/ })).toHaveCount(0);

    const filaAlumno1 = grid.getByRole('row', { name: avisoAlumno1 });
    if (enviado) {
      // Resultado esperado (Enviado): el listado lo muestra con el teléfono «+34600111222», el
      // mensaje «Aviso para Alumno1» y su fecha de envío...
      await expect(filaAlumno1).toBeVisible();
      await expect(filaAlumno1.getByRole('gridcell', { name: avisoAlumno1, exact: true })).toBeVisible();
      await expect(filaAlumno1.getByRole('gridcell', { name: '+34600111222', exact: true })).toBeVisible();
      await expect(filaAlumno1).toContainText(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/);

      // ...y al pulsarlo se abre en solo lectura.
      await filaAlumno1.click();
      await page.waitForURL(/\/edit\/\d+/);
      const datosSms = page.getByRole('region', { name: 'Datos del SMS' });
      await expect(datosSms).toBeVisible();
      await expect(datosSms.getByText(avisoAlumno1, { exact: true })).toBeVisible();
      await expect(datosSms.getByRole('link', { name: '+34 600 111 222' })).toBeVisible();
      await expect(datosSms.locator('input:not([disabled]):not([type="hidden"]), textarea:not([disabled])')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
      await page.getByRole('button', { name: 'Salir' }).click();
    } else {
      // Resultado esperado (Fallido): el listado no lo muestra.
      await expect(filaAlumno1).toHaveCount(0);
    }

    await logout(page);
  });
});
