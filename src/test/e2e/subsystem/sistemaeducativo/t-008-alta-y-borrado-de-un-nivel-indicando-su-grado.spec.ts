import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-008 — Alta y borrado de un nivel indicando su grado
// origen: ESC-006  |  verifica: V-Nivel-001, U-niveles-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-008-alta-y-borrado-de-un-nivel-indicando-su-grado.desc.md

// Marca propia de este test: todo lo que crea lleva esta cadena en el código y en
// el nombre, así la pre-limpieza (§4.3 del contrato de generación) puede reconocer
// y borrar lo que dejara pegado un run anterior que abortara antes del teardown.
const MARCA = 'e2e-t008';

// Los campos `code`/`name` del modelo Nivel todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;

/** El grado al que se adscribe el nivel dado de alta y el de los tres del seed. */
const GRADO = 'Ciclo formativo';

/**
 * Los tres niveles que los datos iniciales cuelgan del grado «Ciclo formativo», en
 * el orden en que los pinta el grid (`orderBy="name"`). Es la lista que el resultado
 * esperado exige ver EXACTAMENTE al terminar, una vez borrado el nivel creado.
 */
const NIVELES_DE_CICLO_FORMATIVO = [
  'Ciclos formativos de grado Básico',
  'Ciclos Formativos de Grado Medio',
  'Ciclos Formativos de Grado Superior',
];

/** Entrada del menú lateral «Sistema educativo» que recorre este test. */
const MENU_NIVELES = 'item:sistemaEducativo-niveles-menuitem';
const BTN_NUEVO_NIVEL = 'Añadir un nuevo nivel';

/**
 * Filas de datos del grid (excluye la cabecera y la fila de búsqueda). El grid las
 * pinta por AJAX, así que hay que esperarlas antes de CONTAR nada: `count()` no
 * reintenta y devolvería 0 sobre un grid aún vacío.
 */
function filasDeDatos(page: Page) {
  return page.locator('[data-testid^="row:"]');
}

/**
 * Las filas del listado cuyo grado es EXACTAMENTE «Ciclo formativo». El "solo los
 * tres" del resultado esperado se comprueba sobre este subconjunto y no sobre el
 * grid entero a propósito: el estado inicial de la descripción solo define los
 * niveles de este grado, y la BD compartida puede arrastrar niveles de OTROS grados
 * ajenos a este test. Filtrar por la columna `grado` (no por el texto de la fila)
 * evita además que un nombre que contenga "Ciclo formativo" cuele una fila de más.
 */
function filasDelGrado(page: Page) {
  return filasDeDatos(page).filter({
    has: page.locator('[data-testid="column:grado"]', { hasText: new RegExp(`^${GRADO}$`) }),
  });
}

/** La celda `nombre` de un conjunto de filas, para comparar la lista completa. */
function nombres(filas: ReturnType<typeof filasDeDatos>) {
  return filas.locator('[data-testid="column:name"]');
}

/**
 * La fila de un nivel localizada por su nombre EXACTO. El `exact` es obligatorio
 * aquí: "Ciclos Formativos de Grado Medio" es prefijo del nombre que este test crea,
 * así que una comparación por subcadena confundiría una fila con la otra.
 */
function filaDelNivel(page: Page, nombre: string) {
  return filasDeDatos(page).filter({
    has: page.locator('[data-testid="column:name"]', { hasText: new RegExp(`^${nombre}$`) }),
  });
}

/**
 * Localiza un campo del formulario por su nombre en el modelo. El `data-testid`
 * `field:<campo>` lo deriva Axelor del propio modelo, así que es estable (no es un
 * id autogenerado tipo `#x-123`).
 */
function campo(page: Page, nombre: string) {
  return page.getByTestId(`field:${nombre}`);
}

/**
 * El input de un campo many-to-one. No se usa `getByRole('combobox', { name })`
 * porque al desplegarse Axelor saca el control del árbol de accesibilidad y el
 * locator por rol deja de resolver a mitad de la interacción.
 */
function combo(page: Page, nombre: string) {
  return campo(page, nombre).getByTestId('input');
}

/**
 * Elige un valor en un campo many-to-one de Axelor que está VACÍO. El desplegable
 * solo filtra con pulsaciones reales (un `fill` cambia el valor sin disparar la
 * búsqueda), de ahí el `pressSequentially`.
 */
async function elegirEnCombo(page: Page, nombreCampo: string, valor: string): Promise<void> {
  const control = combo(page, nombreCampo);
  await control.click();
  await control.pressSequentially(valor);
  await page.getByRole('option', { name: valor, exact: true }).click();
  await expect(control).toHaveValue(valor);
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
    await page.getByTestId('item:sistemaEducativo-menuitem').click();
  }
  await entrada.click();
}

/** Abre el listado de niveles desde el menú y espera a que esté pintado con sus filas. */
async function abrirListadoDeNiveles(page: Page): Promise<void> {
  await abrirMenu(page, MENU_NIVELES);
  await expect(page.getByRole('button', { name: BTN_NUEVO_NIVEL })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Borra desde el listado de niveles el nivel de la fila `fila`: abre su ficha, pulsa
 * "Borrar" y confirma. Axelor vuelve al listado al confirmar.
 */
async function borrarNivelDeLaFila(page: Page, fila: ReturnType<typeof filasDeDatos>): Promise<void> {
  await fila.click();
  await expect(page).toHaveURL(/Nivel-action\/edit/);
  await page.getByRole('button', { name: 'Borrar', exact: true }).click();
  await page.getByRole('dialog').getByRole('button', { name: BTN_CONFIRMAR_BORRADO }).click();
  await expect(page).toHaveURL(/Nivel-action\/list/);
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Borra los niveles que este test dejara pegados (los que llevan su marca). Sirve de
 * pre-limpieza (restos de un run que abortara antes del teardown) y de teardown.
 * LIMIT 10: cota dura para no quedarse en bucle si un borrado no se aplica.
 */
async function limpiarRestos(page: Page): Promise<void> {
  for (let i = 0; i < 10; i++) {
    await abrirListadoDeNiveles(page);
    const restos = filasDeDatos(page).filter({ hasText: MARCA });
    if ((await restos.count()) === 0) {
      return;
    }
    await borrarNivelDeLaFila(page, restos.first());
  }
  throw new Error(`No se pudo limpiar los niveles marcados con "${MARCA}"`);
}

/**
 * Deja la app en la pantalla de inicio partiendo de cualquier sitio. El reload
 * descarta el formulario a medio editar, que si no bloquearía la navegación.
 */
async function volverAlInicio(page: Page): Promise<void> {
  await page.goto('/#/');
  await page.reload();
}

test.describe('Sistema educativo — Niveles', () => {
  test('Alta y borrado de un nivel indicando su grado', async ({ page }) => {
    // Nombre y código únicos por ejecución: la BD es compartida y no se resetea, y
    // el código de un nivel es único. El nombre conserva como prefijo el literal de
    // la descripción ("Ciclos Formativos de Grado Medio Dual").
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigoNivel = `GMD-${sufijo}`;
    const nombreNivel = `Ciclos Formativos de Grado Medio Dual ${sufijo}`;

    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva: restos de un run anterior de ESTE test que abortara
      // antes de su teardown (§4.3 del contrato de generación).
      await limpiarRestos(page);

      // Paso 2: abre el menú "Sistema educativo" → "Niveles" y pulsa "Añadir un
      // nuevo nivel".
      await abrirListadoDeNiveles(page);
      // Precondición: de partida el grado "Ciclo formativo" tiene exactamente los
      // tres niveles del seed; es lo que da sentido al "vuelve a mostrar solo" del
      // resultado esperado (si no, no se estaría comprobando ninguna vuelta atrás).
      await expect(nombres(filasDelGrado(page))).toHaveText(NIVELES_DE_CICLO_FORMATIVO);
      await page.getByRole('button', { name: BTN_NUEVO_NIVEL }).click();

      // Paso 3: rellena el campo "Código" con "GMD" y el campo "Nombre" con "Ciclos
      // Formativos de Grado Medio Dual", y elige el grado "Ciclo formativo".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoNivel);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreNivel);
      await elegirEnCombo(page, 'grado', GRADO);

      // Paso 4: pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 5 / Resultado esperado: el sistema guarda el nivel y vuelve al listado,
      // donde "Ciclos Formativos de Grado Medio Dual" aparece con el grado "Ciclo
      // formativo".
      await expect(page).toHaveURL(/Nivel-action\/list/);
      const filaCreada = filaDelNivel(page, nombreNivel);
      await expect(filaCreada).toHaveCount(1);
      await expect(filaCreada.locator('[data-testid="column:code"]')).toHaveText(codigoNivel);
      await expect(filaCreada.locator('[data-testid="column:grado"]')).toHaveText(GRADO);

      // Paso 6: pulsa la fila "Ciclos Formativos de Grado Medio Dual" y pulsa
      // "Borrar" (y confirma el diálogo de Axelor).
      await borrarNivelDeLaFila(page, filaCreada);

      // Resultado esperado: el sistema borra el nivel.
      await expect(filaDelNivel(page, nombreNivel)).toHaveCount(0);

      // Resultado esperado: el listado vuelve a mostrar solo "Ciclos formativos de
      // grado Básico", "Ciclos Formativos de Grado Medio" y "Ciclos Formativos de
      // Grado Superior", los tres con el grado "Ciclo formativo". `toHaveText` con un
      // array exige esa lista EXACTA (ni una fila de más ni de menos) sobre las filas
      // cuya columna "Grado" es "Ciclo formativo", con lo que las tres llevan ese
      // grado por construcción del filtro.
      await expect(nombres(filasDelGrado(page))).toHaveText(NIVELES_DE_CICLO_FORMATIVO);
    } finally {
      // Teardown: el paso 6 ya borra el nivel creado, así que esto solo actúa si una
      // aserción falló antes de llegar a borrarlo. El reload descarta cualquier
      // formulario a medio editar que si no bloquearía la navegación.
      await volverAlInicio(page);
      await limpiarRestos(page);

      await logout(page);
    }
  });
});
