import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-051 — El administrador ve «Recibidas» y «Todas» pero no «Del centro»
// origen: ESC-076  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-051-el-administrador-ve-recibidas-y-todas-pero-no-del-centro.desc.md

// Idempotencia: el test solo consulta el menú, no crea ni modifica datos, así
// que no necesita nombres únicos ni teardown.
test.describe('Notificaciones — menú', () => {
  test('El administrador ve «Recibidas» y «Todas» pero no «Del centro»', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión.
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre el menú «Notificaciones».
    // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
    // data-testid estable que Axelor deriva del nombre del menuitem.
    const menuNotificaciones = page.getByTestId('item:notificaciones-menuitem');
    await menuNotificaciones.getByText('Notificaciones', { exact: true }).click();

    // Resultado esperado: muestra los submenús «Recibidas» y «Todas»...
    await expect(menuNotificaciones.getByText('Recibidas', { exact: true })).toBeVisible();
    await expect(menuNotificaciones.getByText('Todas', { exact: true })).toBeVisible();
    // ...y no muestra «Del centro».
    await expect(menuNotificaciones.getByText('Del centro', { exact: true })).toHaveCount(0);

    await logout(page);
  });
});
