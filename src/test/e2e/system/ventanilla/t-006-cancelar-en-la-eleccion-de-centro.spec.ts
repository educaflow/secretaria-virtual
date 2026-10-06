import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-006 — Cancelar en la elección de centro
// origen: ESC-006  |  verifica: U-nuevo-expediente-001
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-006-cancelar-en-la-eleccion-de-centro.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA, y además
 * lo es por construcción: lo que comprueba es precisamente que al cancelar NO se crea
 * nada. Abre el asistente de nuevo expediente, mira la pantalla de elección de centro y
 * la cancela; no inserta, modifica ni borra ninguna fila, así que no hay ningún nombre
 * que aislar con un sufijo `Date.now()` ni nada que borrar en el teardown.
 * Tampoco hace falta pre-limpieza defensiva: el listado de centros que comprueba depende
 * solo de los datos maestros del estado inicial (los centros del alumno y sus permisos
 * para iniciar trámites), que ningún test modifica; un run que abortara no puede dejar
 * nada pegado que cambie estas aserciones. Lo único que un fallo puede dejar en pantalla
 * es la propia pestaña del asistente, y el `finally` la cierra para que un run no la
 * herede del anterior.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// alumno en DOS centros, CIPFP Mislata y CIPFP Batoi. Es el usuario que hace falta para
// este escenario: solo a quien tiene más de un centro le pregunta el asistente en cuál
// crea el expediente, que es la pantalla que aquí se cancela.
const USUARIO = 'alumnodoscentros@mislata.es';
const CONTRASENA = 'demo1234';

// Los DOS centros del alumno, EN EL ORDEN EN QUE LOS PINTA EL GRID (alfabético): el
// orden del array es parte de la aserción, porque `toHaveText` compara posición a
// posición.
const CENTROS_DEL_ALUMNO = ['CIPFP Batoi', 'CIPFP Mislata'];

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la primera como
// que al cancelar no queda ninguna. Se comparan con regex anclada porque Axelor añade un
// `*` al título de la pestaña cuando el formulario está sucio, y porque "Nuevo
// expediente" a secas es prefijo de las otras dos (sin anclar, cazaría pestañas que no
// son la suya).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Título que tendría la pestaña de un expediente ya creado: <nº>/<año>-<trámite> V1.
// Cancelar no debe abrir ninguna, sea del trámite que sea.
const TITULO_EXPEDIENTE = /^\d+\/\d{4}-.+ V1\*?$/;

/**
 * Filas de datos del grid de centros (excluye la cabecera). Ya no es un
 * `panel-related field="centrosDisponibles"` (testid `field:centrosDisponibles`), sino
 * un grid independiente embebido en un `panel-dashlet` (testid `panel:centrosPanel`),
 * con los testids estándar de un grid embebido: sus filas siguen llevando `row:`.
 */
function filasDeCentros(page: Page) {
  return page.getByTestId('panel:centrosPanel').locator('[data-testid^="row:"]');
}

/**
 * Abre el asistente desde el menú «Mis trámites» → «Nuevo trámite». El grupo se pliega
 * y despliega al pulsarlo, así que solo se despliega si la entrada no se ve: pulsarlo a
 * ciegas lo cerraría cuando ya venía abierto.
 */
async function abrirNuevoExpediente(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:misTramites-nuevoTramite-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:misTramites-menuitem').getByTestId('title').first().click();
  }
  await entrada.click();
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Cancelar en la elección de centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el alumno `alumnodoscentros@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Cuando abre el menú "Mis trámites" y pulsa "Nuevo trámite".
      await abrirNuevoExpediente(page);

      // Paso 3: Entonces se abre "Nuevo expediente: elija el centro"…
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toBeVisible();
      await expect(page.getByTestId('panel:centrosPanel')).toBeVisible();

      // …con las filas "CIPFP Batoi" y "CIPFP Mislata". `toHaveText` con un array compara
      // posición a posición, así que verifica a la vez el contenido, el número de filas
      // (dos, ni una más) y el orden.
      await expect(filasDeCentros(page)).toHaveText(CENTROS_DEL_ALUMNO);

      // …y un botón "Cancelar" (uno solo: en esta pantalla no hay "Atrás", porque es la
      // primera del asistente y no hay adónde volver).
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      const cancelar = botonera.getByRole('button', { name: 'Cancelar' });
      await expect(cancelar).toBeVisible();

      // Paso 4: Cuando pulsa "Cancelar".
      await cancelar.click();

      // Resultado esperado: el asistente se cierra — la pantalla de elección de centro
      // desaparece, ni su pestaña ni su contenido siguen ahí.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(botonera).toHaveCount(0);

      // Resultado esperado: NO se abre "Nuevo expediente: elija el trámite". Se comprueba
      // por su pestaña y también por su contenido (el grid de trámites), para que el test
      // siga cazando el fallo aunque la pantalla se abriera sin pestaña propia.
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);

      // Resultado esperado: ni ningún expediente. Cancelar tampoco puede haber avanzado a
      // la tercera pantalla del asistente ni haber abierto un expediente creado: no hay
      // pestaña con el título de un expediente (<nº>/<año>-<trámite> V1) ni se pinta el
      // formulario de uno (fase y estado son los campos que todo expediente muestra).
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toHaveCount(0);
      await expect(page.getByTestId('field:namePhase')).toHaveCount(0);
      await expect(page.getByTestId('field:nameState')).toHaveCount(0);

      // Y, en conjunto: cancelar no deja NINGUNA pestaña abierta. La sesión solo abrió el
      // asistente, así que cero pestañas es la forma más directa de comprobar que no
      // queda abierta ninguna pantalla del asistente ni ningún expediente.
      await expect(page.getByRole('tab')).toHaveCount(0);
    } finally {
      // Teardown: si una aserción falló con el asistente todavía abierto, cerrarlo con su
      // propio botón "Cancelar" para no heredar la pestaña al siguiente run. En el camino
      // verde el botón ya no existe: el `.catch` es intencional, no un error tapado.
      await page
        .getByTestId('panel:buttons-panel')
        .getByRole('button', { name: 'Cancelar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
