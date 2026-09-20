import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-013 — Al consultar un ciclo, el nivel solo se ve si el ciclo lo lleva
// origen: ESC-010  |  verifica: U-consulta-ciclo-001, U-ciclos-001, V-Ciclo-002
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-013-al-consultar-un-ciclo-el-nivel-solo-se-ve-si-el-ciclo-lo-lleva.desc.md

// Marca propia de este test: todo lo que crea (el ciclo y el curso) lleva esta
// cadena en el nombre y en el código, así la pre-limpieza (§4.3 del contrato) puede
// reconocer y borrar lo que dejara pegado un run anterior que abortara antes del
// teardown.
const MARCA = 'e2e-t013';

// Los campos `code`/`name` de los modelos Ciclo y Curso todavía no están traducidos,
// así que la UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre".
// El regex acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;

/** Entradas del menú lateral «Sistema educativo» que recorre este test. */
const MENU_SISTEMA_EDUCATIVO = 'item:sistemaEducativo-menuitem';
const MENU_CICLOS = 'item:sistemaEducativo-ciclos-menuitem';
const MENU_CURSOS = 'item:sistemaEducativo-cursos-menuitem';

const BTN_NUEVO_CICLO = 'Añadir un nuevo ciclo';
const BTN_NUEVO_CURSO = 'Nuevo curso';

const FAMILIA_INFORMATICA = 'Informática y Comunicaciones';
const GRADO_ESPECIALIZACION = 'Curso de especialización';
const NIVEL_SUPERIOR = 'Superior';
const LEY_LOFP =
  'Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional';

// Ciclo del seed al que se cambia el curso en el último paso: es de grado «Ciclo
// formativo» y SÍ lleva nivel, que es justo el contraste que exige el resultado
// esperado. Se elige este y no ASIR porque los tests hermanos que editan datos del
// seed tocan «Administración de Sistemas Informáticos en Red», no este.
const CICLO_CON_NIVEL = 'Guía, Información y Asistencia Turística';

/**
 * Filas de datos de un grid (excluye la cabecera y la fila de búsqueda). El grid las
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
    await page.getByTestId(MENU_SISTEMA_EDUCATIVO).click();
  }
  await entrada.click();
}

/** Espera a que un listado esté pintado con sus filas, por su botón de alta. */
async function esperarListadoCargado(page: Page, botonNuevo: string): Promise<void> {
  await expect(page.getByRole('button', { name: botonNuevo })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/** Abre el menú "Sistema educativo" → "Ciclos" y espera al listado. */
async function abrirListadoDeCiclos(page: Page): Promise<void> {
  await abrirMenu(page, MENU_CICLOS);
  await expect(page).toHaveURL(/Ciclo-action\/list/);
  await esperarListadoCargado(page, BTN_NUEVO_CICLO);
}

/** Abre el menú "Sistema educativo" → "Cursos" y espera al listado. */
async function abrirListadoDeCursos(page: Page): Promise<void> {
  await abrirMenu(page, MENU_CURSOS);
  await expect(page).toHaveURL(/Curso-action\/list/);
  await esperarListadoCargado(page, BTN_NUEVO_CURSO);
}

/**
 * Deja la app fuera de cualquier formulario y en el listado que se le pida. La
 * recarga completa es lo que descarta un formulario a medio editar: con solo
 * cambiar el hash, Axelor conserva la pestaña sucia y luego bloquea la navegación
 * con el aviso «Current changes will be lost».
 */
async function volverAlListado(
  page: Page,
  abrirListado: (page: Page) => Promise<void>,
): Promise<void> {
  await page.goto('/#/');
  await page.reload();
  await abrirListado(page);
}

/**
 * Borra, desde un listado ya cargado, todas las filas cuyo texto contenga `texto`.
 * Pulsar la fila abre su formulario (los grids son `canEditOnClick`), y el borrado
 * se confirma en el diálogo de Axelor. LIMIT 10: cota dura para no quedarse en bucle
 * si un borrado no se aplica.
 */
async function borrarFilasQueContengan(
  page: Page,
  texto: string,
  botonNuevo: string,
): Promise<void> {
  for (let i = 0; i < 10; i++) {
    await esperarListadoCargado(page, botonNuevo);
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
 * Abre la consulta en solo lectura del ciclo ya elegido en el curso: es el icono
 * «ver ficha» del propio campo many-to-one (`icon:view`), que abre el formulario de
 * referencia del ciclo en un modal. Devuelve el diálogo, al que hay que acotar
 * TODOS los locators: el formulario del curso que queda detrás tiene campos con los
 * mismos `data-testid` (`field:name`, `field:ciclo`…).
 */
async function abrirConsultaDelCiclo(page: Page): Promise<Locator> {
  await campo(page, 'ciclo').getByTestId('icon:view').click();
  const consulta = page.getByRole('dialog');
  await expect(consulta.getByRole('heading', { name: 'Ciclo' })).toBeVisible();
  return consulta;
}

/** Cierra la consulta del ciclo con su botón "Salir" y espera a que desaparezca. */
async function cerrarConsulta(page: Page, consulta: Locator): Promise<void> {
  await consulta.getByRole('button', { name: 'Salir', exact: true }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
}

test.describe('Sistema educativo — Consulta de un ciclo desde un curso', () => {
  test('Al consultar un ciclo, el nivel solo se ve si el ciclo lo lleva', async ({ page }) => {
    // Nombres y códigos únicos por ejecución: la BD es compartida y no se resetea.
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigoCiclo = `CEIA-${sufijo}`;
    const nombreCiclo = `Inteligencia Artificial y Big Data ${sufijo}`;
    const codigoCurso = `CEIA1-${sufijo}`;
    const nombreCurso = `1º CEIA ${sufijo}`;

    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva: restos de un run anterior de ESTE test que abortara
      // antes de su teardown (§4.3 del contrato de generación). El curso va primero
      // porque referencia al ciclo.
      await abrirListadoDeCursos(page);
      await borrarFilasQueContengan(page, MARCA, BTN_NUEVO_CURSO);
      await abrirListadoDeCiclos(page);
      await borrarFilasQueContengan(page, MARCA, BTN_NUEVO_CICLO);

      // Paso 2: abre el menú "Sistema educativo" → "Ciclos", pulsa "Añadir un nuevo
      // ciclo", rellena "Código" con "CEIA" y "Nombre" con "Inteligencia Artificial y
      // Big Data", y elige la familia profesional "Informática y Comunicaciones".
      await page.getByRole('button', { name: BTN_NUEVO_CICLO }).click();
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoCiclo);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreCiclo);
      await elegirEnCombo(page, 'familiaProfesional', FAMILIA_INFORMATICA);

      // Paso 3: elige el grado "Curso de especialización" y pulsa "Guardar".
      await elegirEnCombo(page, 'grado', GRADO_ESPECIALIZACION);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 4: el sistema guarda el ciclo y vuelve al listado, donde "Inteligencia
      // Artificial y Big Data" aparece con el grado "Curso de especialización".
      await expect(page).toHaveURL(/Ciclo-action\/list/);
      await esperarListadoCargado(page, BTN_NUEVO_CICLO);
      const filaCiclo = filasDeDatos(page).filter({ hasText: nombreCiclo });
      await expect(filaCiclo).toHaveCount(1);
      await expect(filaCiclo.getByTestId('column:grado')).toHaveText(GRADO_ESPECIALIZACION);

      // Paso 5: abre el menú "Sistema educativo" → "Cursos" y pulsa "Nuevo curso".
      await abrirListadoDeCursos(page);
      await page.getByRole('button', { name: BTN_NUEVO_CURSO }).click();

      // Paso 6: rellena "Código" con "CEIA1" y "Nombre" con "1º CEIA", elige el ciclo
      // "Inteligencia Artificial y Big Data" y la ley educativa "Ley Orgánica 3/2022,
      // de 31 de marzo, de Ordenación e Integración de la Formación Profesional", y
      // pulsa "Guardar".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigoCurso);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombreCurso);
      await elegirEnCombo(page, 'ciclo', nombreCiclo);
      await elegirEnCombo(page, 'leyEducativa', LEY_LOFP);
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Paso 7: el sistema guarda el curso y vuelve al listado, donde "1º CEIA"
      // aparece con el ciclo "Inteligencia Artificial y Big Data". El listado de
      // cursos no tiene columna de ciclo, así que el ciclo del curso se comprueba al
      // abrir su ficha, en el paso siguiente.
      await expect(page).toHaveURL(/Curso-action\/list/);
      await esperarListadoCargado(page, BTN_NUEVO_CURSO);
      const filaCurso = filasDeDatos(page).filter({ hasText: nombreCurso });
      await expect(filaCurso).toHaveCount(1);
      await expect(filaCurso.getByTestId('column:code')).toHaveText(codigoCurso);

      // Paso 8: pulsa la fila "1º CEIA" y después pulsa sobre el ciclo "Inteligencia
      // Artificial y Big Data" ya elegido en el curso.
      await filaCurso.click();
      await expect(page).toHaveURL(/Curso-action\/edit/);
      await expect(combo(page, 'ciclo')).toHaveValue(nombreCiclo);
      const consultaSinNivel = await abrirConsultaDelCiclo(page);

      // Paso 9: el sistema abre la consulta del ciclo en solo lectura, muestra el
      // nombre, la familia profesional y el grado "Curso de especialización", y no
      // muestra el campo "Nivel".
      const nombreConsultado = consultaSinNivel.getByRole('textbox', { name: CAMPO_NOMBRE });
      const familiaConsultada = consultaSinNivel.getByRole('textbox', {
        name: 'Familia profesional',
      });
      const gradoConsultado = consultaSinNivel.getByRole('textbox', { name: 'Grado' });
      await expect(nombreConsultado).toHaveValue(nombreCiclo);
      await expect(familiaConsultada).toHaveValue(FAMILIA_INFORMATICA);
      await expect(gradoConsultado).toHaveValue(GRADO_ESPECIALIZACION);
      // Solo lectura: los tres campos se pintan deshabilitados, no editables.
      await expect(nombreConsultado).toBeDisabled();
      await expect(familiaConsultada).toBeDisabled();
      await expect(gradoConsultado).toBeDisabled();
      // Con un grado sin niveles Axelor ni siquiera pinta el campo "Nivel", así que
      // el locator no resuelve a ningún nodo.
      await expect(consultaSinNivel.getByTestId('field:nivel')).toBeHidden();

      // Paso 10: cierra la consulta, vuelve al formulario del curso "1º CEIA", cambia
      // el ciclo del curso a "Guía, Información y Asistencia Turística" y pulsa sobre
      // ese ciclo ya elegido.
      await cerrarConsulta(page, consultaSinNivel);
      await expect(combo(page, 'ciclo')).toHaveValue(nombreCiclo);
      await cambiarCombo(page, 'ciclo', CICLO_CON_NIVEL);
      const consultaConNivel = await abrirConsultaDelCiclo(page);

      // Resultado esperado: el sistema abre la consulta del ciclo "Guía, Información y
      // Asistencia Turística" y muestra además el campo "Nivel" con el valor
      // "Superior".
      await expect(consultaConNivel.getByRole('textbox', { name: CAMPO_NOMBRE })).toHaveValue(
        CICLO_CON_NIVEL,
      );
      await expect(consultaConNivel.getByTestId('field:nivel')).toBeVisible();
      await expect(consultaConNivel.getByRole('textbox', { name: 'Nivel' })).toHaveValue(
        NIVEL_SUPERIOR,
      );

      await cerrarConsulta(page, consultaConNivel);
    } finally {
      // Teardown: borra el curso y el ciclo creados aunque una aserción haya fallado.
      // El curso va primero porque referencia al ciclo, que no se podría borrar
      // mientras siga colgando de él. El cambio de ciclo del último paso se queda sin
      // guardar a propósito (la descripción no lo guarda): lo descarta la recarga
      // completa que hace `volverAlListado`.
      await volverAlListado(page, abrirListadoDeCursos);
      await borrarFilasQueContengan(page, MARCA, BTN_NUEVO_CURSO);
      await abrirListadoDeCiclos(page);
      await borrarFilasQueContengan(page, MARCA, BTN_NUEVO_CICLO);

      await logout(page);
    }
  });
});
