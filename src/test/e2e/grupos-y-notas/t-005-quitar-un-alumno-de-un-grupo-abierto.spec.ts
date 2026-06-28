import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-005 — Quitar un alumno de un grupo abierto
// origen: ESC-005  |  verifica: V-AlumnoGrupo-006
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-005-quitar-un-alumno-de-un-grupo-abierto.desc.md

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

// Teardown idempotente: borra el grupo `nombre` si existe en la lista, liberando a sus
// alumnos (la composición Grupo→AlumnoGrupo es orphanRemoval, así que borrar el grupo
// elimina en cascada la pertenencia de sus alumnos).
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

// Añade un alumno al panel "Alumnos" del grupo abierto vía el botón "Añadir alumno"
// (abre un diálogo con un selector de alumno y su propio "Guardar").
async function anadirAlumno(page: Page, panelAlumnos: ReturnType<Page['getByRole']>, alumno: string): Promise<void> {
  await panelAlumnos.getByRole('button', { name: 'Añadir alumno' }).click();
  const dialogo = page.getByRole('dialog');
  await dialogo.getByRole('combobox', { name: 'Alumno' }).click();
  await page.getByRole('option', { name: alumno, exact: true }).click();
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  await expect(panelAlumnos.getByRole('row', { name: alumno })).toBeVisible();
}

test.describe('Grupos (supervisor)', () => {
  test('Quitar un alumno de un grupo abierto', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el supervisor del CIPFP Mislata inicia sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Idempotencia (BD compartida entre ejecuciones, no se resetea):
    // 1) Nombre de grupo único por ejecución (sufijo con Date.now()) para no chocar con el
    //    unique-constraint de nombre al reejecutar.
    // 2) Se usan "Alumno3" y "Alumno4 CIPFP Mislata", alumnos del seed dedicados a este test
    //    (los demás tests de regresión usan Alumno1/Alumno2 —T-001— y Alumno6 —T-004—; este
    //    par disjunto evita que la validación de pertenencia única por curso académico
    //    (V-AlumnoGrupo-005) provoque colisiones si los tests corren en paralelo). El escenario
    //    original nombra "Alumno1" (permanece) y "Alumno2" (se quita): aquí "Alumno3" permanece
    //    y "Alumno4" se quita; los alumnos concretos son irrelevantes para lo que verifica
    //    V-AlumnoGrupo-006 (que quitar un alumno de un grupo abierto lo elimina del grupo).
    // 3) El bloque `finally` BORRA el grupo creado al terminar (también si una aserción falla),
    //    lo que libera a sus alumnos y deja la BD como estaba: el test es repetible en verde.
    const tag = `t005-${Date.now()}`;
    const grupo = `1º DAM A ${tag}`;
    const alumnoPermanece = 'Alumno3 CIPFP Mislata';
    const alumnoQuitado = 'Alumno4 CIPFP Mislata';

    try {
      // Abrir la pantalla "Grupos".
      await page.getByText('Notas', { exact: true }).click();
      await page.getByText('Grupos', { exact: true }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Paso 1: Dado que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade
      //         a "Alumno1 CIPFP Mislata" y "Alumno2 CIPFP Mislata".
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await page.getByRole('textbox', { name: 'Nombre' }).fill(grupo);
      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: '1º DAM', exact: true }).click();
      await expect(page.getByRole('combobox', { name: 'Curso' })).toHaveValue('1º DAM');
      // Guardar el grupo; la app vuelve a la lista. Se reabre para añadir los alumnos.
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      await page.getByText(grupo, { exact: true }).click();

      const panelAlumnos = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnos.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();
      await anadirAlumno(page, panelAlumnos, alumnoPermanece);
      await anadirAlumno(page, panelAlumnos, alumnoQuitado);
      // Guardar el grupo con sus dos alumnos; la app vuelve a la lista.
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Reabrir el grupo para operar sobre el panel "Alumnos".
      await page.getByText(grupo, { exact: true }).click();
      const panelAlumnosEdit = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnosEdit.getByRole('row', { name: alumnoPermanece })).toBeVisible();
      await expect(panelAlumnosEdit.getByRole('row', { name: alumnoQuitado })).toBeVisible();

      // Paso 2: Cuando en el panel "Alumnos" quita a "Alumno2 CIPFP Mislata".
      // Al pulsar la fila del alumno se abre su editor "Alumno del grupo" y la fila queda
      // seleccionada; se cierra el editor con "Cancelar" (la fila sigue seleccionada) y aparece
      // el botón de papelera (delete) en la barra del panel. El botón de papelera no tiene
      // nombre accesible (es un icono), por eso se localiza por su testid dentro del panel.
      await panelAlumnosEdit.getByRole('row', { name: alumnoQuitado }).click();
      const editorAlumno = page.getByRole('dialog');
      await expect(editorAlumno.getByRole('heading', { name: 'Alumno del grupo' })).toBeVisible();
      await editorAlumno.getByRole('button', { name: 'Cancelar' }).click();
      const botonQuitar = panelAlumnosEdit.getByTestId('item:delete');
      await expect(botonQuitar).toBeVisible();
      await botonQuitar.click();
      // Diálogo "Pregunta": ¿Realmente quieres eliminar el/los registro(s) seleccionado(s)?
      await page.getByRole('button', { name: 'Eliminar' }).click();
      // La fila desaparece del panel ya antes de guardar.
      await expect(panelAlumnosEdit.getByRole('row', { name: alumnoQuitado })).toHaveCount(0);

      // Paso 3: Y guarda el grupo. La app vuelve a la lista.
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Reabrir el grupo guardado para comprobar el resultado persistido.
      await page.getByText(grupo, { exact: true }).click();
      const panelAlumnosVerif = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnosVerif.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();

      // Resultado esperado 1: "Alumno2 CIPFP Mislata" desaparece del grupo (junto con sus notas;
      //   sus notas eran "Sin nota" y se eliminan en cascada al quitar al alumno del grupo).
      await expect(panelAlumnosVerif.getByRole('row', { name: alumnoQuitado })).toHaveCount(0);

      // Resultado esperado 2: "Alumno1 CIPFP Mislata" permanece en el grupo.
      await expect(panelAlumnosVerif.getByRole('row', { name: alumnoPermanece })).toBeVisible();
    } finally {
      // Teardown idempotente: borra el grupo creado (libera a sus alumnos). Best-effort para
      // no enmascarar un fallo real de las aserciones de arriba.
      try {
        await borrarGrupoSiExiste(page, grupo);
      } catch {
        // ignorado: la limpieza es de mejor esfuerzo.
      }
    }

    await logout(page);
  });
});
