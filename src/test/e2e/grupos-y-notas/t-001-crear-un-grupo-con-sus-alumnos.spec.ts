import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-001 — Crear un grupo con sus alumnos
// origen: ESC-001  |  verifica: R-Grupo-001, R-Grupo-002, R-AlumnoGrupo-001, U-grupos-supervisor-001, U-grupos-supervisor-002
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-001-crear-un-grupo-con-sus-alumnos.desc.md

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
// elimina en cascada la pertenencia de los alumnos, dejando libre su (alumno, curso académico)).
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
  test('Crear un grupo con sus alumnos', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el supervisor del CIPFP Mislata inicia sesión.
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    // Idempotencia (BD compartida entre ejecuciones, NO se resetea):
    // 1) Nombre de grupo único por ejecución (sufijo con Date.now()): el nombre tiene
    //    restricción única, así que reejecutar con un nombre fijo chocaría al guardar.
    //    El escenario usa "1º DAM A"; el sufijo no afecta a lo que verifica el test.
    // 2) Los dos alumnos quedan "pegados" al grupo por (alumno, curso académico 2024)
    //    según V-AlumnoGrupo-005: una vez en un grupo de 2024 no pueden entrar en otro.
    //    Por eso el bloque `finally` BORRA el grupo creado al terminar (también si una
    //    aserción falla), lo que libera a los alumnos por orphanRemoval y deja la BD
    //    como estaba: el test es repetible.
    // 3) Se usan "Alumno3"/"Alumno4 CIPFP Mislata": alumnos del seed del centro, de baja
    //    contención y libres en la BD compartida (otros tests usan sobre todo Alumno1/2).
    //    El escenario solo exige "dos alumnos del mismo centro y curso académico"; los
    //    concretos son irrelevantes para lo que verifica.
    const tag = `t001-${Date.now()}`;
    const grupo = `1º DAM A ${tag}`;
    const alumno1 = 'Alumno3 CIPFP Mislata';
    const alumno2 = 'Alumno4 CIPFP Mislata';

    try {
      // Paso 1: Dado que el supervisor está en la pantalla "Grupos".
      await page.getByText('Notas', { exact: true }).click();
      await page.getByText('Grupos', { exact: true }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Paso 2: Cuando pulsa "Nuevo grupo".
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await expect(page.getByRole('textbox', { name: 'Nombre' })).toBeVisible();

      // Paso 3: Y escribe el nombre del grupo.
      await page.getByRole('textbox', { name: 'Nombre' }).fill(grupo);

      // Paso 4: Y elige el curso "1º DAM".
      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: '1º DAM', exact: true }).click();
      await expect(page.getByRole('button', { name: '1º DAM' }).or(page.getByRole('combobox', { name: 'Curso' }))).toBeVisible();

      // Paso 5: Y observa que "Centro" muestra "CIPFP Mislata" en solo lectura
      //         y "Curso académico" muestra "2024" en solo lectura.
      const centro = page.getByRole('textbox', { name: 'Centro' });
      await expect(centro).toHaveValue('CIPFP Mislata');
      await expect(centro).toBeDisabled();
      const cursoAcademico = page.getByRole('textbox', { name: /Curso académico/ });
      await expect(cursoAcademico).toHaveValue('2024');
      await expect(cursoAcademico).toBeDisabled();

      // Paso 6: Y pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar' }).click();
      // Tras guardar, la app vuelve a la lista; reabrimos el grupo para añadir alumnos.
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
      await page.getByText(grupo, { exact: true }).click();
      const panelAlumnos = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnos.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();

      // Paso 7: Y en el panel "Alumnos" pulsa "Añadir alumno", elige cada alumno y guarda.
      for (const alumno of [alumno1, alumno2]) {
        await panelAlumnos.getByRole('button', { name: 'Añadir alumno' }).click();
        const dialogo = page.getByRole('dialog');
        await dialogo.getByRole('combobox', { name: 'Alumno' }).click();
        await page.getByRole('option', { name: alumno, exact: true }).click();
        await dialogo.getByRole('button', { name: 'Guardar' }).click();
        await expect(panelAlumnos.getByRole('row', { name: alumno })).toBeVisible();
      }
      // Guardar el grupo con sus alumnos; la app vuelve a la lista.
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Reabrir el grupo guardado para comprobar el resultado.
      await page.getByText(grupo, { exact: true }).click();

      // Resultado esperado 1: el grupo está en estado "Abierto",
      //                       con centro "CIPFP Mislata" y curso académico "2024".
      await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(grupo);
      await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Abierto');
      await expect(page.getByRole('textbox', { name: 'Centro' })).toHaveValue('CIPFP Mislata');
      await expect(page.getByRole('textbox', { name: /Curso académico/ })).toHaveValue('2024');

      // Resultado esperado 2: el panel "Módulos" muestra "Programación" y "Bases de datos".
      const panelModulos = page.getByRole('region', { name: 'Módulos' });
      await expect(panelModulos.getByRole('row', { name: 'Programación' })).toBeVisible();
      await expect(panelModulos.getByRole('row', { name: 'Bases de datos' })).toBeVisible();

      // Resultado esperado 3: el panel "Alumnos" muestra ambos alumnos con nota media "Sin nota".
      const panelAlumnosVerif = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnosVerif.getByRole('row', { name: `${alumno1} Sin nota` })).toBeVisible();
      await expect(panelAlumnosVerif.getByRole('row', { name: `${alumno2} Sin nota` })).toBeVisible();

      // Resultado esperado 4: al entrar en el módulo "Programación", cada alumno tiene nota "No evaluado".
      await panelModulos.getByRole('row', { name: 'Programación' }).click();
      const dialogoModulo = page.getByRole('dialog');
      await expect(dialogoModulo.getByRole('heading', { name: 'Módulo del grupo' })).toBeVisible();
      await expect(dialogoModulo.getByRole('row', { name: `${alumno1} No evaluado` })).toBeVisible();
      await expect(dialogoModulo.getByRole('row', { name: `${alumno2} No evaluado` })).toBeVisible();
      // El diálogo tiene dos botones "Cerrar" (la X de cabecera y el botón del pie);
      // filtramos por el texto visible "Cerrar" para quedarnos con el del pie.
      await dialogoModulo.getByRole('button', { name: 'Cerrar' }).filter({ hasText: 'Cerrar' }).click();
    } finally {
      // Teardown idempotente: borra el grupo creado (libera a sus alumnos por orphanRemoval).
      // Best-effort para no enmascarar un fallo real de las aserciones de arriba.
      try {
        await borrarGrupoSiExiste(page, grupo);
      } catch {
        // ignorado: la limpieza es de mejor esfuerzo.
      }
    }

    await logout(page);
  });
});
