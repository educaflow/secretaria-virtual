import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-006 — Otros usuarios del centro no ven las pantallas de perfiles
// origen: ESC-020  |  verifica: —
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-006-otros-usuarios-del-centro-no-ven-las-pantallas-de-perfiles.desc.md

// Idempotencia: el test es de solo lectura (no crea, modifica ni borra datos),
// así que no necesita nombres únicos, teardown ni pre-limpieza.

// Entrada de primer nivel del menú lateral de Axelor, por su título exacto.
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
  test('Otros usuarios del centro no ven las pantallas de perfiles', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el director inicia sesión con usuario «director@mislata.es»
    //         y contraseña «demo1234».
    await login(page, 'director@mislata.es', 'demo1234');

    // Paso 2: Cuando despliega el menú.
    // Barrera de carga: el menú lateral ya está pintado (el director tiene al
    // menos «Mis trámites»), para que las aserciones de ausencia no pasen en falso
    // sobre una página aún vacía.
    await expect(entradaMenu(page, 'Mis trámites')).toBeVisible();
    await desplegarSiExiste(page, 'Mi centro');

    // Resultado esperado: el menú no muestra la opción «Mi centro → Perfiles de
    // trámites» (ni visible ni presente en el DOM).
    await expect(entradaMenu(page, 'Perfiles de trámites')).toHaveCount(0);

    await desplegarSiExiste(page, 'Administración');

    // Resultado esperado: el menú no muestra la opción «Administración → Perfiles
    // de trámites por centro» (ni visible ni presente en el DOM).
    await expect(entradaMenu(page, 'Perfiles de trámites por centro')).toHaveCount(0);

    await logout(page);
  });
});
