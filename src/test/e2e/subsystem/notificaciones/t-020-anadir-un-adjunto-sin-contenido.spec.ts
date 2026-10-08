import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-020 — Añadir un adjunto sin contenido
// origen: ESC-020  |  verifica: V-Adjunto-003, U-notificaciones-todas-018
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-020-anadir-un-adjunto-sin-contenido.desc.md

// Idempotencia: el test no guarda nada en la BD (el adjunto se rechaza y el
// correo nunca se guarda: se descarta recargando la página), así que no necesita
// teardown ni nombres únicos.

const FICHERO = 'horario.pdf';

test.describe('Notificaciones — Todas', () => {
  test('Añadir un adjunto sin contenido', async ({ page }) => {
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

    // Paso 2: Cuando en el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre
    //         de fichero «horario.pdf» y no sube ningún fichero.
    const adjuntos = form.getByRole('region', { name: 'Adjuntos' });
    await adjuntos.getByRole('button', { name: 'Añadir adjunto' }).click();
    const ventanaAdjunto = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Adjunto', exact: true }) });
    await expect(ventanaAdjunto).toBeVisible();
    const nombre = ventanaAdjunto.getByRole('textbox', { name: /Nombre del fichero/ });
    await nombre.fill(FICHERO);
    await expect(nombre).toHaveValue(FICHERO);

    // Paso 3: Y pulsa «Guardar» en la ventana del adjunto.
    await ventanaAdjunto.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra «Debe adjuntar el fichero»...
    // Axelor lo pinta en su diálogo de error de acción, encima de la ventana del adjunto.
    const errorAccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' }) });
    await expect(errorAccion.getByText('Debe adjuntar el fichero')).toBeVisible();
    await errorAccion.getByRole('button', { name: 'Close dialog' }).click();
    await expect(errorAccion).toBeHidden();
    // ...y no añade el adjunto: la ventana sigue abierta con el nombre escrito...
    await expect(ventanaAdjunto).toBeVisible();
    await expect(nombre).toHaveValue(FICHERO);
    // ...y el panel «Adjuntos» del correo no tiene ninguna fila de «horario.pdf».
    await expect(adjuntos.getByRole('row', { name: FICHERO })).toHaveCount(0);

    // Se descarta el alta (nada se ha guardado) recargando la página, para que el
    // menú de usuario quede accesible para el logout.
    await page.reload();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    await logout(page);
  });
});
