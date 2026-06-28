import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-018 — Añadir un alumno sin elegir ninguno
// origen: ESC-018  |  verifica: V-AlumnoGrupo-002
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-018-anadir-un-alumno-sin-elegir-ninguno.desc.md

// Vuelve a la lista de Grupos (estado conocido). Si hay un formulario abierto, el botón
// "Cancelar" (acción back) regresa a la lista; si el formulario está sucio, aparece el
// diálogo "Pregunta" de descarte de cambios, que se confirma con "Aceptar".
async function irAListaGrupos(page: Page): Promise<void> {
  const nuevoGrupo = page.getByRole('button', { name: 'Nuevo grupo' });
  if (await nuevoGrupo.isVisible().catch(() => false)) return;
  const cancelar = page.getByRole('button', { name: 'Cancelar' });
  if (await cancelar.first().isVisible().catch(() => false)) {
    await cancelar.first().click();
    // Si había cambios sin guardar, Axelor pide confirmación antes de descartarlos.
    const aceptar = page.getByRole('button', { name: 'Aceptar' });
    if (await aceptar.isVisible().catch(() => false)) {
      await aceptar.click();
    }
  }
  await expect(nuevoGrupo).toBeVisible();
}

// Teardown idempotente: borra el grupo `nombre` si existe en la lista. La composición
// Grupo→AlumnoGrupo es orphanRemoval, así que borrar el grupo limpia también sus alumnos.
async function borrarGrupoSiExiste(page: Page, nombre: string): Promise<void> {
  await irAListaGrupos(page);
  const fila = page.getByText(nombre, { exact: true });
  if ((await fila.count()) === 0) return;
  await fila.first().click();
  await expect(page.getByRole('button', { name: 'Borrar' })).toBeVisible();
  await page.getByRole('button', { name: 'Borrar' }).click();
  // Diálogo "Pregunta": ¿Realmente quieres eliminar el registro seleccionado?
  await page.getByRole('button', { name: 'Eliminar' }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

test.describe('Grupos (supervisor)', () => {
  test('Añadir un alumno sin elegir ninguno', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el supervisor del CIPFP Mislata inicia sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Idempotencia (BD compartida entre ejecuciones, no se resetea): nombre de grupo único
    // por ejecución (sufijo con Date.now()) para no chocar con el unique-constraint de nombre,
    // y teardown en `finally` que borra el grupo creado.
    const grupo = `1º DAM A t018-${Date.now()}`;

    try {
      // Abrir la pantalla "Grupos".
      await page.getByText('Notas', { exact: true }).click();
      await page.getByText('Grupos', { exact: true }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Paso 1: Dado que el supervisor crea el grupo "1º DAM A" (curso "1º DAM").
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await page.getByRole('textbox', { name: 'Nombre' }).fill(grupo);
      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: '1º DAM', exact: true }).click();
      await expect(
        page.getByRole('button', { name: '1º DAM' }).or(page.getByRole('combobox', { name: 'Curso' })),
      ).toBeVisible();
      // Guardar el grupo; la app vuelve a la lista. Se reabre para operar sobre Alumnos.
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      await page.getByText(grupo, { exact: true }).click();

      const panelAlumnos = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnos.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();

      // Paso 2: Cuando en el panel "Alumnos" pulsa "Añadir alumno" y, sin elegir ningún
      //         alumno, guarda. El diálogo "Alumno del grupo" tiene su propio "Guardar":
      //         al pulsarlo sin selección se cierra y deja una fila vacía pendiente; la
      //         validación del servidor salta al guardar el grupo completo.
      await panelAlumnos.getByRole('button', { name: 'Añadir alumno' }).click();
      const dialogoAlumno = page.getByRole('dialog');
      await expect(dialogoAlumno.getByRole('heading', { name: 'Alumno del grupo' })).toBeVisible();
      await dialogoAlumno.getByRole('button', { name: 'Guardar' }).click();
      // El diálogo se cierra y deja la fila vacía pendiente.
      await expect(dialogoAlumno.getByRole('heading', { name: 'Alumno del grupo' })).toBeHidden();
      // Guardar el grupo completo dispara la validación. Tras cerrarse el diálogo conviven dos
      // botones "Guardar" (el del formulario, activo, y el restante del diálogo, deshabilitado):
      // se pulsa el activo.
      await page.getByRole('button', { name: 'Guardar' }).and(page.locator(':enabled')).click();

      // Resultado esperado 1: el sistema muestra "Debe elegir un alumno".
      const dialogoError = page.getByRole('dialog');
      await expect(dialogoError.getByText('Debe elegir un alumno')).toBeVisible();
      // Cerrar el diálogo de error.
      await dialogoError.getByRole('button', { name: 'Aceptar' }).click();

      // Resultado esperado 2: no se añade nada al grupo. Se descartan los cambios (la fila
      // vacía transitoria) y se reabre el grupo: el panel "Alumnos" no tiene ninguna fila
      // de datos (solo la cabecera), confirmando que la validación impidió persistir nada.
      await page.getByRole('button', { name: 'Cancelar' }).click();
      // Diálogo "Pregunta": los cambios actuales se perderán. ¿Realmente quieres continuar?
      await page.getByRole('button', { name: 'Aceptar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      await page.getByText(grupo, { exact: true }).click();
      const panelAlumnosVerif = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnosVerif.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();
      // Solo la fila de cabecera ("Alumno" / "Nota media"); ninguna fila de datos.
      await expect(panelAlumnosVerif.getByRole('row')).toHaveCount(1);
    } finally {
      // Teardown idempotente: borra el grupo creado. Best-effort para no enmascarar un fallo
      // real de las aserciones de arriba.
      try {
        await borrarGrupoSiExiste(page, grupo);
      } catch {
        // ignorado: la limpieza es de mejor esfuerzo.
      }
    }

    await logout(page);
  });
});
