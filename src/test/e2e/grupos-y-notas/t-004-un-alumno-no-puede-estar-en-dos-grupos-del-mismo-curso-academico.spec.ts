import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-004 — Un alumno no puede estar en dos grupos del mismo curso académico
// origen: ESC-004  |  verifica: V-AlumnoGrupo-005
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-004-un-alumno-no-puede-estar-en-dos-grupos-del-mismo-curso-academico.desc.md

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

// Teardown idempotente: borra el grupo `nombre` si existe en la lista, liberando a su
// alumno (la composición Grupo→AlumnoGrupo es orphanRemoval, así que borrar el grupo
// elimina en cascada la pertenencia del alumno).
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
  test('Un alumno no puede estar en dos grupos del mismo curso académico', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el supervisor del CIPFP Mislata inicia sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Idempotencia (BD compartida entre ejecuciones):
    // 1) Nombres de grupo únicos por ejecución (sufijo con Date.now()). El escenario usa
    //    "1º DAM A" y "1º DAM B", pero lo que verifica V-AlumnoGrupo-005 es la pertenencia
    //    a un único grupo del MISMO CURSO ACADÉMICO (el año 2024 del centro), no los nombres
    //    literales; así reejecutar no choca con el unique-constraint de nombre.
    // 2) La validación V-AlumnoGrupo-005 se evalúa por (alumno, curso académico): una vez que
    //    el alumno queda en un grupo de 2024, NO puede entrar en ningún otro grupo de 2024. Por
    //    eso el alumno NO puede reutilizarse entre ejecuciones si queda persistido. Se usa
    //    "Alumno6 CIPFP Mislata" (alumno del centro, libre en el seed) y, sobre todo, el bloque
    //    `finally` BORRA los grupos creados al terminar (también si una aserción falla), lo que
    //    libera al alumno y deja la BD como estaba: el test es repetible. El alumno concreto es
    //    irrelevante para lo que el escenario verifica.
    const tag = `t004-${Date.now()}`;
    const grupoA = `1º DAM A ${tag}`;
    const grupoB = `1º DAM B ${tag}`;
    const alumno = 'Alumno6 CIPFP Mislata';

    try {
      // Abrir la pantalla "Grupos".
      await page.getByText('Notas', { exact: true }).click();
      await page.getByText('Grupos', { exact: true }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Paso 1: Dado que el supervisor crea el grupo "1º DAM A" (curso "1º DAM")
      //         y le añade al alumno.
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await page.getByRole('textbox', { name: 'Nombre' }).fill(grupoA);
      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: '1º DAM', exact: true }).click();
      await expect(page.getByRole('combobox', { name: 'Curso' })).toHaveValue('1º DAM');
      // Guardar el grupo; la app vuelve a la lista. Se reabre para añadir el alumno.
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      await page.getByText(grupoA, { exact: true }).click();

      const panelAlumnosA = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnosA.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();
      await panelAlumnosA.getByRole('button', { name: 'Añadir alumno' }).click();
      let dialogo = page.getByRole('dialog');
      await dialogo.getByRole('combobox', { name: 'Alumno' }).click();
      await page.getByRole('option', { name: alumno, exact: true }).click();
      await dialogo.getByRole('button', { name: 'Guardar' }).click();
      await expect(panelAlumnosA.getByRole('row', { name: alumno })).toBeVisible();
      // Guardar el grupo "1º DAM A" con su alumno; la app vuelve a la lista.
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      await expect(page.getByText(grupoA, { exact: true })).toBeVisible();

      // Paso 2: Y crea el grupo "1º DAM B" (curso "1º DAM").
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await page.getByRole('textbox', { name: 'Nombre' }).fill(grupoB);
      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: '1º DAM', exact: true }).click();
      await expect(page.getByRole('combobox', { name: 'Curso' })).toHaveValue('1º DAM');
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      // Reabrir "1º DAM B" para operar sobre su panel "Alumnos".
      await page.getByText(grupoB, { exact: true }).click();
      const panelAlumnosB = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnosB.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();

      // Paso 3: Cuando en el panel "Alumnos" de "1º DAM B" pulsa "Añadir alumno",
      //         elige al mismo alumno y guarda.
      await panelAlumnosB.getByRole('button', { name: 'Añadir alumno' }).click();
      dialogo = page.getByRole('dialog');
      await dialogo.getByRole('combobox', { name: 'Alumno' }).click();
      await page.getByRole('option', { name: alumno, exact: true }).click();
      await dialogo.getByRole('button', { name: 'Guardar' }).click();
      await expect(panelAlumnosB.getByRole('row', { name: alumno })).toBeVisible();
      // Guardar el grupo dispara la validación de pertenencia única por curso académico.
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado 1: el sistema muestra
      //   "El alumno ya pertenece a otro grupo de este curso académico".
      await expect(
        page.getByText('El alumno ya pertenece a otro grupo de este curso académico'),
      ).toBeVisible();
      await page.getByRole('button', { name: 'Aceptar' }).click();

      // Resultado esperado 2: el alumno no se añade a "1º DAM B".
      // Se descartan los cambios no guardados y se reabre el grupo: el panel "Alumnos"
      // no contiene al alumno (la validación impidió persistirlo).
      await page.getByRole('button', { name: 'Cancelar' }).click();
      await page.getByRole('button', { name: 'Aceptar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      await page.getByText(grupoB, { exact: true }).click();
      const panelAlumnosVerif = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnosVerif.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();
      await expect(panelAlumnosVerif.getByRole('row', { name: alumno })).toHaveCount(0);
    } finally {
      // Teardown idempotente: borra los grupos creados (libera al alumno). Best-effort para
      // no enmascarar un fallo real de las aserciones de arriba.
      try {
        await borrarGrupoSiExiste(page, grupoB);
        await borrarGrupoSiExiste(page, grupoA);
      } catch {
        // ignorado: la limpieza es de mejor esfuerzo.
      }
    }

    await logout(page);
  });
});
