import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-007 — Editar un ciclo sin tocar el grado ni el nivel
// origen: ESC-013  |  verifica: V-Ciclo-001, V-Ciclo-003, U-ciclos-001
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-007-editar-un-ciclo-sin-tocar-el-grado-ni-el-nivel.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test NO crea nada: edita un
 * ciclo que ya viene en los datos iniciales, así que no puede aislarse con un
 * sufijo `Date.now()` ni borrar lo que toca. Se hace idempotente de la otra forma:
 *   - identifica el ciclo por su CÓDIGO (`ASIR`), que no cambia, nunca por el
 *     nombre, que es justo lo que el test modifica;
 *   - deja el nombre original restaurado en el `finally` (teardown), y
 *   - lo restaura TAMBIÉN al arrancar (pre-limpieza defensiva), porque un run que
 *     abortara antes del teardown deja el ciclo renombrado y el paso 2 ya no
 *     encontraría la fila con su nombre original.
 */
const CODIGO_CICLO = 'ASIR';
const NOMBRE_ORIGINAL = 'Administración de Sistemas Informáticos en Red';
const NOMBRE_MODIFICADO = `${NOMBRE_ORIGINAL} (LOFP)`;
const FAMILIA_PROFESIONAL = 'Informática y Comunicaciones';
const GRADO = 'Ciclo formativo';
const NIVEL = 'Superior';

// Los campos `code`/`name` del modelo Ciclo todavía no están traducidos, así que la
// UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
// acepta ambos para que el test siga verde cuando se añada la traducción.
const CAMPO_CODIGO = /^(Code|Código)$/;
const CAMPO_NOMBRE = /^(Name|Nombre)$/;

/** Entradas del menú lateral «Sistema educativo» que recorre este test. */
const MENU_CICLOS = 'item:sistemaEducativo-ciclos-menuitem';
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
 * La fila del ciclo bajo prueba, localizada por su CÓDIGO y no por su nombre: el
 * nombre es lo que el test cambia, así que filtrar por él haría que la fila
 * "desapareciera" a mitad del test. El código es único entre los ciclos.
 */
function filaDelCiclo(page: Page) {
  return filasDeDatos(page).filter({
    has: page.getByRole('gridcell', { name: CODIGO_CICLO, exact: true }),
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

/** Abre el listado de ciclos desde el menú y espera a que esté pintado con sus filas. */
async function abrirListadoDeCiclos(page: Page): Promise<void> {
  await abrirMenu(page, MENU_CICLOS);
  await expect(page.getByRole('button', { name: BTN_NUEVO_CICLO })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Abre la ficha del ciclo desde el listado ya cargado (Axelor navega a
 * `.../Ciclo-action/edit/<id>`) y espera a que el formulario traiga sus datos.
 */
async function abrirFichaDelCiclo(page: Page): Promise<void> {
  await filaDelCiclo(page).click();
  await expect(page).toHaveURL(/Ciclo-action\/edit/);
  await expect(page.getByRole('textbox', { name: CAMPO_CODIGO })).toHaveValue(CODIGO_CICLO);
}

/** Escribe `nombre` en el campo "Nombre" de la ficha abierta y guarda. */
async function guardarConNombre(page: Page, nombre: string): Promise<void> {
  await page.getByRole('textbox', { name: CAMPO_NOMBRE }).fill(nombre);
  await page.getByRole('button', { name: 'Guardar', exact: true }).click();
  await expect(page).toHaveURL(/Ciclo-action\/list/);
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
 * Devuelve el ciclo a su nombre de los datos iniciales. Sirve de pre-limpieza
 * (restos de un run que abortara antes del teardown) y de teardown. Si el nombre ya
 * es el original no abre nada, para no dejar una ficha a medio editar.
 */
async function restaurarNombreOriginal(page: Page): Promise<void> {
  await abrirListadoDeCiclos(page);
  const fila = filaDelCiclo(page);
  await expect(fila).toHaveCount(1);
  const yaRestaurado = await fila
    .getByRole('gridcell', { name: NOMBRE_ORIGINAL, exact: true })
    .count();
  if (yaRestaurado === 1) {
    return;
  }
  await abrirFichaDelCiclo(page);
  await guardarConNombre(page, NOMBRE_ORIGINAL);
}

test.describe('Sistema educativo — Ciclos', () => {
  test('Editar un ciclo sin tocar el grado ni el nivel', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Pre-limpieza defensiva: si un run anterior abortó tras renombrar el ciclo y
      // antes de su teardown, devolverlo a su nombre de los datos iniciales.
      await restaurarNombreOriginal(page);

      // Paso 2: abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila
      // "Administración de Sistemas Informáticos en Red".
      await abrirListadoDeCiclos(page);
      await expect(
        filaDelCiclo(page).getByRole('gridcell', { name: NOMBRE_ORIGINAL, exact: true }),
      ).toBeVisible();
      await abrirFichaDelCiclo(page);

      // Paso 3: el formulario muestra el código "ASIR", la familia profesional
      // "Informática y Comunicaciones", el grado "Ciclo formativo" y el nivel
      // "Superior".
      await expect(page.getByRole('textbox', { name: CAMPO_CODIGO })).toHaveValue(CODIGO_CICLO);
      await expect(page.getByRole('textbox', { name: CAMPO_NOMBRE })).toHaveValue(NOMBRE_ORIGINAL);
      await expect(combo(page, 'familiaProfesional')).toHaveValue(FAMILIA_PROFESIONAL);
      await expect(combo(page, 'grado')).toHaveValue(GRADO);
      await expect(campo(page, 'nivel')).toBeVisible();
      await expect(combo(page, 'nivel')).toHaveValue(NIVEL);

      // Paso 4: cambia el nombre a "Administración de Sistemas Informáticos en Red
      // (LOFP)" y pulsa "Guardar". NO se toca ni el grado ni el nivel: es lo que da
      // sentido al "conserva" del resultado esperado.
      // Resultado esperado: el sistema guarda el ciclo y vuelve al listado (lo
      // comprueba `guardarConNombre` con la URL del listado).
      await guardarConNombre(page, NOMBRE_MODIFICADO);
      await expect(filasDeDatos(page).first()).toBeVisible();

      // Resultado esperado: la fila aparece con el nombre "Administración de Sistemas
      // Informáticos en Red (LOFP)" y conserva el grado "Ciclo formativo" y el nivel
      // "Superior". Las celdas se comprueban con
      // `exact: true` porque el nombre original es prefijo del modificado y una
      // comparación por subcadena no distinguiría uno de otro.
      const fila = filaDelCiclo(page);
      await expect(fila).toHaveCount(1);
      await expect(
        fila.getByRole('gridcell', { name: NOMBRE_MODIFICADO, exact: true }),
      ).toBeVisible();
      // Y ya no queda ninguna celda con el nombre anterior: es lo que distingue un
      // renombrado de verdad de una aserción que pasaría igual sin haber guardado.
      await expect(fila.getByRole('gridcell', { name: NOMBRE_ORIGINAL, exact: true })).toHaveCount(0);
      await expect(fila.getByRole('gridcell', { name: GRADO, exact: true })).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: NIVEL, exact: true })).toBeVisible();
    } finally {
      // Teardown: devolver el ciclo a su nombre de los datos iniciales aunque una
      // aserción haya fallado — el test edita datos maestros compartidos, así que
      // dejarlo renombrado envenenaría a los demás tests. El reload descarta
      // cualquier formulario a medio editar que si no bloquearía la navegación.
      await volverAlInicio(page);
      await restaurarNombreOriginal(page);

      await logout(page);
    }
  });
});
