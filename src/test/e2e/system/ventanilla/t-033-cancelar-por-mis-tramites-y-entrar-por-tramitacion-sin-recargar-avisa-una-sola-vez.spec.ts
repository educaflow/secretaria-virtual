import { test, expect, Page, Request } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-033 — Cancelar por «Mis trámites» y entrar por «Tramitación» sin recargar avisa una sola vez
// origen: —  |  escrito a mano como regresión del cliente (sin fuente en .sdd)

/**
 * IDEMPOTENCIA — test de SOLO LECTURA: abre el asistente por las dos entradas y lo cierra
 * (con «Cancelar» o aceptando el aviso) sin crear nada, así que no hay nada que aislar ni
 * que borrar. El `finally` acepta el aviso si una aserción falló con él en pantalla.
 *
 * Qué prueba (regresión del cliente Axelor): al cerrar el asistente desde una acción
 * («Cancelar») la URL seguía apuntando a la entrada de menú por la que se abrió, y el
 * siguiente clic en un menú reabría ESA entrada a la vez que la nueva. Con el director, que
 * no puede registrar en papel en ningún centro, el aviso «No puede crear expedientes en
 * ninguno de sus centros» de «Tramitación» reaparecía en bucle al aceptarlo y la pestaña
 * «elija el centro» no se cerraba. Por eso el test NO recarga entre las dos entradas,
 * comprueba que tras aceptar el aviso no vuelve a salir y que en la red solo hubo UN
 * `onNew` de «elija el centro», con `_presentadoEnPapel: true`.
 *
 * Por qué el director: ve el grupo «Tramitación» pero no puede iniciar ningún trámite
 * registrándolo en papel, y sí presentándolo él mismo (como profesor) por «Mis trámites».
 * Es el mismo usuario que T-029, que prueba las listas por entrada.
 */

const USUARIO = 'director@mislata.es';
const CONTRASENA = 'demo1234';

const CENTRO = 'CIPFP Mislata';

const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

const AVISO = 'No puede crear expedientes en ninguno de sus centros';

// Tiempo durante el que se vigila que el aviso no reaparece tras aceptarlo: en el fallo
// reaparecía en cuanto llegaba la respuesta del `onNew` que competía, en menos de un segundo.
const VIGILANCIA_MS = 3000;

// Acción `onNew` de «elija el centro»: la comparten las dos entradas, que solo se
// diferencian por el `_presentadoEnPapel` de su contexto.
const ACCION_ON_NEW_ELEGIR_CENTRO = 'sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-onNew-action';

// Acción de la entrada «Mis trámites» → «Nuevo trámite» (la que el fallo dejaba en la URL).
const ACCION_ENTRADA_MIS_TRAMITES = 'AsistenteNuevoExpedientePresentadoElectronicamente-action';

/**
 * Si `request` es el `onNew` de «elija el centro», devuelve el `_presentadoEnPapel` de su
 * contexto (puede ser `undefined` si no viaja); si es otra petición, `null`.
 */
function presentadoEnPapelDelOnNew(request: Request): boolean | undefined | null {
  if (request.method() !== 'POST' || !request.url().endsWith('/ws/action')) {
    return null;
  }
  let cuerpo: { action?: string; data?: { context?: Record<string, unknown> } } | null;
  try {
    cuerpo = request.postDataJSON();
  } catch {
    return null;
  }
  // Solo el ARRANQUE del `onNew`: Axelor manda su acción como primer elemento de una lista
  // separada por comas. Cuando el grupo se interrumpe para mostrar un aviso, el cliente
  // reanuda el resto con otra petición `<acción>[<índice>]`, que es la misma ejecución y no
  // otro `onNew`.
  if (cuerpo?.action?.split(',')[0] !== ACCION_ON_NEW_ELEGIR_CENTRO) {
    return null;
  }
  return cuerpo.data?.context?._presentadoEnPapel as boolean | undefined;
}

/** Abre «<grupo>» → «Nuevo trámite»; el grupo solo se despliega si la entrada no se ve. */
async function abrirNuevoTramite(page: Page, grupo: 'misTramites' | 'tramitacion'): Promise<void> {
  const entrada = page.getByTestId(`item:${grupo}-nuevoTramite-menuitem`);
  if (!(await entrada.isVisible())) {
    await page.getByTestId(`item:${grupo}-menuitem`).getByTestId('title').first().click();
  }
  await entrada.click();
}

/** Comprueba que no queda abierta ninguna pantalla del asistente, ni por su pestaña ni por su contenido. */
async function comprobarAsistenteCerrado(page: Page): Promise<void> {
  await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
  await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
  await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);
  await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
  await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
  await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Cancelar por «Mis trámites» y entrar por «Tramitación» sin recargar avisa una sola vez', async ({
    page,
  }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el director `director@mislata.es` ha iniciado sesión.
    await login(page, USUARIO, CONTRASENA);

    // `_presentadoEnPapel` de cada `onNew` de «elija el centro» enviado a partir del paso 4.
    let capturando = false;
    const onNewsTrasTramitacion: (boolean | undefined)[] = [];
    page.on('request', (request) => {
      const presentadoEnPapel = presentadoEnPapelDelOnNew(request);
      if (capturando && presentadoEnPapel !== null) {
        onNewsTrasTramitacion.push(presentadoEnPapel);
      }
    });

    try {
      // Paso 2: Cuando abre "Mis trámites" → "Nuevo trámite", se abre "elija el trámite"
      // con "CIPFP Mislata".
      await abrirNuevoTramite(page, 'misTramites');
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
      await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(CENTRO);

      // Paso 3: Cuando pulsa "Cancelar", el asistente se cierra.
      await page.getByTestId('panel:buttons-panel').getByRole('button', { name: 'Cancelar' }).click();
      await comprobarAsistenteCerrado(page);
      // La causa del fallo: la URL se quedaba en la acción de la entrada "Mis trámites".
      await expect(page).not.toHaveURL(new RegExp(ACCION_ENTRADA_MIS_TRAMITES));

      // Paso 4: Y, SIN recargar la aplicación, abre "Tramitación" → "Nuevo trámite".
      capturando = true;
      await abrirNuevoTramite(page, 'tramitacion');

      // Resultado esperado: el aviso "No puede crear expedientes en ninguno de sus centros".
      const aviso = page.getByRole('dialog');
      await expect(aviso).toHaveCount(1);
      await expect(aviso).toContainText(AVISO);

      // Paso 5: Cuando lo acepta.
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(aviso).toHaveCount(0);

      // Resultado esperado: el aviso NO vuelve a aparecer (se vigila un rato, porque en el
      // fallo reaparecía al llegar la respuesta del `onNew` que competía)…
      const reaparecio = await aviso
        .waitFor({ state: 'visible', timeout: VIGILANCIA_MS })
        .then(() => true)
        .catch(() => false);
      expect(reaparecio, 'el aviso no debe reaparecer tras aceptarlo').toBe(false);

      // …no queda abierta ninguna pantalla del asistente…
      await comprobarAsistenteCerrado(page);

      // …y en la red solo hubo un `onNew` de "elija el centro", el de "Tramitación".
      expect(onNewsTrasTramitacion).toEqual([true]);
    } finally {
      capturando = false;
      // Si una aserción falló con el aviso en pantalla, aceptarlo; en el camino verde ya no
      // existe y el `.catch` es intencional.
      await page
        .getByRole('dialog')
        .getByRole('button', { name: 'Aceptar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
