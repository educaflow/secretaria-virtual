import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-001 — Alta de un correo que se envía con éxito
// origen: ESC-001  |  verifica: V-Correo-001, V-Correo-003, V-Correo-004, V-Correo-005, V-Correo-009,
//   V-Correo-011, V-Correo-012, R-Correo-001, R-Correo-003, R-Correo-004,
//   U-correos-administracion-formulario-002, U-correos-administracion-formulario-005,
//   U-correos-administracion-formulario-008
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-001-alta-de-un-correo-que-se-envia-con-exito.desc.md
test.describe('Administración de correos', () => {
  test('Alta de un correo que se envía con éxito', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución para poder identificar SIEMPRE la fila que crea este run, incluso
    // si hay correos de ejecuciones anteriores en la BD.
    const asunto = `Convocatoria de reunión t001-${Date.now()}`;

    // Paso 1: Dado que el administrador está en la pantalla "Administración de correos"
    await page.getByText('Correos', { exact: true }).click();
    await page.getByText('Administración de correos', { exact: true }).click();

    // Paso 2: Cuando pulsa "Nuevo correo"
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    // Paso 3: Y rellena el DNI, el nombre, los apellidos, el «para», el asunto, el
    // cuerpo y elige el centro «CIPFP Mislata»
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
    await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
    await page.getByLabel('Asunto').fill(asunto);
    await page.getByLabel('Cuerpo').fill('Le esperamos el lunes a las 9:00');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();

    // Paso 4: Y pulsa "Guardar"
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Paso 5: Entonces el sistema crea el correo y lo muestra en estado "Pendiente"
    // (o ya "Enviado" si el envío asíncrono ha terminado), con la fecha de creación
    // registrada. El listado se ordena por fecha de creación descendente, así que la
    // fila del correo recién creado es identificable por su asunto único.
    const row = page.getByRole('row', { name: asunto });
    await expect(row).toBeVisible();
    await expect(row.getByRole('gridcell', { name: /Pendiente|Enviado/ })).toBeVisible();
    await expect(row).toContainText(/\d{2}\/\d{2}\/\d{4}/); // fecha de creación registrada

    // Abre el detalle del correo recién creado
    await row.click();
    // Espera a que la SPA confirme la navegación al detalle (URL .../edit/<id>) antes
    // de recargar: si se recarga demasiado pronto, el hash aún no se ha actualizado
    // y la recarga vuelve a caer en el listado en vez de en el detalle.
    await page.waitForURL(/\/edit\/\d+/);

    // Paso 6: Y, recargando el detalle del correo poco después, el correo aparece en
    // estado "Enviado" ("SUCCESS"), con la fecha de envío rellena y el botón
    // "Reenviar" ausente. El envío es asíncrono (job del scheduler), así que se
    // recarga en bucle hasta que el estado cambia.
    await expect(async () => {
      await page.reload();
      // El campo "Estado" es un <input role="combobox"> de solo lectura: su valor
      // vive en el atributo value, no como texto — de ahí toHaveValue, no toContainText.
      await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Enviado');
    }).toPass({ timeout: 60_000, intervals: [2_000] });

    // Resultado esperado: el correo queda en SUCCESS, con fecha de envío registrada
    // y número de reintentos igual a 1.
    await expect(page.getByLabel('Fecha de envío')).not.toHaveValue('');
    await expect(page.getByLabel(/reintentos/i)).toHaveValue('1');
    await expect(page.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);

    // Teardown: un Correo ya enviado no se puede borrar desde la UI (el formulario de
    // consulta solo ofrece "Salir", no hay botón de borrado — es un dato inmutable de
    // auditoría). Se confía en el asunto único para no colisionar entre ejecuciones;
    // no es un error tapado, es la excepción documentada por el contrato (§4.5).
    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
