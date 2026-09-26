import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-007 — El supervisor no ve la pantalla del administrador
// origen: ESC-021  |  verifica: —
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-007-el-supervisor-no-ve-la-pantalla-del-administrador.desc.md

// Idempotencia: el test es de solo lectura (no crea, modifica ni borra datos),
// así que no necesita nombres únicos, teardown ni pre-limpieza.

// Entrada del menú lateral de Axelor, por su título exacto.
const entradaMenu = (page: Page, titulo: string) => page.getByText(titulo, { exact: true });

// Si la entrada de primer nivel `padre` existe en el menú, la despliega para que
// sus hijas se rendericen y la aserción de ausencia sea real (no un falso
// negativo por estar plegada). Si no existe, la opción hija tampoco puede estar.
async function desplegarSiExiste(page: Page, padre: string): Promise<void> {
  const entrada = entradaMenu(page, padre);
  if ((await entrada.count()) > 0) {
    await entrada.first().click();
  }
}

test.describe('Perfiles de trámites (visibilidad del menú)', () => {
  test('El supervisor no ve la pantalla del administrador', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el supervisor inicia sesión con usuario
    //         «supervisor1@mislata.es» y contraseña «demo1234».
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Paso 2: Cuando despliega el menú.
    // Barrera de carga: el menú lateral ya está pintado, para que la aserción de
    // ausencia no pase en falso sobre una página aún vacía.
    await expect(entradaMenu(page, 'Mi centro')).toBeVisible();
    await entradaMenu(page, 'Mi centro').click();

    // Resultado esperado: el menú muestra la opción «Mi centro → Perfiles de trámites».
    await expect(entradaMenu(page, 'Perfiles de trámites')).toBeVisible();

    await desplegarSiExiste(page, 'Administración');

    // Resultado esperado: el menú no muestra la opción «Administración → Perfiles
    // de trámites por centro» (ni visible ni presente en el DOM).
    await expect(entradaMenu(page, 'Perfiles de trámites por centro')).toHaveCount(0);

    await logout(page);
  });
});
