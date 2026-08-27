import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-015 — Alta con el asunto demasiado largo
// origen: ESC-023  |  verifica: V-Correo-010
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-015-alta-con-el-asunto-demasiado-largo.desc.md
test.describe('Administración de correos', () => {
  test('Alta con el asunto demasiado largo', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    await page.getByTestId('item:correos-menuitem').getByText('Correos', { exact: true }).click();
    await page.getByText('Administración de correos', { exact: true }).click();

    // Idempotencia (BD compartida, sin reset): este test valida precisamente que el
    // asunto no puede superar 255 caracteres, así que se usa un valor fijo (256
    // "A") sin sufijo único —el correo nunca llega a crearse, no hay fila que
    // buscar por nombre—. En su lugar se compara el total de filas del listado
    // antes y después del intento de alta: si el correo nunca llega a crearse, el
    // total no cambia. Al no crearse nada, tampoco hace falta teardown.
    const contador = page.getByText(/^\d+ to \d+ of \d+$/);
    // El contador tarda un instante en cargar el total real tras la navegación
    // (arranca en "0 to 0 of 0"); se espera a que se estabilice antes de leerlo.
    await expect(contador).not.toHaveText('0 to 0 of 0');
    const totalAntes = await contador.textContent();

    // Paso 1: Dado que el administrador pulsa "Nuevo correo"
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    // Paso 2: Cuando rellena el DNI «86862719E», el nombre «Alumno1», los apellidos
    // «CIPFP Mislata», el «para» «alumno1@mislata.es», un asunto con la letra «A»
    // repetida 256 veces, el cuerpo «texto» y elige el centro «CIPFP Mislata».
    const asuntoDemasiadoLargo = 'A'.repeat(256);
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
    await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
    await page.getByLabel('Asunto').fill(asuntoDemasiadoLargo);
    await page.getByLabel('Cuerpo').fill('texto');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();

    // Paso 3: Y pulsa "Guardar"
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra el mensaje "El asunto no puede superar
    // 255 caracteres" (diálogo modal de "Validation Error", ya que esta validación
    // solo existe en el servicio de servidor, no como check inline del XML) y no
    // crea el correo.
    await expect(page.getByRole('heading', { name: 'Validation Error' })).toBeVisible();
    await expect(page.getByText('El asunto no puede superar 255 caracteres')).toBeVisible();
    await page.getByRole('dialog').getByRole('button', { name: 'OK' }).click();

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
