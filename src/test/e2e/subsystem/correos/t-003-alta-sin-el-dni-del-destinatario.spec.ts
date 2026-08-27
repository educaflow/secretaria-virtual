import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-003 — Alta sin el DNI del destinatario
// origen: ESC-003  |  verifica: V-Correo-001
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-003-alta-sin-el-dni-del-destinatario.desc.md
test.describe('Administración de correos', () => {
  test('Alta sin el DNI del destinatario', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución. Al ser un test de validación negativa, el correo nunca llega a
    // crearse, así que no hay que borrar nada al final: el sufijo único solo sirve
    // para identificar de forma inequívoca la fila (ausente) en el listado.
    const asunto = `Convocatoria de reunión t003-${Date.now()}`;

    // Paso 1: Dado que el administrador pulsa "Nuevo correo"
    await page.getByTestId('item:correos-menuitem').getByText('Correos', { exact: true }).click();
    await page.getByText('Administración de correos', { exact: true }).click();
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    // Paso 2: Cuando rellena el nombre «Alumno1», los apellidos «CIPFP Mislata», el
    // «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo
    // «texto», elige el centro «CIPFP Mislata» y deja vacío el DNI del destinatario.
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
    await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
    await page.getByLabel('Asunto').fill(asunto);
    await page.getByLabel('Cuerpo').fill('texto');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();
    // El DNI del destinatario se deja deliberadamente vacío (no se rellena).

    // Paso 3: Y pulsa "Guardar"
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra el mensaje "El DNI del destinatario es
    // obligatorio" (validación inline bajo el propio campo) y no crea el correo.
    await expect(page.getByText('El DNI del destinatario es obligatorio')).toBeVisible();
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

    await logout(page);
  });
});
