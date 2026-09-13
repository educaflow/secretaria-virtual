import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-006 — El nivel de un ciclo debe pertenecer al grado del ciclo
// origen: ESC-012  |  verifica: V-Ciclo-003, U-ciclos-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-006-el-nivel-de-un-ciclo-debe-pertenecer-al-grado-del-ciclo.desc.md

// Marca propia de este test: todo lo que crea lleva esta cadena en el nombre, así
// la pre-limpieza (§4.3 del contrato) puede reconocer y borrar lo que dejara
// pegado un run anterior que abortara antes del teardown.
const MARCA = 'e2e-t006';

// Los campos `code`/`name` de los modelos todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;
const BTN_ACEPTAR_ERROR = /^(OK|Aceptar)$/;

// Mensaje de V-Ciclo-003, literal de CicloServiceImpl.validateCoherenciaGradoNivel.
const ERROR_NIVEL_DE_OTRO_GRADO = 'El nivel indicado no pertenece al grado del ciclo';

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
 * Borra el ciclo, los niveles y los grados de este test. El ORDEN es obligado por
 * las claves ajenas: el ciclo apunta a un grado y a un nivel, y cada nivel apunta a
 * un grado, así que hay que ir de la hoja a la raíz. Sirve de pre-limpieza (restos
 * de un run que abortara) y de teardown.
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

/** Da de alta un grado desde el listado de grados, ya abierto, y espera su fila. */
async function crearGrado(page: Page, codigo: string, nombre: string): Promise<void> {
  await page.getByRole('button', { name: BTN_NUEVO_GRADO }).click();
  await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigo);
  await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombre);
  await page.getByRole('button', { name: 'Guardar', exact: true }).click();
  await expect(page).toHaveURL(/Grado-action\/list/);
  await expect(filasDeDatos(page).filter({ hasText: nombre })).toHaveCount(1);
}

/** Da de alta un nivel colgando de `nombreGrado`, desde el listado de niveles ya abierto. */
async function crearNivel(
  page: Page,
  codigo: string,
  nombre: string,
  nombreGrado: string,
): Promise<void> {
  await page.getByRole('button', { name: BTN_NUEVO_NIVEL }).click();
  await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigo);
  await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombre);
  await elegirEnCombo(page, 'grado', nombreGrado);
  await page.getByRole('button', { name: 'Guardar', exact: true }).click();
  await expect(page).toHaveURL(/Nivel-action\/list/);
  await expect(filasDeDatos(page).filter({ hasText: nombre })).toHaveCount(1);
}

/** Abre la ficha de una fila del listado ya cargado pulsándola (Axelor navega a `.../edit/<id>`). */
async function abrirFicha(page: Page, nombre: string, urlEdicion: RegExp): Promise<void> {
  await filasDeDatos(page).filter({ hasText: nombre }).first().click();
  await expect(page).toHaveURL(urlEdicion);
  await expect(page.getByRole('textbox', { name: CAMPO_NOMBRE })).toHaveValue(nombre);
}

test.describe('Sistema educativo — Ciclos', () => {
  test('El nivel de un ciclo debe pertenecer al grado del ciclo', async ({ page }) => {
    // Nombres y códigos únicos por ejecución: la BD es compartida y no se resetea.
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigoGradoAvanzado = `H-${sufijo}`;
    const nombreGradoAvanzado = `Certificado profesional avanzado ${sufijo}`;
    const codigoGradoExperto = `I-${sufijo}`;
    const nombreGradoExperto = `Certificado profesional experto ${sufijo}`;
    const codigoNivel1 = `CPA1-${sufijo}`;
    const nombreNivel1 = `Certificado profesional avanzado de nivel 1 ${sufijo}`;
    const codigoNivel2 = `CPA2-${sufijo}`;
    const nombreNivel2 = `Certificado profesional avanzado de nivel 2 ${sufijo}`;
    const codigoCiclo = `CPAINF-${sufijo}`;
    const nombreCiclo = `Certificado profesional avanzado de informática ${sufijo}`;
    // El "(LOFP)" va en medio, así que este nombre NO contiene al anterior: las
    // aserciones del resultado esperado pueden distinguir las dos filas por texto.
    const nombreCicloModificado = `Certificado profesional avanzado de informática (LOFP) ${sufijo}`;

    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva: restos de un run anterior de ESTE test que abortara
      // antes de su teardown (§4.3 del contrato de generación).
      await limpiarRestos(page);

      // Paso 2: abre el menú "Sistema educativo" → "Grados", pulsa "Nuevo Grado",
      // rellena "Código" con "H" y "Nombre" con "Certificado profesional avanzado",
      // y pulsa "Guardar".
      // Paso 3: el sistema guarda el grado y vuelve al listado, donde aparece la
      // fila "Certificado profesional avanzado" (lo comprueba `crearGrado`).
      await abrirListado(page, MENU_GRADOS, BTN_NUEVO_GRADO);
      await crearGrado(page, codigoGradoAvanzado, nombreGradoAvanzado);

      // Paso 4: pulsa "Nuevo Grado", rellena "Código" con "I" y "Nombre" con
      // "Certificado profesional experto", y pulsa "Guardar".
      await crearGrado(page, codigoGradoExperto, nombreGradoExperto);

      // Paso 5: abre "Sistema educativo" → "Niveles", pulsa "Añadir un nuevo nivel",
      // rellena "Código" con "CPA1" y "Nombre" con "Certificado profesional avanzado
      // de nivel 1", elige el grado "Certificado profesional avanzado" y "Guardar".
      await abrirListado(page, MENU_NIVELES, BTN_NUEVO_NIVEL);
      await crearNivel(page, codigoNivel1, nombreNivel1, nombreGradoAvanzado);

      // Paso 6: ídem con "CPA2" / "Certificado profesional avanzado de nivel 2",
      // también del grado "Certificado profesional avanzado".
      await crearNivel(page, codigoNivel2, nombreNivel2, nombreGradoAvanzado);

      // Paso 7: abre "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo ciclo".
      await abrirListado(page, MENU_CICLOS, BTN_NUEVO_CICLO);
      await page.getByRole('button', { name: BTN_NUEVO_CICLO }).click();

      // Paso 8: rellena "Código" con "CPAINF" y "Nombre" con "Certificado profesional
      // avanzado de informática", y elige la familia profesional "Informática y
      // Comunicaciones".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoCiclo);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreCiclo);
      await elegirEnCombo(page, 'familiaProfesional', 'Informática y Comunicaciones');

      // Paso 9: elige el grado "Certificado profesional avanzado".
      await elegirEnCombo(page, 'grado', nombreGradoAvanzado);

      // Paso 10: el sistema muestra el campo "Nivel" y lo señala como obligatorio.
      // El grado recién creado admite nivel porque ya se le colgaron CPA1 y CPA2
      // (CC-Grado-001), y Axelor marca lo obligatorio con aria-required="true".
      await expect(campo(page, 'nivel').getByTestId('label')).toHaveText('Nivel');
      await expect(combo(page, 'nivel')).toBeVisible();
      await expect(campo(page, 'nivel').getByTestId('select')).toHaveAttribute('aria-required', 'true');

      // Paso 11: elige el nivel "Certificado profesional avanzado de nivel 1" y pulsa
      // "Guardar".
      await elegirEnCombo(page, 'nivel', nombreNivel1);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 12: el sistema guarda el ciclo y vuelve al listado, donde el ciclo
      // aparece con el grado "Certificado profesional avanzado" y el nivel
      // "Certificado profesional avanzado de nivel 1".
      await expect(page).toHaveURL(/Ciclo-action\/list/);
      const filaCiclo = page.getByRole('row').filter({ hasText: nombreCiclo });
      await expect(filaCiclo).toHaveCount(1);
      await expect(filaCiclo.getByRole('gridcell', { name: codigoCiclo, exact: true })).toBeVisible();
      await expect(
        filaCiclo.getByRole('gridcell', { name: nombreGradoAvanzado, exact: true }),
      ).toBeVisible();
      await expect(filaCiclo.getByRole('gridcell', { name: nombreNivel1, exact: true })).toBeVisible();

      // Paso 13: abre "Sistema educativo" → "Niveles", pulsa la fila "Certificado
      // profesional avanzado de nivel 1", cambia el grado a "Certificado profesional
      // experto" y pulsa "Guardar".
      await abrirListado(page, MENU_NIVELES, BTN_NUEVO_NIVEL);
      await abrirFicha(page, nombreNivel1, /Nivel-action\/edit/);
      await cambiarCombo(page, 'grado', nombreGradoExperto);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 14: el sistema guarda el nivel y vuelve al listado, donde CPA1 aparece
      // con el grado "Certificado profesional experto" y CPA2 conserva el grado
      // "Certificado profesional avanzado".
      await expect(page).toHaveURL(/Nivel-action\/list/);
      const filaNivel1 = page.getByRole('row').filter({ hasText: nombreNivel1 });
      await expect(filaNivel1).toHaveCount(1);
      await expect(
        filaNivel1.getByRole('gridcell', { name: nombreGradoExperto, exact: true }),
      ).toBeVisible();
      const filaNivel2 = page.getByRole('row').filter({ hasText: nombreNivel2 });
      await expect(filaNivel2).toHaveCount(1);
      await expect(
        filaNivel2.getByRole('gridcell', { name: nombreGradoAvanzado, exact: true }),
      ).toBeVisible();

      // Paso 15: abre "Sistema educativo" → "Ciclos" y pulsa la fila del ciclo.
      await abrirListado(page, MENU_CICLOS, BTN_NUEVO_CICLO);
      await abrirFicha(page, nombreCiclo, /Ciclo-action\/edit/);

      // Paso 16: el formulario muestra el grado "Certificado profesional avanzado",
      // sigue mostrando el campo "Nivel" —porque ese grado conserva el nivel CPA2— y
      // en ese campo muestra el valor guardado CPA1, que ya es de otro grado.
      await expect(combo(page, 'grado')).toHaveValue(nombreGradoAvanzado);
      await expect(campo(page, 'nivel')).toBeVisible();
      await expect(campo(page, 'nivel').getByTestId('label')).toHaveText('Nivel');
      await expect(combo(page, 'nivel')).toHaveValue(nombreNivel1);

      // Paso 17: cambia el nombre a "…(LOFP)" y pulsa "Guardar".
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreCicloModificado);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Resultado esperado: el sistema no guarda el ciclo y muestra el mensaje "El
      // nivel indicado no pertenece al grado del ciclo". Axelor lo pinta en un
      // diálogo de error de validación, con una entrada por mensaje de negocio.
      await expect(page.getByRole('dialog')).toContainText(ERROR_NIVEL_DE_OTRO_GRADO);

      // Resultado esperado (no guarda): al fallar la validación la app se queda en el
      // formulario y NO vuelve al listado, que es lo que hace un guardado correcto.
      await page.getByRole('dialog').getByRole('button', { name: BTN_ACEPTAR_ERROR }).click();
      await expect(page).toHaveURL(/Ciclo-action\/edit/);

      // Resultado esperado: al abrir de nuevo "Sistema educativo" → "Ciclos", el
      // listado sigue mostrando la fila con el nombre original, sin el cambio que no
      // se pudo guardar.
      await volverAlInicio(page);
      await abrirListado(page, MENU_CICLOS, BTN_NUEVO_CICLO);
      await expect(filasDeDatos(page).filter({ hasText: nombreCiclo })).toHaveCount(1);
      await expect(filasDeDatos(page).filter({ hasText: nombreCicloModificado })).toHaveCount(0);
    } finally {
      // Teardown: borra el ciclo, los niveles y los grados creados aunque una
      // aserción haya fallado. El reload descarta cualquier formulario a medio editar
      // que si no bloquearía la navegación.
      await volverAlInicio(page);
      await limpiarRestos(page);

      await logout(page);
    }
  });
});
