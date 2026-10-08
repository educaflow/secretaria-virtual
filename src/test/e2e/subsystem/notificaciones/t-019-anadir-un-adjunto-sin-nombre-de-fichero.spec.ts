import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-019 — Añadir un adjunto sin nombre de fichero
// origen: ESC-019  |  verifica: V-Adjunto-002, U-notificaciones-todas-017
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-019-anadir-un-adjunto-sin-nombre-de-fichero.desc.md

// Idempotencia: el test no guarda nada en la BD (el adjunto se rechaza y el
// correo nunca se guarda: se descarta recargando la página), así que no necesita
// teardown ni nombres únicos.

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

test.describe('Notificaciones — Todas', () => {
  test('Añadir un adjunto sin nombre de fichero', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador ha iniciado sesión y abre el alta de un correo
    //         («Notificaciones» → «Todas» → «Nueva notificación» → «Correo» → «Continuar»).
    await login(page, 'admin', 'admin');
    await page.getByText('Notificaciones', { exact: true }).click();
    await page.getByText('Todas', { exact: true }).click();
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
    await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();

    // Paso 2: Cuando en el panel «Adjuntos» pulsa «Añadir adjunto», sube «horario.pdf»
    //         y deja vacío el nombre de fichero.
    const adjuntos = form.getByRole('region', { name: 'Adjuntos' });
    await adjuntos.getByRole('button', { name: 'Añadir adjunto' }).click();
    const ventanaAdjunto = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });
    await expect(ventanaAdjunto).toBeVisible();
    // El widget binario esconde su `input[type=file]` tras el botón «Subir».
    await ventanaAdjunto
      .getByTestId('field:contenido')
      .locator('input[type="file"]')
      .setInputFiles({ name: FICHERO, mimeType: 'application/pdf', buffer: HORARIO_PDF });
    const nombre = ventanaAdjunto.getByRole('textbox', { name: /Nombre del fichero/ });
    await nombre.fill('');
    await expect(nombre).toHaveValue('');

    // Paso 3: Y pulsa «Guardar» en la ventana del adjunto.
    await ventanaAdjunto.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra «El nombre del fichero es obligatorio»...
    // Axelor lo pinta en su diálogo de error de acción, encima de la ventana del adjunto.
    const errorAccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' }) });
    await expect(errorAccion.getByText('El nombre del fichero es obligatorio')).toBeVisible();
    await errorAccion.getByRole('button', { name: 'Close dialog' }).click();
    await expect(errorAccion).toBeHidden();
    // ...y no añade el adjunto: la ventana sigue abierta, con el nombre aún vacío...
    await expect(ventanaAdjunto).toBeVisible();
    await expect(nombre).toHaveValue('');
    // ...y el panel «Adjuntos» del correo no tiene ninguna fila de «horario.pdf».
    await expect(adjuntos.getByRole('row', { name: FICHERO })).toHaveCount(0);

    // Se descarta el alta (nada se ha guardado) recargando la página, para que el
    // menú de usuario quede accesible para el logout.
    await page.reload();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    await logout(page);
  });
});
