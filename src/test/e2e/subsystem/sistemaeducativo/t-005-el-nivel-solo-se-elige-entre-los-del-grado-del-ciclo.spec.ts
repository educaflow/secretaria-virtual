import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-005 — El nivel solo se elige entre los del grado del ciclo
// origen: ESC-005  |  verifica: U-ciclos-004, U-niveles-002, U-niveles-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-005-el-nivel-solo-se-elige-entre-los-del-grado-del-ciclo.desc.md

// Marca propia de este test: todo lo que crea lleva esta cadena en el nombre, así
// la pre-limpieza (§4.3 del contrato) puede reconocer y borrar lo que dejara
// pegado un run anterior que abortara antes del teardown.
const MARCA = 'e2e-t005';

// Los campos `code`/`name` de los modelos todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;

/**
 * Los tres niveles que los datos iniciales cuelgan del grado «Ciclo formativo»,
 * en el orden en que los pinta el selector (`orderBy="name"`). Es la lista que el
 * resultado esperado exige ver EXACTAMENTE.
 */
const NIVELES_DE_CICLO_FORMATIVO = ['Básico', 'Medio', 'Superior'];

/** Entradas del menú lateral «Sistema educativo» que recorre este test. */
const MENU_GRADOS = 'item:sistemaEducativo-grados-menuitem';
const MENU_NIVELES = 'item:sistemaEducativo-niveles-menuitem';
const MENU_CICLOS = 'item:sistemaEducativo-ciclos-menuitem';

const BTN_NUEVO_GRADO = 'Nuevo Grado';
const BTN_NUEVO_NIVEL = 'Añadir un nuevo nivel';
const BTN_NUEVO_CICLO = 'Añadir un nuevo ciclo';

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

/** Las opciones del desplegable abierto (solo hay un `listbox` a la vez). */
function opcionesDelSelector(page: Page) {
  return page.getByRole('listbox').getByRole('option');
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

/** Abre un listado desde el menú y espera a que esté pintado con sus filas. */
async function abrirListado(page: Page, itemMenu: string, botonNuevo: string): Promise<void> {
  await abrirMenu(page, itemMenu);
  await expect(page.getByRole('button', { name: botonNuevo })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Borra todas las filas de un listado cuyo texto contenga `texto`, abriéndolo desde
 * el menú. LIMIT 10: cota dura para no quedarse en bucle si un borrado no se aplica.
 */
async function borrarFilasQueContengan(
  page: Page,
  itemMenu: string,
  botonNuevo: string,
  texto: string,
): Promise<void> {
  for (let i = 0; i < 10; i++) {
    await abrirListado(page, itemMenu, botonNuevo);
    const filas = filasDeDatos(page).filter({ hasText: texto });
    if ((await filas.count()) === 0) {
      return;
    }
    await filas.first().click();
    await page.getByRole('button', { name: 'Borrar', exact: true }).click();
    await page.getByRole('dialog').getByRole('button', { name: BTN_CONFIRMAR_BORRADO }).click();
  }
  throw new Error(`No se pudo limpiar las filas que contienen "${texto}"`);
}

/**
 * Borra el nivel y el grado de este test. El ORDEN es obligado: el nivel apunta al
 * grado con una FK obligatoria, así que un grado con niveles colgando no se puede
 * borrar. Sirve de pre-limpieza (restos de un run que abortara) y de teardown.
 */
async function limpiarRestos(page: Page): Promise<void> {
  await borrarFilasQueContengan(page, MENU_NIVELES, BTN_NUEVO_NIVEL, MARCA);
  await borrarFilasQueContengan(page, MENU_GRADOS, BTN_NUEVO_GRADO, MARCA);
}

/**
 * Deja la app en la pantalla de inicio partiendo de cualquier sitio. El reload
 * descarta el formulario a medio editar, que si no bloquearía la navegación.
 */
async function volverAlInicio(page: Page): Promise<void> {
  await page.goto('/#/');
  await page.reload();
}

test.describe('Sistema educativo — Ciclos', () => {
  test('El nivel solo se elige entre los del grado del ciclo', async ({ page }) => {
    // Nombres y códigos únicos por ejecución: la BD es compartida y no se resetea.
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigoGrado = `G-${sufijo}`;
    const nombreGrado = `Certificado de profesionalidad ${sufijo}`;
    const codigoNivel = `CPR1-${sufijo}`;
    const nombreNivel = `Certificado de profesionalidad de nivel 1 ${sufijo}`;
    const codigoCiclo = `TSDAW-${sufijo}`;
    const nombreCiclo = `Desarrollo Web Avanzado ${sufijo}`;

    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva: restos de un run anterior de ESTE test que abortara
      // antes de su teardown (§4.3 del contrato de generación).
      await limpiarRestos(page);

      // Paso 2: abre el menú "Sistema educativo" → "Grados" y pulsa "Nuevo Grado".
      await abrirListado(page, MENU_GRADOS, BTN_NUEVO_GRADO);
      await page.getByRole('button', { name: BTN_NUEVO_GRADO }).click();

      // Paso 3: rellena el campo "Código" con "G" y el campo "Nombre" con
      // "Certificado de profesionalidad", y pulsa "Guardar".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoGrado);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreGrado);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();
      await expect(page).toHaveURL(/Grado-action\/list/);
      await expect(filasDeDatos(page).filter({ hasText: nombreGrado })).toHaveCount(1);

      // Paso 4: abre el menú "Sistema educativo" → "Niveles" y pulsa "Añadir un
      // nuevo nivel".
      await abrirListado(page, MENU_NIVELES, BTN_NUEVO_NIVEL);
      await page.getByRole('button', { name: BTN_NUEVO_NIVEL }).click();

      // Paso 5: rellena el campo "Código" con "CPR1" y el campo "Nombre" con
      // "Certificado de profesionalidad de nivel 1", elige el grado "Certificado de
      // profesionalidad" y pulsa "Guardar".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoNivel);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreNivel);
      await elegirEnCombo(page, 'grado', nombreGrado);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();
      await expect(page).toHaveURL(/Nivel-action\/list/);
      await expect(filasDeDatos(page).filter({ hasText: nombreNivel })).toHaveCount(1);

      // Paso 6: abre el menú "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo
      // ciclo".
      await abrirListado(page, MENU_CICLOS, BTN_NUEVO_CICLO);
      await page.getByRole('button', { name: BTN_NUEVO_CICLO }).click();

      // Paso 7: rellena el campo "Código" con "TSDAW" y el campo "Nombre" con
      // "Desarrollo Web Avanzado", y elige la familia profesional "Informática y
      // Comunicaciones".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoCiclo);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreCiclo);
      await elegirEnCombo(page, 'familiaProfesional', 'Informática y Comunicaciones');

      // Paso 8: elige el grado "Ciclo formativo".
      await elegirEnCombo(page, 'grado', 'Ciclo formativo');

      // Paso 9: abre el selector del campo "Nivel" (el campo solo se pinta cuando el
      // grado elegido admite nivel).
      await expect(campo(page, 'nivel')).toBeVisible();
      await combo(page, 'nivel').click();
      await expect(opcionesDelSelector(page).first()).toBeVisible();

      // Resultado esperado: el selector ofrece exactamente "Básico", "Medio" y
      // "Superior". `toHaveText` con un array exige esa lista EXACTA: ni una
      // opción de más, ni de menos.
      await expect(opcionesDelSelector(page)).toHaveText(NIVELES_DE_CICLO_FORMATIVO);

      // Resultado esperado: el selector no ofrece "Certificado de profesionalidad de
      // nivel 1", que pertenece a otro grado. Se busca por el nombre SIN el sufijo
      // único para que la aserción falle también si se colara el nivel homónimo de
      // cualquier otro run.
      await expect(
        opcionesDelSelector(page).filter({ hasText: 'Certificado de profesionalidad' }),
      ).toHaveCount(0);
    } finally {
      // Teardown: borra el nivel y el grado creados aunque una aserción haya fallado.
      // El ciclo nunca llega a guardarse (el test termina con su formulario abierto),
      // así que no hay ciclo que borrar; el reload descarta ese formulario a medio
      // editar, que si no bloquearía la navegación.
      await volverAlInicio(page);
      await limpiarRestos(page);

      await logout(page);
    }
  });
});
