import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-011 — Alta con adjunto sin contenido
// origen: ESC-017  |  verifica: V-Adjunto-005, U-correos-administracion-formulario-adjunto-006
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-011-alta-con-adjunto-sin-contenido.desc.md
test.describe('Administración de correos', () => {
  test('Alta con adjunto sin contenido', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución. Al ser un test de validación negativa, el correo nunca llega a
    // crearse, así que no hay que borrar nada al final: el sufijo único solo sirve
    // para identificar de forma inequívoca la fila (ausente) en el listado.
    const asunto = `Documento adjunto t011-${Date.now()}`;

    // Paso 1: Dado que el administrador pulsa "Nuevo correo" y rellena el DNI, el
    // nombre, los apellidos, el «para», el asunto, el cuerpo y elige el centro
    // «CIPFP Mislata».
    await page.getByTestId('item:correos-menuitem').getByText('Correos', { exact: true }).click();
    await page.getByText('Administración de correos', { exact: true }).click();
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
    await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
    await page.getByLabel('Asunto').fill(asunto);
    await page.getByLabel('Cuerpo').fill('Adjunto el documento solicitado');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();

    // Paso 2: Cuando, en el panel de adjuntos, pulsa "Añadir adjunto".
    await page.getByRole('button', { name: 'Añadir adjunto' }).click();
    const dialogoAdjunto = page.getByRole('dialog');

    // Paso 3: Y rellena el nombre del fichero con «documento.pdf» y deja vacío el
    // contenido (no sube ningún fichero).
    await dialogoAdjunto.getByLabel('Nombre del fichero').fill('documento.pdf');
    // El campo "Contenido" se deja deliberadamente vacío (no se pulsa "Upload").

    // Paso 4: Y pulsa "Guardar" del adjunto.
    await dialogoAdjunto.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado (parte 1): el sistema muestra el mensaje "Debe adjuntar el
    // fichero" (validación inline bajo el propio campo "Contenido", dentro del
    // diálogo de adjunto).
    await expect(dialogoAdjunto.getByText('Debe adjuntar el fichero')).toBeVisible();
    // La validación falla dentro del propio diálogo: no se cierra ni se añade el
    // adjunto al panel del correo. Al seguir abierto como modal, intercepta
    // cualquier clic sobre el formulario del correo (incluido su "Guardar"), así
    // que el paso 5 del escenario ("pulsa Guardar en el correo") queda bloqueado
    // por el propio diálogo: la comprobación de que el correo no se crea se hace
    // tras descartar el diálogo y el formulario, verificando el listado.
    await expect(dialogoAdjunto).toBeVisible();

    // Descarta el diálogo de adjunto (confirmando la pérdida de cambios) para
    // volver al formulario del correo.
    await dialogoAdjunto.getByRole('button', { name: 'Cancelar' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'OK' }).click();

    // El panel de adjuntos del correo queda vacío: el adjunto sin contenido nunca
    // llegó a añadirse.
    await expect(page.getByRole('row', { name: 'documento.pdf' })).toHaveCount(0);
    // La pestaña conserva el asterisco de "sin guardar" y la URL sigue en el
    // formulario de alta (no ha navegado a .../edit/<id>), confirmando que el
    // correo tampoco se ha guardado.
    await expect(page.getByRole('tab', { name: 'Correo*' })).toBeVisible();
    await expect(page).not.toHaveURL(/\/edit\/\d+/);

    // Descarta el formulario del correo (confirmando la pérdida de cambios) para
    // volver al listado y comprobar de forma directa que no se ha creado ninguna
    // fila con este asunto único. Se escopa al tabpanel del correo porque el
    // diálogo de adjunto ya descartado deja en el DOM un botón "Cancelar" residual
    // (oculto) que, si no se acota, provoca un choque de selector (strict mode).
    await page.getByRole('tabpanel', { name: 'Correo*' }).getByRole('button', { name: 'Cancelar' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'OK' }).click();

    // Resultado esperado (parte 2): el sistema no crea el correo.
    await expect(page.getByRole('row', { name: asunto })).toHaveCount(0);

    await logout(page);
  });
});
