import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

// T-010 — Alta con adjunto sin nombre de fichero
// origen: ESC-016  |  verifica: V-Adjunto-004, U-correos-administracion-formulario-adjunto-005
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-010-alta-con-adjunto-sin-nombre-de-fichero.desc.md
test.describe('Administración de correos', () => {
  test('Alta con adjunto sin nombre de fichero', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución. Al ser un test de validación negativa, el correo nunca llega a
    // crearse, así que no hay que borrar nada al final: el sufijo único solo sirve
    // para identificar de forma inequívoca la fila (ausente) en el listado.
    const asunto = `Documento adjunto t010-${Date.now()}`;

    // Fichero temporal para el contenido del adjunto: se genera en cada ejecución y
    // se borra en el finally, no se commitea ningún binario al repositorio (mismo
    // patrón que T-002).
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'adjunto-t010-'));
    const adjuntoPath = path.join(tmpDir, 'documento.pdf');
    fs.writeFileSync(adjuntoPath, '%PDF-1.4\n%%EOF');

    try {
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

      // Paso 3: Y sube un fichero como contenido y deja vacío el nombre del fichero.
      const fileChooserPromise = page.waitForEvent('filechooser');
      await dialogoAdjunto.getByRole('button', { name: 'Upload' }).click();
      const fileChooser = await fileChooserPromise;
      await fileChooser.setFiles(adjuntoPath);
      await expect(dialogoAdjunto.getByRole('button', { name: 'documento.pdf' })).toBeVisible();
      // El campo "Nombre del fichero" se deja deliberadamente vacío (no se rellena).

      // Paso 4: Y pulsa "Guardar" del adjunto.
      await dialogoAdjunto.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado (parte 1): el sistema muestra el mensaje "El nombre del
      // fichero es obligatorio" (validación inline bajo el propio campo, dentro del
      // diálogo de adjunto).
      await expect(dialogoAdjunto.getByText('El nombre del fichero es obligatorio')).toBeVisible();
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

      // El panel de adjuntos del correo queda vacío: el adjunto sin nombre de
      // fichero nunca llegó a añadirse.
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
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    }

    await logout(page);
  });
});
