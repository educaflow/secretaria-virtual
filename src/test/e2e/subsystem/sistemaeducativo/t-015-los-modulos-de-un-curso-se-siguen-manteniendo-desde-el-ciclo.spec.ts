import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-015 — Los módulos de un curso se siguen manteniendo desde el ciclo
// origen: ESC-015  |  verifica: V-Ciclo-001, V-Ciclo-003, U-ciclos-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-015-los-modulos-de-un-curso-se-siguen-manteniendo-desde-el-ciclo.desc.md

// El diálogo de confirmación de borrado de Axelor sale traducido al español
// («¿Realmente quieres eliminar el registro seleccionado?» / «Eliminar»); el regex
// acepta también el inglés por si se ejecuta con otro locale.
const TEXTO_CONFIRMAR_BORRADO = /really want to delete|(quieres|desea) (borrar|eliminar)/i;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;

// Los campos `code`/`name` del modelo Ciclo todavía no están traducidos, así que la
// UI los rotula "Code"/"Name". El regex acepta ambos para que el test siga verde
// cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;

/** Entradas del menú lateral «Sistema educativo» que recorre este test. */
const MENU_SISTEMA_EDUCATIVO = 'item:sistemaEducativo-menuitem';
const MENU_CICLOS = 'item:sistemaEducativo-ciclos-menuitem';
const MENU_CURSOS_MODULOS = 'item:sistemaEducativo-cursosModulos-menuitem';

const BTN_NUEVO_CICLO = 'Añadir un nuevo ciclo';
const BTN_NUEVO_CURSO_MODULO_LISTADO = 'Añadir un nuevo curso-módulo';
const BTN_NUEVO_MODULO_PANEL = 'Añadir un nuevo módulo';

// Ciclo de los datos iniciales sobre el que trabaja el test. Se identifica siempre
// por su CÓDIGO, que es único entre los ciclos y que este test no modifica: filtrar
// por el nombre no distinguiría «Desarrollo de Aplicaciones Web» de «Desarrollo de
// Aplicaciones Multiplataforma» sin `exact`, y el código es más estable.
const CODIGO_CICLO = 'DAW';
const NOMBRE_CICLO = 'Desarrollo de Aplicaciones Web';
const GRADO_CICLO = 'Ciclo formativo';
const NIVEL_CICLO = 'Superior';

// Curso del ciclo sobre el que trabaja el test, y los módulos en juego. `exact: true`
// en todas las comparaciones: "1º DAW" es prefijo de nada, pero "1º DAM" comparte
// módulo «Programación» en los datos iniciales y una comparación por subcadena los
// confundiría en el listado de Cursos-Módulos.
const CURSO = '1º DAW';
const MODULO_INICIAL = 'Itinerario Personal para la Empleabilidad I';
const MODULO_NUEVO = 'Programación';

/**
 * Filas de datos de un grid (excluye la cabecera y la fila de búsqueda). El grid las
 * pinta por AJAX, así que hay que esperarlas antes de CONTAR nada: `count()` no
 * reintenta y devolvería 0 sobre un grid aún vacío.
 */
function filasDeDatos(ambito: Page | Locator) {
  return ambito.locator('[data-testid^="row:"]');
}

/**
 * Localiza un campo del formulario por su nombre en el modelo. El `data-testid`
 * `field:<campo>` lo deriva Axelor del propio modelo, así que es estable (no es un
 * id autogenerado tipo `#x-123`). `getByTestId` compara la cadena ENTERA, así que
 * `field:modulo` (el combo del modal) nunca casa con `field:modulos` (el panel).
 */
function campo(ambito: Page | Locator, nombre: string) {
  return ambito.getByTestId(`field:${nombre}`);
}

/**
 * El input de un campo many-to-one. No se usa `getByRole('combobox', { name })`
 * porque al desplegarse Axelor saca el control del árbol de accesibilidad y el
 * locator por rol deja de resolver a mitad de la interacción.
 */
function combo(ambito: Page | Locator, nombre: string) {
  return campo(ambito, nombre).getByTestId('input');
}

/** El panel-related "Cursos" de la ficha del ciclo. */
function panelDeCursos(page: Page) {
  return campo(page, 'cursos');
}

/**
 * El modal del formulario de un curso, abierto desde el panel "Cursos" del ciclo. Se
 * localiza por su encabezado "Curso" porque mientras se añade un módulo conviven DOS
 * diálogos (el del curso y el del módulo) y `getByRole('dialog')` sería ambiguo.
 */
function modalCurso(page: Page) {
  return page
    .getByRole('dialog')
    .filter({ has: page.getByRole('heading', { name: 'Curso', exact: true }) });
}

/**
 * El modal del formulario de un módulo del curso (entidad CursoModulo), abierto desde
 * el panel "Módulos". Su encabezado es "Módulo", el del curso "Curso".
 */
function modalModulo(page: Page) {
  return page
    .getByRole('dialog')
    .filter({ has: page.getByRole('heading', { name: 'Módulo', exact: true }) });
}

/**
 * El panel-related "Módulos", que vive DENTRO del modal del curso. Se acota al modal
 * porque el formulario del ciclo que queda detrás tiene grids con los mismos
 * `data-testid` de fila (`row:N`).
 */
function panelDeModulos(page: Page) {
  return campo(modalCurso(page), 'modulos');
}

/**
 * Abre una entrada del menú lateral «Sistema educativo». El grupo se pliega y
 * despliega al pulsarlo, y mientras está plegado sus hijos ni siquiera están en el
 * DOM; por eso hay que desplegarlo SOLO si la entrada no se ve: pulsarlo a ciegas
 * lo cerraría cuando ya venía abierto de una navegación anterior.
 */
async function abrirMenu(page: Page, itemMenu: string): Promise<void> {
  const entrada = page.getByTestId(itemMenu);
  if (!(await entrada.isVisible())) {
    await page.getByTestId(MENU_SISTEMA_EDUCATIVO).click();
  }
  await entrada.click();
}

/** Abre el menú "Sistema educativo" → "Ciclos" y espera al listado pintado. */
async function abrirListadoDeCiclos(page: Page): Promise<void> {
  await abrirMenu(page, MENU_CICLOS);
  await expect(page).toHaveURL(/Ciclo-action\/list/);
  await expect(page.getByRole('button', { name: BTN_NUEVO_CICLO })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Abre la ficha del ciclo bajo prueba desde el listado ya cargado y espera a que el
 * formulario traiga sus datos y a que el panel "Cursos" esté pintado: sin esa espera,
 * pulsar una fila del panel fallaría sobre un grid aún vacío.
 */
async function abrirFichaDelCiclo(page: Page): Promise<void> {
  await filasDeDatos(page)
    .filter({ has: page.getByRole('gridcell', { name: CODIGO_CICLO, exact: true }) })
    .click();
  await expect(page).toHaveURL(/Ciclo-action\/edit/);
  await expect(page.getByRole('textbox', { name: CAMPO_CODIGO })).toHaveValue(CODIGO_CICLO);
  await expect(filasDeDatos(panelDeCursos(page)).first()).toBeVisible();
}

/**
 * Abre el modal del curso bajo prueba desde el panel "Cursos" de la ficha del ciclo
 * (el grid es `canEditOnClick`) y espera a que su panel "Módulos" esté pintado.
 */
async function abrirModalDelCurso(page: Page): Promise<void> {
  await filasDeDatos(panelDeCursos(page))
    .filter({ has: page.getByRole('gridcell', { name: CURSO, exact: true }) })
    .click();
  await expect(modalCurso(page)).toBeVisible();
  await expect(filasDeDatos(panelDeModulos(page)).first()).toBeVisible();
}

/**
 * Deja la app en la pantalla de inicio partiendo de cualquier sitio. La recarga
 * completa es lo que descarta un formulario a medio editar: con solo cambiar el hash,
 * Axelor conserva la pestaña sucia y luego bloquea la navegación con el aviso
 * «Current changes will be lost».
 */
async function volverAlInicio(page: Page): Promise<void> {
  await page.goto('/#/');
  await page.reload();
}

/** Abre el menú "Sistema educativo" → "Cursos-Módulos" y espera al listado pintado. */
async function abrirListadoDeCursosModulos(page: Page): Promise<void> {
  await abrirMenu(page, MENU_CURSOS_MODULOS);
  await expect(page).toHaveURL(/CursoModulo-action\/list/);
  await expect(page.getByRole('button', { name: BTN_NUEVO_CURSO_MODULO_LISTADO })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Borra de verdad, desde el listado principal "Cursos-Módulos", la asignación del
 * módulo `MODULO_NUEVO` al curso `CURSO`. Pulsar la fila abre su ficha (el grid es
 * `canEditOnClick`) y el borrado se confirma en el diálogo de Axelor.
 *
 * **CRITICAL — el borrado NO se hace desde el panel "Módulos" del curso**: el botón
 * "Borrar" de ese modal anidado ejecuta `delete-modal`, que solo quita la fila de la
 * colección en el cliente y deja la fila viva en la BD si no se vuelve a guardar el
 * padre. El botón "Borrar" de esta ficha suelta ejecuta `delete`, que la borra en el
 * servidor.
 *
 * Solo toca el par (1º DAW, Programación): los datos iniciales asignan «Programación»
 * a «1º DAM» y «Itinerario Personal para la Empleabilidad I» a «1º DAW», así que este
 * par no es dato maestro y borrarlo no envenena a los tests hermanos.
 * LIMIT 10: cota dura para no quedarse en bucle si un borrado no se aplica.
 */
async function borrarModuloNuevoDelCurso(page: Page): Promise<void> {
  for (let i = 0; i < 10; i++) {
    await expect(page.getByRole('button', { name: BTN_NUEVO_CURSO_MODULO_LISTADO })).toBeVisible();
    await expect(filasDeDatos(page).first()).toBeVisible();
    const filas = filasDeDatos(page)
      .filter({ has: page.getByRole('gridcell', { name: CURSO, exact: true }) })
      .filter({ has: page.getByRole('gridcell', { name: MODULO_NUEVO, exact: true }) });
    if ((await filas.count()) === 0) {
      return;
    }
    await filas.first().click();
    await page.getByRole('button', { name: 'Borrar', exact: true }).click();
    await page
      .getByRole('dialog')
      .filter({ hasText: TEXTO_CONFIRMAR_BORRADO })
      .getByRole('button', { name: BTN_CONFIRMAR_BORRADO })
      .click();
  }
  throw new Error(`No se pudo limpiar el módulo "${MODULO_NUEVO}" del curso "${CURSO}"`);
}

test.describe('Sistema educativo — Módulos de un curso', () => {
  test('Los módulos de un curso se siguen manteniendo desde el ciclo', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva (§4.3 del contrato de generación): lo que este test
      // crea es la pareja fija (curso, módulo) de una entidad con `unique-constraint`
      // sobre esas dos columnas, así que NO admite sufijo único por ejecución: la
      // idempotencia la da borrar la asignación antes de empezar. Sin esto, un run
      // anterior que abortara antes del teardown dejaría el par pegado y el alta
      // chocaría contra la restricción de unicidad.
      await abrirListadoDeCursosModulos(page);
      await borrarModuloNuevoDelCurso(page);

      // Paso 2: abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila
      // "Desarrollo de Aplicaciones Web".
      await abrirListadoDeCiclos(page);
      await expect(
        filasDeDatos(page)
          .filter({ has: page.getByRole('gridcell', { name: CODIGO_CICLO, exact: true }) })
          .getByRole('gridcell', { name: NOMBRE_CICLO, exact: true }),
      ).toBeVisible();
      await abrirFichaDelCiclo(page);

      // Paso 3: en el panel "Cursos" pulsa la fila "1º DAW".
      await abrirModalDelCurso(page);

      // Paso 4: el panel "Módulos" del curso muestra "Itinerario Personal para la
      // Empleabilidad I".
      await expect(
        panelDeModulos(page).getByRole('gridcell', { name: MODULO_INICIAL, exact: true }),
      ).toBeVisible();

      // Paso 5: en el panel "Módulos" pulsa "Añadir un nuevo módulo" y elige el
      // módulo "Programación".
      await panelDeModulos(page).getByRole('button', { name: BTN_NUEVO_MODULO_PANEL }).click();
      await expect(modalModulo(page)).toBeVisible();
      // El desplegable de un many-to-one solo filtra con pulsaciones reales (un `fill`
      // cambiaría el valor sin disparar la búsqueda), de ahí el `pressSequentially`.
      // Este combo NO se acota al modal: al desplegarse, Axelor saca el diálogo del
      // árbol de accesibilidad y el filtro por su encabezado deja de resolver a mitad
      // de la interacción. No hace falta acotarlo porque "modulo" solo existe como
      // campo en el formulario del módulo (el panel del curso es "modulos").
      const comboModulo = combo(page, 'modulo');
      await comboModulo.click();
      await comboModulo.pressSequentially(MODULO_NUEVO);
      await page.getByRole('option', { name: MODULO_NUEVO, exact: true }).click();
      await expect(comboModulo).toHaveValue(MODULO_NUEVO);

      // Paso 6: pulsa "Guardar" en el formulario del módulo. El modal del módulo se
      // cierra y la fila queda en el panel del curso, todavía sin persistir.
      await modalModulo(page).getByRole('button', { name: 'Guardar', exact: true }).click();
      await expect(modalModulo(page)).toHaveCount(0);
      await expect(
        panelDeModulos(page).getByRole('gridcell', { name: MODULO_NUEVO, exact: true }),
      ).toBeVisible();

      // Paso 7: pulsa "Guardar" en el formulario del curso. Sigue sin persistir: lo
      // persiste el guardado del ciclo del paso siguiente.
      await modalCurso(page).getByRole('button', { name: 'Guardar', exact: true }).click();
      await expect(page.getByRole('dialog')).toHaveCount(0);

      // Paso 8: pulsa "Guardar" en el formulario del ciclo. Con los modales ya
      // cerrados, el único "Guardar" de la página es el del ciclo.
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Resultado esperado: el sistema guarda el ciclo y vuelve al listado.
      await expect(page).toHaveURL(/Ciclo-action\/list/);
      await expect(filasDeDatos(page).first()).toBeVisible();

      // Resultado esperado: al pulsar de nuevo la fila "Desarrollo de Aplicaciones
      // Web" y, en el panel "Cursos", la fila "1º DAW", el panel "Módulos" muestra
      // "Itinerario Personal para la Empleabilidad I" y "Programación".
      await abrirFichaDelCiclo(page);
      await abrirModalDelCurso(page);
      await expect(
        panelDeModulos(page).getByRole('gridcell', { name: MODULO_INICIAL, exact: true }),
      ).toBeVisible();
      const filaModuloNuevo = filasDeDatos(panelDeModulos(page)).filter({
        has: page.getByRole('gridcell', { name: MODULO_NUEVO, exact: true }),
      });
      // Una sola fila: el módulo se añadió una vez, no se duplicó al guardar en
      // cascada módulo → curso → ciclo.
      await expect(filaModuloNuevo).toHaveCount(1);

      // Resultado esperado: el formulario del ciclo sigue mostrando el grado "Ciclo
      // formativo" y el nivel "Superior". Se cierra antes
      // el modal del curso ("Cancelar" ejecuta `close`, no toca nada) para leer los
      // campos del ciclo sin ningún diálogo por delante.
      await modalCurso(page).getByRole('button', { name: 'Cancelar', exact: true }).click();
      await expect(page.getByRole('dialog')).toHaveCount(0);
      await expect(combo(page, 'grado')).toHaveValue(GRADO_CICLO);
      await expect(campo(page, 'nivel')).toBeVisible();
      await expect(combo(page, 'nivel')).toHaveValue(NIVEL_CICLO);
    } finally {
      // Teardown: quitar la asignación creada aunque una aserción haya fallado — el
      // curso "1º DAW" es un dato maestro compartido con los tests hermanos y dejarle
      // un módulo de más los envenenaría. El reload descarta cualquier formulario o
      // modal a medio editar que si no bloquearía la navegación.
      await volverAlInicio(page);
      await abrirListadoDeCursosModulos(page);
      await borrarModuloNuevoDelCurso(page);

      await logout(page);
    }
  });
});
