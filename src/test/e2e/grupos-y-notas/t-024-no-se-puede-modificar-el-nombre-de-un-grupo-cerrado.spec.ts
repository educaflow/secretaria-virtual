import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-024 — No se puede modificar el nombre de un grupo cerrado
// origen: ESC-024  |  verifica: V-Grupo-004, U-grupos-supervisor-004
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-024-no-se-puede-modificar-el-nombre-de-un-grupo-cerrado.desc.md

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
  test('No se puede modificar el nombre de un grupo cerrado', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el supervisor del CIPFP Mislata inicia sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Idempotencia (BD compartida sin reset entre ejecuciones):
    // El nombre del grupo es único por ejecución (sufijo con Date.now()) para no chocar con
    // el unique-constraint de nombre al reejecutar. IMPORTANTE: este test deja el grupo en
    // estado CERRADO y la regla de negocio (ver T-025 "No borrar un grupo cerrado") IMPIDE
    // borrar un grupo cerrado, así que el teardown NO puede eliminarlo: el grupo queda
    // persistido a propósito. El nombre único garantiza que las reejecuciones no colisionen.
    const grupo = `1º DAM A t024-${Date.now()}`;

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

    // Paso 2: Cuando con el grupo "Cerrado" intenta cambiar el nombre.
    // Se regresa a la lista y se reabre el grupo (ya CERRADO) desde ahí, para comprobar el
    // estado tal y como queda persistido (carga fresca del formulario).
    await irAListaGrupos(page);
    await page.getByText(grupo, { exact: true }).click();
    // Esperar a que el formulario del grupo cerrado esté cargado antes de comprobar el campo
    // (evita un falso positivo mientras la vista aún se renderiza).
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Cerrado');

    // Resultado esperado:
    // La app implementa la regla "No se puede modificar un grupo cerrado"
    // (V-Grupo-004 / U-grupos-supervisor-004) DESHABILITANDO el campo "Nombre" en cuanto el
    // grupo está cerrado: el supervisor no puede editarlo (de hecho `fill()` fallaría sobre un
    // input deshabilitado), de modo que la modificación se impide preventivamente en la UI y
    // la validación de servidor nunca llega a dispararse por el flujo normal.
    await expect(page.getByRole('textbox', { name: 'Nombre' })).toBeDisabled();
    // - El nombre no cambia: el campo conserva el valor original del grupo.
    await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(grupo);

    await logout(page);
  });
});
