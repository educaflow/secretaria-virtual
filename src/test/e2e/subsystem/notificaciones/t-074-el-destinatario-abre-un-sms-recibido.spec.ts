import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-074 — El destinatario abre un SMS recibido
// origen: ESC-065  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-074-el-destinatario-abre-un-sms-recibido.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-049), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Motivo interno SMS
// abierto» lleva un sufijo único por ejecución: en «Todas» la fila se localiza
// por él. «Recibidas» no muestra el motivo, así que allí la fila propia se
// identifica por tipo + destino + la fecha de envío leída en «Todas».
const MOTIVO_BASE = 'Motivo interno SMS abierto';
const MOTIVO = `${MOTIVO_BASE} t074-${Date.now()}`;

// SMS de referencia (con el motivo sustituido por MOTIVO).
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
  // El destino es el teléfono normalizado a formato internacional.
  destino: '+34600111222',
};

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const TODAS = { estado: 0, fechaEnvio: 10 };
// Columnas del grid «Recibidas», en orden: Tipo, Destino, Expediente, Fecha de envío.
const REC = { fechaEnvio: 3 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);
const FECHA = /\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/;

// Escapa un literal para usarlo dentro de una RegExp.
const literal = (texto: string): string => texto.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const ventanaSms = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });

async function abrirMenu(page: Page, submenu: string): Promise<void> {
  // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
  // data-testid estable que Axelor deriva del nombre del menuitem.
  const menu = page.getByTestId('item:notificaciones-menuitem');
  await menu.getByText('Notificaciones', { exact: true }).click();
  await menu.getByText(submenu, { exact: true }).click();
}

async function altaSms(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: 'SMS' }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();

  const form = ventanaSms(page);
  await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
  // El centro primero: many-to-one con autocompletado que re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(SMS.centro);
  await page.getByRole('option', { name: SMS.centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(SMS.centro);
  await form.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO);
  await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(SMS.dni);
  await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(SMS.nombre);
  await form.getByRole('textbox', { name: 'Apellidos' }).fill(SMS.apellidos);
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
}

// El envío es asíncrono: se recarga «Todas» hasta que la notificación deja
// «Pendiente» y se devuelve su estado final y su fecha de envío.
async function resultadoEnvio(page: Page): Promise<{ estado: string; fechaEnvio: string }> {
  const fila = (): Locator => page.getByRole('row', { name: MOTIVO });
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
  test('El destinatario abre un SMS recibido', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el SMS de referencia con el
    //         motivo «Motivo interno SMS abierto», y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaSms(page);
    // Sin configuración real de SMS el envío acaba «Enviado» o «Fallido»: se
    // anota el resultado para saber qué rama del resultado esperado comprobar.
    const resultado = await resultadoEnvio(page);
    await logout(page);

    // Paso 2: Cuando «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».
    await login(page, 'alumno1@mislata.es', 'demo1234');
    await abrirMenu(page, 'Recibidas');
    const grid = page.getByRole('grid');
    await expect(grid.getByRole('columnheader', { name: 'Fecha de envío' })).toBeVisible();
    const filasSms = grid
      .getByRole('row')
      .filter({ has: page.getByRole('gridcell', { name: 'SMS', exact: true }) })
      .filter({ has: page.getByRole('gridcell', { name: SMS.destino, exact: true }) });

    if (resultado.estado === 'Enviado') {
      // Paso 3: Y si el SMS está «Enviado», pulsa su fila ...
      expect(resultado.fechaEnvio).toMatch(FECHA);
      const fila = filasSms
        .filter({ has: page.getByRole('gridcell', { name: resultado.fechaEnvio, exact: true }) })
        .first();
      await expect(fila).toBeVisible();
      await fila.click();

      // Resultado esperado: el formulario del SMS se abre en solo lectura con
      // teléfono «+34600111222», la fecha de envío y el mensaje «Mañana no hay
      // clase», sin el texto «Motivo interno SMS abierto», sin estado ni número de
      // reintentos y sin «Reenviar».
      const consulta = ventanaSms(page);
      const datos = consulta.getByRole('region', { name: 'Datos del SMS' });
      await expect(datos).toBeVisible();
      const salir = consulta.getByRole('button', { name: 'Salir' });
      await expect(salir).toBeVisible();
      // El teléfono, en solo lectura, se pinta como enlace «tel:» con el número
      // normalizado, sin el input editable del widget.
      await expect(datos.getByRole('link', { name: '+34 600 111 222' })).toHaveAttribute('href', `tel:${SMS.destino}`);
      await expect(datos.getByPlaceholder('+34 123 456 789')).toHaveCount(0);
      const fechaEnvio = datos.getByRole('textbox', { name: 'Fecha de envío' });
      await expect(fechaEnvio).toHaveValue(resultado.fechaEnvio);
      await expect(fechaEnvio).toBeDisabled();
      // El mensaje, en solo lectura, se pinta como texto tras su etiqueta, no como textbox.
      await expect(datos.getByRole('textbox', { name: 'Mensaje' })).toHaveCount(0);
      await expect(datos).toContainText(new RegExp(`Mensaje\\s*${literal(SMS.mensaje)}`));
      // El motivo no se muestra ni como texto ni como campo.
      await expect(consulta).not.toContainText(MOTIVO_BASE);
      await expect(consulta.getByRole('textbox', { name: /^Motivo/ })).toHaveCount(0);
      // Sin estado ni número de reintentos (ni la región de datos del envío).
      await expect(consulta.getByRole('region', { name: 'Datos del envío' })).toHaveCount(0);
      await expect(consulta.getByRole('radio', { name: 'Enviado' })).toHaveCount(0);
      await expect(consulta.getByRole('radio', { name: 'Fallido' })).toHaveCount(0);
      await expect(consulta.getByRole('radio', { name: 'Pendiente' })).toHaveCount(0);
      await expect(consulta.getByText('Estado', { exact: true })).toHaveCount(0);
      await expect(consulta.getByRole('textbox', { name: 'Nº reintentos' })).toHaveCount(0);
      await expect(consulta).not.toContainText('Nº reintentos');
      // Sin «Reenviar» (ni ningún botón de edición).
      await expect(consulta.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);
      await expect(consulta.getByRole('button', { name: 'Guardar' })).toHaveCount(0);

      // Paso 3 (cont.): ... y después «Salir».
      await salir.click();

      // Resultado esperado: «Salir» vuelve al listado de recibidas.
      await expect(consulta).toBeHidden();
      await expect(grid.getByRole('columnheader', { name: 'Fecha de envío' })).toBeVisible();
      await expect(fila).toBeVisible();
    } else {
      // Resultado esperado (si no): el SMS no aparece en «Recibidas». Un SMS
      // fallido no tiene fecha de envío, y en «Recibidas» toda fila de SMS a
      // este teléfono la tiene: ninguna puede ser la de este run.
      expect(resultado.estado).toBe('Fallido');
      const n = await filasSms.count();
      for (let i = 0; i < n; i++) {
        await expect(celda(filasSms.nth(i), REC.fechaEnvio)).toHaveText(FECHA);
      }
    }

    await logout(page);
  });
});
