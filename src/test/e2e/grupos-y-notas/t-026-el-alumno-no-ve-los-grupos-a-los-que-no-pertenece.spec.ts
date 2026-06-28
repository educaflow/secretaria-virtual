import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-026 — El alumno no ve los grupos a los que no pertenece
// origen: ESC-026  |  verifica: —
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-026-el-alumno-no-ve-los-grupos-a-los-que-no-pertenece.desc.md

// Notas de robustez (BD compartida sin reset, verificadas pilotando la app):
// - El test usa "Alumno1 CIPFP Mislata" (grupoA) y "Alumno2 CIPFP Mislata" (grupoB), ambos del
//   curso académico 2024. La validación V-AlumnoGrupo-005 (un alumno no puede estar en dos grupos
//   del mismo curso académico) bloquea guardar grupoA si el alumno quedó "pegado" a un grupo de
//   "1º DAM" de una ejecución previa cuyo teardown falló. Por eso, ANTES de crear nada, una
//   PRE-LIMPIEZA defensiva (como supervisor) borra TODOS los grupos de prueba T-026 que queden de
//   ejecuciones anteriores (filtrando por el token "t026-"); borrar el grupo libera al alumno porque
//   la composición Grupo→AlumnoGrupo es orphanRemoval. Así el test arranca SIEMPRE limpio.
// - Grid del supervisor "Grupos": columnas Curso académico, Ciclo, Nombre, Estado. La lista está
//   muy paginada (decenas de grupos), por eso se FILTRA siempre por la columna "Nombre" (3.ª caja
//   "Buscar...", nth(2)). Al filtrar, la FILA DE FILTRO adopta como nombre accesible el texto
//   tecleado (p.ej. "t026-" o incluso el nombre completo). Para distinguir SOLO filas de datos se
//   exige en el regex que el nombre vaya seguido del ESTADO ("Abierto"/"Cerrado"), que la fila de
//   filtro nunca contiene (la columna "Ciclo" puede salir vacía en grupos recién creados, así que
//   NO se puede usar el nombre del ciclo como discriminador).
// - Los grupos T-026 nunca se cierran → quedan ABIERTOS y por tanto son borrables por el supervisor.
//   El diálogo de confirmación de borrado dice "¿Realmente quieres eliminar el registro
//   seleccionado?" y su botón de confirmación es "Eliminar" (es) / "Delete" (en).

const SUPERVISOR = { login: 'supervisor1@mislata.es', pass: 'demo1234' };
const ALUMNO1 = 'Alumno1 CIPFP Mislata';
const ALUMNO2 = 'Alumno2 CIPFP Mislata';
const CURSO = '1º DAM';
// Token común a todos los grupos de prueba de este test (grupoA y grupoB), estable entre ejecuciones.
const TOKEN_T026 = 't026-';
// Distingue una FILA DE DATOS (nombre del grupo seguido de su estado) de la fila de filtro
// (solo el texto buscado, sin estado). Robusto aunque la columna "Ciclo" salga vacía.
const FILA_DATOS_T026 = /1º DAM [AB] t026-\d+ (Abierto|Cerrado)/;

// Abre la pantalla de supervisor "Grupos" (menú Notas).
async function abrirGruposSupervisor(page: Page): Promise<void> {
  await page.getByText('Notas', { exact: true }).click();
  await page.getByText('Grupos', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

// Buscador de la columna "Nombre" (3.ª caja "Buscar...").
function filtroNombre(page: Page) {
  return page.getByRole('textbox', { name: 'Buscar...' }).nth(2);
}

// Filas de datos T-026 actualmente visibles en el grid (excluye la fila de filtro).
function filasDatosT026(page: Page) {
  return page.getByRole('row', { name: FILA_DATOS_T026 });
}

// Mensaje del grid cuando el filtro no devuelve ningún registro (estado terminal "0 residuos").
function sinRegistros(page: Page) {
  return page.getByText('No se encontraron registros.');
}

// Aplica el filtro de la columna "Nombre" con `texto` y espera a que el grid alcance uno de sus dos
// estados TERMINALES: hay al menos una fila de datos T-026, o aparece "No se encontraron registros.".
// CRÍTICO: la decisión de "0 residuos" se toma por la VISIBILIDAD de ese mensaje, nunca por un
// `count()` instantáneo (que puede leer 0 mientras el grid recarga y provocar un borrado incompleto).
async function filtrarPorNombre(page: Page, texto: string): Promise<void> {
  await filtroNombre(page).fill(texto);
  await filtroNombre(page).press('Enter');
  await expect(filasDatosT026(page).first().or(sinRegistros(page))).toBeVisible({ timeout: 15_000 });
}

// Borra el grupo cargado en el formulario del supervisor (confirmando el diálogo) y vuelve a la lista.
async function borrarGrupoCargado(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Borrar' }).click();
  // Diálogo de confirmación de Axelor: botón "Eliminar" (es) / "Delete" (en).
  await page.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

// Borra TODOS los grupos de prueba T-026 que queden en la BD (sea cual sea su sufijo único),
// liberando a Alumno1/Alumno2 de la regla "un alumno por grupo y curso académico". Idempotente:
// si no queda ninguno, no hace nada. Es la red de seguridad que hace al test auto-sanable.
async function limpiarGruposT026(page: Page): Promise<void> {
  await abrirGruposSupervisor(page);
  for (let i = 0; i < 40; i++) {
    await filtrarPorNombre(page, TOKEN_T026);
    // Estado terminal fiable: si el grid muestra "No se encontraron registros." ya no quedan residuos.
    if (await sinRegistros(page).isVisible()) return;
    await filasDatosT026(page).first().click();
    await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(/t026-/);
    await borrarGrupoCargado(page);
  }
  throw new Error('limpiarGruposT026: superado el LIMIT de 40 borrados sin vaciar los residuos T-026');
}

// Abre, desde la lista de "Grupos", el grupo de nombre `nombre` (filtrando antes para acotar la lista).
async function abrirGrupoEnLista(page: Page, nombre: string): Promise<void> {
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
  await filtrarPorNombre(page, nombre);
  const fila = page.getByText(nombre, { exact: true });
  await expect(fila).toBeVisible();
  await fila.click();
  await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(nombre);
}

// El supervisor crea un grupo del curso "1º DAM" con un único alumno y lo guarda.
// Tras crear (sin alumnos) la app vuelve a la lista; reabrimos el grupo (filtrando)
// para añadir el alumno por el panel "Alumnos" y volvemos a guardar.
async function crearGrupoConAlumno(page: Page, nombre: string, alumno: string): Promise<void> {
  await page.getByRole('button', { name: 'Nuevo grupo' }).click();
  await page.getByRole('textbox', { name: 'Nombre' }).fill(nombre);
  await page.getByRole('combobox', { name: 'Curso' }).click();
  await page.getByRole('option', { name: CURSO, exact: true }).click();
  await expect(
    page.getByRole('button', { name: CURSO }).or(page.getByRole('combobox', { name: 'Curso' })),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Guardar' }).click();
  // Tras guardar, la app vuelve a la lista; reabrimos el grupo para añadir el alumno.
  await abrirGrupoEnLista(page, nombre);

  const panelAlumnos = page.getByRole('region', { name: 'Alumnos' });
  await expect(panelAlumnos.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();
  await panelAlumnos.getByRole('button', { name: 'Añadir alumno' }).click();
  const dialogo = page.getByRole('dialog');
  await dialogo.getByRole('combobox', { name: 'Alumno' }).click();
  await page.getByRole('option', { name: alumno, exact: true }).click();
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  await expect(panelAlumnos.getByRole('row', { name: alumno })).toBeVisible();

  // Guardar el grupo con su alumno; la app vuelve a la lista.
  await page.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

test.describe('Mis notas (alumno)', () => {
  test('El alumno no ve los grupos a los que no pertenece', async ({ page }) => {
    // Test multi-actor y largo (pre-limpieza + supervisor crea 2 grupos + alumno verifica + teardown):
    // se amplía el timeout para que el bloque de teardown SIEMPRE pueda completarse.
    test.setTimeout(240_000);

    // BD compartida sin reset: nombres únicos por ejecución para no chocar con el unique-constraint
    // de nombre ni con datos de ejecuciones previas (todos comparten el token "t026-").
    const sufijo = Date.now();
    const grupoA = `1º DAM A t026-${sufijo}`; // alumno1 SÍ pertenece → debe verlo
    const grupoB = `1º DAM B t026-${sufijo}`; // alumno1 NO pertenece (tiene a alumno2) → NO debe verlo

    await ensureLoggedOut(page);

    // PRE-LIMPIEZA defensiva (self-healing): antes de crear nada, el supervisor borra cualquier grupo
    // de prueba T-026 residual de una ejecución previa, liberando a Alumno1/Alumno2 para que la
    // creación de grupoA/grupoB no choque con la regla "un alumno por grupo y curso académico".
    await login(page, SUPERVISOR.login, SUPERVISOR.pass);
    await limpiarGruposT026(page);
    await logout(page);

    // Captura el error real de las aserciones para que el teardown no lo enmascare.
    let errorPrincipal: unknown;
    try {
      // Paso 1: Dado que el supervisor crea el grupo "1º DAM A" (curso "1º DAM")
      //         y le añade a "Alumno1 CIPFP Mislata".
      // Paso 2: Y crea el grupo "1º DAM B" (curso "1º DAM") y le añade a
      //         "Alumno2 CIPFP Mislata" (sin añadir a "Alumno1 CIPFP Mislata");
      //         después cierra sesión.
      await login(page, SUPERVISOR.login, SUPERVISOR.pass);
      await abrirGruposSupervisor(page);
      await crearGrupoConAlumno(page, grupoA, ALUMNO1);
      await crearGrupoConAlumno(page, grupoB, ALUMNO2);
      await logout(page);

      // Paso 3: Cuando el alumno "Alumno1 CIPFP Mislata" inicia sesión y abre "Mis notas".
      await login(page, 'alumno1@mislata.es', 'demo1234');
      await page.getByText('Mis notas', { exact: true }).click();
      await expect(page.getByRole('grid')).toBeVisible();

      // Resultado esperado 1: el listado muestra "1º DAM A" (al que pertenece).
      await expect(page.getByText(grupoA, { exact: true })).toBeVisible();
      // Resultado esperado 2: el listado NO muestra "1º DAM B" (al que no pertenece).
      await expect(page.getByText(grupoB, { exact: true })).toHaveCount(0);
    } catch (e) {
      errorPrincipal = e;
    } finally {
      // TEARDOWN FIABLE: el supervisor borra de verdad grupoA y grupoB (y cualquier residuo T-026),
      // y se VERIFICA que no queda ninguno. CRÍTICO: dejar la BD limpia evita que Alumno1/Alumno2
      // queden atrapados en un grupo de "1º DAM" y rompan la regla de unicidad en la siguiente
      // ejecución (la BD es compartida y no se resetea). No se enmascara un fallo real de las
      // aserciones: si la fase principal falló, ese error se relanza por encima del de teardown.
      try {
        await ensureLoggedOut(page);
        await login(page, SUPERVISOR.login, SUPERVISOR.pass);
        await limpiarGruposT026(page);
        // Verificación determinista: tras el teardown el grid filtrado por "t026-" muestra
        // "No se encontraron registros." (no quedan grupoA/grupoB ni ningún residuo T-026 en la BD).
        await filtrarPorNombre(page, TOKEN_T026);
        await expect(sinRegistros(page)).toBeVisible();
        await logout(page);
      } catch (limpiezaError) {
        // Si la fase principal pasó, un fallo de limpieza/verificación SÍ debe surgir (no en silencio).
        if (errorPrincipal === undefined) throw limpiezaError;
      }
    }

    if (errorPrincipal !== undefined) throw errorPrincipal;
  });
});
