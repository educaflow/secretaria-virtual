import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-019 — El botón Reenviar no aparece si el correo no ha fallado
// origen: ESC-005  |  verifica: U-correos-administracion-formulario-001
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-019-el-boton-reenviar-no-aparece-si-el-correo-no-ha-fallado.desc.md
test.describe('Administración de correos', () => {
  test('El botón Reenviar no aparece si el correo no ha fallado', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución para poder identificar SIEMPRE la fila que crea este run, incluso
    // si hay correos de ejecuciones anteriores en la BD.
    const asunto = `Correo correcto t019-${Date.now()}`;

    // Paso 1: Dado que el administrador pulsa "Nuevo correo" y rellena el DNI
    // «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para»
    // «alumno1@mislata.es», el asunto «Correo correcto», el cuerpo «texto» y elige
    // el centro «CIPFP Mislata»
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

    // Paso 2: Cuando pulsa "Guardar"
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Paso 3: Y recarga el listado de correos hasta que el correo «Correo correcto»
    // aparezca en estado "Enviado". El envío es asíncrono (job del scheduler), así
    // que se recarga en bucle hasta que el estado cambia.
    const row = page.getByRole('row', { name: asunto });
    await expect(row).toBeVisible();
    await expect(async () => {
      await page.reload();
      await expect(row.getByRole('gridcell', { name: 'Enviado' })).toBeVisible();
    }).toPass({ timeout: 60_000, intervals: [2_000] });

    // Paso 4: Y abre el detalle de ese correo
    await row.click();
    await page.waitForURL(/\/edit\/\d+/);

    // Resultado esperado: el sistema no muestra el botón "Reenviar".
    // El campo "Estado" es un <input role="combobox"> de solo lectura: su valor
    // vive en el atributo value, no como texto — de ahí toHaveValue.
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Enviado');
    await expect(page.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);

    // Teardown: un Correo ya enviado no se puede borrar desde la UI (el formulario de
    // consulta solo ofrece "Salir", no hay botón de borrado — es un dato inmutable de
    // auditoría). Se confía en el asunto único para no colisionar entre ejecuciones;
    // no es un error tapado, es la excepción documentada por el contrato (§4.5).
    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
