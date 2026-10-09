import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-070 — El destinatario no ve las notificaciones de otra persona
// origen: ESC-054  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-070-el-destinatario-no-ve-las-notificaciones-de-otra-persona.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), el motivo
// lleva un sufijo único por ejecución: en «Todas» la fila se localiza por ese
// motivo. «Recibidas» no muestra ni el motivo ni el asunto, así que allí la
// fila propia se identifica por tipo + destino + la fecha de envío leída en «Todas».
const SUFIJO = `t070-${Date.now()}`;
const MOTIVO = `Aviso de prueba de correo ${SUFIJO}`;
const DEST = { centro: 'CIPFP Mislata', dni: '99024353S', nombre: 'Alumno2', apellidos: 'CIPFP Mislata' };
const CORREO = { para: 'alumno2@mislata.es', asunto: 'Aviso para Alumno2', cuerpo: 'La reunión será el lunes a las 10:00.' };

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const TODAS = { estado: 0, fechaEnvio: 10 };
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
// «Pendiente» y se devuelve su estado final y su fecha de envío.
async function resultadoEnvio(page: Page, motivo: string): Promise<{ estado: string; fechaEnvio: string }> {
  const fila = (): Locator => page.getByRole('row', { name: motivo });
  await expect(async () => {
    await page.reload();
    await expect(fila()).toBeVisible({ timeout: 10_000 });
    await expect(celda(fila(), TODAS.estado)).toHaveText(/^(Enviado|Fallido)$/, { timeout: 1_000 });
  }).toPass({ timeout: 60_000 });
  const estado = (await celda(fila(), TODAS.estado).innerText()).trim();
  const fechaEnvio = (await celda(fila(), TODAS.fechaEnvio).innerText()).trim();
  return { estado, fechaEnvio };
}

async function abrirRecibidas(page: Page): Promise<Locator> {
  const menu = page.getByTestId('item:notificaciones-menuitem');
  await menu.getByText('Notificaciones', { exact: true }).click();
  await menu.getByText('Recibidas', { exact: true }).click();
  const grid = page.getByRole('grid');
  await expect(grid.getByRole('columnheader', { name: 'Fecha de envío' })).toBeVisible();
  return grid;
}

const filasDe = (page: Page, grid: Locator, tipo: string, destino: string): Locator =>
  grid
    .getByRole('row')
    .filter({ has: page.getByRole('gridcell') })
    .filter({ has: page.getByRole('gridcell', { name: tipo, exact: true }) })
    .filter({ has: page.getByRole('gridcell', { name: destino, exact: true }) });

test.describe('Notificaciones — Recibidas', () => {
  test('El destinatario no ve las notificaciones de otra persona', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador da de alta el correo de referencia con el
    //         DNI «99024353S», el nombre «Alumno2», el «para» «alumno2@mislata.es»
    //         y el asunto «Aviso para Alumno2», y cierra sesión.
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
    await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
    // El centro primero: es un many-to-one con autocompletado y su selección
    // re-renderiza el formulario.
    await form.getByRole('combobox', { name: 'Centro' }).fill(DEST.centro);
    await page.getByRole('option', { name: DEST.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(DEST.centro);
    await form.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(DEST.dni);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(DEST.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(DEST.apellidos);
    await form.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
    await form.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
    await form.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);
    await form.getByRole('button', { name: 'Guardar' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    // Sin configuración real de correo el envío acaba «Enviado» o «Fallido»:
    // se anota el resultado para saber qué esperar en «Recibidas» de alumno2.
    const correo = await resultadoEnvio(page, MOTIVO);
    await logout(page);

    // Paso 2: Cuando «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».
    await login(page, 'alumno1@mislata.es', 'demo1234');
    const gridAlumno1 = await abrirRecibidas(page);

    // Paso 3: Entonces no aparece «Aviso para Alumno2».
    // «Recibidas» no muestra el asunto: además de no contener el texto, ninguna
    // fila de alumno1 puede ir dirigida a alumno2@mislata.es.
    await expect(gridAlumno1).not.toContainText(CORREO.asunto);
    await expect(gridAlumno1).not.toContainText(CORREO.para);
    await expect(filasDe(page, gridAlumno1, 'Correo', CORREO.para)).toHaveCount(0);

    // Paso 4: Cuando cierra sesión y «alumno2@mislata.es» inicia sesión y abre «Recibidas».
    await logout(page);
    await login(page, 'alumno2@mislata.es', 'demo1234');
    const gridAlumno2 = await abrirRecibidas(page);

    if (correo.estado === 'Enviado') {
      // Resultado esperado: si el correo está «Enviado», aparece con tipo
      // «Correo» y destino «alumno2@mislata.es».
      expect(correo.fechaEnvio).toMatch(FECHA);
      const propia = filasDe(page, gridAlumno2, 'Correo', CORREO.para)
        .filter({ has: page.getByRole('gridcell', { name: correo.fechaEnvio, exact: true }) })
        .first();
      await expect(propia).toBeVisible();
      await expect(celda(propia, REC.tipo)).toHaveText('Correo');
      await expect(celda(propia, REC.destino)).toHaveText(CORREO.para);
      await expect(celda(propia, REC.fechaEnvio)).toHaveText(correo.fechaEnvio);
    } else {
      // Resultado esperado: si no está «Enviado» (Fallido), no aparece. Una
      // fallida no tiene fecha de envío, y en «Recibidas» toda fila de ese
      // tipo/destino la tiene, así que ninguna puede ser la de este run.
      expect(correo.estado).toBe('Fallido');
      const candidatas = filasDe(page, gridAlumno2, 'Correo', CORREO.para);
      const n = await candidatas.count();
      for (let i = 0; i < n; i++) {
        await expect(celda(candidatas.nth(i), REC.fechaEnvio)).toHaveText(FECHA);
      }
    }

    await logout(page);
  });
});
