import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-022 — El supervisor no puede dar de alta SMS
// origen: ESC-022  |  verifica: —
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-022-el-supervisor-no-puede-dar-de-alta-sms.desc.md

// Idempotencia: el test solo navega y comprueba lo que NO aparece; no crea ni modifica datos,
// así que no necesita nombres únicos, teardown ni pre-limpieza (BD compartida sin reset).
test.describe('SMS — Del centro', () => {
  test('El supervisor no puede dar de alta SMS', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el supervisor «supervisor1@mislata.es» ha iniciado sesión con contraseña «demo1234».
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Paso 2: Cuando despliega el menú "SMS".
    await page.getByText('SMS', { exact: true }).click();
    const delCentro = page.getByTestId('item:sms-delCentro-menuitem');
    await expect(delCentro).toBeVisible();

    // Resultado esperado: el menú "SMS" no muestra la entrada "Todos".
    await expect(page.getByTestId('item:sms-todos-menuitem')).toHaveCount(0);
    await expect(page.getByTestId('item:sms-menuitem').getByText('Todos', { exact: true })).toHaveCount(0);

    // Paso 3: Y abre el menú "SMS" → "Del centro".
    await delCentro.click();
    await expect(page).toHaveURL(/\/list\//);

    // Resultado esperado: el listado de "Del centro" se muestra sin el botón "Nuevo SMS".
    const pestana = page.getByRole('tabpanel', { name: 'SMS de mis centros' });
    await expect(pestana).toBeVisible();
    await expect(pestana.getByRole('grid')).toBeVisible();
    await expect(pestana.getByRole('columnheader', { name: 'Centro' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nuevo SMS' })).toHaveCount(0);
    await expect(page.getByText('Nuevo SMS')).toHaveCount(0);
    // El grid subsysSms.Centro@Sms-grid no tiene newButtonTitle: si canNew fuera true, axelor-front
    // pintaría el botón genérico "Nuevo"/"New"/"Nou" con icono "add" (grid.tsx). Se comprueba que
    // tampoco existe, ya con el grid cargado (cabecera "Centro" visible arriba).
    const NUEVO_GENERICO = /^(Nuevo|New|Nou)( SMS)?$/i;
    await expect(page.getByRole('button', { name: NUEVO_GENERICO })).toHaveCount(0);
    await expect(pestana.getByTitle(NUEVO_GENERICO)).toHaveCount(0);
    await expect(
      pestana.getByRole('button').filter({
        has: page.locator('[class*="material-symbols"]', { hasText: /^add$/ }),
      }),
    ).toHaveCount(0);

    await logout(page);
  });
});
