import { test, expect, Page, Route } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-015 — Petición manipulada con una forma de presentar que no tiene
// origen: ESC-015  |  verifica: V-AsistenteNuevoExpediente-005, U-nuevo-expediente-015
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-015-peticion-manipulada-con-una-forma-de-presentar-que-no-tiene.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA: manipula
 * la petición de red para que "Crear expediente" pida presentarlo en papel, una forma de
 * presentar que el alumno no tiene, y comprueba que el servidor la rechaza SIN crear
 * ningún expediente. No hay ningún registro que crear (esa es justo la aserción: que no
 * se cree ninguno), así que no hace falta ni sufijo `Date.now()` ni borrado en el
 * `finally`. Tampoco hace falta pre-limpieza defensiva: un run anterior que abortara no
 * puede haber dejado un expediente a medio crear, porque el servidor rechaza la petición
 * manipulada antes de persistir nada.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md).
const USUARIO = 'alumno1@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados. El alumno solo puede
// presentar este trámite él mismo (no tiene permiso para registrarlo en papel), así que
// la pantalla no le pregunta "¿Cómo se presenta?" y fija `presentadoEnPapel = false`.
const CENTRO_PROPIO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';

// Fragmento de la acción que dispara "Crear expediente"
// (`sysVentanilla.ElegirFormaPresentacion@AsistenteNuevoExpediente-btnCrear-action`, a
// veces con un sufijo `[2]` cuando Axelor encadena una segunda invocación del mismo
// botón): basta con esta subcadena para identificar la petición a manipular sin
// depender de si lleva sufijo.
const ACCION_CREAR = 'AsistenteNuevoExpediente-btnCrear-action';

// Título y mensaje del diálogo de error que debe mostrar el servidor al rechazar la
// petición manipulada.
const TITULO_ERROR = 'No es posible crear el expediente';
const MENSAJE_FORMA_PRESENTACION_AJENA =
  'No puede presentar el expediente de esa forma en el centro indicado';

/**
 * Abre el asistente desde el menú «Ventanilla» → «Nuevo expediente». El grupo se pliega
 * y despliega al pulsarlo, así que solo se despliega si la entrada no se ve: pulsarlo a
 * ciegas lo cerraría cuando ya venía abierto.
 */
async function abrirNuevoExpediente(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:ventanilla-nuevoExpediente-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:ventanilla-menuitem').getByTestId('title').first().click();
  }
  await entrada.click();
}

/** Filas de datos del árbol de trámites (excluye cabecera y filas de agrupación). */
function filasDeTramites(page: Page) {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');
}

/** Filas de agrupación por tipo de trámite del árbol de trámites. */
function gruposDeTipoTramite(page: Page) {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="1"]');
}

/** Despliega todos los grupos de tipo de trámite del árbol (nace con los grupos plegados). */
async function desplegarGruposDeTramites(page: Page): Promise<void> {
  const grupos = gruposDeTipoTramite(page);
  await grupos.first().waitFor();
  const total = await grupos.count();
  for (let i = 0; i < total; i++) {
    const grupo = grupos.nth(i);
    if ((await grupo.getAttribute('aria-expanded')) !== 'true') {
      await grupo.click();
    }
  }
}

/**
 * Intercepta la petición que dispara "Crear expediente" (`POST /ws/action`) y sustituye,
 * en el `context` que viaja en el cuerpo, `presentadoEnPapel` (que la pantalla fijó a
 * `false`, porque el alumno solo puede presentar este trámite él mismo) por `true` —
 * dejando el resto de la petición tal cual la construyó el cliente (centro, trámite y
 * para él mismo). Es la manipulación que describe el paso 3 del `.desc.md`: "la petición
 * manipulada para indicar que está registrando un trámite recibido en papel […] dejando
 * el resto como lo fijó la pantalla". Las demás peticiones a `/ws/action` (login, menús,
 * otras acciones del asistente) siguen intactas.
 */
async function manipularFormaDePresentarEnPeticionDeCrear(page: Page): Promise<void> {
  await page.route('**/ws/action', async (route: Route) => {
    const request = route.request();
    const body = request.postDataJSON() as
      | { data?: { context?: Record<string, unknown> } }
      | undefined;
    const context = body?.data?.context;
    if (
      context &&
      typeof context['presentadoEnPapel'] === 'boolean' &&
      String(body?.['action'] ?? '').includes(ACCION_CREAR)
    ) {
      context['presentadoEnPapel'] = true;
      await route.continue({ postData: JSON.stringify(body) });
    } else {
      await route.continue();
    }
  });
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Petición manipulada con una forma de presentar que no tiene', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el alumno `alumno1@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Y ha llegado a "Nuevo expediente" para el trámite "Anulación de
      // matrícula en ciclo formativo" en el centro "CIPFP Mislata" (menú "Ventanilla" →
      // "Nuevo expediente" → fila del trámite), sin que se le pregunte "¿Cómo se
      // presenta?" (el alumno solo puede presentarlo él mismo).
      await abrirNuevoExpediente(page);
      await desplegarGruposDeTramites(page);
      await filasDeTramites(page).first().click();

      const pantallaContexto = page.getByRole('tab', { name: 'Nuevo expediente', exact: true });
      await expect(pantallaContexto).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO_PROPIO);
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);

      // Paso 3: Cuando pulsa "Crear expediente" con la petición manipulada para indicar
      // que está registrando un trámite recibido en papel, dejando el resto como lo fijó
      // la pantalla: el centro "CIPFP Mislata", el trámite "Anulación de matrícula en
      // ciclo formativo" y el expediente para él mismo.
      await manipularFormaDePresentarEnPeticionDeCrear(page);
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: bajo el título "No es posible crear el expediente" muestra
      // el mensaje "No puede presentar el expediente de esa forma en el centro
      // indicado".
      const aviso = page.getByRole('dialog');
      await expect(aviso).toBeVisible();
      await expect(aviso).toContainText(TITULO_ERROR);
      await expect(aviso).toContainText(MENSAJE_FORMA_PRESENTACION_AJENA);

      // El aviso es modal: la comprobación de que la pantalla "Nuevo expediente" sigue
      // abierta (y que no se ha creado ningún expediente) solo es observable tras
      // aceptarlo, que es además la única salida que el diálogo ofrece al usuario.
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(aviso).toHaveCount(0);

      // Resultado esperado: el sistema no crea ningún expediente. La pantalla "Nuevo
      // expediente" sigue abierta con el trámite "Anulación de matrícula en ciclo
      // formativo" y el centro "CIPFP Mislata", y no se abre ningún expediente (su
      // pestaña llevaría el patrón "<nº>/<año>-<trámite>", nunca el título fijo "Nuevo
      // expediente").
      await expect(pantallaContexto).toBeVisible();
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoCentro).toHaveValue(CENTRO_PROPIO);
      await expect(page.getByRole('tab', { name: new RegExp(`^\\d+/\\d{4}-${TRAMITE}`) })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();
    } finally {
      // Teardown: si una aserción falló con el aviso todavía en pantalla, aceptarlo para
      // no dejar el modal abierto al siguiente run. Si ya se aceptó (camino verde) el
      // botón no existe: el `.catch` es intencional, no un error tapado.
      await page
        .getByRole('dialog')
        .getByRole('button', { name: 'Aceptar' })
        .click({ timeout: 5000 })
        .catch(() => {});
      await page.unroute('**/ws/action');

      await logout(page);
    }
  });
});
