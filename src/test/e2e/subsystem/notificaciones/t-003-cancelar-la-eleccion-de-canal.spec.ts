import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-003 — Cancelar la elección de canal
// origen: ESC-003  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-003-cancelar-la-eleccion-de-canal.desc.md

// Idempotencia: el test no crea nada (precisamente comprueba que cancelar no crea
// ninguna notificación), así que no necesita teardown ni nombres únicos.
// La BD es compartida y no se resetea: otros tests dejan notificaciones (que no se
// pueden borrar, ver T-046), así que «el listado sigue vacío» se verifica como
// «el listado tiene exactamente las mismas notificaciones que antes de pulsar
// "Nueva notificación"» (total del paginador y filas), y además vacío si partía vacío.

// El paginador pinta «0 a 0 de 0» hasta que llega la búsqueda del grid, así que
// las acciones que (re)cargan el listado esperan a la respuesta de su `/search`.
// Devuelve el total que informa el servidor y espera a que el paginador lo pinte.
async function esperandoBusqueda(page: Page, accion: () => Promise<unknown>): Promise<number> {
  const busqueda = page.waitForResponse((r) => /\/ws\/rest\/[^/]+\/search$/.test(new URL(r.url()).pathname) && r.ok());
  await accion();
  const total = Number((await (await busqueda).json()).total ?? 0);
  await expect(paginador(page)).toHaveText(new RegExp(` de ${total}$`));
  return total;
}

async function abrirTodas(page: Page): Promise<number> {
  await page.getByText('Notificaciones', { exact: true }).click();
  const total = await esperandoBusqueda(page, () => page.getByText('Todas', { exact: true }).click());
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
  return total;
}

const panelTodas = (page: Page): Locator => page.getByRole('tabpanel', { name: 'Todas las notificaciones' });
// Texto del paginador de Axelor: «<desde> a <hasta> de <total>».
const paginador = (page: Page): Locator => panelTodas(page).getByRole('toolbar', { name: 'View toolbar' }).getByText(/^\d+ a \d+ de \d+$/);
// Filas de datos: el segundo rowgroup del grid (el primero son la cabecera y la fila de búsqueda).
const filasDeDatos = (page: Page): Locator => panelTodas(page).getByRole('grid').getByRole('rowgroup').nth(1).getByRole('row');

test.describe('Notificaciones — Todas', () => {
  test('Cancelar la elección de canal', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador ha iniciado sesión y abre «Notificaciones» → «Todas».
    const totalAntes = await abrirTodas(page);
    const filasAntes = await filasDeDatos(page).count();

    // Paso 2: Cuando pulsa «Nueva notificación».
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await expect(eleccion).toBeVisible();

    // Paso 3: Y en la ventana de elección de canal pulsa «Cancelar».
    await eleccion.getByRole('button', { name: 'Cancelar' }).click();

    // Resultado esperado: la ventana se cierra...
    await expect(eleccion).toBeHidden();
    await expect(page.getByRole('dialog')).toHaveCount(0);

    // ...se ve el listado...
    await expect(panelTodas(page)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await expect(panelTodas(page).getByRole('columnheader', { name: 'Motivo' })).toBeVisible();

    // ...y no se ha creado ninguna notificación (el listado sigue como estaba;
    // vacío si partía vacío). Se recarga para leer el estado real del servidor.
    const totalDespues = await esperandoBusqueda(page, () => page.reload());
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    expect(totalDespues).toBe(totalAntes);
    await expect(paginador(page)).toHaveText(new RegExp(` de ${totalAntes}$`));
    await expect(filasDeDatos(page)).toHaveCount(filasAntes);
    if (totalAntes === 0) {
      await expect(paginador(page)).toHaveText('0 a 0 de 0');
      await expect(filasDeDatos(page)).toHaveCount(0);
    }

    await logout(page);
  });
});
