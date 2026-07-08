import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

// T-002 — Alta de un correo con adjunto
// origen: ESC-002  |  verifica: V-Adjunto-004, V-Adjunto-005,
//   U-correos-administracion-listado-adjuntos-001, U-correos-administracion-formulario-adjunto-001,
//   R-Correo-001
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-002-alta-de-un-correo-con-adjunto.desc.md
test.describe('Administración de correos', () => {
  test('Alta de un correo con adjunto', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución para poder identificar SIEMPRE la fila que crea este run, incluso
    // si hay correos de ejecuciones anteriores en la BD.
    const asunto = `Documento adjunto t002-${Date.now()}`;

    // Fichero temporal para el contenido del adjunto: se genera en cada ejecución y
    // se borra en el finally, no se commitea ningún binario al repositorio. El
    // widget de contenido muestra como botón el nombre REAL del fichero subido (no
    // el valor del campo "Nombre del fichero"), así que el fichero temporal se
    // llama literalmente "documento.pdf" (en un directorio único por ejecución
    // para no colisionar entre runs concurrentes).
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'adjunto-t002-'));
    const adjuntoPath = path.join(tmpDir, 'documento.pdf');
    fs.writeFileSync(adjuntoPath, '%PDF-1.4\n%%EOF');

    try {
      // Paso 1: Dado que el administrador está en la pantalla "Administración de
      // correos" y pulsa "Nuevo correo"
      await page.getByText('Correos', { exact: true }).click();
      await page.getByText('Administración de correos', { exact: true }).click();
      await page.getByRole('button', { name: 'Nuevo correo' }).click();

      // Paso 2: Cuando rellena el DNI, el nombre, los apellidos, el «para», el
      // asunto, el cuerpo y elige el centro «CIPFP Mislata»
      await page.getByLabel('DNI del destinatario').fill('86862719E');
      await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
      await page.getByLabel('Apellidos').fill('CIPFP Mislata');
      // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
      await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
      await page.getByLabel('Asunto').fill(asunto);
      await page.getByLabel('Cuerpo').fill('Adjunto el documento solicitado');
      await page.getByLabel('Centro', { exact: true }).click();
      await page.getByRole('option', { name: 'CIPFP Mislata' }).click();

      // Paso 3: Y, en el panel de adjuntos, pulsa "Añadir adjunto"
      await page.getByRole('button', { name: 'Añadir adjunto' }).click();

      // Paso 4: Y rellena el nombre del fichero con «documento.pdf» y sube un
      // fichero como contenido
      const dialogoAdjunto = page.getByRole('dialog');
      await dialogoAdjunto.getByLabel('Nombre del fichero').fill('documento.pdf');
      const fileChooserPromise = page.waitForEvent('filechooser');
      await dialogoAdjunto.getByRole('button', { name: 'Upload' }).click();
      const fileChooser = await fileChooserPromise;
      await fileChooser.setFiles(adjuntoPath);
      await expect(dialogoAdjunto.getByRole('button', { name: 'documento.pdf' })).toBeVisible();

      // Paso 5: Y pulsa "Guardar" del adjunto
      await dialogoAdjunto.getByRole('button', { name: 'Guardar' }).click();
      await expect(dialogoAdjunto).not.toBeVisible();

      // El adjunto queda listado en el panel del correo antes de guardar el correo.
      await expect(page.getByRole('row', { name: 'documento.pdf' })).toBeVisible();

      // Paso 6: Y pulsa "Guardar" en el correo
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Paso 7 / Resultado esperado: el sistema crea el correo con un adjunto
      // llamado «documento.pdf» asociado y se intenta su envío asíncrono (el
      // listado se ordena por fecha de creación descendente, así que la fila del
      // correo recién creado es identificable por su asunto único).
      const row = page.getByRole('row', { name: asunto });
      await expect(row).toBeVisible();
      await expect(row.getByRole('gridcell', { name: /Pendiente|Enviado/ })).toBeVisible();

      // Abre el detalle del correo recién creado para verificar el adjunto asociado.
      await row.click();
      await page.waitForURL(/\/edit\/\d+/);
      await expect(page.getByRole('row', { name: 'documento.pdf' })).toBeVisible();

      // "Pendiente" es el estado que se asigna SÍNCRONAMENTE al crear el correo
      // (R-Correo-001), antes de cualquier intento de envío, así que comprobarlo
      // justo tras guardar no demuestra que el envío asíncrono se haya intentado.
      // El número de reintentos, en cambio, solo se incrementa (de 0 a 1) al
      // completar un intento real de envío (con independencia de si acaba en éxito
      // o en fallo) — se recarga en bucle hasta verlo, igual que T-001 espera hasta
      // "Enviado".
      await expect(async () => {
        await page.reload();
        await expect(page.getByLabel(/reintentos/i)).not.toHaveValue('0');
      }).toPass({ timeout: 60_000, intervals: [2_000] });

      // Tras el intento, el estado ya no puede seguir siendo "Pendiente": el correo
      // fue enviado ("Enviado") o el intento falló ("Fallido").
      await expect(page.getByRole('combobox', { name: 'Estado' })).not.toHaveValue('Pendiente');

      // El adjunto sigue asociado tras el intento de envío (la recarga no lo pierde).
      await expect(page.getByRole('row', { name: 'documento.pdf' })).toBeVisible();

      // Teardown: un correo cuyo envío asíncrono ya se ha intentado (Pendiente o ya
      // Enviado) no se puede borrar desde la UI (el formulario de consulta solo
      // ofrece "Salir", no hay botón de borrado — es un dato inmutable de
      // auditoría, igual que en T-001). Se confía en el asunto único para no
      // colisionar entre ejecuciones.
      await page.getByRole('button', { name: 'Salir' }).click();
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    }

    await logout(page);
  });
});
