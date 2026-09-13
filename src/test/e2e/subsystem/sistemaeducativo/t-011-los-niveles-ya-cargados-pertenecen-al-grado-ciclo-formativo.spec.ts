import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-011 — Los niveles ya cargados pertenecen al grado «Ciclo formativo»
// origen: ESC-014  |  verifica: V-Nivel-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-011-los-niveles-ya-cargados-pertenecen-al-grado-ciclo-formativo.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test NO crea nada: edita un
 * nivel que ya viene en los datos iniciales, así que no puede aislarse con un
 * sufijo `Date.now()` ni borrar lo que toca. Se hace idempotente de la otra forma:
 *   - identifica la fila por su CÓDIGO (`GM`), que no cambia, nunca por el nombre,
 *     que es justo lo que el test modifica;
 *   - deja el nombre original restaurado en el `finally` (teardown) — el paso 8 ya
 *     lo restaura, así que el teardown solo actúa si una aserción falló antes, y
 *   - lo restaura TAMBIÉN al arrancar (pre-limpieza defensiva), porque un run que
 *     abortara entre el paso 6 y el 8 deja el nivel renombrado y el paso 3 ya no
 *     vería la lista exacta que exige.
 */
const CODIGO_NIVEL = 'GM';
const NOMBRE_ORIGINAL = 'Ciclos Formativos de Grado Medio';
const NOMBRE_MODIFICADO = `${NOMBRE_ORIGINAL} (LOFP)`;

/** El grado de los tres niveles del seed, el que este test comprueba que conservan. */
const GRADO = 'Ciclo formativo';

// Los campos `code`/`name` del modelo Nivel todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;

/**
 * Los tres niveles que los datos iniciales cuelgan del grado «Ciclo formativo», en
 * el orden en que los pinta el grid (`orderBy="name"`). Es la lista EXACTA que el
 * paso 3 exige ver, y a la que el resultado esperado debe volver al final.
 */
const NIVELES_DE_CICLO_FORMATIVO = [
  'Ciclos formativos de grado Básico',
  NOMBRE_ORIGINAL,
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
 * Las filas del listado cuyo grado es EXACTAMENTE «Ciclo formativo». El "los
 * niveles ya cargados" del escenario se comprueba sobre este subconjunto y no sobre
 * el grid entero a propósito: el estado inicial de la descripción solo define los
 * niveles de este grado, y la BD compartida arrastra niveles de OTROS grados
 * («Certificado de profesionalidad», «Certificado profesional»…) ajenos a este test.
 * Filtrar por la columna `grado` (y no por el texto de la fila) evita además que un
 * nombre que contenga "Ciclo formativo" cuele una fila de más.
 */
function filasDelGrado(page: Page) {
  return filasDeDatos(page).filter({
    has: page.locator('[data-testid="column:grado"]', { hasText: new RegExp(`^${GRADO}$`) }),
  });
}

/** La celda `name` de un conjunto de filas, para comparar la lista completa. */
function nombres(filas: ReturnType<typeof filasDeDatos>) {
  return filas.locator('[data-testid="column:name"]');
}

/**
 * La fila del nivel bajo prueba, localizada por su CÓDIGO y no por su nombre: el
 * nombre es lo que el test cambia, así que filtrar por él haría que la fila
 * "desapareciera" a mitad del test. El código es único entre los niveles.
 */
function filaDelNivel(page: Page) {
  return filasDeDatos(page).filter({
    has: page.locator('[data-testid="column:code"]', { hasText: new RegExp(`^${CODIGO_NIVEL}$`) }),
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
 * Abre la ficha del nivel desde el listado ya cargado (Axelor navega a
 * `.../Nivel-action/edit/<id>`) y espera a que el formulario traiga sus datos.
 */
async function abrirFichaDelNivel(page: Page): Promise<void> {
  await filaDelNivel(page).click();
  await expect(page).toHaveURL(/Nivel-action\/edit/);
  await expect(page.getByRole('textbox', { name: CAMPO_CODIGO })).toHaveValue(CODIGO_NIVEL);
}

/** Escribe `nombre` en el campo "Nombre" de la ficha abierta y guarda. */
async function guardarConNombre(page: Page, nombre: string): Promise<void> {
  await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombre);
  await page.getByRole('button', { name: 'Guardar', exact: true }).click();
  await expect(page).toHaveURL(/Nivel-action\/list/);
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Deja la app en la pantalla de inicio partiendo de cualquier sitio. El reload
 * descarta el formulario a medio editar, que si no bloquearía la navegación.
 */
async function volverAlInicio(page: Page): Promise<void> {
  await page.goto('/#/');
  await page.reload();
}

/**
 * Devuelve el nivel a su nombre de los datos iniciales. Sirve de pre-limpieza
 * (restos de un run que abortara antes del teardown) y de teardown. Si el nombre ya
 * es el original no abre nada, para no dejar una ficha a medio editar.
 */
async function restaurarNombreOriginal(page: Page): Promise<void> {
  await abrirListadoDeNiveles(page);
  const fila = filaDelNivel(page);
  await expect(fila).toHaveCount(1);
  const yaRestaurado = await fila
    .locator('[data-testid="column:name"]', { hasText: new RegExp(`^${NOMBRE_ORIGINAL}$`) })
    .count();
  if (yaRestaurado === 1) {
    return;
  }
  await abrirFichaDelNivel(page);
  await guardarConNombre(page, NOMBRE_ORIGINAL);
}

test.describe('Sistema educativo — Niveles', () => {
  test('Los niveles ya cargados pertenecen al grado «Ciclo formativo»', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva: si un run anterior abortó tras renombrar el nivel y
      // antes de su teardown, devolverlo a su nombre de los datos iniciales.
      await restaurarNombreOriginal(page);

      // Paso 2: abre el menú "Sistema educativo" → "Niveles".
      await abrirListadoDeNiveles(page);

      // Paso 3: el listado muestra las filas "Ciclos formativos de grado Básico",
      // "Ciclos Formativos de Grado Medio" y "Ciclos Formativos de Grado Superior",
      // las tres con el grado "Ciclo formativo". `toHaveText` con un array exige esa
      // lista EXACTA (ni una fila de más ni de menos) sobre las filas cuya columna
      // "Grado" es "Ciclo formativo", con lo que las tres llevan ese grado por
      // construcción del filtro.
      await expect(nombres(filasDelGrado(page))).toHaveText(NIVELES_DE_CICLO_FORMATIVO);

      // Paso 4: pulsa la fila "Ciclos Formativos de Grado Medio".
      await abrirFichaDelNivel(page);

      // Paso 5: el formulario muestra el código "GM", el nombre "Ciclos Formativos
      // de Grado Medio" y el grado "Ciclo formativo".
      await expect(page.getByRole('textbox', { name: CAMPO_CODIGO })).toHaveValue(CODIGO_NIVEL);
      await expect(page.getByRole('textbox', { name: CAMPO_NOMBRE })).toHaveValue(NOMBRE_ORIGINAL);
      await expect(combo(page, 'grado')).toHaveValue(GRADO);

      // Paso 6: cambia el nombre a "Ciclos Formativos de Grado Medio (LOFP)" y pulsa
      // "Guardar". NO se toca el grado: es lo que da sentido al "conserva" del paso 7.
      // Paso 7: el sistema guarda el nivel y vuelve al listado (lo comprueba
      // `guardarConNombre` con la URL del listado).
      await guardarConNombre(page, NOMBRE_MODIFICADO);

      // Paso 7: la fila aparece con el nombre "Ciclos Formativos de Grado Medio
      // (LOFP)" y conserva el grado "Ciclo formativo". Las celdas se comparan con el
      // regex anclado porque el nombre original es prefijo del modificado y una
      // comparación por subcadena no distinguiría uno de otro.
      const fila = filaDelNivel(page);
      await expect(fila).toHaveCount(1);
      await expect(fila.locator('[data-testid="column:name"]')).toHaveText(NOMBRE_MODIFICADO);
      await expect(fila.locator('[data-testid="column:grado"]')).toHaveText(GRADO);
      // Y el nivel renombrado sigue contándose entre los del grado "Ciclo formativo":
      // el renombrado no lo ha sacado del grado ni ha tocado a sus hermanos.
      await expect(nombres(filasDelGrado(page))).toHaveText([
        NIVELES_DE_CICLO_FORMATIVO[0],
        NOMBRE_MODIFICADO,
        NIVELES_DE_CICLO_FORMATIVO[2],
      ]);

      // Paso 8: pulsa de nuevo esa fila, cambia el nombre a "Ciclos Formativos de
      // Grado Medio" y pulsa "Guardar".
      await abrirFichaDelNivel(page);
      await expect(page.getByRole('textbox', { name: CAMPO_NOMBRE })).toHaveValue(NOMBRE_MODIFICADO);

      // Resultado esperado: el sistema guarda el nivel y el listado vuelve a mostrar
      // la fila "Ciclos Formativos de Grado Medio" con el grado "Ciclo formativo".
      await guardarConNombre(page, NOMBRE_ORIGINAL);
      const filaRestaurada = filaDelNivel(page);
      await expect(filaRestaurada).toHaveCount(1);
      await expect(filaRestaurada.locator('[data-testid="column:name"]')).toHaveText(NOMBRE_ORIGINAL);
      await expect(filaRestaurada.locator('[data-testid="column:grado"]')).toHaveText(GRADO);
      // Y el listado vuelve a ser exactamente el de los datos iniciales: es lo que
      // distingue una vuelta atrás de verdad de una aserción que pasaría igual sin
      // haber guardado.
      await expect(nombres(filasDelGrado(page))).toHaveText(NIVELES_DE_CICLO_FORMATIVO);
    } finally {
      // Teardown: devolver el nivel a su nombre de los datos iniciales aunque una
      // aserción haya fallado — el test edita datos maestros compartidos, así que
      // dejarlo renombrado envenenaría a los demás tests. El reload descarta
      // cualquier formulario a medio editar que si no bloquearía la navegación.
      await volverAlInicio(page);
      await restaurarNombreOriginal(page);

      await logout(page);
    }
  });
});
