import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-004 — Al cambiar el grado a uno sin niveles, el nivel se vacía
// origen: ESC-004  |  verifica: U-ciclos-003, U-ciclos-001, V-Ciclo-002
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-004-al-cambiar-el-grado-a-uno-sin-niveles-el-nivel-se-vacia.desc.md

// Marca propia de este test: todo lo que crea lleva esta cadena en el nombre, así
// la pre-limpieza (§4.3 del contrato) puede reconocer y borrar lo que dejara
// pegado un run anterior que abortara antes del teardown.
const MARCA = 'e2e-t004';

// Los campos `code`/`name` del modelo Ciclo todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;

/**
 * Filas de datos del grid (excluye la cabecera y la fila de búsqueda). El grid las
 * pinta por AJAX, así que hay que esperarlas antes de CONTAR nada: `count()` no
 * reintenta y devolvería 0 sobre un grid aún vacío.
 */
function filasDeDatos(page: Page) {
  return page.locator('[data-testid^="row:"]');
}

/** Espera a que el listado de ciclos esté pintado con sus filas. */
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(page.getByRole('button', { name: 'Añadir un nuevo ciclo' })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/** Abre el menú "Sistema educativo" → "Ciclos" y espera al listado. */
async function abrirListadoDeCiclos(page: Page): Promise<void> {
  await page.getByText('Sistema educativo', { exact: true }).click();
  await page.getByText('Ciclos', { exact: true }).click();
  await esperarListadoCargado(page);
}

/**
 * Deja la app en el listado de ciclos partiendo de cualquier sitio. El reload
 * descarta el formulario a medio editar, que si no bloquearía la navegación.
 */
async function volverAlListadoDeCiclos(page: Page): Promise<void> {
  await page.goto('/#/');
  await page.reload();
  await abrirListadoDeCiclos(page);
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
 * Sustituye el valor de un many-to-one que YA tiene uno. A diferencia de
 * `elegirEnCombo`, hay que seleccionar el texto previo (Ctrl+A) antes de teclear:
 * si no, las pulsaciones se añaden al valor actual y el desplegable no encuentra
 * ninguna opción. Sigue haciendo falta `pressSequentially` para que Axelor dispare
 * la búsqueda.
 */
async function cambiarCombo(page: Page, nombreCampo: string, valor: string): Promise<void> {
  const control = combo(page, nombreCampo);
  await control.click();
  await control.press('ControlOrMeta+a');
  await control.pressSequentially(valor);
  await page.getByRole('option', { name: valor, exact: true }).click();
  await expect(control).toHaveValue(valor);
}

/**
 * Abre el formulario de un ciclo desde el listado pulsando su fila (una sola
 * pulsación: Axelor navega a `.../Ciclo-action/edit/<id>`).
 */
async function abrirFichaDelCiclo(page: Page, nombre: string): Promise<void> {
  await esperarListadoCargado(page);
  await filasDeDatos(page).filter({ hasText: nombre }).first().click();
  await expect(page).toHaveURL(/Ciclo-action\/edit/);
  await expect(page.getByRole('textbox', { name: CAMPO_NOMBRE })).toHaveValue(nombre);
}

/**
 * Borra, desde el listado de ciclos ya cargado, todas las filas cuyo texto contenga
 * `texto`. LIMIT 10: cota dura para no quedarse en bucle si un borrado no se aplica.
 */
async function borrarCiclosQueContengan(page: Page, texto: string): Promise<void> {
  for (let i = 0; i < 10; i++) {
    await esperarListadoCargado(page);
    const filas = filasDeDatos(page).filter({ hasText: texto });
    if ((await filas.count()) === 0) {
      return;
    }
    await filas.first().click();
    await page.getByRole('button', { name: 'Borrar', exact: true }).click();
    await page.getByRole('dialog').getByRole('button', { name: BTN_CONFIRMAR_BORRADO }).click();
  }
  throw new Error(`No se pudo limpiar los ciclos que contienen "${texto}"`);
}

test.describe('Sistema educativo — Ciclos', () => {
  test('Al cambiar el grado a uno sin niveles, el nivel se vacía', async ({ page }) => {
    // Nombre y código únicos por ejecución: la BD es compartida y no se resetea.
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigo = `TSMER-${sufijo}`;
    const nombre = `Mecatrónica Industrial ${sufijo}`;

    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 2: abre el menú "Sistema educativo" → "Ciclos".
      await abrirListadoDeCiclos(page);

      // Pre-limpieza defensiva: restos de un run anterior de ESTE test que abortara
      // antes de su teardown (§4.3 del contrato de generación).
      await borrarCiclosQueContengan(page, MARCA);

      // Paso 2 (cont.): pulsa "Añadir un nuevo ciclo".
      await page.getByRole('button', { name: 'Añadir un nuevo ciclo' }).click();

      // Paso 3: rellena el campo "Código" con "TSMER" y el campo "Nombre" con
      // "Mecatrónica Industrial".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigo);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombre);

      // Paso 4: elige la familia profesional "Instalación y Mantenimiento".
      await elegirEnCombo(page, 'familiaProfesional', 'Instalación y Mantenimiento');

      // Paso 5: elige el grado "Ciclo formativo" y el nivel "Superior" (el nivel solo
      // se puede elegir una vez hay grado: antes el campo ni se pinta).
      await elegirEnCombo(page, 'grado', 'Ciclo formativo');
      await elegirEnCombo(page, 'nivel', 'Superior');

      // Paso 6: pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 7: el sistema guarda el ciclo y vuelve al listado, donde "Mecatrónica
      // Industrial" aparece con el grado "Ciclo formativo" y el nivel "Superior".
      await expect(page).toHaveURL(/Ciclo-action\/list/);
      await esperarListadoCargado(page);
      const filaCreada = page.getByRole('row').filter({ hasText: nombre });
      await expect(filaCreada).toHaveCount(1);
      await expect(filaCreada.getByRole('gridcell', { name: codigo, exact: true })).toBeVisible();
      await expect(filaCreada.getByRole('gridcell', { name: 'Ciclo formativo', exact: true })).toBeVisible();
      await expect(
        filaCreada.getByRole('gridcell', { name: 'Superior', exact: true }),
      ).toBeVisible();

      // Paso 8: pulsa la fila "Mecatrónica Industrial".
      await abrirFichaDelCiclo(page, nombre);

      // Paso 9: el sistema abre el formulario del ciclo con el grado "Ciclo formativo"
      // y el campo "Nivel" con el valor "Superior".
      await expect(combo(page, 'grado')).toHaveValue('Ciclo formativo');
      await expect(campo(page, 'nivel').getByTestId('label')).toHaveText('Nivel');
      await expect(combo(page, 'nivel')).toHaveValue('Superior');

      // Paso 10: cambia el grado a "Curso de especialización".
      await cambiarCombo(page, 'grado', 'Curso de especialización');

      // Paso 11: el sistema vacía el nivel y deja de mostrar el campo "Nivel". Con un
      // grado sin niveles Axelor ni siquiera lo pinta, así que el locator no resuelve
      // a ningún nodo (que el nivel quede además VACÍO lo comprueban las dos
      // aserciones del resultado esperado, sobre el dato ya guardado).
      await expect(campo(page, 'nivel')).toBeHidden();

      // Paso 12: pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Resultado esperado: el sistema guarda el ciclo y vuelve al listado, donde la
      // fila "Mecatrónica Industrial" muestra el grado "Curso de especialización" y la
      // columna "Nivel" vacía.
      await expect(page).toHaveURL(/Ciclo-action\/list/);
      await esperarListadoCargado(page);
      const filaGuardada = page.getByRole('row').filter({ hasText: nombre });
      await expect(filaGuardada).toHaveCount(1);
      await expect(filaGuardada.getByRole('gridcell', { name: codigo, exact: true })).toBeVisible();
      await expect(
        filaGuardada.getByRole('gridcell', { name: 'Curso de especialización', exact: true }),
      ).toBeVisible();
      // La celda del nivel existe pero se pinta sin texto: es lo que significa aquí
      // "columna Nivel vacía" (no se busca por rol porque una celda sin nombre
      // accesible no se distingue de las otras vacías).
      await expect(filaGuardada.getByTestId('column:nivel')).toHaveText('');

      // Resultado esperado: al pulsar de nuevo la fila "Mecatrónica Industrial", el
      // formulario muestra el grado "Curso de especialización" y no muestra el campo
      // "Nivel".
      await abrirFichaDelCiclo(page, nombre);
      await expect(combo(page, 'grado')).toHaveValue('Curso de especialización');
      await expect(campo(page, 'nivel')).toBeHidden();
    } finally {
      // Teardown: borra el ciclo creado aunque una aserción haya fallado. El reload
      // descarta cualquier formulario a medio editar que bloquearía la navegación.
      await volverAlListadoDeCiclos(page);
      await borrarCiclosQueContengan(page, MARCA);

      await logout(page);
    }
  });
});
