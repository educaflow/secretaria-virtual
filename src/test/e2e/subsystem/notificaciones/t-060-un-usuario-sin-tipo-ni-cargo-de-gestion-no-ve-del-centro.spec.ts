import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-060 — Un usuario sin tipo ni cargo de gestión no ve «Del centro»
// origen: ESC-047  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-060-un-usuario-sin-tipo-ni-cargo-de-gestion-no-ve-del-centro.desc.md

// Idempotencia: el test solo consulta el menú, no crea ni modifica datos, así
// que no necesita nombres únicos ni teardown.

// Abre el menú «Notificaciones» y comprueba que solo se ve «Recibidas».
// Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
// data-testid estable que Axelor deriva del nombre del menuitem.
async function abrirMenuYComprobarSoloRecibidas(page: Page): Promise<void> {
  const menuNotificaciones = page.getByTestId('item:notificaciones-menuitem');
  await menuNotificaciones.getByText('Notificaciones', { exact: true }).click();

  await expect(menuNotificaciones.getByText('Recibidas', { exact: true })).toBeVisible();
  await expect(menuNotificaciones.getByText('Del centro', { exact: true })).toHaveCount(0);
  await expect(menuNotificaciones.getByText('Todas', { exact: true })).toHaveCount(0);
}

test.describe('Notificaciones — menú', () => {
  test('Un usuario sin tipo ni cargo de gestión no ve «Del centro»', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que «vicesecretario@mislata.es» ha iniciado sesión.
    await login(page, 'vicesecretario@mislata.es', 'demo1234');

    // Paso 2: Cuando abre el menú «Notificaciones».
    // Paso 3: Entonces ve «Recibidas» y no ve «Del centro» ni «Todas».
    await abrirMenuYComprobarSoloRecibidas(page);

    // Paso 4: Cuando cierra sesión, «profesor1@mislata.es» inicia sesión y abre el menú «Notificaciones».
    await logout(page);
    await login(page, 'profesor1@mislata.es', 'demo1234');

    // Resultado esperado: ve «Recibidas» y no ve «Del centro» ni «Todas».
    await abrirMenuYComprobarSoloRecibidas(page);

    await logout(page);
  });
});
