import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-022 — Adjunto cuyo nombre contiene una barra
// origen: ESC-022  |  verifica: V-Adjunto-009, U-notificaciones-todas-020
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-022-adjunto-cuyo-nombre-contiene-una-barra.desc.md

// Idempotencia: el test no guarda nada en la BD (el adjunto se rechaza y el
// correo nunca se guarda: se descarta recargando la página), así que no necesita
// teardown. Aun así el motivo lleva un sufijo único por ejecución, para no
// confundirse con los correos de referencia que crean otros tests.
const MOTIVO = `Aviso de prueba de correo t022-${Date.now()}`;
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
const NOMBRE_CON_BARRA = 'cursos/horario.pdf';
const MENSAJE = 'El nombre del fichero no puede contener los caracteres / \\ ni caracteres de control';

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

test.describe('Notificaciones — Todas', () => {
  test('Adjunto cuyo nombre contiene una barra', async ({ page }) => {
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

    // Paso 2: Cuando en el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre
    //         de fichero «cursos/horario.pdf» y sube «horario.pdf».
    const adjuntos = form.getByRole('region', { name: 'Adjuntos' });
    await adjuntos.getByRole('button', { name: 'Añadir adjunto' }).click();
    const ventanaAdjunto = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });
    await expect(ventanaAdjunto).toBeVisible();
    // El widget binario esconde su `input[type=file]` tras el botón «Subir».
    await ventanaAdjunto
      .getByTestId('field:contenido')
      .locator('input[type="file"]')
      .setInputFiles({ name: FICHERO, mimeType: 'application/pdf', buffer: HORARIO_PDF });
    // El nombre se escribe tras subir el fichero, por si la subida lo autorrellena.
    const nombre = ventanaAdjunto.getByRole('textbox', { name: /Nombre del fichero/ });
    await nombre.fill(NOMBRE_CON_BARRA);
    await expect(nombre).toHaveValue(NOMBRE_CON_BARRA);

    // Paso 3: Y pulsa «Guardar» en la ventana del adjunto.
    await ventanaAdjunto.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: la ventana del adjunto muestra «El nombre del fichero no puede
    // contener los caracteres / \ ni caracteres de control»...
    // Axelor lo pinta en su diálogo de error de acción, encima de la ventana del adjunto;
    // se localiza por el propio mensaje.
    const errorAccion = page.getByRole('dialog').filter({ hasText: MENSAJE });
    await expect(errorAccion).toBeVisible();
    await expect(errorAccion).toContainText(MENSAJE);
    await errorAccion.getByRole('button', { name: 'Close dialog' }).click();
    await expect(errorAccion).toBeHidden();
    // ...y no añade el adjunto: la ventana sigue abierta con el nombre tal cual...
    await expect(ventanaAdjunto).toBeVisible();
    await expect(nombre).toHaveValue(NOMBRE_CON_BARRA);
    // ...y el panel «Adjuntos» del correo no tiene ninguna fila del adjunto.
    await expect(adjuntos.getByRole('row', { name: FICHERO })).toHaveCount(0);

    // Se descarta el alta (nada se ha guardado) recargando la página, para que el
    // menú de usuario quede accesible para el logout.
    await page.reload();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    await logout(page);
  });
});
