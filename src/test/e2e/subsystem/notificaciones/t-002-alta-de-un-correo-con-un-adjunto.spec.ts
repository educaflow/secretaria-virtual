import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-002 — Alta de un correo con un adjunto
// origen: ESC-002  |  verifica: R-Correo-001, R-Adjunto-001, U-notificaciones-todas-015, U-notificaciones-todas-016
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-002-alta-de-un-correo-con-un-adjunto.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Envío de horario» lleva
// un sufijo único por ejecución y todas las aserciones localizan la fila por él.
const MOTIVO = `Envío de horario t002-${Date.now()}`;
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
const COL = { tipo: 1, motivo: 2, destino: 6 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);
const filaDelCorreo = (page: Page): Locator => page.getByRole('row', { name: MOTIVO });

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Alta de un correo con un adjunto', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión.
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    // Paso 3: Y rellena el correo de referencia con el motivo «Envío de horario»,
    //         el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
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

    // Paso 4: Y en el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de
    //         fichero «horario.pdf», sube «horario.pdf» y pulsa «Guardar» en la ventana del adjunto.
    const adjuntos = form.getByRole('region', { name: 'Adjuntos' });
    await adjuntos.getByRole('button', { name: 'Añadir adjunto' }).click();
    const ventanaAdjunto = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });
    await expect(ventanaAdjunto).toBeVisible();
    await ventanaAdjunto.getByRole('textbox', { name: 'Nombre del fichero' }).fill(FICHERO);
    // El widget binario esconde su `input[type=file]` tras el botón «Subir»;
    // `setInputFiles` escribe en él con el fichero en memoria.
    await ventanaAdjunto
      .getByTestId('field:contenido')
      .locator('input[type="file"]')
      .setInputFiles({ name: FICHERO, mimeType: 'application/pdf', buffer: HORARIO_PDF });
    await ventanaAdjunto.getByRole('button', { name: 'Guardar' }).click();
    await expect(ventanaAdjunto).toBeHidden();
    await expect(adjuntos.getByRole('row', { name: FICHERO })).toBeVisible();

    // Paso 5: Y pulsa «Guardar» en el formulario del correo.
    await form.getByRole('button', { name: 'Guardar' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    // Resultado esperado: el listado muestra «Envío de horario» con tipo «Correo»
    // y destino «alumno1@mislata.es».
    const fila = filaDelCorreo(page);
    await expect(fila).toBeVisible();
    await expect(celda(fila, COL.motivo)).toHaveText(MOTIVO);
    await expect(celda(fila, COL.motivo)).toContainText('Envío de horario');
    await expect(celda(fila, COL.tipo)).toHaveText('Correo');
    await expect(celda(fila, COL.destino)).toHaveText(CORREO.para);

    // Paso 6: Y pulsa la fila «Envío de horario».
    await fila.click();

    // Resultado esperado: el formulario del correo muestra el adjunto «horario.pdf»
    // en el panel «Adjuntos».
    const adjuntosGuardados = page.getByRole('region', { name: 'Adjuntos' });
    await expect(adjuntosGuardados).toBeVisible();
    await expect(adjuntosGuardados.getByRole('row', { name: FICHERO })).toBeVisible();
    await expect(adjuntosGuardados.getByRole('gridcell', { name: FICHERO, exact: true })).toBeVisible();

    // El formulario se abre como ventana modal sobre el listado: se cierra antes
    // del logout para que el menú de usuario quede accesible.
    const consulta = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
    await consulta.getByRole('button', { name: 'Salir' }).click();
    await expect(consulta).toBeHidden();

    await logout(page);
  });
});
