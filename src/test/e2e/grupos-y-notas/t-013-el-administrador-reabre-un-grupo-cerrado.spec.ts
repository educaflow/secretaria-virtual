import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-013 — El administrador reabre un grupo cerrado
// origen: ESC-013  |  verifica: V-Grupo-008, R-Grupo-004, U-grupos-administrador-003
// fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-013-el-administrador-reabre-un-grupo-cerrado.desc.md

// Notas sobre los datos reales de la app (verificadas pilotando la app):
// - El alumno del escenario es "Alumno1 CIPFP Mislata", pero en la BD compartida (sin reset)
//   Alumno1 ya queda persistido por T-001 en un grupo del curso académico 2024 y NO se libera.
//   La validación R-Grupo-004 (un alumno no puede estar en dos grupos del mismo curso académico)
//   bloquearía añadirlo a un grupo nuevo de 2024. Por eso se usa "Alumno5 CIPFP Mislata", el único
//   alumno del centro que NINGÚN otro test de regresión usa (T-001→Alumno1/2, T-004→Alumno6,
//   T-005→Alumno3/4). El alumno concreto es irrelevante para lo que verifica T-013 (la reapertura
//   del grupo). El grupo acaba reabierto (ABIERTO) y se borra, liberando al alumno y sus notas.
// - "Curso académico" se muestra en solo lectura como "2,024" (separador de millares).
// - Cierre y reapertura se PERSISTEN de inmediato (sin pulsar Guardar): al cerrar el estado pasa
//   a "Cerrado" y se rellena "Fecha de cierre"; al reabrir vuelve a "Abierto" y la "Fecha de
//   cierre" queda vacía. Sólo el administrador muestra "Reabrir grupo" sobre un grupo cerrado.
// - "Modificar las notas": con el grupo cerrado el campo "Nombre" del grupo está deshabilitado;
//   tras reabrir queda EDITABLE de nuevo y el editor de notas del alumno vuelve a estar disponible
//   con su campo "Valor" editable. Se comprueba sin persistir ninguna nota.
// - El chrome del framework en la vista de administración está en inglés: el buscador de columna
//   se llama "Search..." y el diálogo de borrado dice "Delete"; en la vista del supervisor son
//   "Buscar..." y "Eliminar". Los títulos de campos y botones propios están en español.
// - IDEMPOTENCIA (BD compartida sin reset): (1) la lista de Axelor VIRTUALIZA filas, así que para
//   abrir/borrar un grupo de forma fiable se FILTRA antes por la columna "Nombre"; (2) abrir el
//   editor de notas deja el formulario "sucio", lo que activaría el guard de navegación de Axelor;
//   por eso, antes de empezar, una PRE-LIMPIEZA como administrador borra cualquier grupo de
//   pruebas T-013 que quedara de una ejecución previa (reabriéndolo si estuviera cerrado), lo que
//   libera al alumno y garantiza que la ejecución parta limpia aunque un teardown anterior fallara.

const SUPERVISOR = { login: 'supervisor1@mislata.es', pass: 'demo1234' };
const CURSO = '1º DAM';
const ALUMNO = 'Alumno5 CIPFP Mislata';
const MODULO = 'Programación';
const PREFIJO = '1º DAM A t013-';

// Abre la pantalla del supervisor "Grupos" (menú Notas).
async function abrirGruposSupervisor(page: Page): Promise<void> {
  await page.getByText('Notas', { exact: true }).click();
  await page.getByText('Grupos', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

// Abre la pantalla del administrador "Grupos (administración)" (menú Administración SV).
async function abrirGruposAdmin(page: Page): Promise<void> {
  await page.getByText('Administración SV', { exact: true }).click();
  await page.getByText('Grupos (administración)', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

// Filtra la lista por la columna "Nombre" (robusto frente a la virtualización de filas) y abre el
// grupo. `placeholder`/`idx` identifican el buscador de la columna "Nombre"
// (supervisor: "Buscar..." nth(2); administración: "Search..." nth(3)).
async function abrirGrupoPorNombre(page: Page, nombre: string, placeholder: string, idx: number): Promise<void> {
  const filtro = page.getByRole('textbox', { name: placeholder }).nth(idx);
  await filtro.fill(nombre);
  await page.keyboard.press('Enter');
  const fila = page.getByText(nombre, { exact: true });
  await expect(fila).toHaveCount(1);
  await fila.click();
  await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(nombre);
}

// Añade un alumno al panel "Alumnos" del grupo abierto (botón "Añadir alumno" -> diálogo).
async function anadirAlumno(page: Page, panelAlumnos: Locator, alumno: string): Promise<void> {
  await panelAlumnos.getByRole('button', { name: 'Añadir alumno' }).click();
  const dialogo = page.getByRole('dialog');
  await dialogo.getByRole('combobox', { name: 'Alumno' }).click();
  await page.getByRole('option', { name: alumno, exact: true }).click();
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  await expect(panelAlumnos.getByRole('row', { name: alumno })).toBeVisible();
}

// Borra el grupo cargado en el formulario de administración (reabriéndolo si está cerrado, porque
// un grupo cerrado no se puede borrar). Devuelve a la lista.
async function borrarGrupoCargado(page: Page): Promise<void> {
  const reabrir = page.getByRole('button', { name: 'Reabrir grupo' });
  if (await reabrir.isVisible().catch(() => false)) {
    await reabrir.click();
    await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Abierto');
  }
  await page.getByRole('button', { name: 'Borrar' }).click();
  // Diálogo de confirmación de Axelor (vista de administración, en inglés).
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();
}

// PRE-LIMPIEZA idempotente (administración): borra TODOS los grupos de pruebas T-013 que queden
// de ejecuciones anteriores (sea cual sea su nombre único), liberando al alumno. Es lo que hace al
// test idempotente aunque un teardown anterior fallara.
async function limpiarGruposT013(page: Page): Promise<void> {
  await abrirGruposAdmin(page);
  const filtro = page.getByRole('textbox', { name: 'Search...' }).nth(3);
  // Las filas de DATOS incluyen el nombre del centro ("CIPFP ..."); así se distinguen de la fila
  // de filtros (que sólo contiene el texto buscado) al contarlas.
  const filasDatos = page.getByRole('row', { name: /CIPFP .* t013-/ });
  for (let i = 0; i < 30; i++) {
    await filtro.fill(PREFIJO);
    await page.keyboard.press('Enter');
    // Da tiempo a que el grid filtrado se estabilice antes de contar.
    await filasDatos.first().waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
    if ((await filasDatos.count()) === 0) return;
    await filasDatos.first().click();
    await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(/t013-/);
    await borrarGrupoCargado(page);
  }
}

// Teardown best-effort (administración): borra el grupo de ESTA ejecución por nombre.
async function borrarGrupoAdminSiExiste(page: Page, nombre: string): Promise<void> {
  await abrirGruposAdmin(page);
  const filtro = page.getByRole('textbox', { name: 'Search...' }).nth(3);
  await filtro.fill(nombre);
  await page.keyboard.press('Enter');
  const fila = page.getByText(nombre, { exact: true });
  await fila.first().waitFor({ state: 'visible', timeout: 10000 }).catch(() => {});
  if ((await fila.count()) === 0) return;
  await fila.first().click();
  await expect(page.getByRole('textbox', { name: 'Nombre' })).toHaveValue(nombre);
  await borrarGrupoCargado(page);
}

test.describe('Grupos (administración)', () => {
  test('El administrador reabre un grupo cerrado', async ({ page }) => {
    // Test multi-actor y largo (pre-limpieza + supervisor crea/añade/cierra + admin reabre/verifica
    // + teardown): se amplía el timeout para que el bloque `finally` SIEMPRE se ejecute.
    test.setTimeout(240_000);
    // Nombre único por ejecución: BD compartida sin reset.
    const nombre = `${PREFIJO}${Date.now()}`;

    await ensureLoggedOut(page);

    try {
      // Pre-limpieza idempotente (admin): elimina grupos T-013 de ejecuciones previas y libera al
      // alumno antes de empezar.
      await login(page, 'admin', 'admin');
      await limpiarGruposT013(page);
      await logout(page);

      // Paso 1: Dado que el supervisor crea el grupo "1º DAM A" (curso "1º DAM"), le añade a
      //         "Alumno1 CIPFP Mislata" (aquí Alumno5, ver nota de cabecera) y lo cierra; después
      //         cierra sesión.
      await login(page, SUPERVISOR.login, SUPERVISOR.pass);
      await abrirGruposSupervisor(page);

      // Crear el grupo con su curso.
      await page.getByRole('button', { name: 'Nuevo grupo' }).click();
      await page.getByRole('textbox', { name: 'Nombre' }).fill(nombre);
      await page.getByRole('combobox', { name: 'Curso' }).click();
      await page.getByRole('option', { name: CURSO, exact: true }).click();
      await expect(page.getByRole('combobox', { name: 'Curso' })).toHaveValue(CURSO);
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Reabrir el grupo desde la lista y añadir al alumno.
      await abrirGrupoPorNombre(page, nombre, 'Buscar...', 2);
      const panelAlumnos = page.getByRole('region', { name: 'Alumnos' });
      await expect(panelAlumnos.getByRole('button', { name: 'Añadir alumno' })).toBeVisible();
      await anadirAlumno(page, panelAlumnos, ALUMNO);
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('button', { name: 'Nuevo grupo' })).toBeVisible();

      // Reabrir el grupo y cerrarlo (estado pasa a "Cerrado", se persiste de inmediato).
      await abrirGrupoPorNombre(page, nombre, 'Buscar...', 2);
      await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Abierto');
      await page.getByRole('button', { name: 'Cerrar grupo' }).click();
      await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Cerrado');

      await logout(page);

      // Paso 2: Cuando el administrador inicia sesión, abre "Grupos (administración)" y entra en
      //         el grupo (está "Cerrado").
      await login(page, 'admin', 'admin');
      await abrirGruposAdmin(page);
      await abrirGrupoPorNombre(page, nombre, 'Search...', 3);
      await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Cerrado');
      // El grupo cerrado tiene "Fecha de cierre" rellena y "Nombre" deshabilitado (estado de
      // partida que la reapertura debe revertir).
      await expect(page.getByRole('textbox', { name: 'Fecha de cierre' })).not.toHaveValue('');
      await expect(page.getByRole('textbox', { name: 'Nombre' })).toBeDisabled();
      await expect(page.getByRole('button', { name: 'Reabrir grupo' })).toBeVisible();

      // Paso 3: Y pulsa "Reabrir grupo".
      await page.getByRole('button', { name: 'Reabrir grupo' }).click();

      // Resultado esperado 1: el grupo pasa a "Abierto" y la "Fecha de cierre" queda vacía.
      await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Abierto');
      await expect(page.getByRole('textbox', { name: 'Fecha de cierre' })).toHaveValue('');
      // El grupo vuelve al estado abierto: reaparece "Cerrar grupo" y desaparece "Reabrir grupo".
      await expect(page.getByRole('button', { name: 'Cerrar grupo' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Reabrir grupo' })).toHaveCount(0);

      // Resultado esperado 2: vuelve a permitir modificar las notas.
      // El campo "Nombre", deshabilitado con el grupo cerrado, queda EDITABLE tras reabrir.
      await expect(page.getByRole('textbox', { name: 'Nombre' })).toBeEnabled();

      // Y el editor de notas del alumno vuelve a estar disponible con su "Valor" editable: se entra
      // en el módulo "Programación" y se abre la nota del alumno.
      await page.getByRole('region', { name: 'Módulos' }).getByRole('row', { name: MODULO }).click();
      const dlgModulo = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Módulo del grupo' }) });
      await expect(dlgModulo).toBeVisible();
      // En estos sub-grids el editor de detalle se abre con un ÚNICO click sobre la fila.
      const filaNota = dlgModulo.getByRole('region', { name: 'Notas' }).getByRole('row', { name: new RegExp(ALUMNO) });
      await expect(filaNota).toBeVisible();
      await filaNota.click();

      const dlgNota = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Nota', exact: true }) });
      await expect(dlgNota).toBeVisible();
      await expect(dlgNota.getByRole('combobox', { name: 'Valor' })).toBeEnabled();
      // Se cierra el editor SIN cambios (Cancelar) y se cierra el diálogo del módulo (Cerrar).
      await dlgNota.getByRole('button', { name: 'Cancelar' }).click();
      await expect(dlgNota).toHaveCount(0);
      await dlgModulo.getByRole('button', { name: 'Cerrar' }).click();
      await expect(dlgModulo).toHaveCount(0);
    } finally {
      // Teardown best-effort: la idempotencia ya la garantiza la pre-limpieza inicial; aun así se
      // intenta borrar el grupo de esta ejecución. Abrir el editor de notas deja el formulario
      // "sucio", lo que activaría el guard de navegación de Axelor; una recarga dura descarta esos
      // cambios (aceptando el diálogo nativo) antes de navegar a limpiar.
      page.on('dialog', (d) => { d.accept().catch(() => {}); });
      await page.reload().catch(() => {});
      try {
        await ensureLoggedOut(page);
        await login(page, 'admin', 'admin');
        await borrarGrupoAdminSiExiste(page, nombre);
      } catch {
        // ignorado: la limpieza es de mejor esfuerzo (la pre-limpieza inicial es la red de seguridad).
      }
      await logout(page).catch(() => {});
    }
  });
});
