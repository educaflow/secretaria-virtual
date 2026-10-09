import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-073 — El destinatario ve el «en copia» pero no el «en copia oculta» ni los datos del envío
// origen: ESC-064  |  verifica: U-notificaciones-recibidas-001
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-073-el-destinatario-ve-el-en-copia-pero-no-el-en-copia-oculta-ni-los-datos-del-envio.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Correo con copia oculta»
// lleva un sufijo único por ejecución: en «Todas» la fila se localiza por él.
// «Recibidas» no muestra el motivo, así que allí la fila propia se identifica por
// tipo + destino + la fecha de envío leída en «Todas».
const MOTIVO = `Correo con copia oculta t073-${Date.now()}`;

// Correo de referencia con el «en copia», el «en copia oculta», el asunto y el cuerpo del test.
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '95591733F',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  enCopia: 'familiar1@mislata.es',
  enCopiaOculta: 'supervisor1@mislata.es',
  asunto: 'Aviso con copias',
  cuerpo: 'Texto',
};

const EN_COPIA = /^En copia(?! oculta)/;
const EN_COPIA_OCULTA = /^En copia oculta/;

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const TODAS = { estado: 0, fechaEnvio: 10 };
// Columnas del grid «Recibidas», en orden: Tipo, Destino, Expediente, Fecha de envío.
const REC = { fechaEnvio: 3 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);
const FECHA = /\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/;

const ventanaCorreo = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });

async function abrirMenu(page: Page, submenu: string): Promise<void> {
  // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
  // data-testid estable que Axelor deriva del nombre del menuitem.
  const menu = page.getByTestId('item:notificaciones-menuitem');
  await menu.getByText('Notificaciones', { exact: true }).click();
  await menu.getByText(submenu, { exact: true }).click();
}

async function altaCorreoConCopias(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: 'Correo' }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();

  const form = ventanaCorreo(page);
  await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
  // El centro primero: many-to-one con autocompletado que re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(CORREO.centro);
  await page.getByRole('option', { name: CORREO.centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(CORREO.centro);
  await form.getByRole('textbox', { name: /^Motivo/ }).fill(MOTIVO);
  await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
  await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(CORREO.nombre);
  await form.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
  await form.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
  await form.getByRole('textbox', { name: EN_COPIA }).fill(CORREO.enCopia);
  await form.getByRole('textbox', { name: EN_COPIA_OCULTA }).fill(CORREO.enCopiaOculta);
  await form.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
  await form.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);
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
  test('El destinatario ve el «en copia» pero no el «en copia oculta» ni los datos del envío', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el correo de referencia con el motivo
    //         «Correo con copia oculta», el «en copia» «familiar1@mislata.es», el «en copia
    //         oculta» «supervisor1@mislata.es», el asunto «Aviso con copias» y el cuerpo
    //         «Texto», y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaCorreoConCopias(page);
    // Sin configuración real de correo el envío acaba «Enviado» o «Fallido»: se
    // anota el resultado para saber qué rama del resultado esperado comprobar.
    const resultado = await resultadoEnvio(page);
    await logout(page);

    // Paso 2: Cuando «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».
    await login(page, 'alumno1@mislata.es', 'demo1234');
    await abrirMenu(page, 'Recibidas');
    const grid = page.getByRole('grid');
    await expect(grid.getByRole('columnheader', { name: 'Fecha de envío' })).toBeVisible();
    const filasCorreo = grid
      .getByRole('row')
      .filter({ has: page.getByRole('gridcell', { name: 'Correo', exact: true }) })
      .filter({ has: page.getByRole('gridcell', { name: CORREO.para, exact: true }) });

    if (resultado.estado === 'Enviado') {
      // Paso 3: Y si el correo está «Enviado», pulsa su fila.
      expect(resultado.fechaEnvio).toMatch(FECHA);
      const fila = filasCorreo
        .filter({ has: page.getByRole('gridcell', { name: resultado.fechaEnvio, exact: true }) })
        .first();
      await expect(fila).toBeVisible();
      await fila.click();

      // Resultado esperado: el formulario muestra asunto «Aviso con copias», «para»
      // «alumno1@mislata.es» y «en copia» «familiar1@mislata.es», y no muestra el
      // «en copia oculta», el texto «supervisor1@mislata.es», el estado, el número de
      // reintentos ni la descripción del último fallo.
      const consulta = ventanaCorreo(page);
      const datos = consulta.getByRole('region', { name: 'Datos del correo' });
      await expect(datos).toBeVisible();
      await expect(datos.getByRole('textbox', { name: 'Asunto' })).toHaveValue(CORREO.asunto);
      await expect(datos.getByRole('textbox', { name: /^Para/ })).toHaveValue(CORREO.para);
      await expect(datos.getByRole('textbox', { name: EN_COPIA })).toHaveValue(CORREO.enCopia);
      // Sin «en copia oculta»: ni su campo ni su etiqueta, ni la dirección como texto.
      await expect(consulta.getByRole('textbox', { name: EN_COPIA_OCULTA })).toHaveCount(0);
      await expect(consulta.getByText('En copia oculta')).toHaveCount(0);
      await expect(consulta).not.toContainText(CORREO.enCopiaOculta);
      // El valor de un input no forma parte del texto: se revisa cada campo.
      const campos = consulta.getByRole('textbox');
      const nCampos = await campos.count();
      for (let i = 0; i < nCampos; i++) {
        await expect(campos.nth(i)).not.toHaveValue(new RegExp(CORREO.enCopiaOculta.replace(/[.]/g, '\\.')));
      }
      // Sin los datos del envío: ni la región, ni el estado, ni los reintentos, ni el último fallo.
      await expect(consulta.getByRole('region', { name: 'Datos del envío' })).toHaveCount(0);
      await expect(consulta.getByRole('radio', { name: 'Enviado' })).toHaveCount(0);
      await expect(consulta.getByRole('radio', { name: 'Fallido' })).toHaveCount(0);
      await expect(consulta.getByRole('radio', { name: 'Pendiente' })).toHaveCount(0);
      await expect(consulta.getByText('Estado', { exact: true })).toHaveCount(0);
      await expect(consulta.getByRole('textbox', { name: 'Nº reintentos' })).toHaveCount(0);
      await expect(consulta).not.toContainText('Nº reintentos');
      await expect(consulta).not.toContainText('Descripción del último fallo');

      // La ventana se abre como modal sobre el listado: se cierra antes del
      // logout para que el menú de usuario quede accesible.
      await consulta.getByRole('button', { name: 'Salir' }).click();
      await expect(consulta).toBeHidden();
    } else {
      // Resultado esperado (si no): el correo no aparece en «Recibidas». Un correo
      // fallido no tiene fecha de envío, y en «Recibidas» toda fila de correo a
      // alumno1 la tiene: ninguna puede ser la de este run.
      expect(resultado.estado).toBe('Fallido');
      const n = await filasCorreo.count();
      for (let i = 0; i < n; i++) {
        await expect(celda(filasCorreo.nth(i), REC.fechaEnvio)).toHaveText(FECHA);
      }
    }

    await logout(page);
  });
});
