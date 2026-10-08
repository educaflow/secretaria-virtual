import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-021 — Alta de un correo con dos adjuntos con el mismo nombre
// origen: ESC-021  |  verifica: V-Adjunto-013
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-021-alta-de-un-correo-con-dos-adjuntos-con-el-mismo-nombre.desc.md

// Idempotencia: el alta se rechaza, así que el test no deja nada en la BD y no
// necesita teardown. Aun así el motivo lleva un sufijo único por ejecución para
// poder afirmar sin ambigüedad que NO se ha creado la notificación (la BD es
// compartida y otros tests crean correos con el motivo de referencia).
const MOTIVO = `Aviso de prueba de correo t021-${Date.now()}`;
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
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

async function anadirAdjunto(page: Page, adjuntos: Locator): Promise<void> {
  await adjuntos.getByRole('button', { name: 'Añadir adjunto' }).click();
  const ventanaAdjunto = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });
  await expect(ventanaAdjunto).toBeVisible();
  await ventanaAdjunto.getByRole('textbox', { name: 'Nombre del fichero' }).fill(FICHERO);
  // El widget binario esconde su `input[type=file]` tras el botón «Subir».
  await ventanaAdjunto
    .getByTestId('field:contenido')
    .locator('input[type="file"]')
    .setInputFiles({ name: FICHERO, mimeType: 'application/pdf', buffer: HORARIO_PDF });
  await ventanaAdjunto.getByRole('button', { name: 'Guardar' }).click();
  await expect(ventanaAdjunto).toBeHidden();
}

test.describe('Notificaciones — Todas', () => {
  test('Alta de un correo con dos adjuntos con el mismo nombre', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador ha iniciado sesión, abre el alta de un correo
    //         y rellena el correo de referencia.
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

    // Paso 2: Cuando añade un adjunto con nombre «horario.pdf» y el fichero «horario.pdf»
    //         y pulsa «Guardar» en su ventana.
    const adjuntos = form.getByRole('region', { name: 'Adjuntos' });
    await anadirAdjunto(page, adjuntos);
    await expect(adjuntos.getByRole('row', { name: FICHERO })).toHaveCount(1);

    // Paso 3: Y añade otro adjunto con nombre «horario.pdf» y el fichero «horario.pdf»
    //         y pulsa «Guardar» en su ventana.
    await anadirAdjunto(page, adjuntos);
    await expect(adjuntos.getByRole('row', { name: FICHERO })).toHaveCount(2);

    // Paso 4: Y pulsa «Guardar» en el formulario del correo.
    await form.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra «Ya existe un adjunto con ese nombre en el correo»...
    // El error lo levanta la validación de los adjuntos (hijos del correo) al guardar en
    // cascada, y Axelor lo pinta en un diálogo de error con una línea por adjunto
    // («Adjuntos[#1] › …»); se localiza el diálogo por el propio mensaje, no por su título.
    const MENSAJE = 'Ya existe un adjunto con ese nombre en el correo';
    const errorAccion = page.getByRole('dialog').filter({ hasText: MENSAJE });
    await expect(errorAccion).toBeVisible();
    await expect(errorAccion).toContainText(MENSAJE);
    await errorAccion.getByRole('button', { name: 'Close dialog' }).click();
    await expect(errorAccion).toBeHidden();
    // ...y no crea la notificación: el formulario del correo sigue abierto sin guardar...
    await expect(form).toBeVisible();

    // ...y, descartada el alta recargando la página, el listado no tiene ninguna fila con su motivo.
    await page.reload();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Motivo' })).toBeVisible();
    await expect(page.getByRole('row', { name: MOTIVO })).toHaveCount(0);

    await logout(page);
  });
});
