import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-061 — El gestor del centro no puede dar de alta notificaciones
// origen: ESC-048  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-061-el-gestor-del-centro-no-puede-dar-de-alta-notificaciones.desc.md

// Idempotencia: el test solo consulta el menú y la vista «Del centro», no crea
// ni modifica datos, así que no necesita nombres únicos ni teardown.
test.describe('Notificaciones — Del centro', () => {
  test('El gestor del centro no puede dar de alta notificaciones', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que «supervisor1@mislata.es» ha iniciado sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Paso 2: Cuando abre el menú «Notificaciones».
    // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
    // data-testid estable que Axelor deriva del nombre del menuitem.
    const menuNotificaciones = page.getByTestId('item:notificaciones-menuitem');
    await menuNotificaciones.getByText('Notificaciones', { exact: true }).click();

    // Paso 3: Entonces no ve el submenú «Todas».
    // Se ancla antes en «Del centro» visible para que la ausencia de «Todas» no
    // pase en vacío con el menú aún sin desplegar.
    await expect(menuNotificaciones.getByText('Del centro', { exact: true })).toBeVisible();
    await expect(menuNotificaciones.getByText('Todas', { exact: true })).toHaveCount(0);

    // Paso 4: Cuando abre «Notificaciones» → «Del centro».
    await menuNotificaciones.getByText('Del centro', { exact: true }).click();
    await expect(page.getByRole('tab', { name: 'Notificaciones del centro', selected: true })).toBeVisible();
    const vista = page.getByRole('tabpanel', { name: 'Notificaciones del centro' });
    // La vista ya está renderizada (cabecera del grid visible) antes de afirmar
    // la ausencia del botón, así la aserción negativa no pasa en vacío.
    await expect(vista.getByRole('columnheader', { name: 'Motivo' })).toBeVisible();

    // Resultado esperado: no se ve el botón «Nueva notificación».
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toHaveCount(0);

    await logout(page);
  });
});
