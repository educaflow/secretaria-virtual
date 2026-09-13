import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-012 — El listado de ciclos muestra el grado y el nivel
// origen: ESC-009  |  verifica: —
// fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-012-el-listado-de-ciclos-muestra-el-grado-y-el-nivel.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA: no
 * crea, no edita y no borra nada, así que no necesita sufijo `Date.now()`, ni
 * teardown, ni pre-limpieza (no hay nada que limpiar). Lo que sí necesita es no
 * depender del estado que otros tests dejan en la BD compartida, y por eso:
 *   - identifica cada fila por su NOMBRE EXACTO (regex anclado `^…$`), de modo que
 *     un ciclo residual de otro test cuyo nombre empiece por el mismo texto (los
 *     tests hermanos crean ciclos con sufijo `Date.now()`) no cuela una fila de más;
 *   - comprueba solo las DOS filas del seed que exige el resultado esperado, sin
 *     fijar el número total de filas del grid, que sí varía con lo que haya en la BD;
 *   - usa ciclos poco contendidos: los hermanos que editan datos del seed tocan
 *     «Administración de Sistemas Informáticos en Red» (ASIR), no estos dos.
 */

/** Entrada del menú lateral «Sistema educativo» que recorre este test. */
const MENU_CICLOS = 'item:sistemaEducativo-ciclos-menuitem';
const BTN_NUEVO_CICLO = 'Añadir un nuevo ciclo';

/**
 * Las columnas del listado, EN EL ORDEN en que el resultado esperado exige verlas.
 * Cada una se comprueba por dos vías complementarias: el `data-testid`
 * `column:<campo>` que Axelor deriva del modelo (identifica la columna sin depender
 * del idioma) y el título que la cabecera muestra al usuario.
 *
 * Los campos `code`/`name` del modelo Ciclo todavía no están traducidos, así que la
 * UI los rotula "Code"/"Name" donde la descripción dice "Código"/"Nombre". El regex
 * acepta ambos para que el test siga verde cuando se añada la traducción.
 */
const COLUMNAS: ReadonlyArray<{ campo: string; titulo: string | RegExp }> = [
  { campo: 'code', titulo: /^(Code|Código)$/ },
  { campo: 'name', titulo: /^(Name|Nombre)$/ },
  { campo: 'familiaProfesional', titulo: 'Familia profesional' },
  { campo: 'grado', titulo: 'Grado' },
  { campo: 'nivel', titulo: 'Nivel' },
];

const GRADO_CICLO_FORMATIVO = 'Ciclo formativo';
const NIVEL_SUPERIOR = 'Ciclos Formativos de Grado Superior';
const NIVEL_MEDIO = 'Ciclos Formativos de Grado Medio';

const CICLO_DAW = 'Desarrollo de Aplicaciones Web';
const CICLO_SMR = 'Sistemas Microinformáticos y Redes';

/**
 * Filas de datos del grid (excluye la cabecera y la fila de búsqueda). El grid las
 * pinta por AJAX, así que hay que esperarlas antes de CONTAR nada: `count()` no
 * reintenta y devolvería 0 sobre un grid aún vacío.
 */
function filasDeDatos(page: Page) {
  return page.locator('[data-testid^="row:"]');
}

/**
 * Las cabeceras de columna del grid, en el orden en que se pintan. Se localizan por
 * su rol ARIA (`columnheader`), no por clases CSS generadas por el bundler.
 */
function cabeceras(page: Page) {
  return page.locator('[role="columnheader"]');
}

/**
 * La fila del ciclo cuyo nombre es EXACTAMENTE `nombre`. El regex va anclado a
 * propósito: sin anclar, un ciclo residual de otro run («Desarrollo de Aplicaciones
 * Web t001-1757…») casaría también y la fila dejaría de ser única.
 */
function filaDelCiclo(page: Page, nombre: string) {
  return filasDeDatos(page).filter({
    has: page.locator('[data-testid="column:name"]', { hasText: new RegExp(`^${nombre}$`) }),
  });
}

/** El texto de una celda de la fila, por el nombre del campo en el modelo. */
function celda(fila: ReturnType<typeof filaDelCiclo>, campo: string) {
  return fila.locator(`[data-testid="column:${campo}"]`);
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
  await expect(page).toHaveURL(/Ciclo-action\/list/);
  await expect(page.getByRole('button', { name: BTN_NUEVO_CICLO })).toBeVisible();
  await expect(filasDeDatos(page).first()).toBeVisible();
}

/**
 * Comprueba que la fila del ciclo `nombre` existe una sola vez y muestra ese grado y
 * ese nivel. `toHaveText` compara el texto COMPLETO de la celda, así que distingue
 * «Ciclos Formativos de Grado Medio» de «…Grado Superior» (uno no es subcadena del
 * otro, pero una comparación por subcadena tampoco garantizaría que no sobra texto).
 */
async function esperarFilaConGradoYNivel(
  page: Page,
  nombre: string,
  grado: string,
  nivel: string,
): Promise<void> {
  const fila = filaDelCiclo(page, nombre);
  await expect(fila).toHaveCount(1);
  await expect(celda(fila, 'grado')).toHaveText(grado);
  await expect(celda(fila, 'nivel')).toHaveText(nivel);
}

test.describe('Sistema educativo — Ciclos', () => {
  test('El listado de ciclos muestra el grado y el nivel', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 2: abre el menú "Sistema educativo" → "Ciclos".
      await abrirListadoDeCiclos(page);

      // Resultado esperado: el listado muestra las columnas en el orden "Código",
      // "Nombre", "Familia profesional", "Grado" y "Nivel". Se exige el número exacto
      // de cabeceras (no sobra ninguna columna) y, para cada posición, qué campo del
      // modelo ocupa y con qué título se rotula. El título vive en el atributo
      // `title` del span de la cabecera y no en su texto porque la columna ordenada
      // añade ahí el icono de orden ("sort"), que no forma parte del título.
      const columnas = cabeceras(page);
      await expect(columnas).toHaveCount(COLUMNAS.length);
      for (const [posicion, columna] of COLUMNAS.entries()) {
        const cabecera = columnas.nth(posicion);
        await expect(cabecera).toHaveAttribute('data-testid', `column:${columna.campo}`);
        await expect(cabecera.locator('span[title]').first()).toHaveAttribute(
          'title',
          columna.titulo,
        );
      }

      // Resultado esperado: la fila "Desarrollo de Aplicaciones Web" muestra el grado
      // "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".
      await esperarFilaConGradoYNivel(page, CICLO_DAW, GRADO_CICLO_FORMATIVO, NIVEL_SUPERIOR);

      // Resultado esperado: la fila "Sistemas Microinformáticos y Redes" muestra el
      // grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Medio".
      await esperarFilaConGradoYNivel(page, CICLO_SMR, GRADO_CICLO_FORMATIVO, NIVEL_MEDIO);
    } finally {
      // No hay teardown de datos: el test es de solo lectura y no ha creado ni
      // modificado nada. Lo único que cierra es la sesión, y va en `finally` para que
      // un fallo de aserción no deje la sesión abierta al siguiente test.
      await logout(page);
    }
  });
});
