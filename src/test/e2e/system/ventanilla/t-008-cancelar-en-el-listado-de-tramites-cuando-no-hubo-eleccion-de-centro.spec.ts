import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-008 — Cancelar en el listado de trámites cuando no hubo elección de centro
// origen: ESC-008  |  verifica: U-nuevo-expediente-002
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-008-cancelar-en-el-listado-de-tramites-cuando-no-hubo-eleccion-de-centro.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA, y además
 * lo es por construcción: lo que comprueba es precisamente que al cancelar NO se crea
 * nada. Abre el asistente de nuevo expediente, mira la pantalla de elección de trámite
 * (la primera que ve quien tiene un solo centro) y la cancela; no inserta, modifica ni
 * borra ninguna fila, así que no hay ningún nombre que aislar con un sufijo `Date.now()`
 * ni nada que borrar en el teardown.
 * Tampoco hace falta pre-limpieza defensiva: lo que comprueba depende solo de los datos
 * maestros del estado inicial (el único centro del alumno, los trámites activos en él y
 * sus permisos para iniciarlos), que ningún test modifica; un run que abortara no puede
 * dejar nada pegado que cambie estas aserciones. Lo único que un fallo puede dejar en
 * pantalla es la propia pestaña del asistente, y el `finally` la cierra para que un run
 * no la herede del anterior.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// alumno de UN SOLO centro, CIPFP Mislata. Es el usuario que hace falta para este
// escenario: justamente por tener un único centro el asistente no le pregunta en cuál
// crea el expediente y arranca ya en el listado de trámites, que es la pantalla que aquí
// se cancela.
const USUARIO = 'alumno1@mislata.es';
const CONTRASENA = 'demo1234';

// Único centro del alumno, el que el asistente da por elegido sin preguntar.
const CENTRO = 'CIPFP Mislata';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la del trámite
// como que al cancelar no queda ninguna. Se comparan con regex anclada porque Axelor
// añade un `*` al título de la pestaña cuando el formulario está sucio, y porque "Nuevo
// expediente" a secas es prefijo de las otras dos (sin anclar, cazaría pestañas que no
// son la suya).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Título que tendría la pestaña de un expediente ya creado: <nº>/<año>-<trámite> V1.
// Cancelar no debe abrir ninguna, sea del trámite que sea.
const TITULO_EXPEDIENTE = /^\d+\/\d{4}-.+ V1\*?$/;

/**
 * Filas de datos del árbol de trámites (excluye cabecera y filas de agrupación). El
 * `panel-related` agrupado con testid `field:tramitesDisponibles` fue sustituido por un
 * `<tree>` embebido en un `panel-dashlet` (commit `98755ea`): sus nodos no llevan
 * `data-testid` propio, así que se localizan por rol ARIA de un `treegrid` —
 * `aria-level="2"` son las filas hoja (los trámites).
 */
function filasDeTramites(page: Page) {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');
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
  test('Cancelar en el listado de trámites cuando no hubo elección de centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el alumno `alumno1@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Cuando abre el menú "Mis trámites" y pulsa "Nuevo trámite".
      await abrirNuevoExpediente(page);

      // Paso 3: Entonces se abre DIRECTAMENTE "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      await expect(page.getByTestId('panel:tramitesPanel')).toBeVisible();

      // …"directamente" quiere decir que no hubo elección de centro: esa pantalla del
      // asistente no llegó a abrirse (ni su pestaña ni su panel con el listado de
      // centros). Es la premisa del escenario, así que se comprueba, no se supone.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      // …con el centro "CIPFP Mislata" encima del listado. Primero el valor, que el
      // asistente fijó solo (el campo es de solo lectura: no lo eligió el usuario)…
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(campoCentro).toBeDisabled();
      // …y después el "encima", que es posición real en pantalla, no mera presencia: el
      // panel del centro tiene que quedar por encima del grid de trámites.
      const cajaCentro = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramites = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentro).not.toBeNull();
      expect(cajaTramites).not.toBeNull();
      expect(cajaCentro!.y).toBeLessThan(cajaTramites!.y);

      // …y un único botón debajo, "Cancelar". `toHaveCount(1)` verifica el "único": en
      // esta pantalla no hay "Atrás", porque al no haber habido elección de centro no
      // hay pantalla anterior a la que volver.
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      const cancelar = botonera.getByRole('button', { name: 'Cancelar' });
      await expect(cancelar).toBeVisible();
      await expect(botonera.getByRole('button', { name: 'Atrás' })).toHaveCount(0);
      // "Debajo": la botonera va bajo el listado de trámites.
      const cajaBotonera = await botonera.boundingBox();
      expect(cajaBotonera).not.toBeNull();
      expect(cajaTramites!.y).toBeLessThan(cajaBotonera!.y);

      // Paso 4: Cuando pulsa "Cancelar".
      await cancelar.click();

      // Resultado esperado: el asistente se cierra — la pantalla del listado de trámites
      // desaparece, ni su pestaña ni su contenido siguen ahí.
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:centroPanel')).toHaveCount(0);
      await expect(botonera).toHaveCount(0);
      await expect(filasDeTramites(page)).toHaveCount(0);

      // Resultado esperado: NO se abre "Nuevo expediente" (el contexto del trámite, la
      // pantalla siguiente del asistente). Se comprueba por su pestaña y también por su
      // contenido, para que el test siga cazando el fallo aunque se abriera sin pestaña
      // propia.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);

      // Resultado esperado: ni ningún expediente. No hay pestaña con el título de uno
      // (<nº>/<año>-<trámite> V1) ni se pinta su formulario (fase y estado son los campos
      // que todo expediente muestra).
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
