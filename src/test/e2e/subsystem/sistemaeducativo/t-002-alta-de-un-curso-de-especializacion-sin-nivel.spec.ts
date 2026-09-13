import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-002 — Alta de un curso de especialización sin nivel
// origen: ESC-002  |  verifica: U-ciclos-001, V-Ciclo-002
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-002-alta-de-un-curso-de-especializacion-sin-nivel.desc.md

// Marca propia de este test: todo lo que crea lleva esta cadena en el nombre, así
// la pre-limpieza (§4.3 del contrato) puede reconocer y borrar lo que dejara
// pegado un run anterior que abortara antes del teardown.
const MARCA = 'e2e-t002';

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
 * Elige un valor en un campo many-to-one de Axelor. El desplegable solo filtra con
 * pulsaciones reales (un `fill` cambia el valor sin disparar la búsqueda), de ahí
 * el `pressSequentially`.
 */
async function elegirEnCombo(page: Page, nombreCampo: string, valor: string): Promise<void> {
  const control = combo(page, nombreCampo);
  await control.click();
  await control.pressSequentially(valor);
  await page.getByRole('option', { name: valor, exact: true }).click();
  await expect(control).toHaveValue(valor);
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
  test('Alta de un curso de especialización sin nivel', async ({ page }) => {
    // Nombre y código únicos por ejecución: la BD es compartida y no se resetea.
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigo = `CEIABD-${sufijo}`;
    const nombre = `Inteligencia Artificial y Big Data (especialización) ${sufijo}`;

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

      // Paso 3: rellena el campo "Código" con "CEIABD" y el campo "Nombre" con
      // "Inteligencia Artificial y Big Data (especialización)".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigo);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombre);

      // Paso 4: elige la familia profesional "Informática y Comunicaciones".
      await elegirEnCombo(page, 'familiaProfesional', 'Informática y Comunicaciones');

      // Paso 5: elige el grado "Curso de especialización".
      await elegirEnCombo(page, 'grado', 'Curso de especialización');

      // Paso 6: el sistema NO muestra el campo "Nivel" — con un grado sin niveles
      // Axelor ni siquiera lo pinta, así que el locator no resuelve a ningún nodo.
      await expect(campo(page, 'nivel')).toBeHidden();

      // Paso 7: pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Resultado esperado: el sistema guarda el ciclo y vuelve al listado de ciclos.
      await expect(page).toHaveURL(/Ciclo-action\/list/);
      await esperarListadoCargado(page);

      // Resultado esperado: la fila aparece con el grado "Curso de especialización"
      // y la columna "Nivel" vacía.
      const fila = page.getByRole('row').filter({ hasText: nombre });
      await expect(fila).toHaveCount(1);
      await expect(fila.getByRole('gridcell', { name: codigo, exact: true })).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: 'Curso de especialización', exact: true })).toBeVisible();
      // La celda del nivel existe pero se pinta sin texto: es lo que significa aquí
      // "columna Nivel vacía" (no se busca por rol porque una celda sin nombre
      // accesible no se distingue de las otras vacías).
      await expect(fila.getByTestId('column:nivel')).toHaveText('');
    } finally {
      // Teardown: borra el ciclo creado aunque una aserción haya fallado. El reload
      // descarta cualquier formulario a medio editar que bloquearía la navegación.
      await page.goto('/#/');
      await page.reload();
      await abrirListadoDeCiclos(page);
      await borrarCiclosQueContengan(page, MARCA);

      await logout(page);
    }
  });
});
