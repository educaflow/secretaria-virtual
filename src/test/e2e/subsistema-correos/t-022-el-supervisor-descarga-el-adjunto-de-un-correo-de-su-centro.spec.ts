import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

// T-022 — El supervisor descarga el adjunto de un correo de su centro
// origen: ESC-018  |  verifica: U-correos-administracion-formulario-adjunto-007
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-022-el-supervisor-descarga-el-adjunto-de-un-correo-de-su-centro.desc.md
test.describe('Correos de mi centro', () => {
  test('El supervisor descarga el adjunto de un correo de su centro', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución para poder identificar SIEMPRE la fila que crea este run, incluso si
    // hay correos de ejecuciones anteriores en la BD.
    const asunto = `Circular con adjunto t022-${Date.now()}`;

    // Fichero temporal para el contenido del adjunto: se genera en cada ejecución y
    // se borra en el finally, no se commitea ningún binario al repositorio. El
    // widget de contenido muestra como botón el nombre REAL del fichero subido (no
    // el valor del campo "Nombre del fichero"), y ese mismo nombre es el que
    // Playwright reporta como `suggestedFilename()` al descargarlo — por eso el
    // fichero temporal se llama literalmente "circular.pdf" (en un directorio único
    // por ejecución para no colisionar entre runs concurrentes), igual que T-002.
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'adjunto-t022-'));
    const adjuntoPath = path.join(tmpDir, 'circular.pdf');
    const contenidoAdjunto = '%PDF-1.4\n%%EOF';
    fs.writeFileSync(adjuntoPath, contenidoAdjunto);
    // Ruta donde se guardará el fichero descargado, para poder comparar su
    // contenido byte a byte contra `contenidoAdjunto` (el mismo directorio
    // temporal único por ejecución, así que se limpia junto con `adjuntoPath`).
    const descargaPath = path.join(tmpDir, 'circular-descargado.pdf');

    try {
      // Paso 1: Dado que el administrador ha iniciado sesión, pulsa "Nuevo correo",
      // rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP
      // Mislata», el «para» «alumno1@mislata.es», el asunto «Circular con adjunto»,
      // el cuerpo «texto» y elige el centro «CIPFP Mislata».
      await page.getByText('Correos', { exact: true }).click();
      await page.getByText('Administración de correos', { exact: true }).click();
      await page.getByRole('button', { name: 'Nuevo correo' }).click();

      await page.getByLabel('DNI del destinatario').fill('86862719E');
      await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
      await page.getByLabel('Apellidos').fill('CIPFP Mislata');
      // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
      await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
      await page.getByLabel('Asunto').fill(asunto);
      await page.getByLabel('Cuerpo').fill('texto');
      await page.getByLabel('Centro', { exact: true }).click();
      await page.getByRole('option', { name: 'CIPFP Mislata' }).click();

      // Paso 2: Y, en el panel de adjuntos, pulsa "Añadir adjunto", rellena el
      // nombre del fichero «circular.pdf», sube un fichero como contenido y pulsa
      // "Guardar" del adjunto.
      await page.getByRole('button', { name: 'Añadir adjunto' }).click();

      const dialogoAdjunto = page.getByRole('dialog');
      await dialogoAdjunto.getByLabel('Nombre del fichero').fill('circular.pdf');
      const fileChooserPromise = page.waitForEvent('filechooser');
      await dialogoAdjunto.getByRole('button', { name: 'Upload' }).click();
      const fileChooser = await fileChooserPromise;
      await fileChooser.setFiles(adjuntoPath);
      await expect(dialogoAdjunto.getByRole('button', { name: 'circular.pdf' })).toBeVisible();

      await dialogoAdjunto.getByRole('button', { name: 'Guardar' }).click();
      await expect(dialogoAdjunto).not.toBeVisible();

      // El adjunto queda listado en el panel del correo antes de guardar el correo.
      await expect(page.getByRole('row', { name: 'circular.pdf' })).toBeVisible();

      // Paso 3: Y pulsa "Guardar" en el correo y cierra sesión.
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('row', { name: asunto })).toBeVisible();

      await logout(page);

      // Paso 4: Cuando el supervisor «supervisor1@mislata.es» inicia sesión con
      // contraseña «demo1234» y abre la pantalla "Correos de mi centro".
      await login(page, 'supervisor1@mislata.es', 'demo1234');

      await page.getByText('Correos', { exact: true }).click();
      await page.getByText('Correos de mi centro', { exact: true }).click();
      // La navegación de la SPA actualiza el hash de forma asíncrona tras el
      // click: hay que esperar a que se asiente en la URL de "Correo@Centro"
      // ANTES de entrar en el bucle de recarga; si se recargara demasiado
      // pronto, el reload() volvería a la home (hash todavía sin actualizar) y
      // la fila no aparecería nunca.
      await page.waitForURL(/Correo%40Centro/);

      // Paso 5: Y recarga el listado hasta ver el correo «Circular con adjunto» en
      // estado "Enviado" y abre su detalle; en el panel de adjuntos aparece
      // «circular.pdf». El envío es asíncrono, así que se reintenta la recarga
      // hasta que el estado deje de ser "Pendiente".
      const filaCorreo = page.getByRole('row', { name: asunto });
      await expect(async () => {
        await page.reload();
        await expect(filaCorreo).toBeVisible();
        await expect(filaCorreo.getByRole('gridcell', { name: 'Enviado' })).toBeVisible();
      }).toPass({ timeout: 60_000, intervals: [2_000] });

      await filaCorreo.click();
      await page.waitForURL(/\/edit\/\d+/);
      await expect(page.getByRole('row', { name: 'circular.pdf' })).toBeVisible();

      // Paso 6: Y pulsa la fila del adjunto «circular.pdf» para abrir su formulario
      // de detalle.
      await page.getByRole('row', { name: 'circular.pdf' }).click();
      const dialogoAdjuntoDetalle = page.getByRole('dialog');
      await expect(dialogoAdjuntoDetalle.getByLabel('Nombre del fichero')).toHaveValue('circular.pdf');

      // Paso 7 / Resultado esperado: descarga el fichero del campo contenido y el
      // sistema descarga el fichero «circular.pdf» con el mismo contenido que se
      // subió como adjunto (no solo el nombre): se guarda el fichero descargado y
      // se compara su contenido byte a byte contra `contenidoAdjunto`, para que un
      // backend que sirva bytes incorrectos o vacíos con el nombre correcto NO
      // pase el test.
      const downloadPromise = page.waitForEvent('download');
      await dialogoAdjuntoDetalle.getByRole('button', { name: 'circular.pdf' }).click();
      const download = await downloadPromise;
      expect(download.suggestedFilename()).toBe('circular.pdf');

      await download.saveAs(descargaPath);
      const contenidoDescargado = fs.readFileSync(descargaPath, 'utf-8');
      expect(contenidoDescargado).toBe(contenidoAdjunto);

      await dialogoAdjuntoDetalle.getByRole('button', { name: 'Cerrar' }).click();
      await expect(dialogoAdjuntoDetalle).not.toBeVisible();

      // Teardown: un Correo del centro es solo lectura para el supervisor (no hay
      // botón de borrado en "Correos de mi centro") y, una vez enviado, tampoco se
      // puede borrar desde "Administración de correos" (el formulario de consulta
      // solo ofrece "Reenviar"/"Salir" — es un dato inmutable de auditoría, igual
      // que en T-001/T-002/T-019/T-020). Se confía en el asunto único para no
      // colisionar entre ejecuciones; no es un error tapado, es la excepción
      // documentada por el contrato (§4.5).
      await page.getByRole('button', { name: 'Salir' }).click();

      await logout(page);
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    }
  });
});
