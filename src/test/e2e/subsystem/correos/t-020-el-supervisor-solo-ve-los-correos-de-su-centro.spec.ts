import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-020 — El supervisor solo ve los correos de su centro
// origen: ESC-006  |  verifica: — (permiso `Correo.propio-centro-supervisor`, ver design.md paso 10)
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-020-el-supervisor-solo-ve-los-correos-de-su-centro.desc.md
test.describe('Correos de mi centro', () => {
  test('El supervisor solo ve los correos de su centro', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): los asuntos llevan un sufijo único por
    // ejecución para poder identificar SIEMPRE las filas que crea este run, incluso si
    // hay correos de ejecuciones anteriores en la BD.
    const sufijo = `t020-${Date.now()}`;
    const asuntoMislata = `Aviso Mislata ${sufijo}`;
    const asuntoBatoi = `Aviso Batoi ${sufijo}`;

    // Paso 1: Dado que el administrador ha iniciado sesión, pulsa "Nuevo correo",
    // rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata»,
    // el «para» «alumno1@mislata.es», el asunto «Aviso Mislata», el cuerpo «texto»,
    // elige el centro «CIPFP Mislata» y pulsa "Guardar".
    await page.getByText('Correos', { exact: true }).click();
    await page.getByText('Administración de correos', { exact: true }).click();
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
    await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
    await page.getByLabel('Asunto').fill(asuntoMislata);
    await page.getByLabel('Cuerpo').fill('texto');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();

    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('row', { name: asuntoMislata })).toBeVisible();

    // Paso 2: Y pulsa "Nuevo correo" de nuevo, rellena los mismos datos pero con el
    // asunto «Aviso Batoi», elige el centro «CIPFP Batoi» y pulsa "Guardar".
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
    await page.getByLabel('Asunto').fill(asuntoBatoi);
    await page.getByLabel('Cuerpo').fill('texto');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Batoi' }).click();

    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('row', { name: asuntoBatoi })).toBeVisible();

    // Paso 3: Y cierra sesión.
    await logout(page);

    // Paso 4: Cuando el supervisor «supervisor1@mislata.es» inicia sesión con
    // contraseña «demo1234».
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Paso 5: Y abre la pantalla "Correos de mi centro".
    await page.getByText('Correos', { exact: true }).click();
    await page.getByText('Correos de mi centro', { exact: true }).click();

    // Resultado esperado: el sistema muestra el correo «Aviso Mislata» y no muestra
    // el correo «Aviso Batoi».
    await expect(page.getByRole('row', { name: asuntoMislata })).toBeVisible();
    await expect(page.getByRole('row', { name: asuntoBatoi })).toHaveCount(0);

    await logout(page);

    // Teardown: un Correo del centro es solo lectura para el supervisor (no hay
    // botón de borrado en "Correos de mi centro") y, una vez enviado, tampoco se
    // puede borrar desde "Administración de correos" (el formulario de consulta solo
    // ofrece "Reenviar"/"Salir" — es un dato inmutable de auditoría, igual que en
    // T-001/T-019). Se confía en el sufijo único del asunto para no colisionar entre
    // ejecuciones; no es un error tapado, es la excepción documentada por el
    // contrato (§4.5).
  });
});
