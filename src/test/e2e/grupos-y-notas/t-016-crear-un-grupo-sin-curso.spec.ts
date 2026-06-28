import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-016 — Crear un grupo sin curso
// origen: ESC-016  |  verifica: V-Grupo-002
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-016-crear-un-grupo-sin-curso.desc.md
test.describe('Grupos (supervisor)', () => {
  test('Crear un grupo sin curso', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el supervisor del CIPFP Mislata inicia sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Nombre único por ejecución (BD compartida sin reset). El guardado FALLA,
    // así que no debería persistirse nada; el sufijo es defensivo por si acaso.
    const nombre = `1º DAM A t016-${Date.now()}`;

    // Paso 1: Dado que el supervisor está en la pantalla "Grupos".
    await page.getByText('Notas', { exact: true }).click();
    await page.getByText('Grupos', { exact: true }).click();
    await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

    // Paso 2: Cuando pulsa "Nuevo grupo", escribe el nombre y deja el curso vacío.
    await page.getByRole('button', { name: 'Nuevo grupo' }).click();
    await expect(page.getByRole('textbox', { name: 'Nombre' })).toBeVisible();
    await page.getByRole('textbox', { name: 'Nombre' }).fill(nombre);
    // El curso se deja vacío deliberadamente.
    await expect(page.getByRole('combobox', { name: 'Curso' })).toHaveValue('');

    // Paso 3: Y pulsa "Guardar".
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado 1: el sistema indica que el curso es obligatorio.
    // La app muestra la validación de campo requerido de Axelor: "Curso es requerido".
    await expect(page.getByText('Curso es requerido')).toBeVisible();

    // Resultado esperado 2: no se crea el grupo.
    // El guardado queda bloqueado: el formulario sigue siendo el registro nuevo sin
    // guardar (pestaña "Grupo*" con marca de cambios pendientes) y los botones de
    // edición "Guardar"/"Cancelar" siguen presentes, es decir, no se persistió nada.
    await expect(page.getByRole('tab', { name: 'Grupo*' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Guardar' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cancelar' })).toBeVisible();

    // Descartar el registro nuevo para dejar la app limpia antes de cerrar sesión.
    await page.getByRole('button', { name: 'Cancelar' }).click();

    await logout(page);
  });
});
