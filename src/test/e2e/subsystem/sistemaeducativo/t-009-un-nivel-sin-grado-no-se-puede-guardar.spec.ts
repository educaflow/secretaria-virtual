import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-009 — Un nivel sin grado no se puede guardar
// origen: ESC-007  |  verifica: V-Nivel-001, U-niveles-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-009-un-nivel-sin-grado-no-se-puede-guardar.desc.md

// Marca propia de este test: todo lo que crea lleva esta cadena en el código y en
// el nombre, así la pre-limpieza (§4.3 del contrato de generación) puede reconocer
// y borrar lo que dejara pegado un run anterior que abortara antes del teardown.
const MARCA = 'e2e-t009';

// Los campos `code`/`name` del modelo Nivel todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;

// Botón de confirmación de los diálogos de Axelor (el de "Validation Error" y el de
// "Question"). Todavía sin traducir, de ahí el regex con ambos literales.
const BTN_ACEPTAR_DIALOGO = /^(OK|Aceptar)$/;

// Mensaje de V-Nivel-001, literal de NivelServiceImpl.validateGradoIndicado.
const ERROR_GRADO_OBLIGATORIO = 'El grado es obligatorio';

/**
 * El nombre del nivel que la descripción intenta dar de alta. Se comprueba aparte
 * del nombre único porque el resultado esperado habla de ESTE literal: ninguna fila
 * del listado debe mostrarlo. No colisiona con el nivel del seed "Ciclos Formativos
 * de Grado Superior", que es un PREFIJO suyo y no lo contiene.
 */
const NOMBRE_BASE = 'Ciclos Formativos de Grado Superior Dual';

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
 * Borra los niveles que este test dejara pegados (los que llevan su marca). Este
 * test NO debe llegar a guardar nada, pero si una regresión hiciera que el alta
 * pasara, el nivel quedaría en la BD; sirve de pre-limpieza (restos de un run que
 * abortara antes del teardown) y de teardown.
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
  test('Un nivel sin grado no se puede guardar', async ({ page }) => {
    // Nombre y código únicos por ejecución: la BD es compartida y no se resetea, y
    // el código de un nivel es único. El nombre conserva como prefijo el literal de
    // la descripción ("Ciclos Formativos de Grado Superior Dual").
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigoNivel = `GSD-${sufijo}`;
    const nombreNivel = `${NOMBRE_BASE} ${sufijo}`;

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
      await page.getByRole('button', { name: BTN_NUEVO_NIVEL }).click();
      await expect(page).toHaveURL(/Nivel-action\/edit/);

      // Paso 3: rellena el campo "Código" con "GSD" y el campo "Nombre" con "Ciclos
      // Formativos de Grado Superior Dual", y deja el campo "Grado" vacío.
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoNivel);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreNivel);
      // El grado no se toca: se comprueba que queda vacío, que es la condición que
      // da sentido al error de validación que se espera a continuación.
      await expect(combo(page, 'grado')).toHaveValue('');

      // Paso 4: pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 5: el sistema muestra el mensaje "El grado es obligatorio". Axelor lo
      // pinta en un diálogo "Validation Error" con una entrada por mensaje de
      // negocio, prefijada con el campo al que se ancla ("grado:").
      await expect(page.getByRole('dialog')).toContainText(ERROR_GRADO_OBLIGATORIO);

      // Paso 5 (cont.): el sistema NO guarda el nivel — al fallar la validación la
      // app se queda en el formulario y no vuelve al listado (que es lo que hace un
      // alta correcta).
      await page.getByRole('dialog').getByRole('button', { name: BTN_ACEPTAR_DIALOGO }).click();
      await expect(page).toHaveURL(/Nivel-action\/edit/);

      // Paso 6: pulsa "Cancelar" y vuelve al listado de niveles. Como el formulario
      // tiene cambios sin guardar, Axelor pide confirmación ("Current changes will
      // be lost") antes de descartarlos.
      await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
      await page.getByRole('dialog').getByRole('button', { name: BTN_ACEPTAR_DIALOGO }).click();
      await expect(page).toHaveURL(/Nivel-action\/list/);
      await expect(filasDeDatos(page).first()).toBeVisible();

      // Resultado esperado: el listado no muestra ninguna fila "Ciclos Formativos de
      // Grado Superior Dual". Se comprueba con el literal de la descripción (que no
      // casa con el nivel "Ciclos Formativos de Grado Superior" del seed, del que es
      // sufijo y no subcadena) y además con el nombre único de esta ejecución.
      await expect(filasDeDatos(page).filter({ hasText: NOMBRE_BASE })).toHaveCount(0);
      await expect(filasDeDatos(page).filter({ hasText: nombreNivel })).toHaveCount(0);
      await expect(page.getByRole('row').filter({ hasText: NOMBRE_BASE })).toHaveCount(0);
    } finally {
      // Teardown: el test no debería haber creado nada, pero si el alta llegara a
      // pasar (regresión) hay que borrarla para no dejar la BD sucia. El reload
      // descarta cualquier formulario a medio editar que si no bloquearía la
      // navegación.
      await volverAlInicio(page);
      await limpiarRestos(page);

      await logout(page);
    }
  });
});
