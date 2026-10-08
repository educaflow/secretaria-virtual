import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-026 — Adjunto de más de 10 MB
// origen: ESC-062  |  verifica: U-notificaciones-todas-019, V-Adjunto-008
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-026-adjunto-de-mas-de-10-mb.desc.md

// Idempotencia: el test no guarda nada en la BD (la plataforma rechaza el fichero
// en el navegador y el correo nunca se guarda: se descarta recargando la página),
// así que no necesita teardown ni nombres únicos.

const CORREO = {
  centro: 'CIPFP Mislata',
  motivo: 'Adjunto grande',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Horario',
  cuerpo: 'Le adjuntamos su horario.',
};
const FICHERO = 'grande.pdf';

// «grande.pdf» de 11 MB generado en memoria (cabecera PDF + relleno): el test no
// depende de ningún fichero del árbol. El límite lo fija `data.upload.max-size = 10`.
const GRANDE_PDF = Buffer.concat([
  Buffer.from('%PDF-1.4\n', 'latin1'),
  Buffer.alloc(11 * 1024 * 1024, 0x20),
  Buffer.from('\n%%EOF\n', 'latin1'),
]);

test.describe('Notificaciones — Todas', () => {
  test('Adjunto de más de 10 MB', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador ha iniciado sesión, abre el alta de un correo y
    //         rellena el correo de referencia con el motivo «Adjunto grande», el asunto
    //         «Horario» y el cuerpo «Le adjuntamos su horario.».
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
    await form.getByRole('textbox', { name: 'Motivo' }).fill(CORREO.motivo);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(CORREO.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
    await form.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
    await form.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
    await form.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);

    // Paso 2: Cuando en el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de
    //         fichero «grande.pdf» e intenta subir «grande.pdf» (11 MB).
    const adjuntos = form.getByRole('region', { name: 'Adjuntos' });
    await adjuntos.getByRole('button', { name: 'Añadir adjunto' }).click();
    const ventanaAdjunto = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });
    await expect(ventanaAdjunto).toBeVisible();
    const nombre = ventanaAdjunto.getByRole('textbox', { name: 'Nombre del fichero' });
    await nombre.fill(FICHERO);
    await expect(nombre).toHaveValue(FICHERO);
    // El widget binario esconde su `input[type=file]` tras el botón «Subir».
    await ventanaAdjunto
      .getByTestId('field:contenido')
      .locator('input[type="file"]')
      .setInputFiles({ name: FICHERO, mimeType: 'application/pdf', buffer: GRANDE_PDF });

    // Resultado esperado: la plataforma avisa de que no se puede subir un fichero de más
    // de 10 MB (texto de la plataforma, traducido por el proyecto)...
    await expect(page.getByText('No tienes permiso para subir un archivo mayor a 10 MB.')).toBeVisible();
    // ...y el fichero no queda adjunto: el widget de la ventana no muestra «grande.pdf»...
    await expect(ventanaAdjunto.getByRole('button', { name: FICHERO })).toHaveCount(0);
    // ...y el panel «Adjuntos» del correo no tiene ninguna fila de «grande.pdf».
    await expect(adjuntos.getByRole('row', { name: FICHERO })).toHaveCount(0);

    // Se descarta el alta (nada se ha guardado) recargando la página, para que el
    // menú de usuario quede accesible para el logout.
    await page.reload();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    await logout(page);
  });
});
