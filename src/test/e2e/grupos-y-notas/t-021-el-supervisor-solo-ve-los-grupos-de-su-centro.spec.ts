import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-021 — El supervisor solo ve los grupos de su centro
// origen: ESC-021  |  verifica: —
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-021-el-supervisor-solo-ve-los-grupos-de-su-centro.desc.md

// Abre la pantalla de administrador "Grupos (administración)" (menú Administración SV).
async function abrirGruposAdmin(page: Page): Promise<void> {
  await page.getByText('Administración SV', { exact: true }).click();
  await page.getByText('Grupos (administración)', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

// Abre la pantalla de supervisor "Grupos" (menú Notas).
async function abrirGruposSupervisor(page: Page): Promise<void> {
  await page.getByText('Notas', { exact: true }).click();
  await page.getByText('Grupos', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

// Teardown idempotente: si el grupo `nombre` aparece en la lista actual, lo abre y lo borra.
// La composición Grupo→AlumnoGrupo es orphanRemoval, así que borrar el grupo limpia sus hijos.
async function borrarGrupoSiExiste(page: Page, nombre: string): Promise<void> {
  const fila = page.getByText(nombre, { exact: true });
  if ((await fila.count()) === 0) return;
  await fila.first().click();
  await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(nombre);
  await page.getByRole('button', { name: 'Borrar' }).click();
  // Diálogo de confirmación de Axelor; el botón del pie aparece como "Eliminar" (es) o "Delete" (en)
  // según el idioma del usuario, así que se acepta cualquiera de las dos variantes.
  await page.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

test.describe('Grupos (supervisor)', () => {
  test('El supervisor solo ve los grupos de su centro', async ({ page }) => {
    // BD compartida sin reset: nombres únicos por ejecución para no chocar con el
    // unique-constraint de nombre ni con datos de ejecuciones previas.
    const sufijo = Date.now();
    const grupoBatoi = `1º SMR A t021-${sufijo}`; // creado por el admin en CIPFP Batoi (el supervisor NO debe verlo)
    const grupoMislata = `1º DAM A t021-${sufijo}`; // creado por el supervisor en CIPFP Mislata (sí debe verlo)

    try {
      // Paso 1: Dado que el administrador crea el grupo "1º SMR A" en el centro "CIPFP Batoi"
      //         (curso académico "2024", curso "1º SMR", real "1º 1SMR") desde
      //         "Grupos (administración)"; después cierra sesión.
      await ensureLoggedOut(page);
      await login(page, 'admin', 'admin');
      await abrirGruposAdmin(page);
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await page.getByRole('textbox', { name: 'Nombre' }).fill(grupoBatoi);
      await page.getByRole('combobox', { name: 'Centro' }).click();
      await page.getByRole('option', { name: 'CIPFP Batoi', exact: true }).click();
      await expect(page.getByRole('combobox', { name: 'Centro' })).toHaveValue('CIPFP Batoi');
      await page.getByRole('spinbutton', { name: /Curso académico/ }).fill('2024');
      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: '1º 1SMR', exact: true }).click();
      await expect(page.getByRole('combobox', { name: 'Curso' })).toHaveValue('1º 1SMR');
      await page.getByRole('button', { name: 'Guardar' }).click();
      // Tras guardar vuelve a la lista; el grupo de Batoi debe quedar creado.
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      await expect(page.getByText(grupoBatoi, { exact: true })).toBeVisible();
      await logout(page);

      // Paso 2: Y el supervisor de "CIPFP Mislata" inicia sesión y crea el grupo "1º DAM A"
      //         (curso "1º DAM"). El centro lo fija el servidor (CIPFP Mislata).
      await login(page, 'supervisor1@mislata.es', 'demo1234');
      await abrirGruposSupervisor(page);
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await page.getByRole('textbox', { name: 'Nombre' }).fill(grupoMislata);
      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: '1º DAM', exact: true }).click();
      await expect(
        page.getByRole('button', { name: '1º DAM' }).or(page.getByRole('combobox', { name: 'Curso' })),
      ).toBeVisible();
      await page.getByRole('button', { name: 'Guardar' }).click();
      // Tras guardar, la app vuelve a la lista de "Grupos" del supervisor (Paso 3).
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Paso 3: Cuando el supervisor abre "Grupos" (ya está en su lista tras guardar).
      // Resultado esperado 1: el listado muestra "1º DAM A" (de "CIPFP Mislata").
      await expect(page.getByText(grupoMislata, { exact: true })).toBeVisible();
      // Resultado esperado 2: el listado NO muestra "1º SMR A" (de "CIPFP Batoi").
      await expect(page.getByText(grupoBatoi, { exact: true })).toHaveCount(0);
    } finally {
      // Teardown idempotente y best-effort (no enmascarar un fallo real de las aserciones):
      // 1) borra el grupo de Mislata con el supervisor.
      try {
        await ensureLoggedOut(page);
        await login(page, 'supervisor1@mislata.es', 'demo1234');
        await abrirGruposSupervisor(page);
        await borrarGrupoSiExiste(page, grupoMislata);
        await logout(page);
      } catch {
        // limpieza best-effort
      }
      // 2) borra el grupo de Batoi con el admin (el supervisor no puede verlo).
      try {
        await ensureLoggedOut(page);
        await login(page, 'admin', 'admin');
        await abrirGruposAdmin(page);
        await borrarGrupoSiExiste(page, grupoBatoi);
        await logout(page);
      } catch {
        // limpieza best-effort
      }
    }
  });
});
