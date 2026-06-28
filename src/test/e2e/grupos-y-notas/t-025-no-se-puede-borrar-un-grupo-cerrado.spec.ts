import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-025 — No se puede borrar un grupo cerrado
// origen: ESC-025  |  verifica: V-Grupo-009
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-025-no-se-puede-borrar-un-grupo-cerrado.desc.md

// Vuelve a la lista de Grupos (estado conocido). Si hay un formulario abierto, el botón
// "Cancelar" (acción back) regresa a la lista; si ya estamos en la lista, no hace nada.
async function irAListaGrupos(page: Page): Promise<void> {
  const nuevoGrupo = page.getByRole('button', { name: 'Nuevo grupo' });
  if (await nuevoGrupo.isVisible().catch(() => false)) return;
  const cancelar = page.getByRole('button', { name: 'Cancelar' });
  if (await cancelar.first().isVisible().catch(() => false)) {
    await cancelar.first().click();
  }
  await expect(nuevoGrupo).toBeVisible();
}

test.describe('Grupos (supervisor)', () => {
  test('No se puede borrar un grupo cerrado', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el supervisor del CIPFP Mislata inicia sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Idempotencia (BD compartida sin reset entre ejecuciones):
    // El nombre del grupo es único por ejecución (sufijo con Date.now()) para no chocar con
    // el unique-constraint de nombre al reejecutar. IMPORTANTE: este test deja el grupo en
    // estado CERRADO y, por definición de este propio test, un grupo cerrado NO se puede
    // borrar; por tanto NO hay teardown que lo elimine: el grupo queda persistido a
    // propósito. El nombre único garantiza que las reejecuciones no colisionen.
    const grupo = `1º DAM A t025-${Date.now()}`;

    // Abrir la pantalla "Grupos".
    await page.getByText('Notas', { exact: true }).click();
    await page.getByText('Grupos', { exact: true }).click();
    await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

    // Paso 1: Dado que el supervisor crea el grupo "1º DAM A" (curso "1º DAM")...
    await page.getByRole('button', { name: 'Nuevo grupo' }).click();
    await page.getByRole('textbox', { name: 'Nombre' }).fill(grupo);
    await page.getByRole('combobox', { name: 'Curso' }).click();
    await page.getByRole('option', { name: '1º DAM', exact: true }).click();
    await expect(page.getByRole('combobox', { name: 'Curso' })).toHaveValue('1º DAM');
    // Guardar el grupo; la app vuelve a la lista. Se reabre para poder cerrarlo.
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
    await page.getByText(grupo, { exact: true }).click();

    // Paso 1 (cont.): ...y pulsa "Cerrar grupo".
    // El grupo abierto muestra el botón "Cerrar grupo" en la barra de acciones.
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Abierto');
    await expect(page.getByRole('button', { name: 'Cerrar grupo' })).toBeVisible();
    await page.getByRole('button', { name: 'Cerrar grupo' }).click();
    // Al cerrarlo, el estado pasa a "Cerrado" (se persiste de inmediato).
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Cerrado');

    // Paso 2: Cuando con el grupo "Cerrado" intenta borrarlo.
    // Se regresa a la lista y se reabre el grupo (ya CERRADO) desde ahí, para operar sobre
    // el estado tal y como queda persistido (carga fresca del formulario).
    await irAListaGrupos(page);
    await page.getByText(grupo, { exact: true }).click();
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Cerrado');

    // El botón "Borrar" sigue presente en la UI; la prohibición se aplica en el servidor.
    // Al pulsarlo, Axelor pide confirmación antes de eliminar el registro.
    await page.getByRole('button', { name: 'Borrar', exact: true }).click();
    await expect(
      page.getByText('¿Realmente quieres eliminar el registro seleccionado?'),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Eliminar', exact: true }).click();

    // Resultado esperado:
    // - El sistema muestra "No se puede borrar un grupo cerrado".
    //   El servidor rechaza el borrado y devuelve el mensaje en un diálogo de error.
    await expect(page.getByText('No se puede borrar un grupo cerrado')).toBeVisible();
    // Cerrar el diálogo de error para poder seguir comprobando el estado.
    await page.getByRole('button', { name: 'Aceptar' }).click();

    // - El grupo "1º DAM A" sigue existiendo: al volver a la lista, ahí está.
    await irAListaGrupos(page);
    await expect(page.getByText(grupo, { exact: true })).toBeVisible();

    await logout(page);
  });
});
