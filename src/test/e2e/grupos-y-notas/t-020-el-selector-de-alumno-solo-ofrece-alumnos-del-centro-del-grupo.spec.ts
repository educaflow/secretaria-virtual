import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-020 — El selector de alumno solo ofrece alumnos del centro del grupo
// origen: ESC-020  |  verifica: V-AlumnoGrupo-004, U-grupos-supervisor-006
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-020-el-selector-de-alumno-solo-ofrece-alumnos-del-centro-del-grupo.desc.md

// Vuelve a la lista de Grupos (estado conocido). Si hay un formulario abierto, el botón
// "Cancelar" (acción back) regresa a la lista; si el formulario está sucio, aparece el
// diálogo "Pregunta" de descarte de cambios, que se confirma con "Aceptar".
async function irAListaGrupos(page: Page): Promise<void> {
  const nuevoGrupo = page.getByRole('button', { name: 'Nuevo grupo' });
  if (await nuevoGrupo.isVisible().catch(() => false)) return;
  // Cierra un posible desplegable de opciones abierto y el diálogo "Alumno del grupo"
  // (modal que, si queda abierto, bloquea el resto de interacciones).
  await page.keyboard.press('Escape').catch(() => {});
  const dialogo = page.getByRole('dialog');
  if (await dialogo.isVisible().catch(() => false)) {
    await dialogo.getByRole('button', { name: 'Cancelar' }).click().catch(() => {});
    const aceptarDlg = page.getByRole('button', { name: 'Aceptar' });
    if (await aceptarDlg.isVisible().catch(() => false)) {
      await aceptarDlg.click().catch(() => {});
    }
  }
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
  test('El selector de alumno solo ofrece alumnos del centro del grupo', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el supervisor del CIPFP Mislata inicia sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Idempotencia (BD compartida entre ejecuciones, no se resetea): nombre de grupo único
    // por ejecución (sufijo con Date.now()) para no chocar con el unique-constraint de nombre,
    // y teardown en `finally` que borra el grupo creado.
    const grupo = `1º DAM A t020-${Date.now()}`;

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

      // El grupo pertenece al centro "CIPFP Mislata" (centro del supervisor, fijado por el servidor).
      await expect(page.getByRole('textbox', { name: 'Centro' })).toHaveValue('CIPFP Mislata');

      const panelAlumnos = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnos.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();

      // Paso 2: Cuando en el panel "Alumnos" pulsa "Añadir alumno" y abre el selector de alumno.
      await panelAlumnos.getByRole('button', { name: 'Añadir alumno' }).click();
      const dialogoAlumno = page.getByRole('dialog');
      await expect(dialogoAlumno.getByRole('heading', { name: 'Alumno del grupo' })).toBeVisible();
      await dialogoAlumno.getByRole('combobox', { name: 'Alumno' }).click();

      // El desplegable de opciones de Axelor se renderiza en un portal (listbox "Select options").
      const listbox = page.getByRole('listbox', { name: 'Select options' });
      await expect(listbox).toBeVisible();

      // Resultado esperado 1: el selector ofrece a "Alumno1..4 CIPFP Mislata" (alumnos del propio centro).
      await expect(listbox.getByRole('option', { name: 'Alumno1 CIPFP Mislata', exact: true })).toBeVisible();
      await expect(listbox.getByRole('option', { name: 'Alumno2 CIPFP Mislata', exact: true })).toBeVisible();
      await expect(listbox.getByRole('option', { name: 'Alumno3 CIPFP Mislata', exact: true })).toBeVisible();
      await expect(listbox.getByRole('option', { name: 'Alumno4 CIPFP Mislata', exact: true })).toBeVisible();

      // Resultado esperado 2: el selector NO ofrece a "Director CIPFP Mislata" (profesor del propio
      // centro, no alumno) ni a "Alumno1 CIPFP Batoi" (alumna de otro centro).
      await expect(listbox.getByRole('option', { name: 'Director CIPFP Mislata', exact: true })).toHaveCount(0);
      await expect(listbox.getByRole('option', { name: 'Alumno1 CIPFP Batoi', exact: true })).toHaveCount(0);

      // Cierra el desplegable de opciones y el diálogo "Alumno del grupo" (sin seleccionar
      // ningún alumno) para dejar la página en estado conocido antes del teardown.
      await page.keyboard.press('Escape');
      await dialogoAlumno.getByRole('button', { name: 'Cancelar' }).click();
      await expect(dialogoAlumno.getByRole('heading', { name: 'Alumno del grupo' })).toBeHidden();
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
