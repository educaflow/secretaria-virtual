import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-024 — El destinatario ve un SMS enviado desde otro centro
// origen: ESC-024  |  verifica: —
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-024-el-destinatario-ve-un-sms-enviado-desde-otro-centro.desc.md
test.describe('SMS — Recibidos', () => {
  test('El destinatario ve un SMS enviado desde otro centro', async ({ page }) => {
    test.setTimeout(150_000);
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por ejecución
    // para identificar SIEMPRE la fila que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const mensaje = `Aviso desde Batoi t024-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Batoi",
    // escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono
    // «600111222» y el mensaje «Aviso desde Batoi», y pulsa "Guardar".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Batoi' }).click();
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

    // Paso 3: Y comprueba que en el listado aparece con el teléfono «+34600111222».
    await expect(page).toHaveURL(/\/list\//);
    const fila = page.getByRole('row', { name: mensaje });
    await expect(fila).toBeVisible();
    await expect(fila.getByRole('gridcell', { name: '+34600111222', exact: true })).toBeVisible();
    await expect(fila).toContainText('CIPFP Batoi');

    // Paso 4: Y espera unos segundos, recarga el listado y anota el estado del SMS «Aviso desde Batoi».
    // El envío es asíncrono: se recarga en bucle hasta que el SMS sale de "Pendiente".
    const estadoCelda = fila.getByRole('gridcell', { name: /^(Enviado|Fallido)$/ });
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

    const filaRecibido = grid.getByRole('row', { name: mensaje });
    if (enviado) {
      // Resultado esperado (Enviado): el listado lo muestra con el teléfono «+34600111222», su
      // mensaje y su fecha de envío, aunque lo haya enviado un centro en el que el alumno no está.
      await expect(filaRecibido).toBeVisible();
      await expect(filaRecibido.getByRole('gridcell', { name: mensaje, exact: true })).toBeVisible();
      await expect(filaRecibido.getByRole('gridcell', { name: '+34600111222', exact: true })).toBeVisible();
      await expect(filaRecibido).toContainText(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/);
    } else {
      // Resultado esperado (Fallido): el listado no lo muestra.
      await expect(filaRecibido).toHaveCount(0);
    }

    await logout(page);
  });
});
