import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-069 — El destinatario abre un correo recibido y descarga su adjunto
// origen: ESC-053  |  verifica: U-notificaciones-recibidas-002, R-Correo-001
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-069-el-destinatario-abre-un-correo-recibido-y-descarga-su-adjunto.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Envío de horario» lleva
// un sufijo único por ejecución: en «Todas» la fila se localiza por él. «Recibidas»
// no muestra el motivo, así que allí la fila propia se identifica por tipo +
// destino + la fecha de envío leída en «Todas» (el grid ordena por fecha de envío
// descendente y el correo de este run es el último enviado a alumno1).
const MOTIVO_BASE = 'Envío de horario';
const MOTIVO = `${MOTIVO_BASE} t069-${Date.now()}`;

// Correo de referencia con el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Horario',
  cuerpo: 'Le adjuntamos su horario.',
};
const FICHERO = 'horario.pdf';

// PDF mínimo válido generado en memoria: el test no depende de ningún fichero del árbol.
const HORARIO_PDF = Buffer.from(
  '%PDF-1.4\n' +
    '1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n' +
    '2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n' +
    '3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n' +
    '4 0 obj<</Length 39>>stream\nBT /F1 12 Tf 20 100 Td (Horario) Tj ET\nendstream endobj\n' +
    '5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n' +
    'trailer<</Root 1 0 R>>\n%%EOF\n',
  'latin1',
);

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

const ventanaCorreo = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
const ventanaAdjunto = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });

async function abrirMenu(page: Page, submenu: string): Promise<void> {
  // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
  // data-testid estable que Axelor deriva del nombre del menuitem.
  const menu = page.getByTestId('item:notificaciones-menuitem');
  await menu.getByText('Notificaciones', { exact: true }).click();
  await menu.getByText(submenu, { exact: true }).click();
}

async function altaCorreoConAdjunto(page: Page): Promise<void> {
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
  await form.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO);
  await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
  await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(CORREO.nombre);
  await form.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
  await form.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
  await form.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
  await form.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);

  const adjuntos = form.getByRole('region', { name: 'Adjuntos' });
  await adjuntos.getByRole('button', { name: 'Añadir adjunto' }).click();
  const altaAdjunto = ventanaAdjunto(page);
  await expect(altaAdjunto).toBeVisible();
  await altaAdjunto.getByRole('textbox', { name: 'Nombre del fichero' }).fill(FICHERO);
  // El widget binario esconde su `input[type=file]` tras el botón «Subir»;
  // `setInputFiles` escribe en él con el fichero en memoria.
  await altaAdjunto
    .getByTestId('field:contenido')
    .locator('input[type="file"]')
    .setInputFiles({ name: FICHERO, mimeType: 'application/pdf', buffer: HORARIO_PDF });
  await altaAdjunto.getByRole('button', { name: 'Guardar' }).click();
  await expect(altaAdjunto).toBeHidden();
  await expect(adjuntos.getByRole('row', { name: FICHERO })).toBeVisible();
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
  test('El destinatario abre un correo recibido y descarga su adjunto', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el correo de referencia con el
    //         motivo «Envío de horario», el asunto «Horario», el cuerpo «Le adjuntamos
    //         su horario.» y el adjunto «horario.pdf», y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaCorreoConAdjunto(page);
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
      // Paso 3: Y si el correo está «Enviado», pulsa su fila ...
      expect(resultado.fechaEnvio).toMatch(FECHA);
      const fila = filasCorreo
        .filter({ has: page.getByRole('gridcell', { name: resultado.fechaEnvio, exact: true }) })
        .first();
      await expect(fila).toBeVisible();
      await fila.click();

      // Resultado esperado: el formulario se abre en solo lectura con asunto «Horario»,
      // cuerpo «Le adjuntamos su horario.», «para» «alumno1@mislata.es», sin el campo
      // «en copia», la fecha de envío y el adjunto «horario.pdf», sin el texto
      // «Envío de horario» y sin «Reenviar».
      const consulta = ventanaCorreo(page);
      const datos = consulta.getByRole('region', { name: 'Datos del correo' });
      await expect(datos).toBeVisible();
      const asunto = datos.getByRole('textbox', { name: 'Asunto' });
      await expect(asunto).toHaveValue(CORREO.asunto);
      await expect(asunto).toBeDisabled();
      // El cuerpo, en solo lectura, se pinta como texto tras su etiqueta, no como textbox.
      await expect(datos.getByRole('textbox', { name: 'Cuerpo' })).toHaveCount(0);
      await expect(datos).toContainText(new RegExp(`Cuerpo\\s*${literal(CORREO.cuerpo)}`));
      const para = datos.getByRole('textbox', { name: /^Para/ });
      await expect(para).toHaveValue(CORREO.para);
      await expect(para).toBeDisabled();
      // Sin copias, el campo «en copia» no se pinta (ni su etiqueta ni su input).
      await expect(consulta.getByText('En copia', { exact: true })).toHaveCount(0);
      await expect(consulta.getByRole('textbox', { name: /^En copia/ })).toHaveCount(0);
      const fechaEnvio = datos.getByRole('textbox', { name: 'Fecha de envío' });
      await expect(fechaEnvio).toHaveValue(resultado.fechaEnvio);
      await expect(fechaEnvio).toBeDisabled();
      // El motivo no se muestra ni como texto ni como campo.
      await expect(consulta).not.toContainText(MOTIVO_BASE);
      await expect(consulta.getByRole('textbox', { name: /^Motivo/ })).toHaveCount(0);
      await expect(consulta.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);
      await expect(consulta.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
      const filaAdjunto = consulta.getByRole('region', { name: 'Adjuntos' }).getByRole('row', { name: FICHERO });
      await expect(filaAdjunto).toBeVisible();

      // Paso 3 (cont.): ... y después la fila «horario.pdf» del panel «Adjuntos», y descarga el fichero.
      await filaAdjunto.click();
      const adjunto = ventanaAdjunto(page);
      await expect(adjunto).toBeVisible();
      await expect(adjunto.getByRole('textbox', { name: 'Nombre del fichero' })).toHaveValue(FICHERO);
      const enlaceFichero = adjunto.getByTestId('field:contenido').getByRole('button', { name: FICHERO, exact: true });
      await expect(enlaceFichero).toBeVisible();
      const [descarga] = await Promise.all([page.waitForEvent('download'), enlaceFichero.click()]);

      // Resultado esperado: se descarga «horario.pdf».
      expect(descarga.suggestedFilename()).toBe(FICHERO);
      expect(await descarga.failure()).toBeNull();

      // Las ventanas se abren como modales sobre el listado: se cierran antes del
      // logout para que el menú de usuario quede accesible.
      await adjunto.getByRole('button', { name: 'Salir' }).click();
      await expect(adjunto).toBeHidden();
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
