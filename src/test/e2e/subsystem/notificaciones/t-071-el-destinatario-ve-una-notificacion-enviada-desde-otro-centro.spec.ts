import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-071 — El destinatario ve una notificación enviada desde otro centro
// origen: ESC-055  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-071-el-destinatario-ve-una-notificacion-enviada-desde-otro-centro.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), el motivo
// lleva un sufijo único por ejecución: en «Todas» la fila se localiza por ese
// motivo. «Recibidas» no muestra el motivo, así que allí la fila propia se
// identifica por tipo + destino + la fecha de envío leída en «Todas».
const SUFIJO = `t071-${Date.now()}`;
const MOTIVO = `SMS desde Batoi ${SUFIJO}`;
const DEST = { centro: 'CIPFP Batoi', dni: '86862719E', nombre: 'Alumno1', apellidos: 'CIPFP Mislata' };
const SMS = { telefono: '600111222', mensaje: 'Mañana no hay clase', destino: '+34600111222' };

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const TODAS = { estado: 0, centro: 7, fechaEnvio: 10 };
// Columnas del grid «Recibidas», en orden: Tipo, Destino, Expediente, Fecha de envío.
const REC = { tipo: 0, destino: 1, expediente: 2, fechaEnvio: 3 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);
const FECHA = /\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/;

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

// El envío es asíncrono: se recarga «Todas» hasta que la notificación deja
// «Pendiente» y se devuelve su estado final, su centro y su fecha de envío.
async function resultadoEnvio(page: Page, motivo: string): Promise<{ estado: string; centro: string; fechaEnvio: string }> {
  const fila = (): Locator => page.getByRole('row', { name: motivo });
  await expect(async () => {
    await page.reload();
    await expect(fila()).toBeVisible({ timeout: 10_000 });
    await expect(celda(fila(), TODAS.estado)).toHaveText(/^(Enviado|Fallido)$/, { timeout: 1_000 });
  }).toPass({ timeout: 60_000 });
  const estado = (await celda(fila(), TODAS.estado).innerText()).trim();
  const centro = (await celda(fila(), TODAS.centro).innerText()).trim();
  const fechaEnvio = (await celda(fila(), TODAS.fechaEnvio).innerText()).trim();
  return { estado, centro, fechaEnvio };
}

test.describe('Notificaciones — Recibidas', () => {
  test('El destinatario ve una notificación enviada desde otro centro', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador da de alta el SMS de referencia del centro
    //         «CIPFP Batoi» (DNI «86862719E») con el motivo «SMS desde Batoi», y cierra sesión.
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'SMS' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });
    await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
    // El centro primero: es un many-to-one con autocompletado y su selección
    // re-renderiza el formulario.
    await form.getByRole('combobox', { name: 'Centro' }).fill(DEST.centro);
    await page.getByRole('option', { name: DEST.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(DEST.centro);
    await form.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(DEST.dni);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(DEST.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(DEST.apellidos);
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input:
    // se localiza por su placeholder y se teclea porque formatea al vuelo.
    const telefono = form.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await form.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await form.getByRole('button', { name: 'Guardar' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    // Sin configuración real de SMS el envío acaba «Enviado» o «Fallido»: se
    // anota el resultado para saber qué esperar en «Recibidas». Se comprueba
    // además que la notificación quedó en el otro centro («CIPFP Batoi»).
    const sms = await resultadoEnvio(page, MOTIVO);
    expect(sms.centro).toBe(DEST.centro);
    await logout(page);

    // Paso 2: Cuando «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».
    await login(page, 'alumno1@mislata.es', 'demo1234');
    const menu = page.getByTestId('item:notificaciones-menuitem');
    await menu.getByText('Notificaciones', { exact: true }).click();
    await menu.getByText('Recibidas', { exact: true }).click();
    const grid = page.getByRole('grid');
    await expect(grid.getByRole('columnheader', { name: 'Fecha de envío' })).toBeVisible();
    const candidatas = grid
      .getByRole('row')
      .filter({ has: page.getByRole('gridcell') })
      .filter({ has: page.getByRole('gridcell', { name: 'SMS', exact: true }) })
      .filter({ has: page.getByRole('gridcell', { name: SMS.destino, exact: true }) });

    if (sms.estado === 'Enviado') {
      // Resultado esperado: si el SMS está «Enviado», aparece con tipo «SMS» y
      // destino «+34600111222».
      expect(sms.fechaEnvio).toMatch(FECHA);
      const propia = candidatas
        .filter({ has: page.getByRole('gridcell', { name: sms.fechaEnvio, exact: true }) })
        .first();
      await expect(propia).toBeVisible();
      await expect(celda(propia, REC.tipo)).toHaveText('SMS');
      await expect(celda(propia, REC.destino)).toHaveText(SMS.destino);
      await expect(celda(propia, REC.fechaEnvio)).toHaveText(sms.fechaEnvio);
    } else {
      // Resultado esperado: si no está «Enviado» (Fallido), no aparece. Una
      // fallida no tiene fecha de envío, y en «Recibidas» toda fila de ese
      // tipo/destino la tiene, así que ninguna puede ser la de este run.
      expect(sms.estado).toBe('Fallido');
      const n = await candidatas.count();
      for (let i = 0; i < n; i++) {
        await expect(celda(candidatas.nth(i), REC.fechaEnvio)).toHaveText(FECHA);
      }
    }

    await logout(page);
  });
});
