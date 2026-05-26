// spec: .sdd/drafts/2026-05-21_20-14_correos/implementation/tests.md
// seed: tests/seed.spec.ts
// NOTE: canonical path is:
//   .sdd/drafts/2026-05-21_20-14_correos/implementation/test_e2e/T-002_alta-manual-de-un-correo-con-adjuntos.spec.ts

import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

test.describe('correos — debug E2E', () => {
  test('Alta manual de un Correo con adjuntos', async ({ page }) => {
    // Crear el fichero de adjunto en tmpdir antes del test
    const tmpFilePath = path.join(os.tmpdir(), 'adjunto-t002.txt');
    fs.writeFileSync(tmpFilePath, 'Contenido de prueba para el adjunto T-002');

    // Precondición 1: Navegar a la pantalla de login
    await page.goto('/#/login');

    // Precondición 1: Rellenar usuario y contraseña, pulsar Entrar
    await page.getByTestId('field-username').getByTestId('input').fill('admin');
    await page.getByTestId('field-password').getByTestId('input').fill('admin');
    await page.getByTestId('btn-login').click();

    // Precondición 2: Pulsar la entrada de menú "Correos" para desplegar el submenú
    await page.getByTestId('item:correos-menuitem').getByTestId('title').click();

    // Precondición 2: Pulsar "Todos los correos" en el submenú
    await page.getByText('Todos los correos').click();

    // Precondición 3: Pulsar botón "Nuevo correo" en la toolbar de la lista
    await page.getByTestId('item:new').getByTestId('button').click();

    // Paso 1: Rellenar dniDestinatario con el valor válido
    await page.getByRole('textbox', { name: 'DNI destinatario' }).fill('24362574P');

    // Paso 1: Presionar Tab para disparar el onChange que autocompleta el email
    await page.getByRole('textbox', { name: 'DNI destinatario' }).press('Tab');

    // Paso 1: Verificar que el email se autocompletó server-side (toHaveValue, nunca wait_for/verify_text)
    await expect(page.getByRole('textbox', { name: 'Email destinatario' })).toHaveValue('lorenzo.profesor@gmail.com');

    // Paso 1: Rellenar el asunto
    await page.getByRole('textbox', { name: 'Asunto' }).fill('Asunto de prueba T-002');

    // Paso 1: Rellenar el cuerpo del correo (editor rich-text contenteditable)
    await page.locator('.custom-html-editor-content').click();
    await page.locator('.custom-html-editor-content').fill('Cuerpo del correo de prueba T-002');

    // Paso 2: Pulsar el botón "Añadir adjunto" para abrir el diálogo modal
    await page.getByRole('button', { name: 'Añadir adjunto' }).click();

    // Paso 2: En el diálogo, rellenar el nombre del fichero
    await page.getByRole('dialog').getByRole('textbox', { name: 'Nombre del fichero' }).fill('adjunto-t002.txt');

    // Paso 2: Pulsar "Subir" para abrir el file chooser y subir el fichero creado en tmpdir
    const fileChooserPromise = page.waitForEvent('filechooser');
    await page.getByTestId('btn-upload').click();
    const fileChooser = await fileChooserPromise;
    await fileChooser.setFiles(tmpFilePath);

    // Paso 2: Pulsar "Guardar" del DIÁLOGO para confirmar el adjunto y cerrar el diálogo
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();

    // Paso 2: Verificar que el diálogo se ha cerrado antes de pulsar Guardar del formulario
    await expect(page.getByRole('dialog')).toBeHidden();

    // Paso 3: Pulsar "Guardar" del formulario principal para crear el correo
    // (acotado al tab-panel para evitar la ambigüedad con el botón Guardar del diálogo de adjunto)
    await page.getByTestId('tab-panel:subsysCorreos.Correo@Todos-action').getByRole('button', { name: 'Guardar' }).click();

    // Paso 4: El correo queda creado en estado PENDIENTE; localizar la fila en el grid y abrirla
    await page.getByRole('row', { name: /Asunto de prueba T-002.*Pendiente/ }).click();

    // Verificación: El estado en el formulario de detalle es "Pendiente"
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Pendiente');

    // Verificación: En el grid "Adjuntos", la columna nombreFichero muestra "adjunto-t002.txt"
    // Acotado por data-testid de columna para evitar la ambigüedad: ambas columnas muestran el mismo texto
    await expect(page.getByTestId('column:nombreFichero').filter({ hasText: 'adjunto-t002.txt' })).toBeVisible();

    // Verificación: En la columna Contenido del adjunto aparece el enlace de descarga del fichero subido
    // Acotado por data-testid de columna para evitar la ambigüedad con la columna nombreFichero
    await expect(page.getByTestId('column:contenido').getByRole('link', { name: 'adjunto-t002.txt' })).toBeVisible();

    // Paso 5: Logout — pulsar el avatar "A" (User Menu) y seleccionar "Cerrar sesión"
    await page.getByTestId('item:user').getByTestId('button').click();
    await page.getByTestId('item:logout').click();

    // Verificar que se volvió a la pantalla de login
    await expect(page).toHaveURL(/#\/login/);

    // Limpiar el fichero temporal
    fs.unlinkSync(tmpFilePath);
  });
});
