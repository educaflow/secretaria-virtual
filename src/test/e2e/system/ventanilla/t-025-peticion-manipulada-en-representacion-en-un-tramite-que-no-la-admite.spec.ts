import { test, expect, Page, Route } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-025 — Petición manipulada en representación en un trámite que no la admite
// origen: ESC-024  |  verifica: V-AsistenteNuevoExpediente-006, U-nuevo-expediente-015
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-025-peticion-manipulada-en-representacion-en-un-tramite-que-no-la-admite.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA: manipula
 * la petición de red para que "Crear expediente" pida el expediente en representación de
 * otra persona en un trámite que no lo admite, y comprueba que el servidor la rechaza SIN
 * crear ningún expediente. No hay ningún registro que crear (esa es justo la aserción:
 * que no se cree ninguno), así que no hace falta ni sufijo `Date.now()` ni borrado en el
 * `finally`. Tampoco hace falta pre-limpieza defensiva: un run anterior que abortara no
 * puede haber dejado un expediente a medio crear, porque el servidor rechaza la petición
 * manipulada antes de persistir nada.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// el jefe de estudios, que sobre los trámites del profesor SÍ tiene permiso de
// registrarlos en papel (a diferencia del alumno de T-017/T-015), así que puede entrar por
// «Tramitación» → «Nuevo trámite», la entrada que fija que el expediente se registra en
// papel. Es la vía por la que este test llega a una petición de alta EN PAPEL antes de
// manipularla.
const USUARIO = 'jefeestudios1@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados. "Justificación de falta
// del profesorado" NO admite presentarse en representación de otra persona.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Justificación de falta del profesorado';

// Rótulo de la pregunta eliminada (ya no existe: la forma la fija la entrada de menú) y de
// la única que puede hacer el último paso del asistente (`presentadoEnRepresentacion`).
const PREGUNTA_COMO_SE_PRESENTA = '¿Cómo se presenta?';
const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';

// Fragmento de la acción que dispara "Crear expediente"
// (`sysVentanilla.ElegirFormaPresentacion@AsistenteNuevoExpediente-btnCrear-action`, a
// veces con un sufijo `[2]` cuando Axelor encadena una segunda invocación del mismo
// botón): basta con esta subcadena para identificar la petición a manipular sin depender
// de si lleva sufijo.
const ACCION_CREAR = 'AsistenteNuevoExpediente-btnCrear-action';

// Título y mensaje del diálogo de error que debe mostrar el servidor al rechazar la
// petición manipulada (`TramitadorService.validateRepresentacion`).
const TITULO_ERROR = 'No es posible crear el expediente';
const MENSAJE_REPRESENTACION_NO_ADMITIDA =
  'Este trámite no permite presentar la solicitud en representación de otra persona';

/**
 * Abre el asistente desde el menú «Tramitación» → «Nuevo trámite» (registrar en papel). El grupo se pliega
 * y despliega al pulsarlo, así que solo se despliega si la entrada no se ve: pulsarlo a
 * ciegas lo cerraría cuando ya venía abierto.
 */
async function abrirNuevoExpediente(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:tramitacion-nuevoTramite-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:tramitacion-menuitem').getByTestId('title').first().click();
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
 * en el `context` que viaja en el cuerpo, `presentadoEnRepresentacion` (que la pantalla no
 * llegó a preguntar, porque este trámite no admite representación, y que por tanto viaja
 * con su valor por defecto `false`) por `true` — es decir, "en representación de otra
 * persona" — dejando el resto de la petición tal cual la fijó la pantalla (centro,
 * trámite y presentado en papel). Es la manipulación que describe el paso 4 del
 * `.desc.md`. Las demás peticiones a `/ws/action` (login, menús, otras acciones del
 * asistente) siguen intactas. Devuelve lo que la pantalla puso en `presentadoEnPapel` en
 * la petición manipulada, para comprobar que de verdad era un alta en papel.
 */
async function manipularParaQuienEsEnPeticionDeCrear(page: Page): Promise<{ presentadoEnPapel?: unknown }> {
  const enviado: { presentadoEnPapel?: unknown } = {};
  await page.route('**/ws/action', async (route: Route) => {
    const request = route.request();
    const body = request.postDataJSON() as
      | { data?: { context?: Record<string, unknown> }; action?: string }
      | undefined;
    const context = body?.data?.context;
    if (
      context &&
      typeof context['presentadoEnRepresentacion'] === 'boolean' &&
      String(body?.['action'] ?? '').includes(ACCION_CREAR)
    ) {
      enviado.presentadoEnPapel = context['presentadoEnPapel'];
      context['presentadoEnRepresentacion'] = true;
      await route.continue({ postData: JSON.stringify(body) });
    } else {
      await route.continue();
    }
  });
  return enviado;
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Petición manipulada en representación en un trámite que no la admite', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado sesión
    // con la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Y ha llegado a "Nuevo expediente" para el trámite "Justificación de falta
      // del profesorado" en el centro "CIPFP Mislata" entrando por "Tramitación" → "Nuevo
      // trámite" (registrar en papel; directamente a "elija el trámite", porque el jefe de
      // estudios solo tiene un centro → fila del trámite).
      await abrirNuevoExpediente(page);
      await desplegarGruposDeTramites(page);
      await filasDeTramites(page).filter({ hasText: TRAMITE }).click();

      // Nombre por regex, no exacto: Axelor añade un `*` al título de la pestaña en
      // cuanto el form tiene cambios sin guardar, y la pantalla sigue siendo la misma
      // antes y después de ese marcador.
      const pantallaContexto = page.getByRole('tab', { name: /^Nuevo expediente\*?$/ });
      await expect(pantallaContexto).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);

      // Paso 3: Entonces no se ve "¿Cómo se presenta?" (la forma, en papel, la fija la
      // entrada de menú) ni "¿Para quién es el expediente?" (el trámite no admite
      // representación).
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toHaveCount(0);
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 4: Cuando pulsa "Crear expediente" con la petición manipulada para indicar
      // que el expediente es en representación de otra persona, dejando el resto como
      // está en la pantalla: el centro "CIPFP Mislata", el trámite "Justificación de
      // falta del profesorado" y registrado como recibido en papel.
      const enviado = await manipularParaQuienEsEnPeticionDeCrear(page);
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: bajo el título "No es posible crear el expediente" muestra el
      // mensaje "Este trámite no permite presentar la solicitud en representación de otra
      // persona".
      const aviso = page.getByRole('dialog');
      await expect(aviso).toBeVisible();
      await expect(aviso).toContainText(TITULO_ERROR);
      await expect(aviso).toContainText(MENSAJE_REPRESENTACION_NO_ADMITIDA);

      // El aviso es modal: la comprobación de que la pantalla "Nuevo expediente" sigue
      // abierta (y que no se ha creado ningún expediente) solo es observable tras
      // aceptarlo, que es además la única salida que el diálogo ofrece al usuario.
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(aviso).toHaveCount(0);

      // La petición manipulada era un alta EN PAPEL: es lo que la entrada «Tramitación»
      // fijó y lo que el test no tocó.
      expect(enviado.presentadoEnPapel).toBe(true);

      // Resultado esperado: el sistema no crea ningún expediente. La pantalla "Nuevo
      // expediente" sigue abierta con el trámite "Justificación de falta del
      // profesorado" y el centro "CIPFP Mislata"; no se abre ningún expediente (su pestaña
      // llevaría el patrón "<nº>/<año>-<trámite>", nunca el título fijo "Nuevo
      // expediente").
      await expect(pantallaContexto).toBeVisible();
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoCentro).toHaveValue(CENTRO);
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
