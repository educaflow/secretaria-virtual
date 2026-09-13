import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-014 — Los cursos de un ciclo se siguen manteniendo desde el propio ciclo
// origen: ESC-011  |  verifica: V-Ciclo-001, V-Ciclo-003, U-ciclos-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-014-los-cursos-de-un-ciclo-se-siguen-manteniendo-desde-el-propio-ciclo.desc.md

// Marca propia de este test: el curso que crea lleva esta cadena en el código y en
// el nombre, así la pre-limpieza (§4.3 del contrato de generación) reconoce y borra
// lo que dejara pegado un run anterior que abortara antes del teardown. No se toca
// nada más del ciclo, que es un dato maestro compartido con los tests hermanos.
const MARCA = 'e2e-t014';

// Los campos `code`/`name` de los modelos Ciclo y Curso todavía no están traducidos,
// así que la UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre".
// El regex acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;

// El diálogo de confirmación de borrado de Axelor sale traducido al español
// («¿Realmente quieres eliminar el registro seleccionado?» / «Eliminar»); el regex
// acepta también el inglés por si se ejecuta con otro locale.
const TEXTO_CONFIRMAR_BORRADO = /really want to delete|(quieres|desea) (borrar|eliminar)/i;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;

/** Entradas del menú lateral «Sistema educativo» que recorre este test. */
const MENU_SISTEMA_EDUCATIVO = 'item:sistemaEducativo-menuitem';
const MENU_CICLOS = 'item:sistemaEducativo-ciclos-menuitem';
const MENU_CURSOS = 'item:sistemaEducativo-cursos-menuitem';

const BTN_NUEVO_CICLO = 'Añadir un nuevo ciclo';
// El botón de alta del panel "Cursos" de la ficha del ciclo y el del listado
// principal de cursos NO se llaman igual.
const BTN_NUEVO_CURSO_PANEL = 'Añadir un nuevo curso';
const BTN_NUEVO_CURSO_LISTADO = 'Nuevo curso';

// Ciclo de los datos iniciales sobre el que trabaja el test. Se identifica siempre
// por su CÓDIGO, que es único entre los ciclos y que este test no modifica: filtrar
// por el nombre no distinguiría «Desarrollo de Aplicaciones Web» de «Desarrollo de
// Aplicaciones Multiplataforma» sin `exact`, y el código es más estable.
const CODIGO_CICLO = 'DAW';
const NOMBRE_CICLO = 'Desarrollo de Aplicaciones Web';
const GRADO_CICLO = 'Ciclo formativo';
const NIVEL_CICLO = 'Ciclos Formativos de Grado Superior';

// Cursos del ciclo que ya vienen en los datos iniciales.
const CURSO_1 = '1º DAW';
const CURSO_2 = '2º DAW';

const LEY_LOFP =
  'Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional';

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
 * id autogenerado tipo `#x-123`).
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

/**
 * El panel-related "Cursos" de la ficha del ciclo. TODOS los locators del panel se
 * acotan a él: el formulario del ciclo y el grid de cursos comparten `data-testid`
 * (`field:code`, `field:name`, `row:N`…), así que sin acotar serían ambiguos.
 */
function panelDeCursos(page: Page) {
  return campo(page, 'cursos');
}

/** Filas del panel "Cursos" de la ficha del ciclo. */
function filasDeCursos(page: Page) {
  return filasDeDatos(panelDeCursos(page));
}

/**
 * El modal del formulario de un curso, abierto desde el panel "Cursos". Se localiza
 * por su encabezado "Curso" porque al confirmar un borrado conviven DOS diálogos (el
 * del curso y el de confirmación) y `getByRole('dialog')` a secas sería ambiguo.
 */
function modalCurso(page: Page) {
  return page
    .getByRole('dialog')
    .filter({ has: page.getByRole('heading', { name: 'Curso', exact: true }) });
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
 * contar filas del panel devolvería 0 sobre un grid aún vacío.
 */
async function abrirFichaDelCiclo(page: Page): Promise<void> {
  await filasDeDatos(page)
    .filter({ has: page.getByRole('gridcell', { name: CODIGO_CICLO, exact: true }) })
    .click();
  await expect(page).toHaveURL(/Ciclo-action\/edit/);
  await expect(page.getByRole('textbox', { name: CAMPO_CODIGO })).toHaveValue(CODIGO_CICLO);
  await expect(filasDeCursos(page).first()).toBeVisible();
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

/** Abre el menú "Sistema educativo" → "Cursos" y espera al listado pintado. */
async function abrirListadoDeCursos(page: Page): Promise<void> {
  await abrirMenu(page, MENU_CURSOS);
  await expect(page).toHaveURL(/Curso-action\/list/);
  await expect(page.getByRole('button', { name: BTN_NUEVO_CURSO_LISTADO })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Borra de verdad, desde el listado principal de cursos, todos los cursos cuyo texto
 * contenga `texto`. Pulsar la fila abre su ficha (el grid es `canEditOnClick`) y el
 * borrado se confirma en el diálogo de Axelor.
 *
 * **CRITICAL — el borrado NO se hace desde el panel "Cursos" del ciclo**: el botón
 * "Borrar" del modal anidado ejecuta `delete-modal`, que solo quita la fila de la
 * colección en el cliente y deja la fila viva en la BD si el ciclo no se vuelve a
 * guardar. El botón "Borrar" de esta ficha ejecuta `delete`, que la borra en el
 * servidor. LIMIT 10: cota dura para no quedarse en bucle si un borrado no se aplica.
 */
async function borrarCursosQueContengan(page: Page, texto: string): Promise<void> {
  for (let i = 0; i < 10; i++) {
    await expect(page.getByRole('button', { name: BTN_NUEVO_CURSO_LISTADO })).toBeVisible();
    await expect(filasDeDatos(page).first()).toBeVisible();
    const filas = filasDeDatos(page).filter({ hasText: texto });
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
  throw new Error(`No se pudo limpiar los cursos que contienen "${texto}"`);
}

test.describe('Sistema educativo — Cursos de un ciclo', () => {
  test('Los cursos de un ciclo se siguen manteniendo desde el propio ciclo', async ({ page }) => {
    // Nombre y código únicos por ejecución: la BD es compartida y no se resetea, así
    // que un "3º DAW" fijo se confundiría con el que dejó un run anterior. El nombre
    // conserva el "3º DAW" de la descripción como prefijo.
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigoCurso = `DAW3-${sufijo}`;
    const nombreCurso = `3º DAW ${sufijo}`;

    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva: cursos de este test que dejara pegados un run anterior
      // que abortara antes de su teardown (§4.3 del contrato de generación).
      await abrirListadoDeCursos(page);
      await borrarCursosQueContengan(page, MARCA);

      // Paso 2: abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila
      // "Desarrollo de Aplicaciones Web".
      await abrirListadoDeCiclos(page);
      await expect(
        filasDeDatos(page).filter({
          has: page.getByRole('gridcell', { name: CODIGO_CICLO, exact: true }),
        }).getByRole('gridcell', { name: NOMBRE_CICLO, exact: true }),
      ).toBeVisible();
      await abrirFichaDelCiclo(page);

      // Paso 3: el panel "Cursos" muestra "1º DAW" y "2º DAW". Se comparan las celdas
      // con `exact: true` porque el curso que crea el test lleva "3º DAW" como prefijo
      // y una comparación por subcadena no distinguiría unos de otros.
      await expect(
        panelDeCursos(page).getByRole('gridcell', { name: CURSO_1, exact: true }),
      ).toBeVisible();
      await expect(
        panelDeCursos(page).getByRole('gridcell', { name: CURSO_2, exact: true }),
      ).toBeVisible();

      // Paso 4: en el panel "Cursos" pulsa "Añadir un nuevo curso".
      await panelDeCursos(page).getByRole('button', { name: BTN_NUEVO_CURSO_PANEL }).click();
      const modal = modalCurso(page);
      await expect(modal).toBeVisible();

      // Paso 5: rellena "Código" con "DAW3" y "Nombre" con "3º DAW", y elige la ley
      // educativa "Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de
      // la Formación Profesional". Todo se acota al modal: el formulario del ciclo que
      // queda detrás tiene campos con los mismos `data-testid` y los mismos rótulos.
      await modal.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoCurso);
      await modal.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreCurso);
      // El desplegable de un many-to-one solo filtra con pulsaciones reales (un `fill`
      // cambiaría el valor sin disparar la búsqueda), de ahí el `pressSequentially`.
      // Este combo NO se acota al modal: al desplegarse, Axelor saca el diálogo del
      // árbol de accesibilidad y el filtro por su encabezado deja de resolver a mitad
      // de la interacción. No hace falta acotarlo porque "leyEducativa" es un campo
      // que solo existe en el formulario del curso, no en el del ciclo.
      const comboLey = combo(page, 'leyEducativa');
      await comboLey.click();
      await comboLey.pressSequentially(LEY_LOFP);
      await page.getByRole('option', { name: LEY_LOFP, exact: true }).click();
      await expect(comboLey).toHaveValue(LEY_LOFP);

      // Paso 6: pulsa "Guardar" en el formulario del curso. El modal se cierra y el
      // curso queda en el panel, todavía sin persistir (lo persiste el guardado del
      // ciclo del paso siguiente).
      await modal.getByRole('button', { name: 'Guardar', exact: true }).click();
      await expect(page.getByRole('dialog')).toHaveCount(0);
      await expect(
        panelDeCursos(page).getByRole('gridcell', { name: nombreCurso, exact: true }),
      ).toBeVisible();

      // Paso 7: pulsa "Guardar" en el formulario del ciclo. Con el modal ya cerrado,
      // el único "Guardar" de la página es el del ciclo.
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Resultado esperado: el sistema guarda el ciclo y vuelve al listado.
      await expect(page).toHaveURL(/Ciclo-action\/list/);
      await expect(filasDeDatos(page).first()).toBeVisible();

      // Resultado esperado: al pulsar de nuevo la fila "Desarrollo de Aplicaciones
      // Web", el panel "Cursos" muestra "1º DAW", "2º DAW" y "3º DAW".
      await abrirFichaDelCiclo(page);
      await expect(
        panelDeCursos(page).getByRole('gridcell', { name: CURSO_1, exact: true }),
      ).toBeVisible();
      await expect(
        panelDeCursos(page).getByRole('gridcell', { name: CURSO_2, exact: true }),
      ).toBeVisible();
      const filaCursoNuevo = filasDeCursos(page).filter({ hasText: nombreCurso });
      await expect(filaCursoNuevo).toHaveCount(1);
      await expect(
        filaCursoNuevo.getByRole('gridcell', { name: nombreCurso, exact: true }),
      ).toBeVisible();
      // El curso persistió con el código y la ley educativa que se le dieron: es lo
      // que distingue un guardado de verdad de una fila que siguiera solo en memoria.
      await expect(
        filaCursoNuevo.getByRole('gridcell', { name: codigoCurso, exact: true }),
      ).toBeVisible();
      await expect(
        filaCursoNuevo.getByRole('gridcell', { name: LEY_LOFP, exact: true }),
      ).toBeVisible();

      // Resultado esperado: el formulario del ciclo sigue mostrando el grado "Ciclo
      // formativo" y el nivel "Ciclos Formativos de Grado Superior".
      await expect(combo(page, 'grado')).toHaveValue(GRADO_CICLO);
      await expect(campo(page, 'nivel')).toBeVisible();
      await expect(combo(page, 'nivel')).toHaveValue(NIVEL_CICLO);
    } finally {
      // Teardown: borrar el curso creado aunque una aserción haya fallado — el ciclo
      // es un dato maestro compartido con los tests hermanos y dejarle un curso de más
      // los envenenaría. El reload descarta cualquier formulario o modal a medio
      // editar que si no bloquearía la navegación.
      await volverAlInicio(page);
      await abrirListadoDeCursos(page);
      await borrarCursosQueContengan(page, MARCA);

      await logout(page);
    }
  });
});
