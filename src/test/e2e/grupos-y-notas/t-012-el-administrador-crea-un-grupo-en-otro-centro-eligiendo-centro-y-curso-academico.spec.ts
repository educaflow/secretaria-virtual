import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-012 — El administrador crea un grupo en otro centro eligiendo centro y curso académico
// origen: ESC-012  |  verifica: R-Grupo-001, R-Grupo-002, U-grupos-administrador-001
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-012-el-administrador-crea-un-grupo-en-otro-centro-eligiendo-centro-y-curso-academico.desc.md

// Nota sobre los datos reales de la app:
// - El curso que el .desc.md llama "1º SMR" aparece en el catálogo educativo con el
//   título real "1º 1SMR"; sus módulos son "Montaje y Mantenimiento de Equipos",
//   "Redes Locales" y "Sistemas Operativos Monopuesto".
// - Tras reabrir el grupo, "Curso académico" se muestra con separador de millares ("2,024").

// Abre la pantalla de administrador "Grupos (administración)" (menú Administración SV).
async function abrirGruposAdmin(page: Page): Promise<void> {
  await page.getByText('Administración SV', { exact: true }).click();
  await page.getByText('Grupos (administración)', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

// Teardown idempotente: si el grupo existe, lo abre y lo borra (libera alumnos por orphanRemoval).
async function borrarGrupoSiExiste(page: Page, nombre: string): Promise<void> {
  await abrirGruposAdmin(page);
  const fila = page.getByText(nombre, { exact: true });
  if ((await fila.count()) === 0) return;
  await fila.first().click();
  await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(nombre);
  await page.getByRole('button', { name: 'Borrar' }).click();
  // Diálogo de confirmación de Axelor (en inglés): "Do you really want to delete...".
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

test.describe('Grupos (administración)', () => {
  test('El administrador crea un grupo en otro centro eligiendo centro y curso académico', async ({ page }) => {
    // Nombre único por ejecución: BD compartida sin reset.
    const nombre = `1º SMR A t012-${Date.now()}`;

    await ensureLoggedOut(page);
    // Precondición: el usuario admin ha iniciado sesión.
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla "Grupos (administración)".
      await abrirGruposAdmin(page);

      // Paso 2: Cuando pulsa "Nuevo grupo", escribe el nombre, elige el centro "CIPFP Batoi",
      //         el curso académico "2024" y el curso "1º SMR" (real: "1º 1SMR").
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await page.getByRole('textbox', { name: 'Nombre' }).fill(nombre);

      await page.getByRole('combobox', { name: 'Centro' }).click();
      await page.getByRole('option', { name: 'CIPFP Batoi', exact: true }).click();
      await expect(page.getByRole('combobox', { name: 'Centro' })).toHaveValue('CIPFP Batoi');

      await page.getByRole('spinbutton', { name: /Curso académico/ }).fill('2024');

      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: '1º 1SMR', exact: true }).click();
      await expect(page.getByRole('combobox', { name: 'Curso' })).toHaveValue('1º 1SMR');

      // Paso 3: Y pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar' }).click();
      // Tras guardar, la app vuelve a la lista; reabrimos el grupo para comprobar el resultado.
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      await page.getByText(nombre, { exact: true }).click();
      await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(nombre);

      // Resultado esperado 1: el grupo se crea en estado "Abierto" en el centro "CIPFP Batoi"
      //                       con curso académico "2024".
      await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Abierto');
      await expect(page.getByRole('textbox', { name: 'Centro' })).toHaveValue('CIPFP Batoi');
      await expect(page.getByRole('textbox', { name: /Curso académico/ })).toHaveValue('2,024');

      // Resultado esperado 2: el panel "Módulos" muestra los módulos del curso "1º SMR".
      const panelModulos = page.getByRole('region', { name: 'Módulos' });
      await expect(panelModulos.getByRole('row', { name: 'Montaje y Mantenimiento de Equipos' })).toBeVisible();
      await expect(panelModulos.getByRole('row', { name: 'Redes Locales' })).toBeVisible();
      await expect(panelModulos.getByRole('row', { name: 'Sistemas Operativos Monopuesto' })).toBeVisible();
    } finally {
      // Teardown idempotente: borra el grupo creado para no contaminar la BD compartida.
      await borrarGrupoSiExiste(page, nombre).catch(() => {});
      await logout(page).catch(() => {});
    }
  });
});
