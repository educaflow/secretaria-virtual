import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-062 — El gestor abre una notificación con sus adjuntos y descarga uno
// origen: ESC-049  |  verifica: R-Correo-001
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-062-el-gestor-abre-una-notificacion-con-sus-adjuntos-y-descarga-uno.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Envío de horario» lleva
// un sufijo único por ejecución y todas las aserciones localizan la fila por él.
const MOTIVO = `Envío de horario t062-${Date.now()}`;

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

test.describe('Notificaciones — Del centro', () => {
  test('El gestor abre una notificación con sus adjuntos y descarga uno', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el correo de referencia con el
    //         motivo «Envío de horario», el asunto «Horario», el cuerpo «Le adjuntamos
    //         su horario.» y el adjunto «horario.pdf», y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaCorreoConAdjunto(page);
    await logout(page);

    // Paso 2: Cuando «supervisor1@mislata.es» inicia sesión, abre «Del centro» y
    //         pulsa la fila «Envío de horario».
    await login(page, 'supervisor1@mislata.es', 'demo1234');
    await abrirMenu(page, 'Del centro');
    const fila = page.getByRole('row', { name: MOTIVO });
    await expect(fila).toBeVisible();
    await fila.click();

    // Paso 3: Entonces se abre el formulario del correo en solo lectura, sin «Guardar»
    //         ni «Borrar», con motivo, centro, DNI, nombre, apellidos, «para», asunto,
    //         cuerpo y la fila «horario.pdf» en «Adjuntos».
    const consulta = ventanaCorreo(page);
    const datos = consulta.getByRole('region', { name: 'Datos del correo' });
    await expect(datos).toBeVisible();
    const motivo = datos.getByRole('textbox', { name: /^Motivo/ });
    await expect(motivo).toHaveValue(MOTIVO);
    await expect(motivo).toHaveValue(/^Envío de horario/);
    await expect(motivo).toBeDisabled();
    // El centro, en solo lectura, se pinta como enlace-botón a su registro y no
    // como combobox editable.
    await expect(datos.getByRole('button', { name: CORREO.centro, exact: true })).toBeVisible();
    await expect(datos.getByRole('combobox', { name: 'Centro' })).toHaveCount(0);
    const dni = datos.getByRole('textbox', { name: 'DNI del destinatario' });
    await expect(dni).toHaveValue(CORREO.dni);
    await expect(dni).toBeDisabled();
    const nombre = datos.getByRole('textbox', { name: 'Nombre', exact: true });
    await expect(nombre).toHaveValue(CORREO.nombre);
    await expect(nombre).toBeDisabled();
    const apellidos = datos.getByRole('textbox', { name: 'Apellidos' });
    await expect(apellidos).toHaveValue(CORREO.apellidos);
    await expect(apellidos).toBeDisabled();
    const para = datos.getByRole('textbox', { name: /^Para/ });
    await expect(para).toHaveValue(CORREO.para);
    await expect(para).toBeDisabled();
    const asunto = datos.getByRole('textbox', { name: 'Asunto' });
    await expect(asunto).toHaveValue(CORREO.asunto);
    await expect(asunto).toBeDisabled();
    // El cuerpo, en solo lectura, se pinta como texto tras su etiqueta, no como textbox.
    await expect(datos.getByRole('textbox', { name: 'Cuerpo' })).toHaveCount(0);
    await expect(datos).toContainText(new RegExp(`Cuerpo\\s*${literal(CORREO.cuerpo)}`));
    await expect(consulta.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    await expect(consulta.getByRole('button', { name: 'Borrar' })).toHaveCount(0);
    const filaAdjunto = consulta.getByRole('region', { name: 'Adjuntos' }).getByRole('row', { name: FICHERO });
    await expect(filaAdjunto).toBeVisible();

    // Paso 4: Cuando pulsa la fila «horario.pdf».
    await filaAdjunto.click();

    // Paso 5: Entonces se abre el adjunto en solo lectura con su nombre y su fichero,
    //         sin «Guardar» ni «Borrar».
    const adjunto = ventanaAdjunto(page);
    await expect(adjunto).toBeVisible();
    const nombreFichero = adjunto.getByRole('textbox', { name: 'Nombre del fichero' });
    await expect(nombreFichero).toHaveValue(FICHERO);
    await expect(nombreFichero).toBeDisabled();
    // El binario en solo lectura se pinta como enlace de descarga, sin botón «Subir».
    const contenido = adjunto.getByTestId('field:contenido');
    const enlaceFichero = contenido.getByRole('button', { name: FICHERO, exact: true });
    await expect(enlaceFichero).toBeVisible();
    await expect(contenido.locator('input[type="file"]')).toHaveCount(0);
    await expect(adjunto.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    await expect(adjunto.getByRole('button', { name: 'Borrar' })).toHaveCount(0);

    // Paso 6: Cuando pulsa el fichero.
    const [descarga] = await Promise.all([page.waitForEvent('download'), enlaceFichero.click()]);

    // Resultado esperado: se descarga el fichero «horario.pdf».
    expect(descarga.suggestedFilename()).toBe(FICHERO);
    expect(await descarga.failure()).toBeNull();

    // Las ventanas se abren como modales sobre el listado: se cierran antes del
    // logout para que el menú de usuario quede accesible.
    await adjunto.getByRole('button', { name: 'Salir' }).click();
    await expect(adjunto).toBeHidden();
    await consulta.getByRole('button', { name: 'Salir' }).click();
    await expect(consulta).toBeHidden();

    await logout(page);
  });
});
