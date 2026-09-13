import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-003 — Un ciclo formativo sin nivel no se puede guardar
// origen: ESC-003  |  verifica: V-Ciclo-001, U-ciclos-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-003-un-ciclo-formativo-sin-nivel-no-se-puede-guardar.desc.md

// Marca propia de este test: todo lo que crea lleva esta cadena en el nombre, así
// la pre-limpieza (§4.3 del contrato) puede reconocer y borrar lo que dejara
// pegado un run anterior que abortara antes del teardown.
const MARCA = 'e2e-t003';

// Los campos `code`/`name` del modelo Ciclo todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;
const BTN_CONFIRMAR_BORRADO = /^(Delete|Borrar|Eliminar)$/;

// Mensaje de V-Ciclo-001, literal de CicloServiceImpl.validateCoherenciaGradoNivel.
const ERROR_NIVEL_OBLIGATORIO = 'El nivel es obligatorio para el grado indicado';

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
 * Deja la app en el listado de ciclos partiendo de cualquier sitio. El reload
 * descarta el formulario a medio editar, que si no bloquearía la navegación.
 */
async function volverAlListadoDeCiclos(page: Page): Promise<void> {
  await page.goto('/#/');
  await page.reload();
  await abrirListadoDeCiclos(page);
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
  test('Un ciclo formativo sin nivel no se puede guardar', async ({ page }) => {
    // Nombre y código únicos por ejecución: la BD es compartida y no se resetea.
    const sufijo = `${MARCA}-${Date.now()}`;
    const codigo = `TSAF-${sufijo}`;
    const nombre = `Automatización y Robótica Industrial ${sufijo}`;

    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 2: abre el menú "Sistema educativo" → "Ciclos".
      await abrirListadoDeCiclos(page);

      // Pre-limpieza defensiva (§4.3 del contrato de generación): este test no debe
      // llegar a guardar nada, pero si una regresión hiciera que el alta pasara, el
      // ciclo quedaría en la BD; así un run envenenado no arrastra al siguiente.
      await borrarCiclosQueContengan(page, MARCA);

      // Paso 2 (cont.): pulsa "Añadir un nuevo ciclo".
      await page.getByRole('button', { name: 'Añadir un nuevo ciclo' }).click();

      // Paso 3: rellena el campo "Código" con "TSAF" y el campo "Nombre" con
      // "Automatización y Robótica Industrial".
      await page.getByRole('textbox', { name: CAMPO_CODIGO }).fill(codigo);
      await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombre);

      // Paso 4: elige la familia profesional "Electricidad y Electrónica".
      await elegirEnCombo(page, 'familiaProfesional', 'Electricidad y Electrónica');

      // Paso 5: elige el grado "Ciclo formativo"...
      await elegirEnCombo(page, 'grado', 'Ciclo formativo');

      // ...y deja el campo "Nivel" vacío: con este grado el campo sí se muestra
      // (es lo que hace que el error de validación tenga sentido), pero no se toca.
      await expect(combo(page, 'nivel')).toBeVisible();
      await expect(combo(page, 'nivel')).toHaveValue('');

      // Paso 6: pulsa "Guardar".
      await page.getByRole('button', { name: 'Guardar', exact: true }).click();

      // Resultado esperado: el sistema muestra el mensaje de error. Axelor lo pinta
      // en un diálogo "Validation Error" con una entrada por mensaje de negocio,
      // prefijada con el campo al que se ancla ("nivel:").
      const dialogoError = page.getByRole('dialog');
      await expect(dialogoError).toContainText(ERROR_NIVEL_OBLIGATORIO);

      // Resultado esperado: no guarda — al fallar la validación la app se queda en el
      // formulario y NO vuelve al listado (que es lo que hace un alta correcta).
      await page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ }).click();
      await expect(page).toHaveURL(/Ciclo-action\/edit/);

      // Resultado esperado: al abrir de nuevo "Sistema educativo" → "Ciclos", el
      // listado no muestra ninguna fila del ciclo que se intentó dar de alta.
      await volverAlListadoDeCiclos(page);
      await expect(filasDeDatos(page).filter({ hasText: nombre })).toHaveCount(0);
      await expect(page.getByRole('row').filter({ hasText: nombre })).toHaveCount(0);
    } finally {
      // Teardown: el test no debería haber creado nada, pero si el alta llegara a
      // pasar (regresión) hay que borrarla para no dejar la BD sucia.
      await volverAlListadoDeCiclos(page);
      await borrarCiclosQueContengan(page, MARCA);

      await logout(page);
    }
  });
});
