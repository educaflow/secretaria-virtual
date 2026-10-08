import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-048 — Consultar y descargar un adjunto de un correo guardado
// origen: ESC-067  |  verifica: R-Correo-001, U-notificaciones-todas-015
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-048-consultar-y-descargar-un-adjunto-de-un-correo-guardado.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Envío de horario» lleva
// un sufijo único por ejecución y todas las aserciones localizan la fila por él.
const MOTIVO = `Envío de horario t048-${Date.now()}`;
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

const ventanaCorreo = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
const ventanaAdjunto = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Consultar y descargar un adjunto de un correo guardado', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión, abre el alta de un correo y
    //         rellena el correo de referencia con el motivo «Envío de horario», el asunto
    //         «Horario» y el cuerpo «Le adjuntamos su horario.».
    await login(page, 'admin', 'admin');
    await abrirTodas(page);
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

    // Paso 2: Cuando en «Adjuntos» pulsa «Añadir adjunto», escribe «horario.pdf», sube
    //         «horario.pdf», pulsa «Guardar» en la ventana del adjunto y «Guardar» en el correo.
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

    // Paso 3: Y en el listado pulsa la fila «Envío de horario».
    const fila = page.getByRole('row', { name: MOTIVO });
    await expect(fila).toBeVisible();
    await fila.click();

    // Paso 4: Entonces el panel «Adjuntos» muestra la fila «horario.pdf».
    const consulta = ventanaCorreo(page);
    await expect(consulta.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO);
    const adjuntosGuardados = consulta.getByRole('region', { name: 'Adjuntos' });
    const filaAdjunto = adjuntosGuardados.getByRole('row', { name: FICHERO });
    await expect(filaAdjunto).toBeVisible();

    // Paso 5: Cuando pulsa la fila «horario.pdf».
    await filaAdjunto.click();

    // Paso 6: Entonces se abre el adjunto en solo lectura con el nombre de fichero
    //         «horario.pdf» y su fichero, sin «Guardar» ni «Borrar».
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

    // Paso 7: Cuando pulsa el fichero y lo descarga, y después pulsa «Salir».
    const [descarga] = await Promise.all([page.waitForEvent('download'), enlaceFichero.click()]);

    // Resultado esperado: el fichero descargado se llama «horario.pdf».
    expect(descarga.suggestedFilename()).toBe(FICHERO);

    await adjunto.getByRole('button', { name: 'Salir' }).click();

    // Resultado esperado: el adjunto se cierra y vuelve a verse el formulario del
    // correo «Envío de horario».
    await expect(adjunto).toBeHidden();
    await expect(consulta).toBeVisible();
    await expect(consulta.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
    await expect(consulta.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO);
    await expect(consulta.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(/^Envío de horario/);

    // El formulario se abre como ventana modal sobre el listado: se cierra antes
    // del logout para que el menú de usuario quede accesible.
    await consulta.getByRole('button', { name: 'Salir' }).click();
    await expect(consulta).toBeHidden();

    await logout(page);
  });
});
