import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-068 — El destinatario ve sus correos y SMS enviados, sin el motivo
// origen: ESC-052  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-068-el-destinatario-ve-sus-correos-y-sms-enviados-sin-el-motivo.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), los motivos
// llevan un sufijo único por ejecución: en «Todas» las filas se localizan por
// ese motivo. «Recibidas» no muestra el motivo, así que allí la fila propia se
// identifica por tipo + destino + la fecha de envío leída en «Todas».
const SUFIJO = `t068-${Date.now()}`;
const MOTIVO_CORREO = `Motivo interno correo ${SUFIJO}`;
const MOTIVO_SMS = `Motivo interno SMS ${SUFIJO}`;
const DEST = { centro: 'CIPFP Mislata', dni: '95591733F', nombre: 'Alumno1', apellidos: 'CIPFP Mislata' };
const CORREO = { para: 'alumno1@mislata.es', asunto: 'Reunión de inicio de curso', cuerpo: 'La reunión será el lunes a las 10:00.' };
const SMS = { telefono: '600111222', mensaje: 'Mañana no hay clase', destino: '+34600111222' };

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

async function elegirCanal(page: Page, canal: 'Correo' | 'SMS'): Promise<Locator> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: canal }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();
  const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: canal, exact: true }) });
  await expect(form.getByRole('region', { name: `Datos del ${canal === 'Correo' ? 'correo' : 'SMS'}` })).toBeVisible();
  // El centro primero: es un many-to-one con autocompletado y su selección
  // re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(DEST.centro);
  await page.getByRole('option', { name: DEST.centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(DEST.centro);
  await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(DEST.dni);
  await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(DEST.nombre);
  await form.getByRole('textbox', { name: 'Apellidos' }).fill(DEST.apellidos);
  return form;
}

async function guardar(page: Page, form: Locator): Promise<void> {
  await form.getByRole('button', { name: 'Guardar' }).click();
  await expect(form).toBeHidden();
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

test.describe('Notificaciones — Recibidas', () => {
  test('El destinatario ve sus correos y SMS enviados, sin el motivo', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador da de alta el correo de referencia con el
    //         motivo «Motivo interno correo» y el SMS de referencia con el motivo
    //         «Motivo interno SMS», y cierra sesión.
    await abrirTodas(page);
    const formCorreo = await elegirCanal(page, 'Correo');
    await formCorreo.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO_CORREO);
    await formCorreo.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
    await formCorreo.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
    await formCorreo.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);
    await guardar(page, formCorreo);

    const formSms = await elegirCanal(page, 'SMS');
    await formSms.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO_SMS);
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input:
    // se localiza por su placeholder y se teclea porque formatea al vuelo.
    const telefono = formSms.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await formSms.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await guardar(page, formSms);

    // Sin configuración real de correo/SMS el envío acaba «Enviado» o «Fallido»:
    // se anota el resultado de cada una para saber qué esperar en «Recibidas».
    const correo = await resultadoEnvio(page, MOTIVO_CORREO);
    const sms = await resultadoEnvio(page, MOTIVO_SMS);
    await logout(page);

    // Paso 2: Cuando «alumno1@mislata.es» inicia sesión, espera unos segundos y
    //         abre «Notificaciones» → «Recibidas».
    await login(page, 'alumno1@mislata.es', 'demo1234');
    const menu = page.getByTestId('item:notificaciones-menuitem');
    await menu.getByText('Notificaciones', { exact: true }).click();
    await menu.getByText('Recibidas', { exact: true }).click();
    const grid = page.getByRole('grid');
    await expect(grid.getByRole('columnheader', { name: 'Fecha de envío' })).toBeVisible();
    const filas = grid.getByRole('row').filter({ has: page.getByRole('gridcell') });

    const filasDe = (tipo: string, destino: string): Locator =>
      filas
        .filter({ has: page.getByRole('gridcell', { name: tipo, exact: true }) })
        .filter({ has: page.getByRole('gridcell', { name: destino, exact: true }) });

    const comprobar = async (
      tipo: string,
      destino: string,
      resultado: { estado: string; fechaEnvio: string },
    ): Promise<void> => {
      if (resultado.estado === 'Enviado') {
        // Resultado esperado: la «Enviado» aparece con su tipo, su destino,
        // expediente vacío y su fecha de envío.
        expect(resultado.fechaEnvio).toMatch(FECHA);
        const propia = filasDe(tipo, destino).filter({
          has: page.getByRole('gridcell', { name: resultado.fechaEnvio, exact: true }),
        }).first();
        await expect(propia).toBeVisible();
        await expect(celda(propia, REC.tipo)).toHaveText(tipo);
        await expect(celda(propia, REC.destino)).toHaveText(destino);
        await expect(celda(propia, REC.expediente)).toHaveText('');
        await expect(celda(propia, REC.fechaEnvio)).toHaveText(resultado.fechaEnvio);
      } else {
        // Resultado esperado: la «Fallido» no aparece. Una fallida no tiene
        // fecha de envío, y en «Recibidas» toda fila de ese tipo/destino la tiene.
        expect(resultado.estado).toBe('Fallido');
        const candidatas = filasDe(tipo, destino);
        const n = await candidatas.count();
        for (let i = 0; i < n; i++) {
          await expect(celda(candidatas.nth(i), REC.fechaEnvio)).toHaveText(FECHA);
        }
      }
    };

    await comprobar('Correo', CORREO.para, correo);
    await comprobar('SMS', SMS.destino, sms);

    // Resultado esperado: ninguna fila muestra «Motivo interno correo» ni «Motivo interno SMS».
    await expect(grid).not.toContainText('Motivo interno correo');
    await expect(grid).not.toContainText('Motivo interno SMS');
    await expect(page.getByText(/Motivo interno (correo|SMS)/)).toHaveCount(0);

    await logout(page);
  });
});
