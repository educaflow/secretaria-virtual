import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

// T-023 — El destinatario consulta un correo enviado con éxito y descarga su adjunto
// origen: ESC-008  |  verifica: — (permiso Correo.propio-destinatario/Adjunto.propio-destinatario, ver design.md paso 10)
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-023-el-destinatario-consulta-un-correo-enviado-con-exito-y-descarga-su-adjunto.desc.md
test.describe('Mis correos', () => {
  test('El destinatario consulta un correo enviado con éxito y descarga su adjunto', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución para poder identificar SIEMPRE la fila que crea este run, incluso si
    // hay correos de ejecuciones anteriores en la BD.
    const asunto = `Tu certificado t023-${Date.now()}`;

    // Fichero temporal para el contenido del adjunto: se genera en cada ejecución y
    // se borra en el finally, no se commitea ningún binario al repositorio. El
    // widget de contenido muestra como botón el nombre REAL del fichero subido (no
    // el valor del campo "Nombre del fichero"), y ese mismo nombre es el que
    // Playwright reporta como `suggestedFilename()` al descargarlo — por eso el
    // fichero temporal se llama literalmente "certificado.pdf" (en un directorio
    // único por ejecución para no colisionar entre runs concurrentes), igual que T-022.
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'adjunto-t023-'));
    const adjuntoPath = path.join(tmpDir, 'certificado.pdf');
    const contenidoAdjunto = '%PDF-1.4\n%%EOF';
    fs.writeFileSync(adjuntoPath, contenidoAdjunto);
    // Ruta donde se guardará el fichero descargado, para poder comparar su
    // contenido byte a byte contra `contenidoAdjunto` (el mismo directorio
    // temporal único por ejecución, así que se limpia junto con `adjuntoPath`).
    const descargaPath = path.join(tmpDir, 'certificado-descargado.pdf');

    try {
      // Paso 1: Dado que el administrador ha iniciado sesión y crea un correo
      // dirigido al DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata»,
      // «para» «alumno1@mislata.es», asunto «Tu certificado», cuerpo «Adjunto tu
      // certificado», un adjunto llamado «certificado.pdf» y centro «CIPFP
      // Mislata», y cierra sesión.
      await page.getByText('Correos', { exact: true }).click();
      await page.getByText('Administración de correos', { exact: true }).click();
      await page.getByRole('button', { name: 'Nuevo correo' }).click();

      await page.getByLabel('DNI del destinatario').fill('86862719E');
      await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
      await page.getByLabel('Apellidos').fill('CIPFP Mislata');
      // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
      await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
      await page.getByLabel('Asunto').fill(asunto);
      await page.getByLabel('Cuerpo').fill('Adjunto tu certificado');
      await page.getByLabel('Centro', { exact: true }).click();
      await page.getByRole('option', { name: 'CIPFP Mislata' }).click();

      await page.getByRole('button', { name: 'Añadir adjunto' }).click();

      const dialogoAdjunto = page.getByRole('dialog');
      await dialogoAdjunto.getByLabel('Nombre del fichero').fill('certificado.pdf');
      const fileChooserPromise = page.waitForEvent('filechooser');
      await dialogoAdjunto.getByRole('button', { name: 'Upload' }).click();
      const fileChooser = await fileChooserPromise;
      await fileChooser.setFiles(adjuntoPath);
      await expect(dialogoAdjunto.getByRole('button', { name: 'certificado.pdf' })).toBeVisible();

      await dialogoAdjunto.getByRole('button', { name: 'Guardar' }).click();
      await expect(dialogoAdjunto).not.toBeVisible();

      // El adjunto queda listado en el panel del correo antes de guardar el correo.
      await expect(page.getByRole('row', { name: 'certificado.pdf' })).toBeVisible();

      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('row', { name: asunto })).toBeVisible();

      await logout(page);

      // Paso 2: Cuando el usuario «alumno1@mislata.es» inicia sesión con
      // contraseña «demo1234».
      await login(page, 'alumno1@mislata.es', 'demo1234');

      // Paso 3: Y abre la pantalla "Mis correos".
      // El menú "Mis correos" tiene un único submenú con el mismo texto (no es un
      // rol "menuitem", es un elemento genérico, igual que el patrón de "Correos"
      // → "Administración de correos"), de ahí el índice para distinguir el
      // elemento de menú superior del submenú.
      await page.getByText('Mis correos', { exact: true }).nth(0).click();
      await page.getByText('Mis correos', { exact: true }).nth(1).click();
      await page.waitForURL(/Correo%40Mis/);

      // La pantalla "Mis correos" solo muestra los correos ya enviados con éxito
      // (estado SUCCESS): el envío es asíncrono, así que se reintenta la recarga
      // hasta que la fila aparezca, igual que en T-001/T-002/T-022.
      const filaCorreo = page.getByRole('row', { name: asunto });
      await expect(async () => {
        await page.reload();
        await expect(filaCorreo).toBeVisible();
      }).toPass({ timeout: 60_000, intervals: [2_000] });

      // Paso 4: Y pulsa la fila del correo «Tu certificado» para abrir su detalle.
      await filaCorreo.click();
      await page.waitForURL(/\/edit\/\d+/);

      // Resultado esperado: el sistema muestra el correo «Tu certificado» en solo
      // lectura, con su asunto, cuerpo y fecha de envío.
      await expect(page.getByText(asunto, { exact: true })).toBeVisible();
      await expect(page.getByText('Adjunto tu certificado', { exact: true })).toBeVisible();
      await expect(page.getByLabel('Fecha de envío')).not.toHaveValue('');
      // El formulario de "Mi correo" es de solo lectura: no ofrece más botón que "Salir".
      await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Guardar' })).toHaveCount(0);

      // Paso 5: Y, en el panel de adjuntos, descarga el adjunto «certificado.pdf».
      await expect(page.getByRole('row', { name: 'certificado.pdf' })).toBeVisible();
      await page.getByRole('row', { name: 'certificado.pdf' }).click();
      const dialogoAdjuntoDetalle = page.getByRole('dialog');
      await expect(dialogoAdjuntoDetalle.getByLabel('Nombre del fichero')).toHaveValue('certificado.pdf');

      // Resultado esperado: el sistema descarga el fichero «certificado.pdf» con el
      // mismo contenido que se subió como adjunto (no solo el nombre): se guarda el
      // fichero descargado y se compara su contenido byte a byte contra
      // `contenidoAdjunto`, para que un backend que sirva bytes incorrectos o
      // vacíos con el nombre correcto NO pase el test.
      const downloadPromise = page.waitForEvent('download');
      await dialogoAdjuntoDetalle.getByRole('button', { name: 'certificado.pdf' }).click();
      const download = await downloadPromise;
      expect(download.suggestedFilename()).toBe('certificado.pdf');

      await download.saveAs(descargaPath);
      const contenidoDescargado = fs.readFileSync(descargaPath, 'utf-8');
      expect(contenidoDescargado).toBe(contenidoAdjunto);

      await dialogoAdjuntoDetalle.getByRole('button', { name: 'Salir' }).click();
      await expect(dialogoAdjuntoDetalle).not.toBeVisible();

      // El formulario de "Mi correo" no tiene botón de borrado (es de solo lectura
      // para el destinatario), así que se sale con "Salir".
      await page.getByRole('button', { name: 'Salir' }).click();

      await logout(page);
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    }

    // Teardown: el correo creado por el administrador queda enviado (Enviado) y no
    // se puede borrar desde la UI una vez que el envío asíncrono se ha intentado
    // (el formulario de consulta de "Administración de correos" solo ofrece
    // "Reenviar"/"Salir" — es un dato inmutable de auditoría, igual que en
    // T-001/T-002/T-019/T-020/T-022). Se confía en el asunto único para no
    // colisionar entre ejecuciones; no es un error tapado, es la excepción
    // documentada por el contrato (§4.5).
  });
});
