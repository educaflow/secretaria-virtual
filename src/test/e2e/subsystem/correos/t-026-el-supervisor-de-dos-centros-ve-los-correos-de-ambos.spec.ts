import { test, expect, type Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-026 — El supervisor de dos centros ve los correos de ambos
// origen: escrito a mano al retirar User.centroActivo  |  verifica: dominio de subsysCorreos.Centro@Correo-action
test.describe('Correos de mis centros', () => {
  test('El supervisor de dos centros ve los correos de ambos', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): los asuntos llevan un sufijo único por
    // ejecución para poder identificar SIEMPRE las filas que crea este run.
    const sufijo = `t026-${Date.now()}`;
    const asuntoMislata = `Aviso Mislata ${sufijo}`;
    const asuntoBatoi = `Aviso Batoi ${sufijo}`;

    // Paso 1: Dado que el administrador ha iniciado sesión, crea el correo «Aviso Mislata»
    // del centro «CIPFP Mislata».
    await page.getByText('Correos', { exact: true }).click();
    await page.getByTestId('item:correos-todos-menuitem').click();
    await crearCorreo(page, asuntoMislata, 'CIPFP Mislata');

    // Paso 2: Y crea el correo «Aviso Batoi» del centro «CIPFP Batoi».
    await crearCorreo(page, asuntoBatoi, 'CIPFP Batoi');

    // Paso 3: Y cierra sesión.
    await logout(page);

    // Paso 4: Cuando el supervisor de los dos centros inicia sesión.
    await login(page, 'supervisordoscentros@mislata.es', 'demo1234');

    // Paso 5: Y abre la pantalla "Correos de mis centros".
    await page.getByText('Correos', { exact: true }).click();
    await page.getByTestId('item:correos-delCentro-menuitem').click();

    // Resultado esperado: ve los dos correos, cada uno con su centro en la columna «centro».
    const filaMislata = page.getByRole('row', { name: asuntoMislata });
    const filaBatoi = page.getByRole('row', { name: asuntoBatoi });
    await expect(filaMislata).toBeVisible();
    await expect(filaMislata).toContainText('CIPFP Mislata');
    await expect(filaBatoi).toBeVisible();
    await expect(filaBatoi).toContainText('CIPFP Batoi');

    await logout(page);

    // Teardown: igual que T-020, un correo enviado es un dato inmutable de auditoría y no
    // se puede borrar; se confía en el sufijo único del asunto para no colisionar.
  });
});

async function crearCorreo(page: Page, asunto: string, centro: string) {
  await page.getByRole('button', { name: 'Nuevo correo' }).click();

  await page.getByLabel('DNI del destinatario').fill('95591733F');
  await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
  await page.getByLabel('Apellidos').fill('CIPFP Mislata');
  // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
  await page.getByLabel(/^Para\b/).fill('alumno1@mislata.es');
  await page.getByLabel('Asunto').fill(asunto);
  await page.getByLabel('Cuerpo').fill('texto');
  await page.getByLabel('Centro', { exact: true }).click();
  await page.getByRole('option', { name: centro }).click();

  await page.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByRole('row', { name: asunto })).toBeVisible();
}
