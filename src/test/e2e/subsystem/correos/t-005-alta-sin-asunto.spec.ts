import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-005 — Alta sin asunto
// origen: ESC-011  |  verifica: V-Correo-009
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-005-alta-sin-asunto.desc.md
test.describe('Administración de correos', () => {
  test('Alta sin asunto', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    await page.getByTestId('item:correos-menuitem').getByText('Correos', { exact: true }).click();
    await page.getByText('Administración de correos', { exact: true }).click();

    // Idempotencia (BD compartida, sin reset): este test valida precisamente que el
    // campo «asunto» es obligatorio, así que se deja vacío a propósito y no hay un
    // valor único que buscar luego en el listado (a diferencia de otros tests
    // negativos que sí rellenan el asunto con un sufijo). En su lugar se compara el
    // total de filas del listado antes y después del intento de alta: si el correo
    // nunca llega a crearse, el total no cambia. Al no crearse nada, tampoco hace
    // falta teardown.
    const contador = page.getByText(/^\d+ to \d+ of \d+$/);
    // El contador tarda un instante en cargar el total real tras la navegación
    // (arranca en "0 to 0 of 0"); se espera a que se estabilice antes de leerlo.
    await expect(contador).not.toHaveText('0 to 0 of 0');
    const totalAntes = await contador.textContent();

    // Paso 1: Dado que el administrador pulsa "Nuevo correo"
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    // Paso 2: Cuando rellena el DNI «86862719E», el nombre «Alumno1», los apellidos
    // «CIPFP Mislata», el «para» «alumno1@mislata.es», el cuerpo «texto», elige el
    // centro «CIPFP Mislata» y deja vacío el asunto.
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
    await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
    await page.getByLabel('Cuerpo').fill('texto');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();
    // El asunto se deja deliberadamente vacío (no se rellena).

    // Paso 3: Y pulsa "Guardar"
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra el mensaje "El asunto es obligatorio"
    // (validación inline bajo el propio campo) y no crea el correo.
    await expect(page.getByText('El asunto es obligatorio')).toBeVisible();
    // La pestaña conserva el asterisco de "sin guardar" y la URL sigue en el
    // formulario de alta (no ha navegado a .../edit/<id>), confirmando que el
    // guardado no se ha producido.
    await expect(page.getByRole('tab', { name: 'Correo*' })).toBeVisible();
    await expect(page).not.toHaveURL(/\/edit\/\d+/);

    // Descarta el formulario (confirmando la pérdida de cambios) para volver al
    // listado y comprobar de forma directa que el total de filas no ha cambiado
    // (no se ha creado ningún correo nuevo).
    await page.getByRole('button', { name: 'Cancelar' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'OK' }).click();
    await expect(contador).toHaveText(totalAntes ?? /^\d+ to \d+ of \d+$/);

    await logout(page);
  });
});
