import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-010 — Un grado empieza sin niveles y pasa a exigirlos en cuanto se le crea uno
// origen: ESC-008  |  verifica: U-ciclos-001, U-ciclos-002, U-ciclos-005, V-Ciclo-001, V-Ciclo-002, V-Nivel-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-010-un-grado-empieza-sin-niveles-y-pasa-a-exigirlos-en-cuanto-se-le-crea-uno.desc.md

// Marca propia de este test: todo lo que crea lleva esta cadena en el nombre, así
// la pre-limpieza (§4.3 del contrato) puede reconocer y borrar lo que dejara
// pegado un run anterior que abortara antes del teardown.
const MARCA = 'e2e-t010';

// Los campos `code`/`name` de los modelos todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;
const BTN_ACEPTAR_ERROR = /^(OK|Aceptar)$/;

// Mensaje de V-Ciclo-001, literal de CicloServiceImpl.
const ERROR_NIVEL_OBLIGATORIO = 'El nivel es obligatorio para el grado indicado';

/** Los dos grados que cargan los datos iniciales y que el selector debe seguir ofreciendo. */
const GRADOS_DEL_SEED = ['Ciclo formativo', 'Curso de especialización'];

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
 * Borra el ciclo, el nivel y el grado de este test. El ORDEN es obligado por las
 * claves ajenas: el ciclo apunta al grado, y el nivel también, así que hay que ir de
 * la hoja a la raíz. Sirve de pre-limpieza (restos de un run que abortara) y de
 * teardown.
 */
async function limpiarRestos(page: Page): Promise<void> {
  await borrarFilasQueContengan(page, MENU_CICLOS, BTN_NUEVO_CICLO, MARCA);
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

/** Abre la ficha de una fila del listado ya cargado pulsándola (Axelor navega a `.../edit/<id>`). */
async function abrirFicha(page: Page, nombre: string, urlEdicion: RegExp): Promise<void> {
  await filasDeDatos(page).filter({ hasText: nombre }).first().click();
  await expect(page).toHaveURL(urlEdicion);
  await expect(page.getByRole('textbox', { name: CAMPO_NOMBRE })).toHaveValue(nombre);
}

test.describe('Sistema educativo — Ciclos', () => {
  test('Un grado empieza sin niveles y pasa a exigirlos en cuanto se le crea uno', async ({
    page,
  }) => {
    // Nombres y códigos únicos por ejecución: la BD es compartida y no se resetea.
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigoGrado = `F-${sufijo}`;
    const nombreGrado = `Certificado profesional ${sufijo}`;
    const codigoNivel = `CP1-${sufijo}`;
    const nombreNivel = `Certificado profesional de nivel 1 ${sufijo}`;
    const codigoCiclo = `CPINF-${sufijo}`;
    const nombreCiclo = `Certificado profesional de informática ${sufijo}`;

    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva: restos de un run anterior de ESTE test que abortara
      // antes de su teardown (§4.3 del contrato de generación).
      await limpiarRestos(page);

      // Paso 2: abre el menú "Sistema educativo" → "Grados", pulsa "Nuevo Grado",
      // rellena "Código" con "F" y "Nombre" con "Certificado profesional", y pulsa
      // "Guardar".
      await abrirListado(page, MENU_GRADOS, BTN_NUEVO_GRADO);
      await page.getByRole('button', { name: BTN_NUEVO_GRADO }).click();
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoGrado);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreGrado);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 3: el sistema guarda el grado y vuelve al listado, donde aparece la fila
      // "Certificado profesional" con el código "F".
      await expect(page).toHaveURL(/Grado-action\/list/);
      const filaGrado = page.getByRole('row').filter({ hasText: nombreGrado });
      await expect(filaGrado).toHaveCount(1);
      await expect(filaGrado.getByRole('gridcell', { name: codigoGrado, exact: true })).toBeVisible();

      // Paso 4: abre el menú "Sistema educativo" → "Ciclos", pulsa "Añadir un nuevo
      // ciclo", rellena "Código" con "CPINF" y "Nombre" con "Certificado profesional
      // de informática", y elige la familia profesional "Informática y Comunicaciones".
      await abrirListado(page, MENU_CICLOS, BTN_NUEVO_CICLO);
      await page.getByRole('button', { name: BTN_NUEVO_CICLO }).click();
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoCiclo);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreCiclo);
      await elegirEnCombo(page, 'familiaProfesional', 'Informática y Comunicaciones');

      // Paso 5: abre el selector del campo "Grado".
      await combo(page, 'grado').click();
      await expect(opcionesDelSelector(page).first()).toBeVisible();

      // Paso 6: el selector ofrece, además de "Ciclo formativo" y "Curso de
      // especialización", el grado "Certificado profesional" recién creado — aunque
      // todavía no tenga ningún nivel colgando.
      for (const grado of [...GRADOS_DEL_SEED, nombreGrado]) {
        await expect(
          page.getByRole('listbox').getByRole('option', { name: grado, exact: true }),
        ).toBeVisible();
      }

      // Paso 7: elige el grado "Certificado profesional". El desplegable ya está
      // abierto, así que se pulsa su opción directamente (volver a teclear en el
      // control lo cerraría).
      await page.getByRole('option', { name: nombreGrado, exact: true }).click();
      await expect(combo(page, 'grado')).toHaveValue(nombreGrado);

      // Paso 8: el sistema NO muestra el campo "Nivel" — con un grado sin niveles
      // Axelor ni siquiera lo pinta, así que el locator no resuelve a ningún nodo.
      await expect(campo(page, 'nivel')).toBeHidden();

      // Paso 9: pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 10: el sistema guarda el ciclo y vuelve al listado, donde el ciclo
      // aparece con el grado "Certificado profesional" y la columna "Nivel" vacía.
      await expect(page).toHaveURL(/Ciclo-action\/list/);
      const filaCiclo = page.getByRole('row').filter({ hasText: nombreCiclo });
      await expect(filaCiclo).toHaveCount(1);
      await expect(filaCiclo.getByRole('gridcell', { name: codigoCiclo, exact: true })).toBeVisible();
      await expect(filaCiclo.getByRole('gridcell', { name: nombreGrado, exact: true })).toBeVisible();
      // La celda del nivel existe pero se pinta sin texto: es lo que significa aquí
      // "columna Nivel vacía" (no se busca por rol porque una celda sin nombre
      // accesible no se distingue de las otras vacías).
      await expect(filaCiclo.getByTestId('column:nivel')).toHaveText('');

      // Paso 11: abre el menú "Sistema educativo" → "Niveles", pulsa "Añadir un nuevo
      // nivel", rellena "Código" con "CP1" y "Nombre" con "Certificado profesional de
      // nivel 1", elige el grado "Certificado profesional" y pulsa "Guardar".
      await abrirListado(page, MENU_NIVELES, BTN_NUEVO_NIVEL);
      await page.getByRole('button', { name: BTN_NUEVO_NIVEL }).click();
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoNivel);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreNivel);
      await elegirEnCombo(page, 'grado', nombreGrado);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 12: el sistema guarda el nivel y vuelve al listado, donde "Certificado
      // profesional de nivel 1" aparece con el grado "Certificado profesional".
      await expect(page).toHaveURL(/Nivel-action\/list/);
      const filaNivel = page.getByRole('row').filter({ hasText: nombreNivel });
      await expect(filaNivel).toHaveCount(1);
      await expect(filaNivel.getByRole('gridcell', { name: nombreGrado, exact: true })).toBeVisible();

      // Paso 13: abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila
      // "Certificado profesional de informática".
      await abrirListado(page, MENU_CICLOS, BTN_NUEVO_CICLO);
      await abrirFicha(page, nombreCiclo, /Ciclo-action\/edit/);

      // Paso 14: el sistema muestra ahora el campo "Nivel", vacío, y lo señala como
      // obligatorio. El grado ya admite nivel (CC-Grado-001) porque se le colgó CP1, y
      // Axelor marca lo obligatorio con aria-required="true".
      await expect(combo(page, 'grado')).toHaveValue(nombreGrado);
      await expect(campo(page, 'nivel')).toBeVisible();
      await expect(campo(page, 'nivel').getByTestId('label')).toHaveText('Nivel');
      await expect(combo(page, 'nivel')).toHaveValue('');
      await expect(campo(page, 'nivel').getByTestId('select')).toHaveAttribute(
        'aria-required',
        'true',
      );

      // Paso 15: pulsa "Guardar" sin elegir ningún nivel.
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Resultado esperado: el sistema muestra el mensaje "El nivel es obligatorio
      // para el grado indicado". Axelor lo pinta en un diálogo de error de validación.
      await expect(page.getByRole('dialog')).toContainText(ERROR_NIVEL_OBLIGATORIO);

      // Resultado esperado (no guarda): al fallar la validación la app se queda en el
      // formulario y NO vuelve al listado, que es lo que hace un guardado correcto.
      await page.getByRole('dialog').getByRole('button', { name: BTN_ACEPTAR_ERROR }).click();
      await expect(page).toHaveURL(/Ciclo-action\/edit/);

      // Resultado esperado (no guarda): al volver al listado, el ciclo sigue con la
      // columna "Nivel" vacía — el guardado rechazado no dejó rastro.
      await volverAlInicio(page);
      await abrirListado(page, MENU_CICLOS, BTN_NUEVO_CICLO);
      const filaCicloTrasError = page.getByRole('row').filter({ hasText: nombreCiclo });
      await expect(filaCicloTrasError).toHaveCount(1);
      await expect(filaCicloTrasError.getByTestId('column:nivel')).toHaveText('');
    } finally {
      // Teardown: borra el ciclo, el nivel y el grado creados aunque una aserción haya
      // fallado. El reload descarta cualquier formulario a medio editar que si no
      // bloquearía la navegación.
      await volverAlInicio(page);
      await limpiarRestos(page);

      await logout(page);
    }
  });
});
