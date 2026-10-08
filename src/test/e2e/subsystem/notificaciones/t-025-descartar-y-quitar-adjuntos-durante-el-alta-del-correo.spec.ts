import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-025 — Descartar y quitar adjuntos durante el alta del correo
// origen: ESC-061  |  verifica: U-notificaciones-todas-015, U-notificaciones-todas-021
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-025-descartar-y-quitar-adjuntos-durante-el-alta-del-correo.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Correo con adjunto
// quitado» lleva un sufijo único por ejecución y la fila se localiza por él.
const MOTIVO = `Correo con adjunto quitado t025-${Date.now()}`;
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

const ventanaAdjunto = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });
const pregunta = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Pregunta', exact: true }) });

// Abre la ventana del adjunto desde el panel y rellena su nombre y su fichero.
async function rellenarAdjunto(page: Page, adjuntos: Locator, nombre: string): Promise<Locator> {
  await adjuntos.getByRole('button', { name: 'Añadir adjunto' }).click();
  const ventana = ventanaAdjunto(page);
  await expect(ventana).toBeVisible();
  await ventana.getByRole('textbox', { name: 'Nombre del fichero' }).fill(nombre);
  // El widget binario esconde su `input[type=file]` tras el botón «Subir».
  await ventana
    .getByTestId('field:contenido')
    .locator('input[type="file"]')
    .setInputFiles({ name: FICHERO, mimeType: 'application/pdf', buffer: HORARIO_PDF });
  await expect(ventana.getByRole('button', { name: FICHERO })).toBeVisible();
  return ventana;
}

async function anadirAdjunto(page: Page, adjuntos: Locator, nombre: string): Promise<void> {
  const ventana = await rellenarAdjunto(page, adjuntos, nombre);
  await ventana.getByRole('button', { name: 'Guardar' }).click();
  await expect(ventana).toBeHidden();
  await expect(adjuntos.getByRole('row', { name: nombre })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Descartar y quitar adjuntos durante el alta del correo', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador ha iniciado sesión, abre el alta de un correo y
    //         rellena el correo de referencia con el motivo «Correo con adjunto quitado»,
    //         el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
    await login(page, 'admin', 'admin');
    await page.getByText('Notificaciones', { exact: true }).click();
    await page.getByText('Todas', { exact: true }).click();
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();
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

    // Paso 2: Cuando pulsa «Añadir adjunto», escribe «borrador.pdf», sube «horario.pdf»
    //         y pulsa «Cancelar» en la ventana del adjunto.
    const adjuntos = form.getByRole('region', { name: 'Adjuntos' });
    const ventanaBorrador = await rellenarAdjunto(page, adjuntos, 'borrador.pdf');
    await ventanaBorrador.getByRole('button', { name: 'Cancelar' }).click();
    // Axelor pide confirmar que se descartan los cambios de la ventana.
    await expect(pregunta(page)).toContainText('Los cambios actuales se perderán');
    await pregunta(page).getByRole('button', { name: 'Aceptar' }).click();

    // Paso 3: Entonces la ventana se cierra y el panel «Adjuntos» no muestra «borrador.pdf».
    await expect(ventanaBorrador).toBeHidden();
    await expect(adjuntos.getByRole('row', { name: 'borrador.pdf' })).toHaveCount(0);

    // Paso 4: Cuando añade el adjunto «horario.pdf» (fichero «horario.pdf») y el adjunto
    //         «notas.pdf» (fichero «horario.pdf»), pulsando «Guardar» en cada ventana.
    await anadirAdjunto(page, adjuntos, 'horario.pdf');
    await anadirAdjunto(page, adjuntos, 'notas.pdf');

    // Paso 5: Y pulsa la fila «notas.pdf» y, en la ventana del adjunto, pulsa «Borrar».
    await adjuntos.getByRole('row', { name: 'notas.pdf' }).click();
    const ventanaNotas = ventanaAdjunto(page);
    await expect(ventanaNotas.getByRole('textbox', { name: 'Nombre del fichero' })).toHaveValue('notas.pdf');
    await ventanaNotas.getByRole('button', { name: 'Borrar' }).click();
    // Axelor pide confirmar la eliminación del registro.
    await expect(pregunta(page)).toContainText('¿Realmente quieres eliminar');
    await pregunta(page).getByRole('button', { name: 'Eliminar' }).click();
    await expect(ventanaNotas).toBeHidden();

    // Paso 6: Entonces el panel «Adjuntos» muestra «horario.pdf» y no «notas.pdf».
    await expect(adjuntos.getByRole('row', { name: 'horario.pdf' })).toBeVisible();
    await expect(adjuntos.getByRole('row', { name: 'notas.pdf' })).toHaveCount(0);

    // Paso 7: Cuando pulsa «Guardar» en el formulario del correo y pulsa la fila
    //         «Correo con adjunto quitado» del listado.
    await form.getByRole('button', { name: 'Guardar' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    const fila = page.getByRole('row', { name: MOTIVO });
    await expect(fila).toBeVisible();
    await fila.click();

    // Resultado esperado: el panel «Adjuntos» muestra solo «horario.pdf» y no muestra
    // el botón «Añadir adjunto».
    const consulta = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
    const adjuntosGuardados = consulta.getByRole('region', { name: 'Adjuntos' });
    await expect(adjuntosGuardados).toBeVisible();
    await expect(adjuntosGuardados.getByRole('gridcell', { name: 'horario.pdf', exact: true })).toBeVisible();
    // «solo»: la única fila de datos es la de «horario.pdf» (la otra es la cabecera).
    const filasDatos = adjuntosGuardados.getByRole('row').filter({ hasNot: page.getByRole('columnheader') });
    await expect(filasDatos).toHaveCount(1);
    await expect(filasDatos).toHaveText(/horario\.pdf/);
    await expect(adjuntosGuardados.getByRole('row', { name: 'notas.pdf' })).toHaveCount(0);
    await expect(adjuntosGuardados.getByRole('row', { name: 'borrador.pdf' })).toHaveCount(0);
    await expect(adjuntosGuardados.getByRole('button', { name: 'Añadir adjunto' })).toHaveCount(0);

    // El formulario se abre como ventana modal sobre el listado: se cierra antes
    // del logout para que el menú de usuario quede accesible.
    await consulta.getByRole('button', { name: 'Salir' }).click();
    await expect(consulta).toBeHidden();

    await logout(page);
  });
});
