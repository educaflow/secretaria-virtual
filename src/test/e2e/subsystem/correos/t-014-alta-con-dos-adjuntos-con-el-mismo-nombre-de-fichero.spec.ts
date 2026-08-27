import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

// T-014 — Alta con dos adjuntos con el mismo nombre de fichero
// origen: ESC-022  |  verifica: V-Adjunto-006
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-014-alta-con-dos-adjuntos-con-el-mismo-nombre-de-fichero.desc.md
test.describe('Administración de correos', () => {
  test('Alta con dos adjuntos con el mismo nombre de fichero', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución. Al ser un test de validación negativa, el correo nunca llega a
    // crearse (la excepción de validación aborta la transacción de guardado, así
    // que tampoco quedan adjuntos huérfanos en la BD), así que no hay que borrar
    // nada al final: el sufijo único solo sirve para identificar de forma
    // inequívoca la fila (ausente) en el listado.
    const asunto = `Documento adjunto t014-${Date.now()}`;

    // Fichero temporal para el contenido de los adjuntos: se genera en cada
    // ejecución y se borra en el finally, no se commitea ningún binario al
    // repositorio (mismo patrón que T-002/T-010). Se reutiliza el mismo fichero
    // para los dos adjuntos: lo que provoca la validación es el campo "Nombre del
    // fichero" (idéntico en ambos), no el contenido subido.
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'adjunto-t014-'));
    const adjuntoPath = path.join(tmpDir, 'documento.pdf');
    fs.writeFileSync(adjuntoPath, '%PDF-1.4\n%%EOF');

    try {
      // Paso 1: Dado que el administrador pulsa "Nuevo correo" y rellena el DNI
      // «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para»
      // «alumno1@mislata.es», el asunto «Documento adjunto», el cuerpo «texto» y
      // elige el centro «CIPFP Mislata».
      await page.getByTestId('item:correos-menuitem').getByText('Correos', { exact: true }).click();
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

      // Paso 2: Cuando, en el panel de adjuntos, pulsa "Añadir adjunto", rellena el
      // nombre del fichero «documento.pdf», sube un fichero como contenido y pulsa
      // "Guardar" del adjunto.
      await page.getByRole('button', { name: 'Añadir adjunto' }).click();
      let dialogoAdjunto = page.getByRole('dialog');
      await dialogoAdjunto.getByLabel('Nombre del fichero').fill('documento.pdf');
      let fileChooserPromise = page.waitForEvent('filechooser');
      await dialogoAdjunto.getByRole('button', { name: 'Upload' }).click();
      let fileChooser = await fileChooserPromise;
      await fileChooser.setFiles(adjuntoPath);
      await expect(dialogoAdjunto.getByRole('button', { name: 'documento.pdf' })).toBeVisible();
      await dialogoAdjunto.getByRole('button', { name: 'Guardar' }).click();
      await expect(dialogoAdjunto).not.toBeVisible();

      // El primer adjunto queda listado en el panel del correo, sin error alguno
      // en este momento: la validación de nombre duplicado (V-Adjunto-006) vive en
      // el servicio de servidor y solo se dispara al guardar el correo completo
      // (JPA.edit necesita ensamblar el grafo con todos los hermanos), no como
      // acción cliente del propio diálogo de adjunto.
      await expect(page.getByRole('row', { name: 'documento.pdf' })).toHaveCount(1);

      // Paso 3: Y pulsa de nuevo "Añadir adjunto", rellena otra vez el nombre del
      // fichero «documento.pdf», sube un fichero como contenido y pulsa "Guardar"
      // del adjunto.
      await page.getByRole('button', { name: 'Añadir adjunto' }).click();
      dialogoAdjunto = page.getByRole('dialog');
      await dialogoAdjunto.getByLabel('Nombre del fichero').fill('documento.pdf');
      fileChooserPromise = page.waitForEvent('filechooser');
      await dialogoAdjunto.getByRole('button', { name: 'Upload' }).click();
      fileChooser = await fileChooserPromise;
      await fileChooser.setFiles(adjuntoPath);
      await expect(dialogoAdjunto.getByRole('button', { name: 'documento.pdf' })).toBeVisible();
      await dialogoAdjunto.getByRole('button', { name: 'Guardar' }).click();
      await expect(dialogoAdjunto).not.toBeVisible();

      // El segundo adjunto también se añade sin problema al panel: ahora hay dos
      // filas «documento.pdf» (mismo nombre), todavía sin ningún error, porque la
      // comprobación de duplicados aún no se ha disparado.
      await expect(page.getByRole('row', { name: 'documento.pdf' })).toHaveCount(2);

      // Paso 4: Y pulsa "Guardar" en el correo.
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado: el sistema muestra el mensaje "Ya existe un adjunto
      // con ese nombre en el correo" y no crea el correo. La excepción de
      // validación de servidor (V-Adjunto-006, jakarta.validation.ValidationException
      // vía BusinessMessages.throwIfInvalid) la renderiza el cliente de Axelor como
      // un diálogo "Internal Server Error" (distinto del diálogo "Validation Error"
      // de otras validaciones, pero el mismo mecanismo de fondo): al comparar cada
      // adjunto contra sus hermanos, el mensaje aparece una vez por cada uno de los
      // dos adjuntos duplicados, de ahí el .first().
      await expect(page.getByRole('heading', { name: 'Internal Server Error' })).toBeVisible();
      await expect(
        page.getByText('Ya existe un adjunto con ese nombre en el correo').first()
      ).toBeVisible();
      await page.getByRole('dialog').getByRole('button', { name: 'OK' }).click();

      // La pestaña conserva el asterisco de "sin guardar" y la URL sigue en el
      // formulario de alta (no ha navegado a .../edit/<id>), confirmando que el
      // guardado no se ha producido.
      await expect(page.getByRole('tab', { name: 'Correo*' })).toBeVisible();
      await expect(page).not.toHaveURL(/\/edit\/\d+/);

      // Descarta el formulario (confirmando la pérdida de cambios) para volver al
      // listado y comprobar de forma directa que no se ha creado ninguna fila con
      // este asunto único.
      await page.getByRole('button', { name: 'Cancelar' }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'OK' }).click();
      await expect(page.getByRole('row', { name: asunto })).toHaveCount(0);
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    }

    await logout(page);
  });
});
