import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-004 — Alta sin destinatario en el «para»
// origen: ESC-010  |  verifica: V-Correo-005
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-004-alta-sin-destinatario-en-el-para.desc.md
test.describe('Administración de correos', () => {
  test('Alta sin destinatario en el «para»', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución. Al ser un test de validación negativa, el correo nunca llega a
    // crearse, así que no hay que borrar nada al final: el sufijo único solo sirve
    // para identificar de forma inequívoca la fila (ausente) en el listado.
    const asunto = `Convocatoria de reunión t004-${Date.now()}`;

    // Paso 1: Dado que el administrador pulsa "Nuevo correo"
    await page.getByTestId('item:correos-menuitem').getByText('Correos', { exact: true }).click();
    await page.getByText('Administración de correos', { exact: true }).click();
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    // Paso 2: Cuando rellena el DNI «86862719E», el nombre «Alumno1», los apellidos
    // «CIPFP Mislata», el asunto «Convocatoria de reunión», el cuerpo «texto», elige
    // el centro «CIPFP Mislata» y deja vacío el «para».
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    await page.getByLabel('Asunto').fill(asunto);
    await page.getByLabel('Cuerpo').fill('texto');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();
    // El «para» se deja deliberadamente vacío (no se rellena).

    // Paso 3: Y pulsa "Guardar"
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra el mensaje "Debe indicar al menos un
    // destinatario en el «para»" (validación inline bajo el propio campo) y no crea
    // el correo.
    await expect(page.getByText('Debe indicar al menos un destinatario en el «para»')).toBeVisible();
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
